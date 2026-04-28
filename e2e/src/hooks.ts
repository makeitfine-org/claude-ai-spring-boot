import { Before, After, BeforeAll, AfterAll, ITestCaseHookParameter } from '@cucumber/cucumber'
import { chromium } from 'playwright'
import { CustomWorld } from './world'
import { bringStackUp, tearDownStack, waitForStack } from './support/stack'
import { cleanupE2EPersons } from './support/seed'
import * as path from 'path'
import * as fs from 'fs'

BeforeAll(async function () {
  if (process.env['E2E_MANAGE_STACK'] === '1') {
    bringStackUp()
    await waitForStack()
  }
})

AfterAll(async function () {
  if (
    process.env['E2E_MANAGE_STACK'] === '1' &&
    process.env['E2E_KEEP_STACK'] !== '1'
  ) {
    tearDownStack()
  }
})

Before(async function (this: CustomWorld) {
  this.browser = await chromium.launch({ headless: true })
  this.page = await this.browser.newPage()

  // Recreate a fresh DbClient per scenario to avoid connection pool issues
  const { DbClient } = require('./support/db-client')
  this.dbClient = new DbClient()
  await this.dbClient.connect()
})

After(async function (this: CustomWorld, scenario: ITestCaseHookParameter) {
  if (scenario.result?.status === 'FAILED') {
    const screenshotDir = path.join(__dirname, '..', 'test-results')
    if (!fs.existsSync(screenshotDir)) {
      fs.mkdirSync(screenshotDir, { recursive: true })
    }
    const screenshotName = scenario.pickle.name
      .replace(/[^a-zA-Z0-9]/g, '-')
      .toLowerCase()
    const screenshotPath = path.join(screenshotDir, `${screenshotName}.png`)

    if (this.page) {
      try {
        const screenshot = await this.page.screenshot({ fullPage: true })
        await this.attach(screenshot, 'image/png')
        fs.writeFileSync(screenshotPath, screenshot)
      } catch {
        // page may already be closed
      }
    }
  }

  if (this.page) {
    try { await this.page.close() } catch { /* ignore */ }
  }
  if (this.browser) {
    try { await this.browser.close() } catch { /* ignore */ }
  }

  if (this.dbClient) {
    try {
      await cleanupE2EPersons(this.dbClient)
      await this.dbClient.disconnect()
    } catch { /* ignore */ }
  }
})
