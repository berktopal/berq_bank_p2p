import { ArrowLeftRight } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { Amount, Avatar, Badge } from '@/components/ui/primitives'
import { formatTime } from '@/lib/format'
import type { Transaction } from '@/lib/types'

export function TransactionRow({ tx, showAccount = false }: { tx: Transaction; showAccount?: boolean }) {
  const { t } = useTranslation()
  const meta = [t(`categories.${tx.category}`), tx.description].filter(Boolean).join(' · ')
  return (
    <li>
      <Link
        to={`/app/transactions/${tx.id}`}
        className="hover:bg-surface-2 focus-visible:bg-surface-2 -mx-2 flex items-center gap-3 rounded-xl px-2 py-3 transition"
      >
        {tx.internal ? (
          <span className="bg-primary-soft text-primary grid size-10 shrink-0 place-items-center rounded-full" aria-hidden>
            <ArrowLeftRight className="size-5" />
          </span>
        ) : (
          <Avatar name={tx.counterparty.name} />
        )}
        <div className="min-w-0 flex-1">
          <p className="flex items-center gap-2 truncate font-medium">
            <span className="truncate">{tx.counterparty.name}</span>
            {tx.internal && <Badge tone="primary">{t('transactions.internal')}</Badge>}
          </p>
          <p className="text-muted truncate text-sm">
            {meta}
            {showAccount && <> · {tx.account.name}</>}
          </p>
        </div>
        <div className="text-right">
          <Amount value={tx.amount} currency={tx.currency} direction={tx.direction} />
          <p className="text-muted text-xs">
            <span className="sr-only">{tx.direction === 'INCOMING' ? t('transactions.incoming') : t('transactions.outgoing')} · </span>
            {formatTime(tx.createdAt)}
          </p>
        </div>
      </Link>
    </li>
  )
}
