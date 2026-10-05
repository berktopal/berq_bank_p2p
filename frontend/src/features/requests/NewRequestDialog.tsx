import { useState } from 'react'
import { CheckCircle2 } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { toast } from 'sonner'
import { Button } from '@/components/ui/Button'
import { Dialog } from '@/components/ui/Dialog'
import { Field, Input, Select } from '@/components/ui/Field'
import { Spinner } from '@/components/ui/Spinner'
import { useAccountLookup, useAccounts } from '@/features/accounts/api'
import { useContacts } from '@/features/contacts/api'
import { IbanInput } from '@/features/transfer/IbanInput'
import { cn } from '@/lib/cn'
import { errorMessage } from '@/lib/errors'
import { parseAmount } from '@/lib/format'
import { isTrIbanFormat } from '@/lib/iban'
import { useCreateRequest } from './api'

/** onCreated diyaloğu kapatmaktan da sorumludur (sekme değişikliğiyle aynı URL güncellemesinde). */
export function NewRequestDialog({ open, onClose, onCreated }: { open: boolean; onClose: () => void; onCreated: () => void }) {
  const { t } = useTranslation()
  const accounts = useAccounts()
  const contacts = useContacts()
  const create = useCreateRequest()
  const [toAccountId, setToAccountId] = useState<number | null>(null)
  const [payerIban, setPayerIban] = useState('TR')
  const [amountText, setAmountText] = useState('')
  const [description, setDescription] = useState('')
  const [touched, setTouched] = useState(false)

  const complete = isTrIbanFormat(payerIban)
  const lookup = useAccountLookup(open && complete ? payerIban : null)
  const toAccount = accounts.data?.find((a) => a.id === toAccountId) ?? accounts.data?.[0]
  const amount = parseAmount(amountText)
  const amountError = amount === null || amount < 0.01 ? t('validation.amount') : null
  const payerError = lookup.data?.ownAccount ? t('errors.REQUEST_SELF') : null
  const canSubmit = Boolean(toAccount && lookup.data && !payerError && !amountError)

  const reset = () => {
    setPayerIban('TR')
    setAmountText('')
    setDescription('')
    setTouched(false)
    create.reset()
  }
  const close = () => {
    reset()
    onClose()
  }

  return (
    <Dialog open={open} onClose={close} title={t('requests.newTitle')} description={t('requests.newBody')}>
      <form
        noValidate
        className="flex flex-col gap-5"
        onSubmit={(e) => {
          e.preventDefault()
          setTouched(true)
          if (!canSubmit || !toAccount || amount === null) return
          create.mutate(
            { toAccountId: toAccount.id, payerIban, amount, description: description.trim() || undefined },
            {
              onSuccess: () => {
                toast.success(t('requests.created'))
                reset()
                onCreated()
              },
            },
          )
        }}
      >
        {create.isError && (
          <p role="alert" className="bg-danger-soft text-danger rounded-xl px-4 py-3 text-sm">
            {errorMessage(t, create.error)}
          </p>
        )}

        <div className="flex flex-col gap-2">
          <span className="text-sm font-medium">{t('requests.payer')}</span>
          {(contacts.data?.length ?? 0) > 0 && (
            <div className="flex flex-wrap gap-2">
              {contacts.data?.map((c) => (
                <button
                  key={c.id}
                  type="button"
                  aria-pressed={payerIban === c.iban}
                  onClick={() => setPayerIban(c.iban)}
                  className={cn(
                    'rounded-full border px-3 py-1 text-sm font-medium transition',
                    payerIban === c.iban ? 'border-primary bg-primary-soft text-primary' : 'border-border hover:bg-surface-2',
                  )}
                >
                  {c.nickname}
                </button>
              ))}
            </div>
          )}
          <label htmlFor="payer-iban" className="sr-only">
            {t('requests.payerIban')}
          </label>
          <IbanInput
            id="payer-iban"
            value={payerIban}
            onChange={setPayerIban}
            placeholder={t('transfer.ibanPlaceholder')}
            aria-describedby="payer-status"
          />
          <p id="payer-status" aria-live="polite" className="min-h-5 text-sm">
            {complete && lookup.isFetching && <Spinner className="text-muted size-4" />}
            {complete && lookup.isError && <span className="text-danger">{errorMessage(t, lookup.error)}</span>}
            {complete && lookup.data && !payerError && (
              <span className="text-success inline-flex items-center gap-1.5 font-medium">
                <CheckCircle2 className="size-4" aria-hidden /> {lookup.data.ownerName}
              </span>
            )}
            {payerError && <span className="text-danger">{payerError}</span>}
          </p>
        </div>

        <Field label={t('requests.toAccount')}>
          {({ id }) => (
            <Select id={id} value={toAccount?.id ?? ''} onChange={(e) => setToAccountId(Number(e.target.value))}>
              {accounts.data?.map((a) => (
                <option key={a.id} value={a.id}>
                  {a.name} · {a.currency}
                </option>
              ))}
            </Select>
          )}
        </Field>

        <Field label={t('transfer.amount')} error={touched && amountError ? amountError : undefined}>
          {({ id, describedBy, invalid }) => (
            <Input
              id={id}
              inputMode="decimal"
              placeholder="0,00"
              value={amountText}
              onChange={(e) => setAmountText(e.target.value.replace(/[^\d.,]/g, ''))}
              aria-describedby={describedBy}
              aria-invalid={invalid}
              className="tabular text-lg font-semibold"
            />
          )}
        </Field>

        <Field label={t('transfer.description')} optional>
          {({ id }) => (
            <Input
              id={id}
              maxLength={140}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder={t('transfer.descriptionPlaceholder')}
            />
          )}
        </Field>

        <div className="flex justify-end gap-2">
          <Button variant="secondary" onClick={close}>
            {t('common.cancel')}
          </Button>
          <Button type="submit" loading={create.isPending} disabled={touched && !canSubmit}>
            {t('requests.new')}
          </Button>
        </div>
      </form>
    </Dialog>
  )
}
