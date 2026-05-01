import { Pool, PoolClient } from 'pg'

export class DbClient {
  private pool: Pool
  private client: PoolClient | null = null

  constructor() {
    this.pool = new Pool({
      host: process.env['DB_HOST'] || 'localhost',
      port: parseInt(process.env['DB_PORT'] || '5432', 10),
      database: process.env['DB_NAME'] || 'persondb',
      user: process.env['DB_USER'] || 'postgres',
      password: process.env['DB_PASSWORD'] || 'postgres',
      connectionTimeoutMillis: 10000,
      idleTimeoutMillis: 30000,
    })
  }

  async connect(): Promise<void> {
    this.client = await this.pool.connect()
  }

  async disconnect(): Promise<void> {
    if (this.client) {
      this.client.release()
      this.client = null
    }
    await this.pool.end()
  }

  private getClient(): PoolClient {
    if (!this.client) {
      throw new Error('DbClient not connected. Call connect() first.')
    }
    return this.client
  }

  async queryPersonByEmail(email: string): Promise<Record<string, unknown> | null> {
    const result = await this.getClient().query(
      'SELECT * FROM persons WHERE email = $1',
      [email]
    )
    return (result.rows[0] as Record<string, unknown>) ?? null
  }

  async queryPersonById(id: number): Promise<Record<string, unknown> | null> {
    const result = await this.getClient().query(
      'SELECT * FROM persons WHERE id = $1',
      [id]
    )
    return (result.rows[0] as Record<string, unknown>) ?? null
  }

  async deletePersonByEmail(email: string): Promise<void> {
    await this.getClient().query(
      'DELETE FROM persons WHERE email = $1',
      [email]
    )
  }

  async deleteTestPersons(): Promise<void> {
    await this.getClient().query(
      "DELETE FROM persons WHERE email LIKE '%e2e-test%' OR first_name = 'E2ETest'"
    )
  }

  async queryUserByEmail(email: string): Promise<{ sub: string; username: string; display_name: string | null } | null> {
    const result = await this.getClient().query(
      'SELECT sub::text, username, display_name FROM users WHERE email = $1',
      [email]
    )
    return (result.rows[0] as { sub: string; username: string; display_name: string | null }) ?? null
  }

  async deleteTestUsers(): Promise<void> {
    await this.getClient().query(
      "DELETE FROM users WHERE email LIKE 'e2e-user-%'"
    )
  }
}
