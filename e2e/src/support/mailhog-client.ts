import axios from 'axios'

const MAILHOG_URL = process.env['MAILHOG_URL'] || 'http://localhost:8025'

interface MailHogRecipient {
  Mailbox: string
  Domain: string
  Params: string
}

interface MailHogMessage {
  To: MailHogRecipient[]
  Content: {
    Body: string
  }
  Raw: {
    Data: string
  }
}

interface MailHogResponse {
  total: number
  count: number
  items: MailHogMessage[]
}

/**
 * Deletes all messages from MailHog (clean slate before a registration test).
 */
export async function clearMailHog(): Promise<void> {
  await axios.delete(`${MAILHOG_URL}/api/v1/messages`)
}

/**
 * Polls MailHog until an email arrives for the given recipient.
 * Retries up to {@code maxRetries} times with a 2-second interval.
 *
 * @returns the email body text, or null if not found within the retry window
 */
export async function waitForEmailTo(
  recipientEmail: string,
  maxRetries = 15,
  intervalMs = 2000,
): Promise<string | null> {
  for (let attempt = 0; attempt < maxRetries; attempt++) {
    try {
      const response = await axios.get<MailHogResponse>(
        `${MAILHOG_URL}/api/v2/messages?limit=50`,
      )
      const messages = response.data.items || []

      for (const msg of messages) {
        const recipients = msg.To || []
        const matches = recipients.some(
          (r) => `${r.Mailbox}@${r.Domain}`.toLowerCase() === recipientEmail.toLowerCase(),
        )
        if (matches) {
          // Prefer the plain-text body; fall back to raw data
          return msg.Content?.Body || msg.Raw?.Data || null
        }
      }
    } catch {
      // ignore transient MailHog errors
    }

    if (attempt < maxRetries - 1) {
      await sleep(intervalMs)
    }
  }
  return null
}

/**
 * Extracts the Keycloak email-verification link from an email body.
 * The link contains "action-token" in its path.
 */
export function extractVerificationLink(emailBody: string): string | null {
  // Keycloak links appear in plain-text bodies as bare URLs and in HTML as hrefs.
  const patterns = [
    /https?:\/\/[^\s<>"]+action-token[^\s<>"]+/gi,
    /https?:\/\/[^\s<>"]+verify-email[^\s<>"]+/gi,
  ]
  for (const pattern of patterns) {
    const match = emailBody.match(pattern)
    if (match) {
      return match[0].replace(/&amp;/g, '&')
    }
  }
  return null
}

/**
 * Rewrites any internal Keycloak hostname so the URL is reachable from the
 * host machine running the e2e tests.
 *
 * Internal → external mappings (configurable via env vars):
 *   keycloak:8180 → localhost:8180
 */
export function rewriteInternalUrl(url: string): string {
  const keycloakInternal = process.env['KEYCLOAK_INTERNAL_HOST'] || 'keycloak:8180'
  const keycloakExternal = process.env['KEYCLOAK_URL'] || 'http://localhost:8180'
  return url.replace(new RegExp(`https?://${keycloakInternal}`, 'g'), keycloakExternal)
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}
