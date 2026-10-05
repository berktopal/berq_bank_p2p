import { useTranslation } from 'react-i18next'
import { cn } from '@/lib/cn'
import { formatMoney } from '@/lib/format'
import type { Budget } from '@/lib/types'

const FILL = { OK: 'bg-primary', WARNING: 'bg-warning', EXCEEDED: 'bg-danger' } as const

/**
 * Bütçe göstergesi. Doluluk rengi durumu taşır ama tek bilgi kaynağı değildir:
 * yüzde, harcanan/limit ve durum etiketi metin olarak da yazılır.
 */
export function BudgetMeter({ budget, compact = false }: { budget: Budget; compact?: boolean }) {
  const { t } = useTranslation()
  const width = Math.min(budget.percent, 100)
  const label = t(`categories.${budget.category}`)
  return (
    <div>
      <div className="mb-1.5 flex items-baseline justify-between gap-3 text-sm">
        <span className="font-medium">{label}</span>
        <span className="tabular">
          <span className="font-semibold">{formatMoney(budget.spent, budget.currency)}</span>
          <span className="text-muted"> / {formatMoney(budget.monthlyLimit, budget.currency)}</span>
        </span>
      </div>
      <div
        className="bg-surface-2 h-2.5 overflow-hidden rounded-full"
        role="meter"
        aria-label={label}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={Math.min(budget.percent, 100)}
        aria-valuetext={`%${budget.percent} · ${t(`budgets.status.${budget.status}`)}`}
      >
        <div className={cn('h-full rounded-full transition-all', FILL[budget.status])} style={{ width: `${width}%` }} />
      </div>
      {!compact && (
        <div className="text-muted mt-1.5 flex justify-between text-xs">
          <span className={cn(budget.status === 'EXCEEDED' && 'text-danger font-semibold', budget.status === 'WARNING' && 'text-warning font-semibold')}>
            %{budget.percent} · {t(`budgets.status.${budget.status}`)}
          </span>
          <span>
            {budget.status === 'EXCEEDED'
              ? t('budgets.over', { amount: formatMoney(budget.spent - budget.monthlyLimit, budget.currency) })
              : t('budgets.remaining', { amount: formatMoney(budget.remaining, budget.currency) })}
          </span>
        </div>
      )}
    </div>
  )
}
