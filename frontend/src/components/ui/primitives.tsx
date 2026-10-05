import { useState, type CSSProperties, type HTMLAttributes, type ReactNode } from 'react'
import { Check, Copy } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { toast } from 'sonner'
import { cn } from '@/lib/cn'
import { formatMoney, initials } from '@/lib/format'

export function Card({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cn('card', className)} {...props} />
}

export function CardHeader({ title, action, className }: { title: ReactNode; action?: ReactNode; className?: string }) {
  return (
    <div className={cn('flex items-center justify-between gap-3', className)}>
      <h2 className="text-base font-semibold">{title}</h2>
      {action}
    </div>
  )
}

export function PageHeader({ title, subtitle, actions }: { title: ReactNode; subtitle?: ReactNode; actions?: ReactNode }) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 className="text-2xl font-bold tracking-tight sm:text-[1.75rem]">{title}</h1>
        {subtitle && <p className="text-muted mt-1">{subtitle}</p>}
      </div>
      {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
    </div>
  )
}

type Tone = 'neutral' | 'success' | 'danger' | 'warning' | 'primary'
const tones: Record<Tone, string> = {
  neutral: 'bg-surface-2 text-muted',
  success: 'bg-success-soft text-success',
  danger: 'bg-danger-soft text-danger',
  warning: 'bg-warning-soft text-warning',
  primary: 'bg-primary-soft text-primary',
}

export function Badge({ tone = 'neutral', className, children }: { tone?: Tone; className?: string; children: ReactNode }) {
  return (
    <span className={cn('inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold', tones[tone], className)}>
      {children}
    </span>
  )
}

export function Skeleton({ className }: { className?: string }) {
  return <div className={cn('bg-surface-2 animate-pulse rounded-lg', className)} aria-hidden />
}

export function EmptyState({
  icon,
  title,
  hint,
  action,
}: {
  icon: ReactNode
  title: ReactNode
  hint?: ReactNode
  action?: ReactNode
}) {
  return (
    <div className="flex flex-col items-center gap-3 px-6 py-12 text-center">
      <div className="bg-primary-soft text-primary grid size-14 place-items-center rounded-2xl [&>svg]:size-7">{icon}</div>
      <p className="font-semibold">{title}</p>
      {hint && <p className="text-muted max-w-sm text-sm">{hint}</p>}
      {action}
    </div>
  )
}

/** İsimden türetilen, temaya uyumlu sabit renkli baş harf avatarı. */
const avatarHues = [212, 248, 160, 28, 330, 190, 268, 100]
export function Avatar({ name, size = 'md' }: { name: string; size?: 'sm' | 'md' | 'lg' }) {
  let hash = 0
  for (const ch of name) hash = (hash * 31 + ch.charCodeAt(0)) >>> 0
  const hue = avatarHues[hash % avatarHues.length]
  const dims = { sm: 'size-8 text-xs', md: 'size-10 text-sm', lg: 'size-14 text-lg' }[size]
  return (
    <span
      aria-hidden
      className={cn(
        'grid shrink-0 place-items-center rounded-full font-semibold',
        'bg-[hsl(var(--hue)_70%_50%/0.15)] text-[hsl(var(--hue)_60%_38%)] dark:text-[hsl(var(--hue)_75%_75%)]',
        dims,
      )}
      style={{ '--hue': hue } as CSSProperties}
    >
      {initials(name)}
    </span>
  )
}

/** Gelen/giden tutarı işaret ve renkle gösterir (renk tek bilgi taşıyıcısı değildir: işaret de var). */
export function Amount({
  value,
  currency,
  direction,
  className,
}: {
  value: number
  currency: string
  direction?: 'INCOMING' | 'OUTGOING' | 'NEUTRAL'
  className?: string
}) {
  const signed = direction === 'INCOMING' ? value : direction === 'OUTGOING' ? -value : value
  return (
    <span
      className={cn(
        'tabular font-semibold whitespace-nowrap',
        direction === 'INCOMING' && 'text-success',
        className,
      )}
    >
      {formatMoney(signed, currency, { signed: direction === 'INCOMING' || direction === 'OUTGOING' })}
    </span>
  )
}

export function CopyButton({ value, label, toastText }: { value: string; label: string; toastText?: string }) {
  const { t } = useTranslation()
  const [copied, setCopied] = useState(false)
  return (
    <button
      type="button"
      onClick={async () => {
        try {
          await navigator.clipboard.writeText(value)
          setCopied(true)
          toast.success(toastText ?? t('common.copied'))
          setTimeout(() => setCopied(false), 1500)
        } catch {
          // pano izni yoksa sessizce geç
        }
      }}
      className="text-muted hover:bg-surface-2 hover:text-text inline-grid size-8 place-items-center rounded-lg transition"
      aria-label={label}
      title={label}
    >
      {copied ? <Check className="text-success size-4" /> : <Copy className="size-4" />}
    </button>
  )
}

interface SegmentedProps<T extends string> {
  value: T
  onChange: (value: T) => void
  options: { value: T; label: ReactNode }[]
  label: string
  className?: string
}

/** Erişilebilir tekli seçim (radio group) — filtreler ve dönem seçimi için. */
export function Segmented<T extends string>({ value, onChange, options, label, className }: SegmentedProps<T>) {
  return (
    <div role="radiogroup" aria-label={label} className={cn('bg-surface-2 inline-flex rounded-xl p-1', className)}>
      {options.map((o) => (
        <button
          key={o.value}
          type="button"
          role="radio"
          aria-checked={value === o.value}
          onClick={() => onChange(o.value)}
          className={cn(
            'rounded-lg px-3 py-1.5 text-sm font-medium transition',
            value === o.value ? 'bg-surface text-text shadow-sm' : 'text-muted hover:text-text',
          )}
        >
          {o.label}
        </button>
      ))}
    </div>
  )
}
