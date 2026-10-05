import { useEffect, type RefObject } from 'react'
import { AlertTriangle, CheckCircle2, XCircle } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Button } from '@/components/ui/Button'
import { Avatar, Skeleton } from '@/components/ui/primitives'
import { Spinner } from '@/components/ui/Spinner'
import { useAccountLookup, useAccounts } from '@/features/accounts/api'
import { useContacts } from '@/features/contacts/api'
import { cn } from '@/lib/cn'
import { errorMessage } from '@/lib/errors'
import { hasValidChecksum, isTrIbanFormat, shortIban } from '@/lib/iban'
import { IbanInput } from './IbanInput'
import type { TransferDraft } from './TransferPage'

interface Props {
  headingRef: RefObject<HTMLHeadingElement | null>
  draft: TransferDraft
  update: (patch: Partial<TransferDraft>) => void
  autoAdvance: boolean
  onNext: () => void
}

export function RecipientStep({ headingRef, draft, update, autoAdvance, onNext }: Props) {
  const { t } = useTranslation()
  const contacts = useContacts()
  const accounts = useAccounts()
  const complete = isTrIbanFormat(draft.toIban)
  const lookup = useAccountLookup(complete ? draft.toIban : null)

  // Bulunan alıcıyı taslağa yaz; IBAN değişirse temizle
  const lookupData = complete ? lookup.data : undefined
  useEffect(() => {
    if (lookupData && lookupData.iban !== draft.recipient?.iban) update({ recipient: lookupData })
    if (!lookupData && draft.recipient) update({ recipient: null })
  }, [lookupData, draft.recipient, update])

  // Kayıtlı alıcıdan/dekonttan gelindiyse doğrulama biter bitmez tutara geç
  useEffect(() => {
    if (autoAdvance && draft.recipient) onNext()
  }, [autoAdvance, draft.recipient, onNext])

  const ownAccounts = accounts.data ?? []

  return (
    <div className="flex flex-col gap-6">
      <h2 ref={headingRef} tabIndex={-1} className="text-lg font-semibold outline-none">
        {t('transfer.recipientTitle')}
      </h2>

      {(contacts.isPending || (contacts.data?.length ?? 0) > 0) && (
        <section>
          <h3 className="text-muted mb-3 text-sm font-medium">{t('transfer.savedRecipients')}</h3>
          {contacts.isPending ? (
            <div className="flex gap-3">
              {[0, 1, 2].map((i) => (
                <Skeleton key={i} className="h-16 w-40" />
              ))}
            </div>
          ) : (
            <div className="-mx-1 flex snap-x gap-3 overflow-x-auto px-1 pb-2">
              {contacts.data?.map((c) => {
                const selected = draft.toIban === c.iban
                return (
                  <button
                    key={c.id}
                    type="button"
                    aria-pressed={selected}
                    onClick={() => update({ toIban: c.iban })}
                    className={cn(
                      'flex min-w-44 shrink-0 snap-start items-center gap-3 rounded-xl border p-3 text-left transition',
                      selected ? 'border-primary bg-primary-soft' : 'border-border hover:bg-surface-2',
                    )}
                  >
                    <Avatar name={c.nickname} size="sm" />
                    <span className="min-w-0">
                      <span className="block truncate text-sm font-semibold">{c.nickname}</span>
                      <span className="text-muted tabular block truncate text-xs">{shortIban(c.iban)}</span>
                    </span>
                  </button>
                )
              })}
            </div>
          )}
        </section>
      )}

      {ownAccounts.length > 1 && (
        <section>
          <h3 className="text-muted mb-3 text-sm font-medium">{t('transfer.ownAccounts')}</h3>
          <div className="flex flex-wrap gap-2">
            {ownAccounts.map((a) => (
              <button
                key={a.id}
                type="button"
                aria-pressed={draft.toIban === a.iban}
                onClick={() => update({ toIban: a.iban })}
                className={cn(
                  'rounded-full border px-3 py-1.5 text-sm font-medium transition',
                  draft.toIban === a.iban ? 'border-primary bg-primary-soft text-primary' : 'border-border hover:bg-surface-2',
                )}
              >
                {a.name} · {a.currency}
              </button>
            ))}
          </div>
        </section>
      )}

      <section className="flex flex-col gap-2">
        <label htmlFor="to-iban" className="text-sm font-medium">
          {t('transfer.iban')}
        </label>
        <IbanInput
          id="to-iban"
          value={draft.toIban}
          onChange={(toIban) => update({ toIban })}
          placeholder={t('transfer.ibanPlaceholder')}
          aria-describedby="iban-status"
          aria-invalid={lookup.isError || undefined}
        />
        <div id="iban-status" aria-live="polite" className="min-h-6 text-sm">
          {complete && lookup.isFetching && (
            <span className="text-muted flex items-center gap-2">
              <Spinner className="size-4" /> {t('transfer.lookingUp')}
            </span>
          )}
          {complete && !lookup.isFetching && lookup.isError && (
            <span className="text-danger flex items-center gap-2">
              <XCircle className="size-4" aria-hidden /> {errorMessage(t, lookup.error)}
            </span>
          )}
          {draft.recipient && !lookup.isFetching && (
            <span className="flex flex-col gap-1">
              <span className="text-success flex items-center gap-2 font-semibold">
                <CheckCircle2 className="size-4" aria-hidden />
                {t('transfer.recipientFound', { name: draft.recipient.ownerName })} · {draft.recipient.currency}
              </span>
              {draft.recipient.ownAccount && <span className="text-muted">{t('transfer.ownAccountNote')}</span>}
            </span>
          )}
          {complete && !hasValidChecksum(draft.toIban) && !draft.recipient && !lookup.isFetching && (
            <span className="text-warning mt-1 flex items-center gap-2">
              <AlertTriangle className="size-4" aria-hidden /> {t('validation.ibanChecksum')}
            </span>
          )}
        </div>
      </section>

      <div className="flex justify-end">
        <Button size="lg" disabled={!draft.recipient} onClick={onNext}>
          {t('common.continue')}
        </Button>
      </div>
    </div>
  )
}
