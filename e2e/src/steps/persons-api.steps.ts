import { Given, When, Then } from '@cucumber/cucumber'
import { expect } from '@playwright/test'
import { CustomWorld } from '../world'
import { loginApi, getAuthHeaders } from '../support/api-client'
import { AxiosResponse } from 'axios'

Given('I have a valid auth token', async function (this: CustomWorld) {
  const email = process.env['TEST_USER_EMAIL'] || 'test@example.com'
  const password = process.env['TEST_USER_PASSWORD'] || 'password'
  const tokens = await loginApi(email, password)
  this.accessToken = tokens.accessToken
  this.refreshToken = tokens.refreshToken
})

When(
  'I create a person via API with first name {string} last name {string} email {string}',
  async function (this: CustomWorld, firstName: string, lastName: string, email: string) {
    try {
      const response: AxiosResponse = await this.apiClient.post(
        '/api/persons',
        { firstName, lastName, email, active: true },
        { headers: getAuthHeaders(this.accessToken) }
      )
      this.lastResponse = response
      // Capture the created person ID for subsequent steps
      const data = response.data as { id?: number }
      if (data.id !== undefined) {
        this.lastPersonId = data.id
      }
    } catch (err: unknown) {
      if (err && typeof err === 'object' && 'response' in err) {
        this.lastResponse = (err as { response: unknown }).response
      } else {
        throw err
      }
    }
  }
)

Then('the API response status is {int}', async function (this: CustomWorld, expectedStatus: number) {
  const response = this.lastResponse as { status: number }
  expect(response.status).toBe(expectedStatus)
})

Then(
  'the API response contains first name {string}',
  async function (this: CustomWorld, expectedFirstName: string) {
    const response = this.lastResponse as { data: { firstName?: string; content?: Array<{ firstName?: string }> } }
    if (response.data.content !== undefined) {
      // Paginated list response
      const found = response.data.content.some((p) => p.firstName === expectedFirstName)
      expect(found).toBe(true)
    } else {
      expect(response.data.firstName).toBe(expectedFirstName)
    }
  }
)

When('I retrieve the person by id', async function (this: CustomWorld) {
  if (this.lastPersonId === null) {
    throw new Error('No person ID stored. Run a create step first.')
  }
  try {
    const response: AxiosResponse = await this.apiClient.get(
      `/api/persons/${this.lastPersonId}`,
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
})

When(
  'I update the person last name to {string} via API',
  async function (this: CustomWorld, newLastName: string) {
    if (this.lastPersonId === null) {
      throw new Error('No person ID stored. Run a create step first.')
    }
    // Fetch current data first so we don't lose required fields
    const getResponse: AxiosResponse = await this.apiClient.get(
      `/api/persons/${this.lastPersonId}`,
      { headers: getAuthHeaders(this.accessToken) }
    )
    const current = getResponse.data as Record<string, unknown>
    try {
      const response: AxiosResponse = await this.apiClient.put(
        `/api/persons/${this.lastPersonId}`,
        { ...current, lastName: newLastName },
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

When('I delete the person via API', async function (this: CustomWorld) {
  if (this.lastPersonId === null) {
    throw new Error('No person ID stored. Run a create step first.')
  }
  try {
    const response: AxiosResponse = await this.apiClient.delete(
      `/api/persons/${this.lastPersonId}`,
      { headers: getAuthHeaders(this.accessToken) }
    )
    this.lastResponse = response
    this.lastPersonId = null
  } catch (err: unknown) {
    if (err && typeof err === 'object' && 'response' in err) {
      this.lastResponse = (err as { response: unknown }).response
    } else {
      throw err
    }
  }
})

When('I list persons via API', async function (this: CustomWorld) {
  try {
    const response: AxiosResponse = await this.apiClient.get(
      '/api/persons?page=0&size=20&sort=lastName,asc',
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
})

Then(
  'the API response contains at least {int} persons',
  async function (this: CustomWorld, minCount: number) {
    const response = this.lastResponse as {
      data: {
        content?: unknown[]
        totalElements?: number
      }
    }
    const count =
      response.data.content?.length ??
      response.data.totalElements ??
      0
    expect(count).toBeGreaterThanOrEqual(minCount)
  }
)
