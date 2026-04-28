import 'dotenv/config'
import { setWorldConstructor, World, IWorldOptions } from '@cucumber/cucumber'
import { Browser, Page } from 'playwright'
import { AxiosInstance } from 'axios'
import { apiClient } from './support/api-client'
import { DbClient } from './support/db-client'

export class CustomWorld extends World {
  page!: Page
  browser!: Browser
  apiClient: AxiosInstance = apiClient
  dbClient: DbClient = new DbClient()
  accessToken: string = ''
  refreshToken: string = ''
  lastResponse: unknown = null
  lastPersonId: number | null = null

  constructor(options: IWorldOptions) {
    super(options)
  }
}

setWorldConstructor(CustomWorld)
