import { useEffect, useState, type RefObject } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, ButtonLink } from '@/components/ui/Button'
import { inputStyles } from '@/components/ui/Field'
import { Avatar, Segmented, Skeleton } from '@/components/ui/primitives'
import { useAccounts } from '@/features/accounts/api'
import { cn } from '@/lib/cn'
import { dayKey, formatMoney, parseAmount } from '@/lib/format'
import { shortIban } from '@/lib/iban'
import { CATEGORIES } from '@/lib/types'
import type { Timing, TransferDraft } from './TransferPage'

interface Props {
  headingRef: RefObject<HTMLHeadingElement | null>
  draft: TransferDraft
  update: (patch: Partial<TransferDraft>) => void
  onBack: () => void
  onNext: () => void
}

const QUICK_AMOUNTS = [100, 500, 1000]

/** İstanbul saatine göre bugün ve yarın (YYYY-MM-DD) — tarih seçicilerin alt sınırı */
function istanbulDays() {
  const now = Date.now()
  return { today: dayKey(new Date(now).toISOString()), tomorrow: dayKey(new Date(now + 86_400_000).toISOString()) }
}

export function AmountStep({ headingRef, draft, update, onBack, onNext }: Props) {
  const { t } = useTranslation()
  const accounts = useAccounts()
  const [touched, setTouched] = useState(false)
  const recipient = draft.recipient!

  // Alıcıyla aynı para biriminde ve alıcı hesap olmayan hesaplar seçilebilir
  const eligible = (accounts.data ?? []).filter((a) => a.currency === recipient.currency && a.iban !== recipient.iban)
  const from = eligible.find((a) => a.id === draft.fromAccountId)

  useEffect(() => {
    if (!from && eligible.length > 0) {
      const richest = [...eligible].sort((a, b) => b.balance - a.balance)[0]!
      update({ fromAccountId: richest.id })
    }
  }, [from, eligible, update])

  const [{ today, tomorrow }] = useState(istanbulDays)
  const amount = parseAmount(draft.amountText)
  const error = !draft.amountText.trim()
    ? t('validation.required')
    : amount === null
      ? t('validation.amount')
      : amount < 0.01
        ? t('validation.amountMin')
        : // İleri tarihli talimatta bakiye o gün kontrol edilir
          draft.timing === 'NOW' && from && amount > from.balance
          ? t('validation.amountMax')
          : null
  const minStart = draft.timing === 'LATER' ? tomorrow : today
  const dateError =
    draft.timing === 'NOW'
      ? null
      : !draft.startDate || draft.startDate < minStart || (draft.timing === 'RECURRING' && draft.endDate && draft.endDate < draft.startDate)
        ? t('errors.SCHEDULE_INVALID_DATES')
        : null

  const submit = () => {
    setTouched(true)
    if (error || dateError || !from || amount === null) return
    update({ amount })
    onNext()
  }

  return (
    <form
      noValidate
      className="flex flex-col gap-6"
      onSubmit={(e) => {
        e.preventDefault()
        submit()
      }}
    >
      <h2 ref={headingRef} tabIndex={-1} className="text-lg font-semibold outline-none">
        {t('transfer.amountTitle')}
      </h2>

      <div className="bg-surface-2 flex items-center gap-3 rounded-xl p-3">
        <Avatar name={recipient.ownerName} size="sm" />
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-semibold">{recipient.ownerName}</p>
          <p className="text-muted tabular text-xs">{shortIban(recipient.iban)}</p>
        </div>
        <button type="button" onClick={onBack} className="text-primary text-sm font-semibold hover:underline">
          {t('transfer.change')}
        </button>
      </div>

      <fieldset className="flex flex-col gap-2">
        <legend className="mb-2 text-sm font-medium">{t('transfer.from')}</legend>
        {accounts.isPending ? (
          <Skeleton className="h-16 w-full" />
        ) : eligible.length === 0 ? (
          <div className="bg-warning-soft text-warning flex flex-col items-start gap-3 rounded-xl p-4 text-sm">
            {t('transfer.noMatchingAccount', { currency: recipient.currency })}
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
                draft.fromAccountId === a.id ? 'border-primary bg-primary-soft' : 'border-border hover:bg-surface-2',
              )}
            >
              <span className="flex items-center gap-3">
                <input
                  type="radio"
                  name="from"
                  className="accent-primary size-4"
                  checked={draft.fromAccountId === a.id}
                  onChange={() => update({ fromAccountId: a.id })}
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
      </fieldset>

      <div className="flex flex-col gap-2">
        <label htmlFor="amount" className="text-sm font-medium">
          {t('transfer.amount')}
        </label>
        <div className="relative">
          <input
            id="amount"
            inputMode="decimal"
            autoComplete="off"
            placeholder="0,00"
            value={draft.amountText}
            onChange={(e) => update({ amountText: e.target.value.replace(/[^\d.,]/g, '') })}
            onBlur={() => setTouched(true)}
            aria-invalid={touched && Boolean(error)}
            aria-describedby="amount-help"
            className={cn(inputStyles, 'tabular h-16 pr-16 text-3xl font-bold sm:text-3xl')}
          />
          <span className="text-muted pointer-events-none absolute inset-y-0 right-4 grid place-items-center font-semibold">
            {recipient.currency}
          </span>
        </div>
        <p id="amount-help" className={cn('text-sm', touched && error ? 'text-danger' : 'text-muted')} role={touched && error ? 'alert' : undefined}>
          {touched && error ? error : from ? t('transfer.available', { amount: formatMoney(from.balance, from.currency) }) : ' '}
        </p>
        <div className="flex flex-wrap gap-2">
          {QUICK_AMOUNTS.map((v) => (
            <button
              key={v}
              type="button"
              onClick={() => update({ amountText: String(v) })}
              className="border-border hover:bg-surface-2 tabular rounded-full border px-3 py-1 text-sm font-medium"
            >
              {formatMoney(v, recipient.currency)}
            </button>
          ))}
          {from && from.balance > 0 && (
            <button
              type="button"
              onClick={() => update({ amountText: String(from.balance) })}
              className="border-border hover:bg-surface-2 rounded-full border px-3 py-1 text-sm font-medium"
            >
              {t('transfer.all')}
            </button>
          )}
        </div>
      </div>

      <div className="flex flex-col gap-2">
        <label htmlFor="description" className="text-sm font-medium">
          {t('transfer.description')} <span className="text-muted font-normal">({t('common.optional')})</span>
        </label>
        <input
          id="description"
          maxLength={140}
          value={draft.description}
          onChange={(e) => update({ description: e.target.value })}
          placeholder={t('transfer.descriptionPlaceholder')}
          className={inputStyles}
        />
      </div>

      <fieldset>
        <legend className="mb-2 text-sm font-medium">{t('transfer.category')}</legend>
        <div className="flex flex-wrap gap-2">
          {CATEGORIES.map((c) => (
            <label
              key={c}
              className={cn(
                'cursor-pointer rounded-full border px-3 py-1.5 text-sm font-medium transition has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-[var(--ring)]',
                draft.category === c ? 'border-primary bg-primary-soft text-primary' : 'border-border hover:bg-surface-2',
              )}
            >
              <input type="radio" name="category" value={c} checked={draft.category === c} onChange={() => update({ category: c })} className="sr-only" />
              {t(`categories.${c}`)}
            </label>
          ))}
        </div>
      </fieldset>

      <fieldset className="flex flex-col gap-3">
        <legend className="mb-2 text-sm font-medium">{t('transfer.when')}</legend>
        <Segmented<Timing>
          label={t('transfer.when')}
          value={draft.timing}
          onChange={(timing) => update({ timing, startDate: timing === 'NOW' ? '' : draft.startDate || (timing === 'LATER' ? tomorrow : today) })}
          options={[
            { value: 'NOW', label: t('transfer.whenNow') },
            { value: 'LATER', label: t('transfer.whenLater') },
            { value: 'RECURRING', label: t('transfer.whenRecurring') },
          ]}
          className="self-start"
        />
        {draft.timing === 'RECURRING' && (
          <Segmented<'WEEKLY' | 'MONTHLY'>
            label={t('transfer.frequency')}
            value={draft.frequency}
            onChange={(frequency) => update({ frequency })}
            options={[
              { value: 'WEEKLY', label: t('schedules.frequency.WEEKLY') },
              { value: 'MONTHLY', label: t('schedules.frequency.MONTHLY') },
            ]}
            className="self-start"
          />
        )}
        {draft.timing !== 'NOW' && (
          <div className="grid gap-3 sm:grid-cols-2">
            <label className="flex flex-col gap-1.5 text-sm font-medium">
              {draft.timing === 'LATER' ? t('transfer.date') : t('transfer.startDate')}
              <input
                type="date"
                min={minStart}
                value={draft.startDate}
                onChange={(e) => update({ startDate: e.target.value })}
                className={inputStyles}
                aria-invalid={touched && Boolean(dateError)}
              />
            </label>
            {draft.timing === 'RECURRING' && (
              <label className="flex flex-col gap-1.5 text-sm font-medium">
                <span>
                  {t('transfer.endDate')} <span className="text-muted font-normal">({t('common.optional')})</span>
                </span>
                <input
                  type="date"
                  min={draft.startDate || today}
                  value={draft.endDate}
                  onChange={(e) => update({ endDate: e.target.value })}
                  className={inputStyles}
                />
              </label>
            )}
          </div>
        )}
        {draft.timing !== 'NOW' && (
          <p className={cn('text-sm', touched && dateError ? 'text-danger' : 'text-muted')} role={touched && dateError ? 'alert' : undefined}>
            {touched && dateError ? dateError : t('transfer.scheduledHint')}
          </p>
        )}
      </fieldset>

      <div className="flex justify-between gap-2">
        <Button variant="secondary" size="lg" onClick={onBack}>
          {t('common.back')}
        </Button>
        <Button type="submit" size="lg" disabled={eligible.length === 0}>
          {t('common.continue')}
        </Button>
      </div>
    </form>
  )
}
