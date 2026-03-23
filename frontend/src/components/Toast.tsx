import { AlertCircle, CheckCircle2, Info, X } from 'lucide-react';
import { createContext, type ReactNode, useCallback, useContext, useRef, useState } from 'react';

type ToastType = 'error' | 'success' | 'info';

interface Toast {
  id: number;
  message: string;
  type: ToastType;
}

interface ToastContextValue {
  showToast: (message: string, type?: ToastType) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

const MAX_VISIBLE = 3;
export const AUTO_DISMISS_MS = 5000;

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const nextId = useRef(0);

  const removeToast = useCallback((id: number) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const showToast = useCallback(
    (message: string, type: ToastType = 'error') => {
      const id = nextId.current++;
      setToasts((prev) => [...prev.slice(-(MAX_VISIBLE - 1)), { id, message, type }]);
      setTimeout(() => removeToast(id), AUTO_DISMISS_MS);
    },
    [removeToast],
  );

  // Expose for e2e tests (dev only, tree-shaken in prod)
  if (process.env.NODE_ENV !== 'production') {
    (window as any).__showToast = showToast;
  }

  const typeIcons: Record<ToastType, { icon: typeof AlertCircle; color: string }> = {
    error: { icon: AlertCircle, color: 'text-efi-error' },
    success: { icon: CheckCircle2, color: 'text-efi-success' },
    info: { icon: Info, color: 'text-efi-info' },
  };

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      <div className="fixed top-4 left-4 right-4 sm:left-auto z-50 flex flex-col gap-2 max-w-sm">
        {toasts.map((toast) => {
          const Icon = typeIcons[toast.type].icon;
          return (
            <div
              key={toast.id}
              className="px-4 py-3 rounded-lg text-sm text-efi-text-primary shadow-lg glass-crystal animate-[slide-in-right_0.3s_ease-out]"
            >
              <div className="flex items-center gap-3">
                <Icon className={`w-5 h-5 shrink-0 ${typeIcons[toast.type].color}`} />
                <span className="flex-1">{toast.message}</span>
                <button
                  type="button"
                  onClick={() => removeToast(toast.id)}
                  className="text-efi-text-tertiary hover:text-efi-text-primary shrink-0 cursor-pointer p-1.5 rounded hover:bg-white/8 transition-colors focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
                  aria-label="Dismiss"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          );
        })}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast(): ToastContextValue {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used within ToastProvider');
  return ctx;
}
