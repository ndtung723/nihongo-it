import { useToast } from 'vue-toast-notification'

/**
 * Handles session expiration by showing a notification and redirecting to login
 * @param currentPath The current path to redirect back to after login
 */
export const handleSessionExpiration = (currentPath: string): void => {
  try {
    // Show toast notification
    const toast = useToast()
    toast.error('Your session has expired. Please log in again.', {
      position: 'top',
      duration: 5000
    })
  } catch (e) {
  }

  // Redirect to login page if not already there
  if (!currentPath.includes('/login')) {
    setTimeout(() => {
      window.location.href = `/login?redirect=${encodeURIComponent(currentPath)}&error=Your+session+has+expired.+Please+log+in+again.`
    }, 2000)
  }
}
