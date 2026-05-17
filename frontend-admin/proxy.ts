// Next.js 16: `proxy.ts` replaces the old `middleware.ts`. The exported function
// must be named `proxy`. Runtime is Node.js (not Edge).
//
// Admin app: every route except /login is protected. Role check (must be ADMIN)
// happens client-side in the (admin) layout — proxy only checks cookie presence.
import { NextResponse, type NextRequest } from 'next/server'

const PUBLIC_PATHS = ['/login']

function isPublic(pathname: string): boolean {
  return PUBLIC_PATHS.some((p) => pathname === p || pathname.startsWith(`${p}/`))
}

export function proxy(req: NextRequest) {
  const { pathname } = req.nextUrl
  if (isPublic(pathname)) {
    return NextResponse.next()
  }

  const hasRefreshCookie = req.cookies.has('refresh_token')
  if (!hasRefreshCookie) {
    const url = req.nextUrl.clone()
    url.pathname = '/login'
    url.searchParams.set('redirect', pathname)
    return NextResponse.redirect(url)
  }

  return NextResponse.next()
}

export const config = {
  matcher: ['/((?!_next|api|favicon.ico|.*\\..*).*)'],
}
