import { useToast, type ToastProps } from "vue-toast-notification";

const DEFAULTS: ToastProps = {
  position: "top-right",
  duration: 3000,
  dismissible: true,
};

export function useAppToast() {
  const toast = useToast();

  return {
    success: (message: string, opts?: ToastProps) =>
      toast.success(message, { ...DEFAULTS, ...opts }),
    error: (message: string, opts?: ToastProps) =>
      toast.error(message, { ...DEFAULTS, duration: 5000, ...opts }),
    warning: (message: string, opts?: ToastProps) =>
      toast.warning(message, { ...DEFAULTS, ...opts }),
    info: (message: string, opts?: ToastProps) =>
      toast.info(message, { ...DEFAULTS, ...opts }),
  };
}
