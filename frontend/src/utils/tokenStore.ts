// In-memory access token storage. Intentionally ephemeral — cleared on page close.
// The refresh token lives in an httpOnly cookie (set by backend) and is used to
// silently restore the access token on page load via initializeAuth().
let _token: string | null = null;

export function getAccessToken(): string | null {
  return _token;
}

export function setAccessToken(token: string): void {
  _token = token;
}

export function clearAccessToken(): void {
  _token = null;
}
