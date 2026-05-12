import { ref, shallowRef } from "vue";

interface ConfirmOptions {
  title?: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  type?: "error" | "warning" | "info";
}

const visible = ref(false);
const options = shallowRef<ConfirmOptions>({ message: "" });
let resolvePromise: (value: boolean) => void = () => {};

export function useConfirm() {
  function confirm(opts: ConfirmOptions | string): Promise<boolean> {
    options.value = typeof opts === "string" ? { message: opts } : opts;
    visible.value = true;
    return new Promise((resolve) => {
      resolvePromise = resolve;
    });
  }

  function onConfirm() {
    visible.value = false;
    resolvePromise(true);
  }

  function onCancel() {
    visible.value = false;
    resolvePromise(false);
  }

  return { confirm, visible, options, onConfirm, onCancel };
}
