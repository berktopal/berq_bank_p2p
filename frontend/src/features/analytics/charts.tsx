import { useTranslation } from 'react-i18next'
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis, type TooltipContentProps } from 'recharts'
import { formatMoney, formatMonth } from '@/lib/format'
import type { AnalyticsSummary } from '@/lib/types'

type Monthly = AnalyticsSummary['monthly']

/** Kısa eksen etiketi: 12.500 → 12,5B / 12.5K */
function compact(value: number, lang: string) {
  return new Intl.NumberFormat(lang === 'en' ? 'en-US' : 'tr-TR', { notation: 'compact', maximumFractionDigits: 1 }).format(value)
}

function CashflowTooltip({ active, payload, label, currency }: TooltipContentProps<number, string> & { currency: string }) {
  const { t } = useTranslation()
  if (!active || !payload?.length) return null
  return (
    <div className="bg-surface border-border rounded-xl border px-3 py-2 text-sm shadow-lg">
      <p className="text-muted mb-1 font-medium">{formatMonth(String(label))}</p>
      {payload.map((p) => (
        <p key={String(p.dataKey)} className="flex items-center gap-2">
          <span className="h-0.5 w-3 rounded" style={{ background: p.color }} aria-hidden />
          <span className="tabular font-semibold">{formatMoney(Number(p.value), currency)}</span>
          <span className="text-muted">{p.dataKey === 'incoming' ? t('dashboard.incoming') : t('dashboard.outgoing')}</span>
        </p>
      ))}
    </div>
  )
}

export function SeriesLegend() {
  const { t } = useTranslation()
  return (
    <div className="text-muted flex items-center gap-4 text-sm">
      <span className="flex items-center gap-1.5">
        <span className="bg-series-1 size-2.5 rounded-sm" aria-hidden /> {t('dashboard.incoming')}
      </span>
      <span className="flex items-center gap-1.5">
        <span className="bg-series-2 size-2.5 rounded-sm" aria-hidden /> {t('dashboard.outgoing')}
      </span>
    </div>
  )
}

/** Aylık gelen/giden yan yana sütunlar. Tek y ekseni; değerler tabloda da erişilebilir. */
export function CashflowChart({ data, currency, height = 260 }: { data: Monthly; currency: string; height?: number }) {
  const { t, i18n } = useTranslation()
  return (
    <figure>
      <div style={{ height }} aria-hidden>
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={data} barGap={2} barCategoryGap="30%" margin={{ top: 8, right: 4, bottom: 0, left: 0 }}>
            <CartesianGrid vertical={false} stroke="var(--grid)" strokeWidth={1} />
            <XAxis
              dataKey="month"
              tickFormatter={(m: string) => formatMonth(m, true)}
              tickLine={false}
              axisLine={false}
              tick={{ fill: 'var(--muted)', fontSize: 12 }}
            />
            <YAxis
              tickFormatter={(v: number) => compact(v, i18n.language)}
              tickLine={false}
              axisLine={false}
              width={48}
              tick={{ fill: 'var(--muted)', fontSize: 12 }}
            />
            <Tooltip
              cursor={{ fill: 'var(--surface-2)' }}
              content={(props) => <CashflowTooltip {...(props as TooltipContentProps<number, string>)} currency={currency} />}
            />
            <Bar dataKey="incoming" fill="var(--series-1)" radius={[4, 4, 0, 0]} maxBarSize={24} />
            <Bar dataKey="outgoing" fill="var(--series-2)" radius={[4, 4, 0, 0]} maxBarSize={24} />
          </BarChart>
        </ResponsiveContainer>
      </div>
      <details className="mt-3 text-sm">
        <summary className="text-muted hover:text-text cursor-pointer">{t('analytics.showTable')}</summary>
        <table className="mt-2 w-full text-left">
          <thead className="text-muted">
            <tr>
              <th className="py-1 font-medium">{t('analytics.period')}</th>
              <th className="py-1 text-right font-medium">{t('dashboard.incoming')}</th>
              <th className="py-1 text-right font-medium">{t('dashboard.outgoing')}</th>
            </tr>
          </thead>
          <tbody className="tabular">
            {data.map((m) => (
              <tr key={m.month} className="border-border border-t">
                <td className="py-1">{formatMonth(m.month)}</td>
                <td className="py-1 text-right">{formatMoney(m.incoming, currency)}</td>
                <td className="py-1 text-right">{formatMoney(m.outgoing, currency)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </details>
    </figure>
  )
}

/**
 * Yatay çubuk listesi (kategori/alıcı büyüklükleri). Tek seri olduğu için lejant yok;
 * değer ve pay metin olarak her satırda yazılı, renk tek bilgi taşıyıcısı değil.
 */
export function BarList({
  items,
  currency,
}: {
  items: { key: string; label: string; sublabel?: string; value: number }[]
  currency: string
}) {
  const max = Math.max(...items.map((i) => i.value), 1)
  const total = items.reduce((s, i) => s + i.value, 0) || 1
  return (
    <ul className="flex flex-col gap-4">
      {items.map((item) => (
        <li key={item.key}>
          <div className="mb-1.5 flex items-baseline justify-between gap-3 text-sm">
            <span className="min-w-0 truncate font-medium">
              {item.label}
              {item.sublabel && <span className="text-muted font-normal"> · {item.sublabel}</span>}
            </span>
            <span className="tabular shrink-0">
              <span className="font-semibold">{formatMoney(item.value, currency)}</span>
              <span className="text-muted ml-2 inline-block w-11 text-right">%{Math.round((item.value / total) * 100)}</span>
            </span>
          </div>
          <div className="bg-surface-2 h-2 overflow-hidden rounded-full" aria-hidden>
            <div className="bg-series-1 h-full rounded-full" style={{ width: `${(item.value / max) * 100}%` }} />
          </div>
        </li>
      ))}
    </ul>
  )
}
