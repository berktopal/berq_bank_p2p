import { useCallback, useEffect, useRef, useState } from 'react'
import { Check } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router'
import { Card, PageHeader } from '@/components/ui/primitives'
import { cn } from '@/lib/cn'
import { newIdempotencyKey } from '@/lib/id'
import { dayKey } from '@/lib/format'
import { sanitizeIbanInput } from '@/lib/iban'
import type { AccountLookup, Category, ScheduledTransfer, Transaction } from '@/lib/types'
import { AmountStep } from './AmountStep'
import { DoneStep } from './DoneStep'
import { RecipientStep } from './RecipientStep'
import { ReviewStep } from './ReviewStep'

export type Step = 'recipient' | 'amount' | 'review' | 'done'
const STEPS: Step[] = ['recipient', 'amount', 'review', 'done']

export interface TransferDraft {
  toIban: string
  recipient: AccountLookup | null
  fromAccountId: number | null
  amountText: string
  amount: number | null
  description: string
  category: Category
  /** NOW: hemen gönder; LATER: ileri tarihte bir kez; RECURRING: haftalık/aylık talimat */
  timing: Timing
  frequency: 'WEEKLY' | 'MONTHLY'
  startDate: string
  endDate: string
}

export type Timing = 'NOW' | 'LATER' | 'RECURRING'

export type TransferOutcome = { kind: 'transfer'; tx: Transaction } | { kind: 'schedule'; schedule: ScheduledTransfer }

/** İleri tarih için yarın, düzenli talimat için bugün (İstanbul saatiyle) */
function defaultStartDate(timing: Timing): string {
  if (timing === 'NOW') return ''
  return dayKey(new Date(Date.now() + (timing === 'LATER' ? 86_400_000 : 0)).toISOString())
}

const emptyDraft = (toIban = 'TR', fromAccountId: number | null = null, timing: Timing = 'NOW'): TransferDraft => ({
  toIban,
  recipient: null,
  fromAccountId,
  amountText: '',
  amount: null,
  description: '',
  category: 'GENERAL',
  timing,
  frequency: 'MONTHLY',
  startDate: defaultStartDate(timing),
  endDate: '',
})

export function TransferPage() {
  const { t } = useTranslation()
  const [params] = useSearchParams()
  const initialTo = params.get('to')
  const initialFrom = Number(params.get('from')) || null
  const initialAmount = params.get('amount')
  const initialTiming: Timing = params.get('when') === 'RECURRING' ? 'RECURRING' : params.get('when') === 'LATER' ? 'LATER' : 'NOW'

  const [step, setStep] = useState<Step>('recipient')
  const [draft, setDraft] = useState<TransferDraft>(() => ({
    ...emptyDraft(initialTo ? sanitizeIbanInput(initialTo) : 'TR', initialFrom, initialTiming),
    amountText: initialAmount ?? '',
  }))
  const [idempotencyKey, setIdempotencyKey] = useState(newIdempotencyKey)
  const [result, setResult] = useState<TransferOutcome | null>(null)
  // ?to= ile gelindiyse alıcı doğrulanınca bir kez otomatik ilerle (geri dönülünce tekrar etmez)
  const [autoAdvance, setAutoAdvance] = useState(Boolean(initialTo))
  const headingRef = useRef<HTMLHeadingElement>(null)

  // Adım değişince odak başlığa taşınır: ekran okuyucu yeni adımı duyurur
  useEffect(() => {
    headingRef.current?.focus()
  }, [step])

  const update = useCallback((patch: Partial<TransferDraft>) => setDraft((d) => ({ ...d, ...patch })), [])
  const toAmount = useCallback(() => {
    setAutoAdvance(false)
    setStep('amount')
  }, [])
  const index = STEPS.indexOf(step)

  return (
    <div className="mx-auto max-w-2xl">
      <PageHeader title={t('transfer.title')} subtitle={t('transfer.subtitle')} />

      <ol className="mb-6 flex items-center gap-2" aria-label={t('transfer.stepOf', { current: index + 1, total: STEPS.length })}>
        {STEPS.map((s, i) => (
          <li key={s} className="flex flex-1 items-center gap-2" aria-current={s === step ? 'step' : undefined}>
            <span
              className={cn(
                'grid size-7 shrink-0 place-items-center rounded-full text-xs font-bold transition',
                i < index && 'bg-success text-white',
                i === index && 'bg-primary text-on-primary',
                i > index && 'bg-surface-2 text-muted',
              )}
            >
              {i < index ? <Check className="size-4" aria-hidden /> : i + 1}
            </span>
            <span className={cn('hidden text-sm font-medium sm:inline', i === index ? 'text-text' : 'text-muted')}>
              {t(`transfer.steps.${s}`)}
            </span>
            {i < STEPS.length - 1 && <span className={cn('h-px flex-1', i < index ? 'bg-success' : 'bg-border')} aria-hidden />}
          </li>
        ))}
      </ol>

      <Card className="p-5 sm:p-7">
        {step === 'recipient' && (
          <RecipientStep
            headingRef={headingRef}
            draft={draft}
            update={update}
            autoAdvance={autoAdvance}
            onNext={toAmount}
          />
        )}
        {step === 'amount' && (
          <AmountStep
            headingRef={headingRef}
            draft={draft}
            update={update}
            onBack={() => setStep('recipient')}
            onNext={() => {
              // Yeni bir onay denemesi = yeni anahtar; aynı onay ekranındaki tekrarlar aynı anahtarı kullanır
              setIdempotencyKey(newIdempotencyKey())
              setStep('review')
            }}
          />
        )}
        {step === 'review' && (
          <ReviewStep
            headingRef={headingRef}
            draft={draft}
            idempotencyKey={idempotencyKey}
            onBack={() => setStep('amount')}
            onDone={(outcome) => {
              setResult(outcome)
              setStep('done')
            }}
          />
        )}
        {step === 'done' && result && (
          <DoneStep
            headingRef={headingRef}
            outcome={result}
            recipient={draft.recipient}
            onNew={() => {
              setDraft(emptyDraft())
              setResult(null)
              setStep('recipient')
            }}
          />
        )}
      </Card>
    </div>
  )
}
