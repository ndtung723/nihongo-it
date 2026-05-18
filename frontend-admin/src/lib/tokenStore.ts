// In-memory access token storage. Intentionally ephemeral — cleared on tab close.
// The refresh token lives in an httpOnly cookie (set by backend) and is used to
// silently restore the access token on app mount via the auth store's initialize().
let accessToken: string | null = null

export function getAccessToken(): string | null {
  return accessToken
}

export function setAccessToken(token: string | null): void {
  accessToken = token
}

export function clearAccessToken(): void {
  accessToken = null
}
