import { pushAppToast, type ToastType } from '../components/ui/AppToast';

/** Drop-in cosmetic replacement for `alert()` — same message, in-app toast. */
export const appNotify = {
  success: (message: string) => pushAppToast('success', message),
  error: (message: string) => pushAppToast('error', message),
  info: (message: string) => pushAppToast('info', message),
  warning: (message: string) => pushAppToast('warning', message),
  show: (type: ToastType, message: string) => pushAppToast(type, message),
};
