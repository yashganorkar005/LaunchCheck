import React from 'react';
import { AlertCircle } from 'lucide-react';

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  hint?: string;
  icon?: React.ReactNode;
  monospace?: boolean;
}

export function Input({
  label,
  error,
  hint,
  icon,
  monospace = false,
  className = '',
  id,
  ...props
}: InputProps) {
  const inputId = id || (label ? label.toLowerCase().replace(/\s+/g, '-') : undefined);

  return (
    <div className="w-full space-y-1.5">
      {label && (
        <label
          htmlFor={inputId}
          className="block text-xs font-mono font-medium uppercase tracking-wider text-[#C0CAC2]"
        >
          {label}
        </label>
      )}
      <div className="relative">
        {icon && (
          <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-[#849188]">
            {icon}
          </div>
        )}
        <input
          id={inputId}
          className={`w-full bg-[#0C110E] border rounded-[2px] py-2.5 text-xs text-[#F4F8F4] placeholder-[#4D5750] transition-colors duration-150 focus:outline-none ${
            icon ? 'pl-9 pr-3' : 'px-3'
          } ${
            error
              ? 'border-[#FF5A78] focus:border-[#FF5A78]'
              : 'border-[#25352B] focus:border-[#38FF8E]'
          } ${monospace ? 'font-mono' : 'font-sans'} ${className}`}
          {...props}
        />
      </div>
      {error && (
        <div className="flex items-center space-x-1.5 text-[11px] font-mono text-[#FF5A78] pt-0.5">
          <AlertCircle className="w-3 h-3 flex-shrink-0" />
          <span>{error}</span>
        </div>
      )}
      {!error && hint && (
        <p className="text-[11px] font-mono text-[#849188] pt-0.5">{hint}</p>
      )}
    </div>
  );
}
