import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { toast } from 'sonner'
import { Button, ButtonLink } from '@/components/ui/Button'
import { Dialog } from '@/components/ui/Dialog'
import { useAccounts } from '@/features/accounts/api'
import { cn } from '@/lib/cn'
import { errorMessage } from '@/lib/errors'
import { formatMoney } from '@/lib/format'
import { formatIban, shortIban } from '@/lib/iban'
import type { PaymentRequest } from '@/lib/types'
import { usePayRequest } from './api'

/** Gelen isteği öderken yalnızca aynı para birimindeki ve bakiyesi yeten hesaplar seçilebilir. */
export function PayRequestDialog({ request, onClose }: { request: PaymentRequest | null; onClose: () => void }) {
  const { t } = useTranslation()
  const accounts = useAccounts()
  const pay = usePayRequest()
  const [chosen, setChosen] = useState<number | null>(null)

  const eligible = (accounts.data ?? []).filter((a) => request && a.currency === request.currency)
  const fallback = eligible.find((a) => request && a.balance >= request.amount) ?? eligible[0]
  const fromId = chosen ?? fallback?.id ?? null

  const close = () => {
    setChosen(null)
    pay.reset()
    onClose()
  }

  return (
    <Dialog
      open={request !== null}
      onClose={close}
      title={t('requests.payTitle')}
      description={
        request
          ? t('requests.payBody', {
              amount: formatMoney(request.amount, request.currency),
              name: request.counterpartyName,
              iban: formatIban(request.requesterIban),
            })
          : undefined
      }
      footer={
        request && (
          <>
            <Button variant="secondary" onClick={close}>
              {t('common.cancel')}
            </Button>
            <Button
              variant="brand"
              disabled={fromId === null}
              loading={pay.isPending}
              onClick={() =>
                fromId !== null &&
                pay.mutate(
                  { id: request.id, fromAccountId: fromId },
                  {
                    onSuccess: () => {
                      toast.success(t('requests.paid'))
                      close()
                    },
                  },
                )
              }
            >
              {t('requests.pay')} · {formatMoney(request.amount, request.currency)}
            </Button>
          </>
        )
      }
    >
      {request && (
        <fieldset className="flex flex-col gap-2">
          <legend className="mb-2 text-sm font-medium">{t('requests.payFrom')}</legend>
          {eligible.length === 0 ? (
            <div className="bg-warning-soft text-warning flex flex-col items-start gap-3 rounded-xl p-4 text-sm">
              {t('transfer.noMatchingAccount', { currency: request.currency })}
              <ButtonLink to="/app/accounts?open=1" size="sm" variant="secondary">
                {t('accounts.open')}
              </ButtonLink>
            </div>
          ) : (
            eligible.map((a) => (
              <label
                key={a.id}
                className={cn(
                  'flex cursor-pointer items-center justify-between gap-3 rounded-xl border p-3 transition',
                  fromId === a.id ? 'border-primary bg-primary-soft' : 'border-border hover:bg-surface-2',
                )}
              >
                <span className="flex items-center gap-3">
                  <input
                    type="radio"
                    name="pay-from"
                    className="accent-primary size-4"
                    checked={fromId === a.id}
                    onChange={() => setChosen(a.id)}
                  />
                  <span>
                    <span className="block text-sm font-semibold">{a.name}</span>
                    <span className="text-muted tabular block text-xs">{shortIban(a.iban)}</span>
                  </span>
                </span>
                <span className="tabular text-sm font-semibold">{formatMoney(a.balance, a.currency)}</span>
              </label>
            ))
          )}
          {pay.isError && (
            <p role="alert" className="bg-danger-soft text-danger mt-2 rounded-xl px-4 py-3 text-sm">
              {errorMessage(t, pay.error, request.currency)}
            </p>
          )}
        </fieldset>
      )}
    </Dialog>
  )
}
