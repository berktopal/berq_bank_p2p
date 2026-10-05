import { useId } from 'react'
import { cn } from '@/lib/cn'

export function LogoMark({ className }: { className?: string }) {
  // Sayfada birden çok logo olabilir (gizli kenar çubuğu + mobil başlık); gradient id'si benzersiz olmalı
  const gradientId = `berq-logo-${useId().replace(/[^a-zA-Z0-9-]/g, '')}`
  return (
    <svg viewBox="0 0 64 64" className={cn('size-8', className)} aria-hidden>
      <defs>
        <linearGradient id={gradientId} x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#6c5ce7" />
          <stop offset="1" stopColor="#3498db" />
        </linearGradient>
      </defs>
      <rect width="64" height="64" rx="16" fill={`url(#${gradientId})`} />
      <path
        d="M20 16h14.5c6.4 0 10.5 3.3 10.5 8.6 0 3.4-1.8 5.9-4.7 7 3.9 1 6.2 3.9 6.2 7.9 0 5.9-4.6 9.5-11.6 9.5H20V16Zm8 6.5v7.4h5.4c2.6 0 4.1-1.4 4.1-3.7s-1.5-3.7-4.1-3.7H28Zm0 13.4v8.1h6c2.9 0 4.6-1.5 4.6-4s-1.7-4.1-4.6-4.1h-6Z"
        fill="#fff"
      />
    </svg>
  )
}

/** v1 arayüzündeki "Ber<q>q</q> Bank" vurgusu korunur. */
export function Logo({ className }: { className?: string }) {
  return (
    <span className={cn('inline-flex items-center gap-2.5 text-lg font-extrabold tracking-tight', className)}>
      <LogoMark />
      <span>
        Ber<span className="text-primary">q</span> Bank
      </span>
    </span>
  )
}
