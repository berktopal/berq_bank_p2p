import { useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { Pencil, Plus, ReceiptText, Send, Wallet } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { Link, useSearchParams } from 'react-router'
import { toast } from 'sonner'
import { z } from 'zod'
import { Button } from '@/components/ui/Button'
import { Dialog } from '@/components/ui/Dialog'
import { Field, Input } from '@/components/ui/Field'
import { Card, CopyButton, EmptyState, PageHeader, Segmented, Skeleton } from '@/components/ui/primitives'
import { cn } from '@/lib/cn'
import { errorMessage, fieldMessage } from '@/lib/errors'
import { formatDate, formatMoney } from '@/lib/format'
import { formatIban } from '@/lib/iban'
import type { Account, Currency } from '@/lib/types'
import { useAccounts, useOpenAccount, useRenameAccount } from './api'

const nameSchema = z.object({ name: z.string().trim().min(1, 'validation.required').max(60, 'validation.required') })

export function AccountsPage() {
  const { t } = useTranslation()
  const accounts = useAccounts()
  const [params, setParams] = useSearchParams()
  const [renaming, setRenaming] = useState<Account | null>(null)
  const openDialog = params.get('open') === '1'
  const setOpenDialog = (open: boolean) =>
    setParams(
      (p) => {
        if (open) p.set('open', '1')
        else p.delete('open')
        return p
      },
      { replace: true },
    )

  return (
    <>
      <PageHeader
        title={t('accounts.title')}
        subtitle={t('accounts.subtitle')}
        actions={
          <Button icon={<Plus className="size-4" />} onClick={() => setOpenDialog(true)}>
            {t('accounts.open')}
          </Button>
        }
      />

      {accounts.isPending ? (
        <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
          {[0, 1, 2].map((i) => (
            <Skeleton key={i} className="h-56 rounded-2xl" />
          ))}
        </div>
      ) : accounts.isError ? (
        <Card>
          <EmptyState
            icon={<Wallet />}
            title={errorMessage(t, accounts.error)}
            action={<Button onClick={() => accounts.refetch()}>{t('common.retry')}</Button>}
          />
        </Card>
      ) : accounts.data.length === 0 ? (
        <Card>
          <EmptyState icon={<Wallet />} title={t('accounts.empty')} />
        </Card>
      ) : (
        <ul className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
          {accounts.data.map((account, i) => (
            <li key={account.id}>
              <AccountCard account={account} featured={i === 0} onRename={() => setRenaming(account)} />
            </li>
          ))}
        </ul>
      )}

      <OpenAccountDialog open={openDialog} onClose={() => setOpenDialog(false)} />
      <RenameAccountDialog account={renaming} onClose={() => setRenaming(null)} />
    </>
  )
}

function AccountCard({ account, featured, onRename }: { account: Account; featured: boolean; onRename: () => void }) {
  const { t } = useTranslation()
  return (
    <article
      className={cn(
        'flex h-full flex-col justify-between gap-6 rounded-2xl p-5',
        featured ? 'brand-gradient text-white shadow-lg' : 'card',
      )}
      aria-label={account.name}
    >
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className={cn('truncate font-semibold', featured ? 'text-white' : '')}>{account.name}</p>
          <p className={cn('text-xs', featured ? 'text-white/75' : 'text-muted')}>
            {account.currency} · {t('accounts.openedAt', { date: formatDate(account.createdAt) })}
          </p>
        </div>
        <button
          type="button"
          onClick={onRename}
          className={cn('rounded-lg p-2 transition', featured ? 'text-white/80 hover:bg-white/15' : 'text-muted hover:bg-surface-2 hover:text-text')}
          aria-label={`${t('accounts.rename')}: ${account.name}`}
        >
          <Pencil className="size-4" />
        </button>
      </div>

      <div>
        <p className={cn('text-sm', featured ? 'text-white/80' : 'text-muted')}>{t('accounts.balance')}</p>
        <p className="tabular text-3xl font-bold">{formatMoney(account.balance, account.currency)}</p>
        <div className={cn('mt-2 flex items-center gap-1', featured && '[&_button]:text-white/80 [&_button:hover]:bg-white/15')}>
          <code className={cn('tabular text-sm', featured ? 'text-white/90' : 'text-muted')}>{formatIban(account.iban)}</code>
          <CopyButton value={account.iban} label={t('accounts.copyIban')} toastText={t('accounts.ibanCopied')} />
        </div>
      </div>

      <div className="flex gap-2">
        <Link
          to={`/app/transfer?from=${account.id}`}
          className={cn(
            'inline-flex h-9 flex-1 items-center justify-center gap-1.5 rounded-lg text-sm font-semibold transition',
            featured ? 'bg-white text-[#3b3fb8] hover:bg-white/90' : 'bg-primary text-on-primary hover:bg-primary-hover',
          )}
        >
          <Send className="size-4" aria-hidden /> {t('common.send')}
        </Link>
        <Link
          to={`/app/transactions?accountId=${account.id}`}
          className={cn(
            'inline-flex h-9 flex-1 items-center justify-center gap-1.5 rounded-lg text-sm font-semibold transition',
            featured ? 'bg-white/15 text-white hover:bg-white/25' : 'border-border hover:bg-surface-2 border',
          )}
        >
          <ReceiptText className="size-4" aria-hidden /> {t('accounts.history')}
        </Link>
      </div>
    </article>
  )
}

function OpenAccountDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { t } = useTranslation()
  const openAccount = useOpenAccount()
  const [currency, setCurrency] = useState<Currency>('TRY')
  const { register, handleSubmit, reset, formState } = useForm({ resolver: zodResolver(nameSchema), defaultValues: { name: '' } })

  const close = () => {
    reset()
    openAccount.reset()
    onClose()
  }

  return (
    <Dialog open={open} onClose={close} title={t('accounts.openTitle')} description={t('accounts.openBody')}>
      <form
        noValidate
        className="flex flex-col gap-5"
        onSubmit={handleSubmit(({ name }) =>
          openAccount.mutate(
            { name, currency },
            {
              onSuccess: () => {
                toast.success(t('accounts.opened'))
                close()
              },
            },
          ),
        )}
      >
        {openAccount.isError && (
          <p role="alert" className="bg-danger-soft text-danger rounded-xl px-4 py-3 text-sm">
            {errorMessage(t, openAccount.error)}
          </p>
        )}
        <Field label={t('accounts.name')} error={fieldMessage(t, formState.errors.name?.message)}>
          {({ id, describedBy, invalid }) => (
            <Input id={id} placeholder={t('accounts.namePlaceholder')} maxLength={60} aria-describedby={describedBy} aria-invalid={invalid} {...register('name')} />
          )}
        </Field>
        <div className="flex flex-col gap-1.5">
          <span className="text-sm font-medium" id="currency-label">
            {t('accounts.currency')}
          </span>
          <Segmented
            label={t('accounts.currency')}
            value={currency}
            onChange={setCurrency}
            options={(['TRY', 'USD', 'EUR'] as const).map((c) => ({ value: c, label: c }))}
            className="self-start"
          />
        </div>
        <div className="flex justify-end gap-2">
          <Button variant="secondary" onClick={close}>
            {t('common.cancel')}
          </Button>
          <Button type="submit" loading={openAccount.isPending}>
            {t('accounts.open')}
          </Button>
        </div>
      </form>
    </Dialog>
  )
}

function RenameAccountDialog({ account, onClose }: { account: Account | null; onClose: () => void }) {
  const { t } = useTranslation()
  const rename = useRenameAccount()
  const { register, handleSubmit, formState } = useForm({
    resolver: zodResolver(nameSchema),
    values: { name: account?.name ?? '' },
  })
  return (
    <Dialog open={account !== null} onClose={onClose} title={t('accounts.rename')}>
      <form
        noValidate
        className="flex flex-col gap-5"
        onSubmit={handleSubmit(({ name }) =>
          account &&
          rename.mutate(
            { id: account.id, name },
            {
              onSuccess: () => {
                toast.success(t('accounts.renamed'))
                onClose()
              },
              onError: (e) => toast.error(errorMessage(t, e)),
            },
          ),
        )}
      >
        <Field label={t('accounts.name')} error={fieldMessage(t, formState.errors.name?.message)}>
          {({ id, describedBy, invalid }) => (
            <Input id={id} maxLength={60} aria-describedby={describedBy} aria-invalid={invalid} {...register('name')} />
          )}
        </Field>
        <div className="flex justify-end gap-2">
          <Button variant="secondary" onClick={onClose}>
            {t('common.cancel')}
          </Button>
          <Button type="submit" loading={rename.isPending}>
            {t('common.save')}
          </Button>
        </div>
      </form>
    </Dialog>
  )
}
