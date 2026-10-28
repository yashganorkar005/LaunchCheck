import React from 'react';
import { CheckCircle2, AlertTriangle, XCircle, Info, MinusCircle } from 'lucide-react';

export type BadgeStatus = 'PASS' | 'WARN' | 'FAIL' | 'SKIPPED' | 'INFO';

interface BadgeProps {
  status: BadgeStatus | string;
  label?: string;
  size?: 'sm' | 'md';
  showIcon?: boolean;
  className?: string;
}

export function Badge({ status, label, size = 'sm', showIcon = true, className = '' }: BadgeProps) {
  const normStatus = status.toUpperCase() as BadgeStatus;

  const config = {
    PASS: {
      bg: 'bg-[#38FF8E]/10',
      border: 'border-[#38FF8E]/40',
      text: 'text-[#38FF8E]',
      icon: CheckCircle2,
      defaultLabel: 'PASS'
    },
    WARN: {
      bg: 'bg-[#FFE282]/10',
      border: 'border-[#FFE282]/40',
      text: 'text-[#FFE282]',
      icon: AlertTriangle,
      defaultLabel: 'WARN'
    },
    FAIL: {
      bg: 'bg-[#FF5A78]/10',
      border: 'border-[#FF5A78]/40',
      text: 'text-[#FF5A78]',
      icon: XCircle,
      defaultLabel: 'FAIL'
    },
    SKIPPED: {
      bg: 'bg-[#849188]/10',
      border: 'border-[#849188]/40',
      text: 'text-[#849188]',
      icon: MinusCircle,
      defaultLabel: 'SKIPPED'
    },
    INFO: {
      bg: 'bg-[#65F8FF]/10',
      border: 'border-[#65F8FF]/40',
      text: 'text-[#65F8FF]',
      icon: Info,
      defaultLabel: 'INFO'
    }
  }[normStatus] || {
    bg: 'bg-[#849188]/10',
    border: 'border-[#849188]/40',
    text: 'text-[#C0CAC2]',
    icon: Info,
    defaultLabel: normStatus
  };

  const IconComponent = config.icon;
  const displayLabel = label || config.defaultLabel;

  const sizeClasses = size === 'sm'
    ? 'text-[11px] px-2 py-0.5 gap-1.5'
    : 'text-xs px-2.5 py-1 gap-2';

  return (
    <span
      className={`inline-flex items-center font-mono font-medium uppercase tracking-wider border rounded-[2px] ${config.bg} ${config.border} ${config.text} ${sizeClasses} ${className}`}
    >
      {showIcon && <IconComponent className={size === 'sm' ? 'w-3 h-3' : 'w-3.5 h-3.5'} />}
      <span>{displayLabel}</span>
    </span>
  );
}
