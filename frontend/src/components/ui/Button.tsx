import React from 'react';
import { Loader2 } from 'lucide-react';

export type ButtonVariant = 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger';
export type ButtonSize = 'sm' | 'md' | 'lg';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  isLoading?: boolean;
  icon?: React.ReactNode;
}

export function Button({
  children,
  variant = 'primary',
  size = 'md',
  isLoading = false,
  icon,
  className = '',
  disabled,
  ...props
}: ButtonProps) {
  const baseClasses =
    'relative inline-flex items-center justify-center font-mono font-medium transition-all duration-150 focus:outline-none focus:ring-1 focus:ring-[#38FF8E] disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer select-none rounded-[2px]';

  const sizeClasses = {
    sm: 'text-xs px-3 py-1.5 gap-1.5',
    md: 'text-xs px-4 py-2.5 gap-2',
    lg: 'text-sm px-6 py-3.5 gap-2.5 font-semibold'
  }[size];

  const variantClasses = {
    primary:
      'bg-[#38FF8E] text-[#070A08] font-bold hover:bg-[#2CE67C] active:bg-[#23C96A] shadow-[0_0_15px_rgba(56,255,142,0.25)] hover:shadow-[0_0_20px_rgba(56,255,142,0.4)]',
    secondary:
      'bg-[#111913] text-[#F4F8F4] border border-[#25352B] hover:border-[#38FF8E]/50 hover:bg-[#162019]',
    outline:
      'bg-transparent text-[#38FF8E] border border-[#38FF8E]/40 hover:bg-[#38FF8E]/10 hover:border-[#38FF8E]',
    ghost:
      'bg-transparent text-[#C0CAC2] hover:text-[#F4F8F4] hover:bg-[#111913]',
    danger:
      'bg-[#FF5A78]/10 text-[#FF5A78] border border-[#FF5A78]/40 hover:bg-[#FF5A78]/20'
  }[variant];

  return (
    <button
      className={`${baseClasses} ${sizeClasses} ${variantClasses} ${className}`}
      disabled={disabled || isLoading}
      {...props}
    >
      {isLoading ? (
        <Loader2 className="w-3.5 h-3.5 animate-spin text-current" />
      ) : (
        icon && <span className="flex-shrink-0">{icon}</span>
      )}
      <span>{children}</span>
    </button>
  );
}
