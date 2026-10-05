import { useState } from 'react'
import { ArrowDownLeft, ArrowUpRight, BarChart3, HandCoins, PiggyBank, Plus, ReceiptText, Send, UserPlus, Wallet } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { Button, ButtonLink } from '@/components/ui/Button'
import { Avatar, Card, CardHeader, EmptyState, Skeleton } from '@/components/ui/primitives'
import { totalsByCurrency, useAccounts } from '@/features/accounts/api'
import { CashflowChart, SeriesLegend } from '@/features/analytics/charts'
import { useAnalytics } from '@/features/analytics/api'
import { useBudgets } from '@/features/budgets/api'
import { BudgetMeter } from '@/features/budgets/BudgetMeter'
import { usePaymentRequests } from '@/features/requests/api'
import { PayRequestDialog } from '@/features/requests/PayRequestDialog'
import type { PaymentRequest } from '@/lib/types'
import { useMe } from '@/features/auth/api'
import { useContacts } from '@/features/contacts/api'
import { useTransactions } from '@/features/transactions/api'
import { TransactionRow } from '@/features/transactions/TransactionRow'
import { formatDate, formatMoney } from '@/lib/format'
import { shortIban } from '@/lib/iban'

function greetingKey() {
  const h = new Date().getHours()
  return h < 12 ? 'dashboard.greeting_morning' : h < 18 ? 'dashboard.greeting_afternoon' : 'dashboard.greeting_evening'
}

export function DashboardPage() {
  const { t } = useTranslation()
  const { data: user } = useMe()
  const accounts = useAccounts()
  const contacts = useContacts()
  const recent = useTransactions({ size: 6 })
  const primary = accounts.data?.find((a) => a.currency === 'TRY') ?? accounts.data?.[0]
  const analytics = useAnalytics(primary?.id, 6)
  const thisMonth = analytics.data?.monthly.at(-1)
  const incoming = usePaymentRequests('IN', 'PENDING')
  const budgets = useBudgets()
  const [paying, setPaying] = useState<PaymentRequest | null>(null)
  const pendingRequests = incoming.data?.content ?? []

  return (
    <div className="flex flex-col gap-6">
      <div>
        <p className="text-muted text-sm">{formatDate(new Date().toISOString(), 'long')}</p>
        <h1 className="text-2xl font-bold tracking-tight sm:text-[1.75rem]">
          {t(greetingKey() as 'dashboard.greeting_morning', { name: user?.firstName })}
        </h1>
      </div>

      {/* Bakiye kahraman kartı */}
      <section className="brand-gradient relative overflow-hidden rounded-3xl p-6 text-white shadow-lg sm:p-8">
        <div className="absolute -top-20 -right-16 size-64 rounded-full bg-white/10" aria-hidden />
        <div className="relative flex flex-wrap items-end justify-between gap-6">
          <div>
            <p className="text-sm font-medium text-white/80">{t('dashboard.totalBalance')}</p>
            {accounts.isPending ? (
              <Skeleton className="mt-2 h-12 w-56 bg-white/20" />
            ) : (
              <div className="mt-1 flex flex-col">
                {totalsByCurrency(accounts.data ?? []).map(({ currency, total }, i) => (
                  <p key={currency} className={i === 0 ? 'tabular text-4xl font-bold sm:text-5xl' : 'tabular text-lg font-semibold text-white/90'}>
                    {formatMoney(total, currency)}
                  </p>
                ))}
              </div>
            )}
            <p className="mt-2 text-sm text-white/80">{t('dashboard.acrossAccounts', { count: accounts.data?.length ?? 0 })}</p>
          </div>
          <div className="flex gap-2">
            <Link
              to="/app/transfer"
              className="inline-flex h-11 items-center gap-2 rounded-xl bg-white px-4 text-sm font-semibold text-[#3b3fb8] shadow-sm transition hover:bg-white/90"
            >
              <Send className="size-4" aria-hidden /> {t('nav.transfer')}
            </Link>
            <Link
              to="/app/accounts"
              className="inline-flex h-11 items-center gap-2 rounded-xl bg-white/15 px-4 text-sm font-semibold text-white transition hover:bg-white/25"
            >
              <Wallet className="size-4" aria-hidden /> {t('nav.accounts')}
            </Link>
          </div>
        </div>
      </section>

      {pendingRequests.length > 0 && (
        <Card className="border-warning/40 p-5">
          <CardHeader
            title={
              <span className="inline-flex items-center gap-2">
                <HandCoins className="text-warning size-5" aria-hidden /> {t('dashboard.pendingRequests')}
              </span>
            }
            action={
              <Link to="/app/requests?tab=IN" className="text-primary text-sm font-semibold hover:underline">
                {t('common.seeAll')}
              </Link>
            }
          />
          <ul className="mt-3 flex flex-col gap-2">
            {pendingRequests.slice(0, 3).map((r) => (
              <li key={r.id} className="bg-surface-2 flex flex-wrap items-center gap-3 rounded-xl p-3">
                <Avatar name={r.counterpartyName} size="sm" />
                <p className="min-w-0 flex-1 text-sm">
                  <span className="font-semibold">
                    {t('dashboard.requestFrom', { name: r.counterpartyName, amount: formatMoney(r.amount, r.currency) })}
                  </span>
                  {r.description && <span className="text-muted"> · {r.description}</span>}
                </p>
                <Button size="sm" onClick={() => setPaying(r)}>
                  {t('requests.pay')}
                </Button>
              </li>
            ))}
          </ul>
        </Card>
      )}

      <div className="grid gap-6 lg:grid-cols-3">
        {/* Bu ay gelen / giden */}
        <Card className="p-5">
          <CardHeader title={t('dashboard.thisMonth')} />
          <div className="mt-4 grid grid-cols-2 gap-3">
            <div className="bg-surface-2 rounded-xl p-3">
              <p className="text-muted flex items-center gap-1.5 text-sm">
                <ArrowDownLeft className="text-series-1 size-4" aria-hidden /> {t('dashboard.incoming')}
              </p>
              {analytics.isPending ? (
                <Skeleton className="mt-1 h-6 w-24" />
              ) : (
                <p className="tabular mt-1 text-lg font-semibold">{formatMoney(thisMonth?.incoming ?? 0, primary?.currency ?? 'TRY')}</p>
              )}
            </div>
            <div className="bg-surface-2 rounded-xl p-3">
              <p className="text-muted flex items-center gap-1.5 text-sm">
                <ArrowUpRight className="text-series-2 size-4" aria-hidden /> {t('dashboard.outgoing')}
              </p>
              {analytics.isPending ? (
                <Skeleton className="mt-1 h-6 w-24" />
              ) : (
                <p className="tabular mt-1 text-lg font-semibold">{formatMoney(thisMonth?.outgoing ?? 0, primary?.currency ?? 'TRY')}</p>
              )}
            </div>
          </div>

          <h3 className="mt-6 text-sm font-semibold">{t('dashboard.quickActions')}</h3>
          <div className="mt-3 grid grid-cols-4 gap-2">
            {[
              { to: '/app/transfer', icon: Send, label: t('common.send') },
              { to: '/app/accounts?open=1', icon: Plus, label: t('dashboard.openAccount') },
              { to: '/app/transactions', icon: ReceiptText, label: t('nav.transactions') },
              { to: '/app/analytics', icon: BarChart3, label: t('nav.analytics') },
            ].map(({ to, icon: Icon, label }) => (
              <Link key={to} to={to} className="hover:bg-surface-2 flex flex-col items-center gap-1.5 rounded-xl p-2 text-center text-xs font-medium">
                <span className="bg-primary-soft text-primary grid size-11 place-items-center rounded-xl">
                  <Icon className="size-5" aria-hidden />
                </span>
                {label}
              </Link>
            ))}
          </div>
        </Card>

        {/* Nakit akışı grafiği */}
        <Card className="p-5 lg:col-span-2">
          <CardHeader title={t('dashboard.cashflow')} action={<SeriesLegend />} />
          <div className="mt-4">
            {analytics.data ? (
              <div className={analytics.isFetching ? 'opacity-60 transition' : 'transition'}>
                <CashflowChart data={analytics.data.monthly} currency={analytics.data.currency} height={220} />
              </div>
            ) : (
              <Skeleton className="h-[220px] w-full" />
            )}
          </div>
        </Card>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="flex flex-col gap-6">
        {/* Hızlı gönder */}
        <Card className="p-5">
          <CardHeader
            title={t('dashboard.quickSend')}
            action={
              <Link to="/app/contacts" className="text-primary text-sm font-semibold hover:underline">
                {t('common.seeAll')}
              </Link>
            }
          />
          {contacts.isPending ? (
            <div className="mt-4 flex gap-4">
              {[0, 1, 2].map((i) => (
                <Skeleton key={i} className="size-14 rounded-full" />
              ))}
            </div>
          ) : contacts.data?.length ? (
            <ul className="mt-4 flex flex-wrap gap-4">
              {contacts.data.slice(0, 6).map((c) => (
                <li key={c.id}>
                  <Link
                    to={`/app/transfer?to=${c.iban}`}
                    className="hover:bg-surface-2 flex w-16 flex-col items-center gap-1.5 rounded-xl p-1 text-center"
                    title={`${c.ownerName} · ${shortIban(c.iban)}`}
                  >
                    <Avatar name={c.nickname} size="lg" />
                    <span className="w-full truncate text-xs font-medium">{c.nickname}</span>
                  </Link>
                </li>
              ))}
            </ul>
          ) : (
            <div className="mt-4 flex flex-col items-start gap-3">
              <p className="text-muted text-sm">{t('dashboard.noContacts')}</p>
              <ButtonLink to="/app/contacts?add=1" variant="secondary" size="sm" icon={<UserPlus className="size-4" />}>
                {t('dashboard.addContact')}
              </ButtonLink>
            </div>
          )}
        </Card>

        {/* Bütçeler */}
        <Card className="p-5">
          <CardHeader
            title={t('dashboard.budgetsTitle')}
            action={
              <Link to="/app/budgets" className="text-primary text-sm font-semibold hover:underline">
                {t('common.seeAll')}
              </Link>
            }
          />
          {budgets.isPending ? (
            <Skeleton className="mt-4 h-20 w-full" />
          ) : budgets.data?.length ? (
            <div className="mt-4 flex flex-col gap-4">
              {[...budgets.data]
                .sort((a, b) => b.percent - a.percent)
                .slice(0, 3)
                .map((b) => (
                  <BudgetMeter key={b.id} budget={b} compact />
                ))}
            </div>
          ) : (
            <div className="mt-4 flex flex-col items-start gap-3">
              <p className="text-muted text-sm">{t('dashboard.noBudgets')}</p>
              <ButtonLink to="/app/budgets" variant="secondary" size="sm" icon={<PiggyBank className="size-4" />}>
                {t('dashboard.addBudget')}
              </ButtonLink>
            </div>
          )}
        </Card>
        </div>

        {/* Son hareketler */}
        <Card className="p-5 lg:col-span-2">
          <CardHeader
            title={t('dashboard.recent')}
            action={
              <Link to="/app/transactions" className="text-primary text-sm font-semibold hover:underline">
                {t('common.seeAll')}
              </Link>
            }
          />
          {recent.isPending ? (
            <div className="mt-4 flex flex-col gap-4">
              {[0, 1, 2, 3].map((i) => (
                <Skeleton key={i} className="h-12 w-full" />
              ))}
            </div>
          ) : recent.data?.content.length ? (
            <ul className="mt-2">
              {recent.data.content.map((tx) => (
                <TransactionRow key={tx.id} tx={tx} />
              ))}
            </ul>
          ) : (
            <EmptyState
              icon={<ReceiptText />}
              title={t('dashboard.noTransactions')}
              action={<ButtonLink to="/app/transfer">{t('nav.transfer')}</ButtonLink>}
            />
          )}
        </Card>
      </div>
      <PayRequestDialog request={paying} onClose={() => setPaying(null)} />
    </div>
  )
}
