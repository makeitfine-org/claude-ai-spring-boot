export interface AuthRequest {
  email: string
  password: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
}

export interface UserProfile {
  sub: string
  username: string
  displayName: string | null
  email: string
  hasAvatar: boolean
}
