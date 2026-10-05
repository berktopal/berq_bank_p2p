import { useEffect, useMemo, useState } from 'react'
import { ChevronLeft, ChevronRight, Download, ReceiptText, Search, X } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router'
import { Button, buttonStyles } from '@/components/ui/Button'
import { Input, Select, inputStyles } from '@/components/ui/Field'
import { Card, EmptyState, PageHeader, Segmented, Skeleton } from '@/components/ui/primitives'
import { useAccounts } from '@/features/accounts/api'
import { useDebouncedValue } from '@/hooks/useDebouncedValue'
import { cn } from '@/lib/cn'
import { errorMessage } from '@/lib/errors'
import { dayKey, formatDate } from '@/lib/format'
import { CATEGORIES, type Category, type Direction, type Transaction, type TransactionFilters } from '@/lib/types'
import { exportUrl, useTransactions } from './api'
import { TransactionRow } from './TransactionRow'

const PAGE_SIZE = 20

/** Filtre durumu URL'de tutulur: link paylaşılabilir, geri tuşu ve yenileme filtreyi korur. */
function useFilterParams() {
  const [params, setParams] = useSearchParams()
  const filters: TransactionFilters = {
    accountId: Number(params.get('accountId')) || undefined,
    direction: (params.get('direction') as Direction | null) ?? undefined,
    category: (params.get('category') as Category | null) ?? undefined,
    from: params.get('from') ?? undefined,
    to: params.get('to') ?? undefined,
    q: params.get('q') ?? undefined,
    page: Number(params.get('page')) || 0,
    size: PAGE_SIZE,
  }
  const set = (patch: Partial<TransactionFilters>) =>
    setParams(
      (p) => {
        for (const [k, v] of Object.entries(patch)) {
          if (v === undefined || v === '' || v === null) p.delete(k)
          else p.set(k, String(v))
        }
        // Filtre değişince ilk sayfaya dön
        if (!('page' in patch)) p.delete('page')
        return p
      },
      { replace: !('page' in patch) },
    )
  const clear = () => setParams({}, { replace: true })
  return { filters, set, clear }
}

function todayAndYesterday() {
  const now = Date.now()
  return { today: dayKey(new Date(now).toISOString()), yesterday: dayKey(new Date(now - 86_400_000).toISOString()) }
}

function groupByDay(items: Transaction[]) {
  const groups: { day: string; items: Transaction[] }[] = []
  for (const tx of items) {
    const day = dayKey(tx.createdAt)
    const last = groups.at(-1)
    if (last?.day === day) last.items.push(tx)
    else groups.push({ day, items: [tx] })
  }
  return groups
}

export function TransactionsPage() {
  const { t } = useTranslation()
  const { filters, set, clear } = useFilterParams()
  const accounts = useAccounts()
  const [search, setSearch] = useState(filters.q ?? '')
  const debounced = useDebouncedValue(search, 350)

  useEffect(() => {
    if ((debounced || undefined) !== filters.q) set({ q: debounced || undefined })
    // eslint-disable-next-line react-hooks/exhaustive-deps -- yalnızca arama metni değişince URL'e yaz
  }, [debounced])

  const query = useTransactions(filters)
  const groups = useMemo(() => groupByDay(query.data?.content ?? []), [query.data])
  const hasFilters = Boolean(filters.accountId || filters.direction || filters.category || filters.from || filters.to || filters.q)
  const [{ today, yesterday }] = useState(todayAndYesterday)
  const dayLabel = (day: string) =>
    day === today ? t('transactions.today') : day === yesterday ? t('transactions.yesterday') : formatDate(`${day}T12:00:00Z`, 'long')

  return (
    <>
      <PageHeader
        title={t('transactions.title')}
        subtitle={t('transactions.subtitle')}
        actions={
          <a href={exportUrl(filters)} className={buttonStyles('secondary', 'md')} download>
            <Download className="size-4" aria-hidden /> {t('transactions.export')}
          </a>
        }
      />

      <Card className="mb-6 p-4">
        <div className="flex flex-col gap-3">
          <div className="relative">
            <Search className="text-muted pointer-events-none absolute top-1/2 left-3.5 size-4 -translate-y-1/2" aria-hidden />
            <Input
              type="search"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder={t('transactions.searchPlaceholder')}
              aria-label={t('common.search')}
              className="pl-10"
            />
          </div>
          <div className="flex flex-wrap items-center gap-3">
            <Segmented
              label={t('transactions.direction')}
              value={filters.direction ?? 'ALL'}
              onChange={(v) => set({ direction: v === 'ALL' ? undefined : v })}
              options={[
                { value: 'ALL', label: t('common.all') },
                { value: 'INCOMING', label: t('transactions.incoming') },
                { value: 'OUTGOING', label: t('transactions.outgoing') },
              ]}
            />
            <Select
              aria-label={t('transactions.account')}
              value={filters.accountId ?? ''}
              onChange={(e) => set({ accountId: Number(e.target.value) || undefined })}
              className="h-10 w-auto min-w-40"
            >
              <option value="">{t('transactions.allAccounts')}</option>
              {accounts.data?.map((a) => (
                <option key={a.id} value={a.id}>
                  {a.name} · {a.currency}
                </option>
              ))}
            </Select>
            <Select
              aria-label={t('transactions.category')}
              value={filters.category ?? ''}
              onChange={(e) => set({ category: (e.target.value as Category) || undefined })}
              className="h-10 w-auto min-w-40"
            >
              <option value="">{t('transactions.allCategories')}</option>
              {CATEGORIES.map((c) => (
                <option key={c} value={c}>
                  {t(`categories.${c}`)}
                </option>
              ))}
            </Select>
            <label className="text-muted flex items-center gap-2 text-sm">
              {t('transactions.from')}
              <input
                type="date"
                value={filters.from ?? ''}
                max={filters.to}
                onChange={(e) => set({ from: e.target.value || undefined })}
                className={cn(inputStyles, 'h-10 w-auto')}
              />
            </label>
            <label className="text-muted flex items-center gap-2 text-sm">
              {t('transactions.to')}
              <input
                type="date"
                value={filters.to ?? ''}
                min={filters.from}
                onChange={(e) => set({ to: e.target.value || undefined })}
                className={cn(inputStyles, 'h-10 w-auto')}
              />
            </label>
            {hasFilters && (
              <Button
                variant="ghost"
                size="sm"
                icon={<X className="size-4" />}
                onClick={() => {
                  setSearch('')
                  clear()
                }}
              >
                {t('transactions.clearFilters')}
              </Button>
            )}
          </div>
        </div>
      </Card>

      <Card className="p-4 sm:p-6">
        {query.isPending ? (
          <div className="flex flex-col gap-4">
            {Array.from({ length: 6 }, (_, i) => (
              <Skeleton key={i} className="h-12 w-full" />
            ))}
          </div>
        ) : query.isError ? (
          <EmptyState
            icon={<ReceiptText />}
            title={errorMessage(t, query.error)}
            action={<Button onClick={() => query.refetch()}>{t('common.retry')}</Button>}
          />
        ) : query.data.content.length === 0 ? (
          <EmptyState icon={<ReceiptText />} title={t('transactions.empty')} hint={hasFilters ? t('transactions.emptyHint') : undefined} />
        ) : (
          <div className={cn('transition', query.isPlaceholderData && 'opacity-60')} aria-busy={query.isFetching}>
            <p className="text-muted mb-2 text-sm" aria-live="polite">
              {t('transactions.results', { count: query.data.totalElements })}
            </p>
            {groups.map((g) => (
              <section key={g.day} aria-label={dayLabel(g.day)}>
                <h2 className="text-muted bg-surface sticky top-16 z-10 py-2 text-xs font-semibold tracking-wide uppercase">
                  {dayLabel(g.day)}
                </h2>
                <ul>
                  {g.items.map((tx) => (
                    <TransactionRow key={tx.id} tx={tx} showAccount={(accounts.data?.length ?? 0) > 1 && !filters.accountId} />
                  ))}
                </ul>
              </section>
            ))}

            {query.data.totalPages > 1 && (
              <nav className="border-border mt-4 flex items-center justify-between border-t pt-4" aria-label={t('common.pagination')}>
                <Button
                  variant="secondary"
                  size="sm"
                  icon={<ChevronLeft className="size-4" />}
                  disabled={(filters.page ?? 0) === 0}
                  onClick={() => set({ page: (filters.page ?? 0) - 1 || undefined })}
                >
                  {t('common.previous')}
                </Button>
                <span className="text-muted text-sm">
                  {t('common.pageOf', { page: (filters.page ?? 0) + 1, total: query.data.totalPages })}
                </span>
                <Button
                  variant="secondary"
                  size="sm"
                  disabled={(filters.page ?? 0) + 1 >= query.data.totalPages}
                  onClick={() => set({ page: (filters.page ?? 0) + 1 })}
                >
                  {t('common.next')} <ChevronRight className="size-4" aria-hidden />
                </Button>
              </nav>
            )}
          </div>
        )}
      </Card>
    </>
  )
}
