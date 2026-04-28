import { Then } from '@cucumber/cucumber'
import { expect } from '@playwright/test'
import { CustomWorld } from '../world'

Then(
  'the database has a person with email {string}',
  async function (this: CustomWorld, email: string) {
    // Retry briefly to account for async persistence after API/UI call
    let row: Record<string, unknown> | null = null
    const deadline = Date.now() + 10000
    while (Date.now() < deadline) {
      row = await this.dbClient.queryPersonByEmail(email)
      if (row !== null) break
      await new Promise((resolve) => setTimeout(resolve, 500))
    }
    expect(row).not.toBeNull()
  }
)

Then(
  'the database does not have a person with email {string}',
  async function (this: CustomWorld, email: string) {
    // Retry briefly to allow delete to propagate
    let row: Record<string, unknown> | null = { placeholder: true }
    const deadline = Date.now() + 10000
    while (Date.now() < deadline) {
      row = await this.dbClient.queryPersonByEmail(email)
      if (row === null) break
      await new Promise((resolve) => setTimeout(resolve, 500))
    }
    expect(row).toBeNull()
  }
)

Then(
  'the database person with email {string} has last name {string}',
  async function (this: CustomWorld, email: string, expectedLastName: string) {
    let row: Record<string, unknown> | null = null
    const deadline = Date.now() + 10000
    while (Date.now() < deadline) {
      row = await this.dbClient.queryPersonByEmail(email)
      if (row !== null && row['last_name'] === expectedLastName) break
      await new Promise((resolve) => setTimeout(resolve, 500))
    }
    expect(row).not.toBeNull()
    expect(row!['last_name']).toBe(expectedLastName)
  }
)
