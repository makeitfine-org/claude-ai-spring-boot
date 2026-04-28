import { spawnSync } from 'child_process'
import * as path from 'path'
import * as http from 'http'

const REPO_ROOT = path.resolve(__dirname, '..', '..', '..')
const COMPOSE_FILE = path.join(REPO_ROOT, 'docker-compose.yml')

export function bringStackUp(): void {
  console.log('[stack] Starting docker-compose stack...')
  const result = spawnSync(
    'docker',
    ['compose', '-f', COMPOSE_FILE, 'up', '-d', '--build'],
    { stdio: 'inherit', cwd: REPO_ROOT }
  )
  if (result.status !== 0) {
    throw new Error(`[stack] docker compose up failed with exit code ${result.status}`)
  }
}

export function tearDownStack(): void {
  console.log('[stack] Tearing down docker-compose stack...')
  spawnSync(
    'docker',
    ['compose', '-f', COMPOSE_FILE, 'down', '-v'],
    { stdio: 'inherit', cwd: REPO_ROOT }
  )
}

function httpGet(url: string): Promise<number> {
  return new Promise((resolve, reject) => {
    const req = http.get(url, { timeout: 5000 }, (res) => {
      resolve(res.statusCode ?? 0)
    })
    req.on('error', reject)
    req.on('timeout', () => {
      req.destroy()
      reject(new Error(`Timeout fetching ${url}`))
    })
  })
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

export async function waitForStack(timeoutMs = 120000): Promise<void> {
  const apiHealth = `${process.env['API_BASE_URL'] || 'http://localhost:8080'}/actuator/health`
  const frontendUrl = process.env['FRONTEND_URL'] || 'http://localhost:3000'
  const interval = 3000
  const deadline = Date.now() + timeoutMs

  console.log('[stack] Waiting for backend health...')
  while (Date.now() < deadline) {
    try {
      const status = await httpGet(apiHealth)
      if (status === 200) {
        console.log('[stack] Backend is up.')
        break
      }
    } catch {
      // not ready yet
    }
    if (Date.now() + interval > deadline) {
      throw new Error(`[stack] Backend did not become healthy within ${timeoutMs}ms`)
    }
    await sleep(interval)
  }

  console.log('[stack] Waiting for frontend...')
  while (Date.now() < deadline) {
    try {
      const status = await httpGet(frontendUrl)
      if (status >= 200 && status < 400) {
        console.log('[stack] Frontend is up.')
        return
      }
    } catch {
      // not ready yet
    }
    if (Date.now() + interval > deadline) {
      throw new Error(`[stack] Frontend did not become available within ${timeoutMs}ms`)
    }
    await sleep(interval)
  }

  throw new Error(`[stack] Stack did not come up within ${timeoutMs}ms`)
}
