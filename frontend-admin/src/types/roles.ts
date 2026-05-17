export const ROLES = { ADMIN: 1, USER: 2 } as const
export type Role = (typeof ROLES)[keyof typeof ROLES]
