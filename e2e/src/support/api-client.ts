import axios, { AxiosInstance } from 'axios'

export const apiClient: AxiosInstance = axios.create({
  baseURL: process.env['API_BASE_URL'] || 'http://localhost:8080',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

export interface AuthTokens {
  accessToken: string
  refreshToken: string
}

export async function loginApi(email: string, password: string): Promise<AuthTokens> {
  const response = await apiClient.post<AuthTokens>('/api/auth/login', { email, password })
  return response.data
}

export function getAuthHeaders(token: string): Record<string, string> {
  return { Authorization: `Bearer ${token}` }
}
