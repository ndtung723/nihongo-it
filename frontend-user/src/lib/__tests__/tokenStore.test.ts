import { describe, it, expect, beforeEach } from 'vitest'
import { getAccessToken, setAccessToken, clearAccessToken } from '../tokenStore'

describe('tokenStore', () => {
  beforeEach(() => clearAccessToken())

  it('starts with no token', () => {
    expect(getAccessToken()).toBeNull()
  })

  it('stores and retrieves a token', () => {
    setAccessToken('jwt-abc')
    expect(getAccessToken()).toBe('jwt-abc')
  })

  it('clears the token', () => {
    setAccessToken('jwt-abc')
    clearAccessToken()
    expect(getAccessToken()).toBeNull()
  })

  it('overwrites a previous token', () => {
    setAccessToken('first')
    setAccessToken('second')
    expect(getAccessToken()).toBe('second')
  })
})
