import type { ReactNode, RefObject } from 'react'
import { AlertTriangle, CalendarClock, ShieldCheck } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Button } from '@/components/ui/Button'
import { useAccounts } from '@/features/accounts/api'
import { budgetImpact, useBudgets } from '@/features/budgets/api'
import { useCreateSchedule } from '@/features/schedules/api'
import { useTransfer } from '@/features/transactions/api'
import { cn } from '@/lib/cn'
import { errorMessage } from '@/lib/errors'
import { formatDate, formatMoney } from '@/lib/format'
import { formatIban } from '@/lib/iban'
import type { TransferDraft, TransferOutcome } from './TransferPage'

interface Props {
  headingRef: RefObject<HTMLHeadingElement | null>
  draft: TransferDraft
  idempotencyKey: string
  onBack: () => void
  onDone: (outcome: TransferOutcome) => void
}

function Row({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="border-border flex items-start justify-between gap-4 border-b py-3 last:border-0">
      <dt className="text-muted text-sm">{label}</dt>
      <dd className="text-right text-sm font-medium">{children}</dd>
    </div>
  )
}

export function ReviewStep({ headingRef, draft, idempotencyKey, onBack, onDone }: Props) {
  const { t } = useTranslation()
  const accounts = useAccounts()
  const budgets = useBudgets()
  const transfer = useTransfer()
  const schedule = useCreateSchedule()
  const from = accounts.data?.find((a) => a.id === draft.fromAccountId)
  const recipient = draft.recipient!
  const amount = draft.amount!
  const currency = recipient.currency
  const scheduled = draft.timing !== 'NOW'
  const pending = transfer.isPending || schedule.isPending
  const error = transfer.error ?? schedule.error
  // Kendi hesaplarına virman harcama sayılmaz; bütçe uyarısı yalnızca başkasına gönderimde
  const impact = recipient.ownAccount ? null : budgetImpact(budgets.data, draft.category, currency, amount)

  const input = {
    fromAccountId: draft.fromAccountId!,
    toIban: recipient.iban,
    amount,
    description: draft.description.trim() || undefined,
    category: draft.category,
  }

  const confirm = () => {
    if (scheduled) {
      schedule.mutate(
        {
          ...input,
          frequency: draft.timing === 'LATER' ? 'ONCE' : draft.frequency,
          startDate: draft.startDate,
          endDate: draft.timing === 'RECURRING' && draft.endDate ? draft.endDate : undefined,
        },
        { onSuccess: (s) => onDone({ kind: 'schedule', schedule: s }) },
      )
    } else {
      transfer.mutate({ input, idempotencyKey }, { onSuccess: (tx) => onDone({ kind: 'transfer', tx }) })
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <h2 ref={headingRef} tabIndex={-1} className="text-lg font-semibold outline-none">
        {t('transfer.reviewTitle')}
      </h2>

      <div className="bg-surface-2 rounded-2xl p-5 text-center">
        <p className="text-muted text-sm">{t('transfer.amount')}</p>
        <p className="tabular mt-1 text-4xl font-bold">{formatMoney(amount, currency)}</p>
        {scheduled && (
          <p className="text-primary mt-2 inline-flex items-center gap-1.5 text-sm font-semibold">
            <CalendarClock className="size-4" aria-hidden />
            {draft.timing === 'LATER'
              ? t('schedules.once', { date: formatDate(draft.startDate) })
              : t(`schedules.frequency.${draft.frequency}`)}
          </p>
        )}
      </div>

      <dl>
        <Row label={t('transfer.recipient')}>
          <span className="block">{recipient.ownerName}</span>
          <span className="text-muted tabular block text-xs font-normal">{formatIban(recipient.iban)}</span>
        </Row>
        <Row label={t('transfer.from')}>
          <span className="block">{from?.name}</span>
          <span className="text-muted tabular block text-xs font-normal">{from && formatIban(from.iban)}</span>
        </Row>
        {draft.description.trim() && <Row label={t('transfer.description')}>{draft.description.trim()}</Row>}
        <Row label={t('transfer.category')}>{t(`categories.${draft.category}`)}</Row>
        {draft.timing === 'RECURRING' && (
          <>
            <Row label={t('transfer.startDate')}>{formatDate(draft.startDate)}</Row>
            {draft.endDate && <Row label={t('transfer.endDate')}>{formatDate(draft.endDate)}</Row>}
          </>
        )}
        {!scheduled && from && <Row label={t('transfer.balanceAfter')}>{formatMoney(from.balance - amount, currency)}</Row>}
      </dl>

      {impact && impact.status !== 'OK' && (
        <p
          className={cn(
            'flex items-start gap-2 rounded-xl px-4 py-3 text-sm',
            impact.status === 'EXCEEDED' ? 'bg-danger-soft text-danger' : 'bg-warning-soft text-warning',
          )}
          role="status"
        >
          <AlertTriangle className="mt-0.5 size-4 shrink-0" aria-hidden />
          {impact.status === 'EXCEEDED'
            ? t('transfer.budgetExceeded', {
                category: t(`categories.${draft.category}`),
                limit: formatMoney(impact.budget.monthlyLimit, currency),
              })
            : t('transfer.budgetWarning', {
                category: t(`categories.${draft.category}`),
                percent: Math.round((impact.after / impact.budget.monthlyLimit) * 100),
              })}
        </p>
      )}

      {error && (
        <div role="alert" className="bg-danger-soft text-danger rounded-xl px-4 py-3 text-sm font-medium">
          {errorMessage(t, error, currency)}
        </div>
      )}

      <p className="text-muted flex items-center gap-2 text-sm">
        <ShieldCheck className="text-success size-4 shrink-0" aria-hidden />
        {scheduled ? t('transfer.scheduledHint') : t('transfer.reviewHint')}
      </p>

      <div className="flex flex-col-reverse justify-between gap-2 sm:flex-row">
        <Button variant="secondary" size="lg" onClick={onBack} disabled={pending}>
          {t('common.back')}
        </Button>
        <Button variant="brand" size="lg" onClick={confirm} loading={pending}>
          {pending
            ? t('transfer.sending')
            : scheduled
              ? t('transfer.scheduleConfirm')
              : t('transfer.confirm', { amount: formatMoney(amount, currency) })}
        </Button>
      </div>
    </div>
  )
}
