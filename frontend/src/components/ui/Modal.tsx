import React, { useEffect } from 'react';
import { X } from 'lucide-react';

interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  eyebrow?: string;
  children: React.ReactNode;
  maxWidth?: 'sm' | 'md' | 'lg' | 'xl';
}

export function Modal({
  isOpen,
  onClose,
  title,
  eyebrow,
  children,
  maxWidth = 'md'
}: ModalProps) {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };
    if (isOpen) {
      document.body.style.overflow = 'hidden';
      window.addEventListener('keydown', handleKeyDown);
    }
    return () => {
      document.body.style.overflow = 'unset';
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const maxWidthClasses = {
    sm: 'max-w-sm',
    md: 'max-w-md',
    lg: 'max-w-lg',
    xl: 'max-w-2xl'
  }[maxWidth];

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-[#070A08]/85 backdrop-blur-sm animate-in fade-in duration-200"
    >
      <div
        className="fixed inset-0"
        onClick={onClose}
        aria-hidden="true"
      />
      <div
        className={`relative w-full ${maxWidthClasses} bg-[#0C110E] border border-[#25352B] p-6 lg:p-7 rounded-[2px] shadow-[0_10px_30px_rgba(0,0,0,0.8)] z-10`}
      >
        {/* Subtle mechanical corner tags */}
        <div className="absolute top-0 right-0 w-2.5 h-2.5 border-t border-r border-[#38FF8E]" />
        <div className="absolute bottom-0 left-0 w-2.5 h-2.5 border-b border-l border-[#38FF8E]" />

        <div className="flex items-start justify-between pb-4 mb-5 border-b border-[#1D2A22]">
          <div>
            {eyebrow && (
              <div className="text-[10px] font-mono uppercase tracking-widest text-[#38FF8E] mb-1">
                // {eyebrow}
              </div>
            )}
            <h2 className="text-lg font-mono font-bold uppercase tracking-tight text-[#F4F8F4]">
              {title}
            </h2>
          </div>
          <button
            onClick={onClose}
            className="p-1 text-[#849188] hover:text-[#F4F8F4] transition-colors rounded-[2px] focus:outline-none focus:ring-1 focus:ring-[#38FF8E]"
            aria-label="Close dialog"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <div>{children}</div>
      </div>
    </div>
  );
}
