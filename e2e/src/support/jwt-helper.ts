import { createHmac } from 'crypto'

/**
 * Creates a local HMAC-SHA256 JWT for e2e testing.
 *
 * The signing key must match the backend's `JWT_SECRET` environment variable
 * (base64-encoded key bytes). The generated token uses a UUID as subject so
 * it is accepted by the resource server and profile controller.
 */
export function createTestJwt(
  sub: string,
  jwtSecretBase64: string,
  expirationSeconds = 3600,
): string {
  const keyBytes = Buffer.from(jwtSecretBase64, 'base64')

  const header = toBase64Url(Buffer.from(JSON.stringify({ alg: 'HS256', typ: 'JWT' })))
  const now = Math.floor(Date.now() / 1000)
  const payload = toBase64Url(
    Buffer.from(
      JSON.stringify({
        sub,
        iat: now,
        exp: now + expirationSeconds,
      }),
    ),
  )

  const signingInput = `${header}.${payload}`
  const signature = toBase64Url(
    createHmac('sha256', keyBytes).update(signingInput).digest(),
  )

  return `${signingInput}.${signature}`
}

function toBase64Url(buffer: Buffer): string {
  return buffer
    .toString('base64')
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=/g, '')
}
