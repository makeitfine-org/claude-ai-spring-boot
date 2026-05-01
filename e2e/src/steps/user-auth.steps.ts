import { Given, When, Then } from '@cucumber/cucumber'
import { expect } from '@playwright/test'
import * as path from 'path'
import * as fs from 'fs'
import * as os from 'os'
import { CustomWorld } from '../world'
import {
  clearMailHog,
  waitForEmailTo,
  extractVerificationLink,
  rewriteInternalUrl,
} from '../support/mailhog-client'
import { createTestJwt } from '../support/jwt-helper'

const FRONTEND_URL = process.env['FRONTEND_URL'] || 'http://localhost:3000'
const API_BASE_URL = process.env['API_BASE_URL'] || 'http://localhost:8080'
const JWT_SECRET =
  process.env['JWT_SECRET'] ||
  'dGhpcy1pcy1hLXZlcnktbG9uZy1zZWNyZXQta2V5LWZvci1qd3QtdG9rZW4tZ2VuZXJhdGlvbg=='

// ── Helpers ──────────────────────────────────────────────────────────────────

/** Counter incremented per scenario to produce unique X-Forwarded-For IPs */
let scenarioCounter = 0

function generateTestUser() {
  const ts = Date.now().toString().slice(-9)
  return {
    username: `e2eu${ts}`.slice(0, 15),
    email: `e2e-user-${ts}@example.com`,
    password: 'Test1234!',
    displayName: `E2E User ${ts}`,
  }
}

/**
 * Registers a test user via the API and stores the resulting sub+JWT in world.
 * The route interception on `this.page` is configured so that every request
 * to the API backend carries the test JWT as a Bearer token.
 *
 * Each call uses a unique X-Forwarded-For header so that the in-memory rate
 * limiter (5 req / 15 min per IP) does not reject consecutive test scenarios.
 */
async function setupRegisteredUser(world: CustomWorld): Promise<void> {
  const user = generateTestUser()
  world.registeredUserEmail = user.email
  world.registeredUserPassword = user.password
  world.registeredUserDisplayName = user.displayName

  // Unique fake source IP per scenario — avoids registration rate-limit (5/15 min/IP)
  const fakeIp = `10.0.1.${(++scenarioCounter % 254) + 1}`

  const response = await world.apiClient.post<{ sub: string }>(
    '/api/register',
    {
      username: user.username,
      email: user.email,
      password: user.password,
      displayName: user.displayName,
    },
    { headers: { 'X-Forwarded-For': fakeIp } },
  )

  const sub = response.data.sub
  world.registeredUserSub = sub

  const jwt = createTestJwt(sub, JWT_SECRET)
  world.registeredUserJwt = jwt

  // Inject Bearer token into every XMLHttpRequest made by the browser.
  //
  // We MUST set the header at the JavaScript level (not via page.route or
  // setExtraHTTPHeaders which operate at the CDP/network level). For non-simple
  // cross-origin requests (e.g. PUT multipart/form-data), the browser sends a
  // CORS preflight whose Access-Control-Request-Headers is derived from the
  // JavaScript-level headers only. CDP-level headers are invisible to this
  // determination, so they are never reflected in the preflight — causing the
  // browser to block the actual response as a CORS violation.
  //
  // page.addInitScript patches XHR before the page's own JavaScript runs, so
  // the Authorization header flows through the full CORS lifecycle correctly.
  await world.page.addInitScript((token: string) => {
    const _send = XMLHttpRequest.prototype.send
    const _open = XMLHttpRequest.prototype.open
    const tokenMap = new WeakMap<XMLHttpRequest, string>()

    XMLHttpRequest.prototype.open = function (
      method: string,
      url: string | URL,
      ...rest: unknown[]
    ) {
      tokenMap.set(this, token)
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      return (_open as any).call(this, method, url, ...rest)
    }

    XMLHttpRequest.prototype.send = function (body?: Document | XMLHttpRequestBodyInit | null) {
      const t = tokenMap.get(this)
      if (t) {
        try { this.setRequestHeader('Authorization', `Bearer ${t}`) } catch { /* ignore */ }
      }
      return _send.call(this, body)
    }
  }, jwt)
}

// ── Given ─────────────────────────────────────────────────────────────────────

Given('I visit the registration page', async function (this: CustomWorld) {
  await this.page.goto(`${FRONTEND_URL}/register`)
  await this.page.waitForLoadState('networkidle')
})

Given('I am not authenticated', async function (this: CustomWorld) {
  // Ensure no cookies / tokens from previous scenarios
  await this.page.context().clearCookies()
})

Given('I have a registered and active test account', async function (this: CustomWorld) {
  await setupRegisteredUser(this)
})

// ── When ──────────────────────────────────────────────────────────────────────

When('I fill in valid registration details', async function (this: CustomWorld) {
  const user = generateTestUser()
  // Store for use in subsequent steps (email needed for MailHog lookup)
  this.registeredUserEmail = user.email
  this.registeredUserPassword = user.password
  this.registeredUserDisplayName = user.displayName

  // Clear MailHog so only this scenario's email is present
  await clearMailHog()

  await this.page.locator('#username').fill(user.username)
  await this.page.locator('#email').fill(user.email)
  await this.page.locator('#password').fill(user.password)
  await this.page.locator('#displayName').fill(user.displayName)
})

When('I submit the registration form', async function (this: CustomWorld) {
  await this.page.getByRole('button', { name: /create account/i }).click()
})

When('the email is verified via MailHog', async function (this: CustomWorld) {
  if (!this.registeredUserEmail) {
    throw new Error('No registered user email stored. Run the registration steps first.')
  }

  // Wait for verification email
  const emailBody = await waitForEmailTo(this.registeredUserEmail)
  if (!emailBody) {
    throw new Error(
      `Verification email for ${this.registeredUserEmail} not found in MailHog after retries`,
    )
  }

  const rawLink = extractVerificationLink(emailBody)
  if (!rawLink) {
    throw new Error(`Could not extract verification link from email body:\n${emailBody.slice(0, 500)}`)
  }

  const verificationUrl = rewriteInternalUrl(rawLink)

  // Stash the registered user's sub for later assertions; the local /api/auth/login flow
  // (used by the login form) issues the access token, so we deliberately do NOT inject
  // a pre-built JWT here — that would short-circuit the form-based sign-in we want to test.
  const dbUser = await this.dbClient.queryUserByEmail(this.registeredUserEmail)
  if (dbUser) {
    this.registeredUserSub = dbUser.sub
  }

  // Visit the verification link (may redirect through Keycloak)
  await this.page.goto(verificationUrl, { waitUntil: 'networkidle', timeout: 30000 })

  // Handle a "Proceed" or "Continue" button that Keycloak may show after verification
  try {
    const continueLocator = this.page.locator(
      'a[href*="/login"], button:has-text("Continue"), a:has-text("Proceed"), a:has-text("Continue to log in")',
    )
    const visible = await continueLocator.first().isVisible({ timeout: 4000 })
    if (visible) {
      await continueLocator.first().click()
      await this.page.waitForLoadState('networkidle', { timeout: 15000 })
    }
  } catch {
    // No continue button — that's fine
  }
})

When('I sign in via the login form with the registered email', { timeout: 60000 }, async function (this: CustomWorld) {
  if (!this.registeredUserEmail) {
    throw new Error('Registered user email not stored.')
  }

  // The email-verification flow leaves a Keycloak OIDC session cookie behind. Clearing
  // cookies here guarantees /login renders the credentials form (instead of LoginPage
  // detecting an already-authenticated session and redirecting straight to /persons).
  await this.page.context().clearCookies()

  // Local /api/auth/login looks the user up in the DB by email and validates against
  // the demo password configured in CustomUserDetailsService. The actual Keycloak
  // password chosen at registration is not relevant for this local-JWT login path.
  await this.page.goto(`${FRONTEND_URL}/login`, { waitUntil: 'networkidle' })
  await this.page.locator('#email').waitFor({ state: 'visible', timeout: 10000 })
  await this.page.locator('#email').fill(this.registeredUserEmail)
  await this.page.locator('#password').fill('password')
  await this.page.getByRole('button', { name: /sign in/i }).click()
  await this.page.waitForURL('**/persons', { timeout: 30000 })
})

When('I navigate to the persons page directly', async function (this: CustomWorld) {
  await this.page.goto(`${FRONTEND_URL}/persons`)
})

When('I request the persons API endpoint without authentication', async function (this: CustomWorld) {
  try {
    const response = await this.apiClient.get('/api/persons?page=0&size=10')
    this.lastResponse = response
  } catch (err: unknown) {
    if (err && typeof err === 'object' && 'response' in err) {
      this.lastResponse = (err as { response: unknown }).response
    } else {
      throw err
    }
  }
})

When('I navigate to my profile page', async function (this: CustomWorld) {
  await this.page.goto(`${FRONTEND_URL}/profile`)
  await this.page.waitForLoadState('networkidle')
})

When('I edit my display name to {string}', async function (this: CustomWorld, newName: string) {
  // Click "Edit" button in the display name section
  await this.page.getByRole('button', { name: /edit/i }).click()

  // Clear and fill the display name input
  const nameInput = this.page.locator('#displayName')
  await nameInput.waitFor({ state: 'visible', timeout: 8000 })
  await nameInput.clear()
  await nameInput.fill(newName)

  // Click "Save"
  await this.page.getByRole('button', { name: /^save$/i }).click()

  // Wait for the editing form to close
  await this.page.locator('#displayName').waitFor({ state: 'hidden', timeout: 15000 })
  await this.page.waitForLoadState('networkidle')
})

When('I upload a valid PNG avatar', async function (this: CustomWorld) {
  // Minimal valid 10×10 red PNG — verified to decode via Java ImageIO.read().
  // Generated by Pillow: Image.new('RGB', (10, 10), 'red').save(...)
  const pngBytes = Buffer.from(
    '89504e470d0a1a0a0000000d494844520000000a0000000a0802000000025058ea' +
    '0000001249444154789c63fccf800f30e1951db1d200412c0113b10a73130000000049454e44ae426082',
    'hex',
  )

  const tmpDir = os.tmpdir()
  const tmpFile = path.join(tmpDir, `e2e-avatar-${Date.now()}.png`)
  fs.writeFileSync(tmpFile, pngBytes)

  try {
    // Trigger the hidden file input via the "Upload avatar" button
    const [fileChooser] = await Promise.all([
      this.page.waitForEvent('filechooser', { timeout: 10000 }),
      this.page.getByRole('button', { name: /upload avatar/i }).click(),
    ])
    await fileChooser.setFiles(tmpFile)

    // Wait for the upload XHR to complete before deleting the temp file —
    // Chromium reads the file lazily when the form data is serialised.
    await this.page.waitForResponse(
      (r) => r.url().endsWith('/api/users/me/avatar') && r.request().method() === 'PUT',
      { timeout: 15000 },
    )
    await this.page.waitForLoadState('networkidle')
  } finally {
    if (fs.existsSync(tmpFile)) fs.unlinkSync(tmpFile)
  }
})

When('I remove the avatar', async function (this: CustomWorld) {
  await this.page.getByRole('button', { name: /remove avatar/i }).click()
  await this.page.waitForLoadState('networkidle')
})

When('I delete my account from the profile page', async function (this: CustomWorld) {
  // Click "Delete account" (destructive button at bottom of profile page)
  await this.page.getByRole('button', { name: /delete account/i }).click()

  // Confirm in the dialog
  const dialog = this.page.locator('[role="dialog"]')
  await dialog.waitFor({ state: 'visible', timeout: 8000 })
  await dialog.getByRole('button', { name: /^delete$/i }).click()

  // Wait for redirect away from the profile page
  await this.page.waitForURL(new RegExp(`^${FRONTEND_URL}`), { timeout: 15000 })
})

// ── Then ──────────────────────────────────────────────────────────────────────

Then('I see the registration success message', async function (this: CustomWorld) {
  await this.page.waitForURL('**/register/success', { timeout: 15000 })
  // The success page should mention email verification
  const body = this.page.locator('body')
  await expect(body).toContainText(/email|verif/i, { timeout: 10000 })
})

Then('I am on the persons page and can see my display name in the nav', async function (this: CustomWorld) {
  // After OIDC login the app navigates to the SPA root, then React Router
  // redirects to /persons. Wait for that final URL.
  await this.page.waitForURL('**/persons', { timeout: 30000 })

  // The displayName set during registration should appear in the header
  const header = this.page.locator('header')
  await expect(header).toContainText(
    this.registeredUserDisplayName ?? '',
    { timeout: 15000 },
  )
})

Then('I am redirected to the login page', async function (this: CustomWorld) {
  await this.page.waitForURL('**/login', { timeout: 15000 })
  expect(this.page.url()).toContain('/login')
})

Then('the response status is {int}', async function (this: CustomWorld, expectedStatus: number) {
  const response = this.lastResponse as { status: number }
  expect(response.status).toBe(expectedStatus)
})

Then('the profile page shows the display name {string}', async function (this: CustomWorld, name: string) {
  // After saving, profile re-fetches and shows the new name
  const displaySection = this.page.locator('section').filter({ hasText: /display name/i })
  await expect(displaySection).toContainText(name, { timeout: 15000 })
})

Then('the avatar image is visible on the profile page', async function (this: CustomWorld) {
  // The avatar <img> is visible (not the UserIcon placeholder)
  const avatarImg = this.page.locator('img[alt="Avatar"]')
  await expect(avatarImg).toBeVisible({ timeout: 15000 })
})

Then('the placeholder icon is shown on the profile page', async function (this: CustomWorld) {
  // The <img> tag should be gone; instead a UserIcon SVG is shown
  const avatarImg = this.page.locator('img[alt="Avatar"]')
  await expect(avatarImg).not.toBeVisible({ timeout: 15000 })
})

Then('I am redirected and no longer authenticated', async function (this: CustomWorld) {
  // After account deletion, logout() clears tokens and redirects to /login.
  await this.page.waitForURL(/\/login(\?|$|\/)/, { timeout: 15000 })
})

Then('the deleted account cannot be accessed with the old token', async function (this: CustomWorld) {
  if (!this.registeredUserJwt) {
    // If no JWT was stored (account deletion happened without JWT auth), skip
    return
  }

  // The user no longer exists in the DB; the resource server will authenticate
  // the JWT signature but profileService.getProfile will throw UserNotFoundException → 404
  try {
    const response = await this.apiClient.get('/api/users/me', {
      headers: { Authorization: `Bearer ${this.registeredUserJwt}` },
    })
    // Should NOT reach here
    expect(response.status).toBe(404)
  } catch (err: unknown) {
    const axiosErr = err as { response?: { status?: number } }
    expect(axiosErr.response?.status).toBe(404)
  }

  // Clear the email so the After hook does not try to delete it again
  this.registeredUserEmail = null
})
