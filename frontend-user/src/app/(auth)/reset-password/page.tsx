import { Suspense } from 'react'
import { ResetPasswordForm } from './ResetPasswordForm'
import { Loader } from '@/components/common/Loader'

// Server wrapper: useSearchParams() in ResetPasswordForm requires a Suspense
// boundary for Next.js 16 prerendering.
export default function ResetPasswordPage() {
  return (
    <Suspense fallback={<Loader />}>
      <ResetPasswordForm />
    </Suspense>
  )
}
