import { Suspense } from 'react'
import { LoginForm } from './LoginForm'
import { Loader } from '@/components/common/Loader'

// Server wrapper: useSearchParams() in LoginForm requires a Suspense boundary
// for Next.js 16 prerendering (otherwise build fails with CSR bailout error).
export default function LoginPage() {
  return (
    <Suspense fallback={<Loader />}>
      <LoginForm />
    </Suspense>
  )
}
