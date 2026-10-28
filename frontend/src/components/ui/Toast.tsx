import React, { createContext, useContext, useState, useCallback } from 'react';
import { CheckCircle2, AlertTriangle, XCircle, Info, X } from 'lucide-react';

export type ToastType = 'success' | 'error' | 'warn' | 'info';

export interface ToastMessage {
  id: string;
  type: ToastType;
  title: string;
  description?: string;
}

interface ToastContextValue {
  showToast: (toast: Omit<ToastMessage, 'id'>) => void;
  toast: {
    success: (title: string, description?: string) => void;
    error: (title: string, description?: string) => void;
    warn: (title: string, description?: string) => void;
    info: (title: string, description?: string) => void;
  };
  dismissToast: (id: string) => void;
}

const ToastContext = createContext<ToastContextValue | undefined>(undefined);

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  const dismissToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const showToast = useCallback((toastData: Omit<ToastMessage, 'id'>) => {
    const id = Math.random().toString(36).substring(2, 9);
    const newToast: ToastMessage = { ...toastData, id };

    setToasts((prev) => [...prev, newToast]);

    setTimeout(() => {
      dismissToast(id);
    }, 4000);
  }, [dismissToast]);

  const toast = {
    success: (title: string, description?: string) => showToast({ type: 'success', title, description }),
    error: (title: string, description?: string) => showToast({ type: 'error', title, description }),
    warn: (title: string, description?: string) => showToast({ type: 'warn', title, description }),
    info: (title: string, description?: string) => showToast({ type: 'info', title, description }),
  };

  return (
    <ToastContext.Provider value={{ showToast, toast, dismissToast }}>
      {children}
      <div className="fixed bottom-5 right-5 z-50 flex flex-col space-y-2.5 max-w-sm sm:max-w-md w-full px-4 sm:px-0 pointer-events-none">
        {toasts.map((t) => {
          const config = {
            success: {
              border: 'border-l-2 border-l-[#38FF8E] border-[#25352B]',
              icon: CheckCircle2,
              iconColor: 'text-[#38FF8E]',
              typeLabel: 'SUCCESS'
            },
            error: {
              border: 'border-l-2 border-l-[#FF5A78] border-[#25352B]',
              icon: XCircle,
              iconColor: 'text-[#FF5A78]',
              typeLabel: 'ERROR'
            },
            warn: {
              border: 'border-l-2 border-l-[#FFE282] border-[#25352B]',
              icon: AlertTriangle,
              iconColor: 'text-[#FFE282]',
              typeLabel: 'WARNING'
            },
            info: {
              border: 'border-l-2 border-l-[#65F8FF] border-[#25352B]',
              icon: Info,
              iconColor: 'text-[#65F8FF]',
              typeLabel: 'TELEMETRY'
            }
          }[t.type];

          const IconComponent = config.icon;

          return (
            <div
              key={t.id}
              role="alert"
              className={`pointer-events-auto bg-[#0C110E] ${config.border} p-3.5 rounded-[2px] shadow-[0_10px_25px_rgba(0,0,0,0.85)] flex items-start space-x-3 font-mono transition-all duration-200 animate-in fade-in slide-in-from-bottom-2`}
            >
              <IconComponent className={`w-4 h-4 flex-shrink-0 mt-0.5 ${config.iconColor}`} />
              <div className="flex-1 min-w-0 pr-1">
                <div className="flex items-center space-x-2">
                  <span className={`text-[10px] font-bold tracking-wider uppercase ${config.iconColor}`}>
                    [{config.typeLabel}]
                  </span>
                  <span className="text-xs font-bold text-[#F4F8F4] truncate">{t.title}</span>
                </div>
                {t.description && (
                  <p className="text-[11px] text-[#C0CAC2] mt-1 leading-relaxed break-words">
                    {t.description}
                  </p>
                )}
              </div>
              <button
                onClick={() => dismissToast(t.id)}
                className="text-[#849188] hover:text-[#F4F8F4] transition-colors p-0.5 cursor-pointer flex-shrink-0"
                aria-label="Close notification"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            </div>
          );
        })}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error('useToast must be used within a ToastProvider');
  }
  return context;
}
