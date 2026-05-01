import { Given, When, Then } from '@cucumber/cucumber'
import { expect } from '@playwright/test'
import { CustomWorld } from '../world'
import { loginApi } from '../support/api-client'
import { getAuthHeaders } from '../support/api-client'

const FRONTEND_URL = process.env['FRONTEND_URL'] || 'http://localhost:3000'
const API_BASE_URL = process.env['API_BASE_URL'] || 'http://localhost:8080'

Given('I am logged in as {string}', async function (this: CustomWorld, email: string) {
  const password =
    email === process.env['TEST_USER_EMAIL']
      ? (process.env['TEST_USER_PASSWORD'] || 'password')
      : 'password'

  // Obtain a JWT token via the local auth endpoint (independent of Keycloak).
  const tokens = await loginApi(email, password)
  this.accessToken = tokens.accessToken
  this.refreshToken = tokens.refreshToken

  // Inject Bearer token into all API requests so persons CRUD works.
  // Registered first = lower priority (LIFO); specific mock below overrides /api/users/me.
  await this.page.route(`${API_BASE_URL}/api/**`, async (route) => {
    const headers = {
      ...route.request().headers(),
      'Authorization': `Bearer ${tokens.accessToken}`,
    }
    await route.continue({ headers })
  })

  // The local JWT uses email as subject, but /api/users/me expects a UUID sub.
  // Registered last = highest priority (LIFO); fulfills GET so AuthContext sees
  // isAuthenticated = true without going through Keycloak OIDC.
  await this.page.route(`${API_BASE_URL}/api/users/me`, async (route) => {
    if (route.request().method() === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          sub: '00000000-0000-0000-0000-000000000001',
          username: email,
          displayName: 'Test User',
          email: email,
          hasAvatar: false,
        }),
      })
    } else {
      await route.fallback()
    }
  })

  await this.page.goto(`${FRONTEND_URL}/persons`)
  await this.page.waitForURL('**/persons', { timeout: 15000 })
})

Given('I am on the login page', async function (this: CustomWorld) {
  await this.page.goto(`${FRONTEND_URL}/login`)
  await this.page.waitForLoadState('networkidle')
})

Then('I should see the email field', async function (this: CustomWorld) {
  await expect(this.page.locator('#email')).toBeVisible({ timeout: 5000 })
})

Then('I should see the password field', async function (this: CustomWorld) {
  await expect(this.page.locator('#password')).toBeVisible({ timeout: 5000 })
})

When(
  'I enter email {string} and password {string}',
  async function (this: CustomWorld, email: string, password: string) {
    await this.page.locator('#email').fill(email)
    await this.page.locator('#password').fill(password)
  }
)

When('I submit the login form', async function (this: CustomWorld) {
  await this.page.getByRole('button', { name: /sign in/i }).click()
  await this.page.waitForTimeout(1500)
})

Then('I should be redirected to the persons page', async function (this: CustomWorld) {
  await this.page.waitForURL('**/persons', { timeout: 15000 })
  expect(this.page.url()).toContain('/persons')
})

Then('I should see the sign in button', async function (this: CustomWorld) {
  const signInButton = this.page.getByRole('button', { name: /sign in/i })
  await expect(signInButton).toBeVisible({ timeout: 5000 })
})

Then('I should see an error message', async function (this: CustomWorld) {
  const errorLocator = this.page.locator('[role="alert"]')
  await expect(errorLocator).toBeVisible({ timeout: 8000 })
})

Given('I have a valid JWT token', async function (this: CustomWorld) {
  const email = process.env['TEST_USER_EMAIL'] || 'test@example.com'
  const password = process.env['TEST_USER_PASSWORD'] || 'password'
  const tokens = await loginApi(email, password)
  this.accessToken = tokens.accessToken
  this.refreshToken = tokens.refreshToken
})

When(
  'I call the refresh endpoint with my refresh token',
  async function (this: CustomWorld) {
    try {
      const response = await this.apiClient.post(
        '/api/auth/refresh',
        { refreshToken: this.refreshToken },
        { headers: getAuthHeaders(this.accessToken) }
      )
      this.lastResponse = response
    } catch (err: unknown) {
      if (err && typeof err === 'object' && 'response' in err) {
        this.lastResponse = (err as { response: unknown }).response
      } else {
        throw err
      }
    }
  }
)

When('I click the logout button', async function (this: CustomWorld) {
  await this.page.getByRole('button', { name: /^logout$/i }).click()
  await this.page.waitForURL('**/login', { timeout: 15000 })
})

Then('I should receive a new access token', async function (this: CustomWorld) {
  const response = this.lastResponse as { status: number; data: { accessToken?: string } }
  expect(response.status).toBe(200)
  expect(typeof response.data.accessToken).toBe('string')
  expect(response.data.accessToken!.length).toBeGreaterThan(0)
})
