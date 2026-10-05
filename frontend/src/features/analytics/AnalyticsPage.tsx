import { useState } from 'react'
import { BarChart3 } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Select } from '@/components/ui/Field'
import { Card, CardHeader, EmptyState, PageHeader, Segmented, Skeleton } from '@/components/ui/primitives'
import { useAccounts } from '@/features/accounts/api'
import { cn } from '@/lib/cn'
import { errorMessage } from '@/lib/errors'
import { formatMoney } from '@/lib/format'
import { useAnalytics } from './api'
import { BarList, CashflowChart, SeriesLegend } from './charts'

const PERIODS = ['3', '6', '12'] as const

export function AnalyticsPage() {
  const { t } = useTranslation()
  const accounts = useAccounts()
  const [accountId, setAccountId] = useState<number | undefined>()
  const [months, setMonths] = useState<(typeof PERIODS)[number]>('6')
  const selected = accountId ?? accounts.data?.find((a) => a.currency === 'TRY')?.id ?? accounts.data?.[0]?.id
  const query = useAnalytics(selected, Number(months))
  const data = query.data

  return (
    <>
      <PageHeader title={t('analytics.title')} subtitle={t('analytics.subtitle')} />

      {/* Filtreler: tek satır, tüm grafiklerin üstünde; hepsi aynı kesiti gösterir */}
      <div className="mb-6 flex flex-wrap items-center gap-3">
        <Segmented
          label={t('analytics.period')}
          value={months}
          onChange={setMonths}
          options={PERIODS.map((p) => ({ value: p, label: t('analytics.months', { count: Number(p) }) }))}
        />
        <Select
          aria-label={t('analytics.account')}
          value={selected ?? ''}
          onChange={(e) => setAccountId(Number(e.target.value))}
          className="h-10 w-auto min-w-48"
        >
          {accounts.data?.map((a) => (
            <option key={a.id} value={a.id}>
              {a.name} · {a.currency}
            </option>
          ))}
        </Select>
      </div>

      {query.isError ? (
        <Card>
          <EmptyState icon={<BarChart3 />} title={errorMessage(t, query.error)} />
        </Card>
      ) : !data ? (
        <div className="grid gap-6">
          <div className="grid gap-4 sm:grid-cols-3">
            {[0, 1, 2].map((i) => (
              <Skeleton key={i} className="h-24 rounded-2xl" />
            ))}
          </div>
          <Skeleton className="h-80 rounded-2xl" />
        </div>
      ) : (
        <div className={cn('grid gap-6 transition', query.isPlaceholderData && 'opacity-60')}>
          <dl className="grid gap-4 sm:grid-cols-3">
            {[
              { label: t('analytics.totalIncoming'), value: data.totalIncoming },
              { label: t('analytics.totalOutgoing'), value: data.totalOutgoing },
              { label: t('analytics.net'), value: data.net, signed: true },
            ].map((tile) => (
              <Card key={tile.label} className="p-5">
                <dt className="text-muted text-sm">{tile.label}</dt>
                <dd className="tabular mt-1 text-2xl font-semibold">{formatMoney(tile.value, data.currency, { signed: tile.signed })}</dd>
              </Card>
            ))}
          </dl>

          <Card className="p-5">
            <CardHeader title={t('analytics.monthlyTitle')} action={<SeriesLegend />} />
            <div className="mt-4">
              <CashflowChart data={data.monthly} currency={data.currency} />
            </div>
          </Card>

          <div className="grid gap-6 lg:grid-cols-2">
            <Card className="p-5">
              <CardHeader title={t('analytics.categoryTitle')} />
              <div className="mt-5">
                {data.byCategory.length === 0 ? (
                  <p className="text-muted text-sm">{t('analytics.noData')}</p>
                ) : (
                  <BarList
                    currency={data.currency}
                    items={data.byCategory.map((c) => ({
                      key: c.category,
                      label: t(`categories.${c.category}`),
                      sublabel: t('analytics.transactionsCount', { count: c.count }),
                      value: c.total,
                    }))}
                  />
                )}
              </div>
            </Card>
            <Card className="p-5">
              <CardHeader title={t('analytics.recipientsTitle')} />
              <div className="mt-5">
                {data.topRecipients.length === 0 ? (
                  <p className="text-muted text-sm">{t('analytics.noData')}</p>
                ) : (
                  <BarList
                    currency={data.currency}
                    items={data.topRecipients.map((r) => ({
                      key: r.iban,
                      label: r.name,
                      sublabel: t('analytics.transactionsCount', { count: r.count }),
                      value: r.total,
                    }))}
                  />
                )}
              </div>
            </Card>
          </div>
        </div>
      )}
    </>
  )
}
