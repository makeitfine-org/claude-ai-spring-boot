import axios from 'axios'

const KEYCLOAK_URL = process.env['KEYCLOAK_URL'] || 'http://localhost:8180'
const KEYCLOAK_REALM = process.env['KEYCLOAK_REALM'] || 'claude-ai'
const KEYCLOAK_ADMIN_USER = process.env['KEYCLOAK_ADMIN_USER'] || 'admin'
const KEYCLOAK_ADMIN_PASSWORD = process.env['KEYCLOAK_ADMIN_PASSWORD'] || 'admin'

/**
 * Obtains a short-lived admin access token from the Keycloak master realm.
 */
async function getAdminToken(): Promise<string> {
  const params = new URLSearchParams()
  params.append('grant_type', 'password')
  params.append('client_id', 'admin-cli')
  params.append('username', KEYCLOAK_ADMIN_USER)
  params.append('password', KEYCLOAK_ADMIN_PASSWORD)

  const response = await axios.post<{ access_token: string }>(
    `${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token`,
    params,
    { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } },
  )
  return response.data.access_token
}

/**
 * Deletes a Keycloak user identified by email from the configured realm.
 * Silently ignores "user not found" (404) errors.
 */
export async function deleteKeycloakUserByEmail(email: string): Promise<void> {
  try {
    const token = await getAdminToken()
    const headers = { Authorization: `Bearer ${token}` }

    // Look up the user by email
    const searchResponse = await axios.get<Array<{ id: string }>>(
      `${KEYCLOAK_URL}/admin/realms/${KEYCLOAK_REALM}/users?email=${encodeURIComponent(email)}&exact=true`,
      { headers },
    )

    const users = searchResponse.data
    if (!users || users.length === 0) {
      return // user already gone
    }

    const userId = users[0]!.id
    await axios.delete(
      `${KEYCLOAK_URL}/admin/realms/${KEYCLOAK_REALM}/users/${userId}`,
      { headers },
    )
  } catch (err: unknown) {
    const axiosErr = err as { response?: { status?: number } }
    if (axiosErr.response?.status === 404) {
      return // already deleted
    }
    // Log but don't fail the test on cleanup errors
    console.error(`Failed to delete Keycloak user ${email}:`, err)
  }
}
