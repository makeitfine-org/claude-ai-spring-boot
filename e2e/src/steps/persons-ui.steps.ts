import { When, Then } from '@cucumber/cucumber'
import { expect } from '@playwright/test'
import { CustomWorld } from '../world'

const FRONTEND_URL = process.env['FRONTEND_URL'] || 'http://localhost:3000'

When('I open the persons page', async function (this: CustomWorld) {
  await this.page.goto(`${FRONTEND_URL}/persons`)
  await this.page.waitForLoadState('networkidle')
  // Wait for the table to appear (either rows or "No persons found")
  await this.page.waitForSelector('table', { timeout: 15000 })
})

When('I click the add person button', async function (this: CustomWorld) {
  // Button contains "New person" text with a plus icon
  await this.page.getByRole('button', { name: /new person/i }).click()
  // Wait for dialog to open
  await this.page.waitForSelector('[role="dialog"]', { timeout: 8000 })
})

When(
  'I fill in the person form with first name {string} and last name {string} and email {string}',
  async function (this: CustomWorld, firstName: string, lastName: string, email: string) {
    await this.page.locator('#firstName').fill(firstName)
    await this.page.locator('#lastName').fill(lastName)
    // The email field inside dialog — use the one inside the open dialog
    const dialog = this.page.locator('[role="dialog"]')
    await dialog.locator('#email').fill(email)
  }
)

When('I submit the person form', async function (this: CustomWorld) {
  // Button text is "Create" for new, "Save changes" for edit — match both
  const dialog = this.page.locator('[role="dialog"]')
  await dialog
    .getByRole('button', { name: /save changes|create/i })
    .click()
  // Wait for dialog to close
  await this.page.waitForSelector('[role="dialog"]', { state: 'hidden', timeout: 15000 })
  // Wait for the list to refresh
  await this.page.waitForLoadState('networkidle')
})

Then('the persons list shows {string}', async function (this: CustomWorld, text: string) {
  // Allow enough time for the table to refresh after a mutation
  const tableBody = this.page.locator('tbody')
  await expect(tableBody).toContainText(text, { timeout: 15000 })
})

Then('the persons list does not show {string}', async function (this: CustomWorld, text: string) {
  const tableBody = this.page.locator('tbody')
  await expect(tableBody).not.toContainText(text, { timeout: 15000 })
})

When('I click edit for {string}', async function (this: CustomWorld, fullName: string) {
  // Find the table row that contains the full name text and click Edit in it
  const [firstName, ...rest] = fullName.split(' ')
  const lastName = rest.join(' ')

  // Look for a row whose cells contain firstName and lastName
  const row = this.page.locator('tbody tr').filter({
    has: this.page.locator(`td:has-text("${firstName}")`),
  }).filter({
    has: this.page.locator(`td:has-text("${lastName}")`),
  }).first()

  await row.getByRole('button', { name: /edit/i }).click()
  await this.page.waitForSelector('[role="dialog"]', { timeout: 8000 })
})

When('I update the last name to {string}', async function (this: CustomWorld, newLastName: string) {
  const dialog = this.page.locator('[role="dialog"]')
  const lastNameInput = dialog.locator('#lastName')
  await lastNameInput.clear()
  await lastNameInput.fill(newLastName)
})

When('I click delete for {string}', async function (this: CustomWorld, fullName: string) {
  const [firstName, ...rest] = fullName.split(' ')
  const lastName = rest.join(' ')

  const row = this.page.locator('tbody tr').filter({
    has: this.page.locator(`td:has-text("${firstName}")`),
  }).filter({
    has: this.page.locator(`td:has-text("${lastName}")`),
  }).first()

  await row.getByRole('button', { name: /delete/i }).click()
  // Wait for the confirmation dialog to open
  await this.page.waitForSelector('[role="dialog"]', { timeout: 8000 })
})

When('I confirm the deletion', async function (this: CustomWorld) {
  // The ConfirmDeleteDialog has a "Delete" button (destructive variant)
  const dialog = this.page.locator('[role="dialog"]')
  await dialog.getByRole('button', { name: /^delete$/i }).click()
  // Wait for dialog to close
  await this.page.waitForSelector('[role="dialog"]', { state: 'hidden', timeout: 15000 })
  await this.page.waitForLoadState('networkidle')
})

Then(
  'the persons list shows at least one row',
  async function (this: CustomWorld) {
    const rows = this.page.locator('tbody tr')
    const count = await rows.count()
    expect(count).toBeGreaterThan(0)

    // Make sure it's not the "No persons found" placeholder row
    const firstRowText = await rows.first().textContent()
    expect(firstRowText?.trim()).not.toContain('No persons found')
  }
)

When('I search for {string}', async function (this: CustomWorld, query: string) {
  const searchInput = this.page.locator('input[placeholder*="Search"]')
  await searchInput.fill(query)
  // Wait for debounce (400ms in the component) + network response
  await this.page.waitForTimeout(600)
  await this.page.waitForLoadState('networkidle')
})
