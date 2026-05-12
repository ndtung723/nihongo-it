import { useAppToast } from "@/composables/useAppToast";

export const handleSessionExpiration = (currentPath: string): void => {
  try {
    const toast = useAppToast();
    toast.error("Your session has expired. Please log in again.");
  } catch {}

  // Redirect to login page if not already there
  if (!currentPath.includes("/login")) {
    setTimeout(() => {
      window.location.href = `/login?redirect=${encodeURIComponent(currentPath)}&error=Your+session+has+expired.+Please+log+in+again.`;
    }, 2000);
  }
};
