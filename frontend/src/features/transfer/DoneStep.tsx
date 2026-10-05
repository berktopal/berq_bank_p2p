import { useState, type RefObject } from 'react'
import { CalendarCheck, CheckCircle2, UserPlus } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { toast } from 'sonner'
import { Button, ButtonLink } from '@/components/ui/Button'
import { Input } from '@/components/ui/Field'
import { CopyButton } from '@/components/ui/primitives'
import { useContacts, useCreateContact } from '@/features/contacts/api'
import { errorMessage } from '@/lib/errors'
import { formatDate, formatMoney } from '@/lib/format'
import type { AccountLookup, ScheduledTransfer, Transaction } from '@/lib/types'
import type { TransferOutcome } from './TransferPage'

interface Props {
  headingRef: RefObject<HTMLHeadingElement | null>
  outcome: TransferOutcome
  recipient: AccountLookup | null
  onNew: () => void
}

export function DoneStep({ outcome, ...rest }: Props) {
  return outcome.kind === 'schedule' ? (
    <ScheduleDone schedule={outcome.schedule} headingRef={rest.headingRef} onNew={rest.onNew} />
  ) : (
    <TransferDone result={outcome.tx} {...rest} />
  )
}

function ScheduleDone({
  schedule,
  headingRef,
  onNew,
}: {
  schedule: ScheduledTransfer
  headingRef: RefObject<HTMLHeadingElement | null>
  onNew: () => void
}) {
  const { t } = useTranslation()
  return (
    <div className="flex flex-col items-center gap-6 text-center">
      <span className="bg-primary-soft text-primary grid size-20 place-items-center rounded-full">
        <CalendarCheck className="size-10" aria-hidden />
      </span>
      <div>
        <h2 ref={headingRef} tabIndex={-1} className="text-2xl font-bold outline-none">
          {t('transfer.scheduleSuccessTitle')}
        </h2>
        <p className="text-muted mt-2">
          {t('transfer.scheduleSuccessBody', {
            amount: formatMoney(schedule.amount, schedule.currency),
            name: schedule.toName,
            date: formatDate(schedule.nextRunDate),
          })}
        </p>
      </div>
      <div className="flex w-full flex-col gap-2 sm:flex-row">
        <ButtonLink to="/app/scheduled" variant="secondary" size="lg" className="flex-1">
          {t('transfer.viewSchedules')}
        </ButtonLink>
        <Button size="lg" className="flex-1" onClick={onNew}>
          {t('transfer.newTransfer')}
        </Button>
      </div>
    </div>
  )
}

function TransferDone({
  headingRef,
  result,
  recipient,
  onNew,
}: {
  headingRef: RefObject<HTMLHeadingElement | null>
  result: Transaction
  recipient: AccountLookup | null
  onNew: () => void
}) {
  const { t } = useTranslation()
  const contacts = useContacts()
  const createContact = useCreateContact()
  const [nickname, setNickname] = useState(result.counterparty.name.split(' ')[0] ?? '')
  const alreadySaved = contacts.data?.some((c) => c.iban === result.counterparty.iban)
  const canSave = !result.internal && !recipient?.ownAccount && !alreadySaved && !createContact.isSuccess

  return (
    <div className="flex flex-col items-center gap-6 text-center">
      <span className="bg-success-soft text-success grid size-20 place-items-center rounded-full">
        <CheckCircle2 className="size-10" aria-hidden />
      </span>
      <div>
        <h2 ref={headingRef} tabIndex={-1} className="text-2xl font-bold outline-none">
          {t('transfer.successTitle')}
        </h2>
        <p className="text-muted mt-2">
          {t('transfer.successBody', { amount: formatMoney(result.amount, result.currency), name: result.counterparty.name })}
        </p>
      </div>

      <dl className="bg-surface-2 w-full rounded-2xl p-4 text-sm">
        <div className="flex items-center justify-between gap-3">
          <dt className="text-muted">{t('transfer.reference')}</dt>
          <dd className="tabular flex items-center gap-1 font-semibold">
            {result.reference}
            <CopyButton value={result.reference} label={t('common.copy')} />
          </dd>
        </div>
        {result.balanceAfter !== undefined && (
          <div className="mt-2 flex items-center justify-between gap-3">
            <dt className="text-muted">{t('transfer.balanceAfter')}</dt>
            <dd className="tabular font-semibold">{formatMoney(result.balanceAfter, result.currency)}</dd>
          </div>
        )}
      </dl>

      {canSave && (
        <form
          className="border-border flex w-full flex-col gap-2 rounded-2xl border border-dashed p-4 text-left sm:flex-row sm:items-end"
          onSubmit={(e) => {
            e.preventDefault()
            if (!nickname.trim()) return
            createContact.mutate(
              { nickname: nickname.trim(), iban: result.counterparty.iban },
              {
                onSuccess: () => toast.success(t('transfer.contactSaved')),
                onError: (err) => toast.error(errorMessage(t, err)),
              },
            )
          }}
        >
          <label className="flex flex-1 flex-col gap-1.5 text-sm font-medium">
            {t('transfer.nickname')}
            <Input value={nickname} maxLength={40} onChange={(e) => setNickname(e.target.value)} />
          </label>
          <Button type="submit" variant="secondary" loading={createContact.isPending} icon={<UserPlus className="size-4" />}>
            {t('transfer.saveContact')}
          </Button>
        </form>
      )}

      <div className="flex w-full flex-col gap-2 sm:flex-row">
        <ButtonLink to={`/app/transactions/${result.id}`} variant="secondary" size="lg" className="flex-1">
          {t('transfer.viewReceipt')}
        </ButtonLink>
        <Button size="lg" className="flex-1" onClick={onNew}>
          {t('transfer.newTransfer')}
        </Button>
      </div>
    </div>
  )
}
