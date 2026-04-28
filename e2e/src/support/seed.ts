import { DbClient } from './db-client'

/**
 * No-op for now — the test user (test@example.com / password) is hardcoded
 * in the Spring Boot backend and does not need database seeding.
 */
export async function seedTestUser(): Promise<void> {
  // intentionally empty
}

/**
 * Deletes all persons created by e2e tests, identified by the
 * `e2e-test-*@example.com` email pattern or `E2ETest` first name.
 */
export async function cleanupE2EPersons(dbClient: DbClient): Promise<void> {
  await dbClient.deleteTestPersons()
}
