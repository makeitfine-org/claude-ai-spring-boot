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

  await this.page.goto(`${FRONTEND_URL}/login`)
  await this.page.waitForLoadState('networkidle')

  await this.page.locator('#email').fill(email)
  await this.page.locator('#password').fill(password)
  await this.page.getByRole('button', { name: 'Sign in' }).click()

  await this.page.waitForURL('**/persons', { timeout: 15000 })

  // Also store the token for any API steps that follow
  try {
    const tokens = await loginApi(email, password)
    this.accessToken = tokens.accessToken
    this.refreshToken = tokens.refreshToken
  } catch {
    // Token storage is best-effort; UI login already succeeded
  }
})

Given('I am on the login page', async function (this: CustomWorld) {
  await this.page.goto(`${FRONTEND_URL}/login`)
  await this.page.waitForLoadState('networkidle')
})

When(
  'I enter email {string} and password {string}',
  async function (this: CustomWorld, email: string, password: string) {
    await this.page.locator('#email').fill(email)
    await this.page.locator('#password').fill(password)
  }
)

When('I submit the login form', async function (this: CustomWorld) {
  await this.page.getByRole('button', { name: 'Sign in' }).click()
  // Give the page time to react (redirect or error)
  await this.page.waitForTimeout(1500)
})

Then('I should be redirected to the persons page', async function (this: CustomWorld) {
  await this.page.waitForURL('**/persons', { timeout: 15000 })
  expect(this.page.url()).toContain('/persons')
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

Then('I should receive a new access token', async function (this: CustomWorld) {
  const response = this.lastResponse as { status: number; data: { accessToken?: string } }
  expect(response.status).toBe(200)
  expect(typeof response.data.accessToken).toBe('string')
  expect(response.data.accessToken!.length).toBeGreaterThan(0)
})
