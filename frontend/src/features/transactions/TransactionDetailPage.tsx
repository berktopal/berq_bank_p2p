import type { ReactNode } from 'react'
import { ArrowLeft, Printer, ReceiptText, Repeat, UserPlus } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router'
import { toast } from 'sonner'
import { Button, ButtonLink } from '@/components/ui/Button'
import { LogoMark } from '@/components/layout/Logo'
import { Badge, Card, CopyButton, EmptyState, Skeleton } from '@/components/ui/primitives'
import { useContacts, useCreateContact } from '@/features/contacts/api'
import { errorMessage } from '@/lib/errors'
import { formatDateTime, formatMoney } from '@/lib/format'
import { formatIban } from '@/lib/iban'
import { useTransaction } from './api'

function Row({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="border-border grid grid-cols-[minmax(0,10rem)_1fr] gap-4 border-b py-3 last:border-0">
      <dt className="text-muted text-sm">{label}</dt>
      <dd className="text-right text-sm font-medium break-words">{children}</dd>
    </div>
  )
}

/** Yazdırılabilir dekont. Yazdırırken yalnızca .print-area görünür (bkz. index.css). */
export function TransactionDetailPage() {
  const { t } = useTranslation()
  const { id } = useParams()
  const query = useTransaction(Number(id))
  const contacts = useContacts()
  const createContact = useCreateContact()

  if (query.isPending) return <Skeleton className="mx-auto h-[32rem] max-w-xl rounded-2xl" />
  if (query.isError)
    return (
      <Card className="mx-auto max-w-xl">
        <EmptyState
          icon={<ReceiptText />}
          title={errorMessage(t, query.error)}
          action={<ButtonLink to="/app/transactions">{t('receipt.backToList')}</ButtonLink>}
        />
      </Card>
    )

  const tx = query.data
  const outgoing = tx.direction === 'OUTGOING'
  const sender = outgoing ? { name: tx.account.name, iban: tx.account.iban } : tx.counterparty
  const receiver = outgoing ? tx.counterparty : { name: tx.account.name, iban: tx.account.iban }
  const saved = contacts.data?.some((c) => c.iban === tx.counterparty.iban)
  const statusTone = tx.status === 'SUCCESS' ? 'success' : tx.status === 'FAILED' ? 'danger' : 'warning'

  return (
    <div className="mx-auto max-w-xl">
      <Link to="/app/transactions" className="text-muted hover:text-text no-print mb-4 inline-flex items-center gap-1.5 text-sm font-medium">
        <ArrowLeft className="size-4" aria-hidden /> {t('receipt.backToList')}
      </Link>

      <Card className="print-area overflow-hidden p-0">
        <div className="border-border flex items-center justify-between border-b p-5">
          <div className="flex items-center gap-3">
            <LogoMark />
            <div>
              <h1 className="font-bold">{t('receipt.title')}</h1>
              <p className="text-muted tabular text-xs">{tx.reference}</p>
            </div>
          </div>
          <Badge tone={statusTone}>{t(`receipt.status.${tx.status}`)}</Badge>
        </div>

        <div className="p-5 text-center">
          <p className={outgoing ? 'tabular text-4xl font-bold' : 'tabular text-success text-4xl font-bold'}>
            {formatMoney(outgoing ? -tx.amount : tx.amount, tx.currency, { signed: true })}
          </p>
          <p className="text-muted mt-1 text-sm">{formatDateTime(tx.createdAt)}</p>
        </div>

        <dl className="px-5 pb-2">
          <Row label={t('receipt.sender')}>
            <span className="block">{sender.name}</span>
            <span className="text-muted tabular block text-xs font-normal">{formatIban(sender.iban)}</span>
          </Row>
          <Row label={t('receipt.receiver')}>
            <span className="block">{receiver.name}</span>
            <span className="text-muted tabular block text-xs font-normal">{formatIban(receiver.iban)}</span>
          </Row>
          <Row label={t('receipt.reference')}>
            <span className="tabular inline-flex items-center gap-1">
              {tx.reference}
              <span className="no-print">
                <CopyButton value={tx.reference} label={t('common.copy')} />
              </span>
            </span>
          </Row>
          <Row label={t('receipt.date')}>{formatDateTime(tx.createdAt)}</Row>
          {tx.description && <Row label={t('receipt.description')}>{tx.description}</Row>}
          <Row label={t('receipt.category')}>{t(`categories.${tx.category}`)}</Row>
          {tx.balanceAfter !== undefined && (
            <Row label={t('receipt.balanceAfter')}>{formatMoney(tx.balanceAfter, tx.currency)}</Row>
          )}
        </dl>
        <p className="text-muted bg-surface-2 px-5 py-3 text-center text-xs">{t('receipt.footer')}</p>
      </Card>

      <div className="no-print mt-4 flex flex-wrap gap-2">
        <Button variant="secondary" icon={<Printer className="size-4" />} onClick={() => window.print()}>
          {t('receipt.print')}
        </Button>
        {outgoing && (
          <ButtonLink
            variant="secondary"
            icon={<Repeat className="size-4" />}
            to={`/app/transfer?to=${tx.counterparty.iban}&from=${tx.account.id}&amount=${tx.amount}`}
          >
            {t('receipt.repeat')}
          </ButtonLink>
        )}
        {!tx.internal && !saved && !createContact.isSuccess && (
          <Button
            variant="secondary"
            icon={<UserPlus className="size-4" />}
            loading={createContact.isPending}
            onClick={() =>
              createContact.mutate(
                { nickname: tx.counterparty.name.split(' ')[0] ?? tx.counterparty.name, iban: tx.counterparty.iban },
                {
                  onSuccess: () => toast.success(t('contacts.added')),
                  onError: (e) => toast.error(errorMessage(t, e)),
                },
              )
            }
          >
            {t('receipt.saveContact')}
          </Button>
        )}
      </div>
    </div>
  )
}
