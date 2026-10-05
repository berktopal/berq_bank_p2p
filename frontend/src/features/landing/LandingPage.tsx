import { ArrowDownLeft, ArrowRight, ArrowUpRight, BarChart3, FileText, Lock, ShieldCheck, Users, Wallet, Zap } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { ButtonLink } from '@/components/ui/Button'
import { Avatar } from '@/components/ui/primitives'
import { useMe } from '@/features/auth/api'
import { formatMoney } from '@/lib/format'

const FEATURES = [
  { key: 'instant', icon: Zap },
  { key: 'secure', icon: ShieldCheck },
  { key: 'insights', icon: BarChart3 },
  { key: 'contacts', icon: Users },
  { key: 'receipts', icon: FileText },
  { key: 'multi', icon: Wallet },
] as const

const SECURITY = ['locking', 'idempotency', 'limits', 'privacy'] as const

/** Ürün önizlemesi: gerçek veri değil, açıkça örnek amaçlı sabit değerler. */
function PreviewCard() {
  const { t } = useTranslation()
  return (
    <div className="relative mx-auto w-full max-w-sm" aria-hidden>
      <div className="brand-gradient absolute -inset-6 rounded-[2rem] opacity-20 blur-2xl" />
      <div className="card relative flex flex-col gap-4 p-5">
        <div className="brand-gradient rounded-2xl p-5 text-white">
          <p className="text-sm text-white/80">{t('landing.previewBalance')}</p>
          <p className="tabular mt-1 text-3xl font-bold">{formatMoney(48250.75, 'TRY')}</p>
          <p className="tabular mt-3 text-sm text-white/80">TR12 0099 9000 •••• 4821</p>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div className="bg-surface-2 rounded-xl p-3">
            <p className="text-muted flex items-center gap-1 text-xs">
              <ArrowDownLeft className="text-series-1 size-3.5" /> {t('landing.previewIncoming')}
            </p>
            <p className="tabular mt-1 font-semibold">{formatMoney(52400, 'TRY')}</p>
          </div>
          <div className="bg-surface-2 rounded-xl p-3">
            <p className="text-muted flex items-center gap-1 text-xs">
              <ArrowUpRight className="text-series-2 size-3.5" /> {t('landing.previewOutgoing')}
            </p>
            <p className="tabular mt-1 font-semibold">{formatMoney(31870, 'TRY')}</p>
          </div>
        </div>
        <ul className="flex flex-col gap-3">
          {[
            { name: 'Ayşe Kaya', note: t('categories.FOOD'), amount: -420.5 },
            { name: 'Can Öztürk', note: t('categories.GENERAL'), amount: 12500 },
            { name: 'Mehmet Demir', note: t('categories.RENT'), amount: -17500 },
          ].map((row) => (
            <li key={row.name} className="flex items-center gap-3">
              <Avatar name={row.name} size="sm" />
              <div className="flex-1">
                <p className="text-sm font-medium">{row.name}</p>
                <p className="text-muted text-xs">{row.note}</p>
              </div>
              <p className={row.amount > 0 ? 'tabular text-success text-sm font-semibold' : 'tabular text-sm font-semibold'}>
                {formatMoney(row.amount, 'TRY', { signed: true })}
              </p>
            </li>
          ))}
        </ul>
      </div>
    </div>
  )
}

export function LandingPage() {
  const { t } = useTranslation()
  const { data: user } = useMe()

  return (
    <>
      <section className="mx-auto grid max-w-6xl items-center gap-12 px-4 py-16 sm:px-6 lg:grid-cols-2 lg:py-24">
        <div>
          <p className="bg-primary-soft text-primary inline-flex items-center gap-2 rounded-full px-3 py-1 text-sm font-semibold">
            <Zap className="size-4" aria-hidden /> {t('landing.badge')}
          </p>
          <h1 className="mt-5 text-4xl leading-[1.1] font-extrabold tracking-tight text-balance sm:text-5xl lg:text-6xl">
            {t('landing.title')}
          </h1>
          <p className="text-muted mt-5 max-w-xl text-lg">{t('landing.subtitle')}</p>
          <div className="mt-8 flex flex-wrap gap-3">
            {user ? (
              <ButtonLink to="/app" variant="brand" size="lg">
                {t('nav.openApp')} <ArrowRight className="size-5" aria-hidden />
              </ButtonLink>
            ) : (
              <>
                <ButtonLink to="/register" variant="brand" size="lg">
                  {t('landing.ctaPrimary')} <ArrowRight className="size-5" aria-hidden />
                </ButtonLink>
                <ButtonLink to="/login" variant="secondary" size="lg">
                  {t('landing.ctaSecondary')}
                </ButtonLink>
              </>
            )}
          </div>
          <p className="text-muted mt-6 flex items-center gap-2 text-sm">
            <Lock className="size-4" aria-hidden /> {t('landing.trust')}
          </p>
        </div>
        <PreviewCard />
      </section>

      <section className="bg-surface border-border border-y py-16 lg:py-24" aria-labelledby="features-title">
        <div className="mx-auto max-w-6xl px-4 sm:px-6">
          <h2 id="features-title" className="max-w-2xl text-3xl font-bold tracking-tight">
            {t('landing.featuresTitle')}
          </h2>
          <ul className="mt-10 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {FEATURES.map(({ key, icon: Icon }) => (
              <li key={key} className="border-border rounded-2xl border p-6">
                <span className="bg-primary-soft text-primary grid size-11 place-items-center rounded-xl">
                  <Icon className="size-5" aria-hidden />
                </span>
                <h3 className="mt-4 font-semibold">{t(`landing.features.${key}.title`)}</h3>
                <p className="text-muted mt-1.5 text-sm">{t(`landing.features.${key}.body`)}</p>
              </li>
            ))}
          </ul>
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-4 py-16 sm:px-6 lg:py-24" aria-labelledby="how-title">
        <h2 id="how-title" className="text-3xl font-bold tracking-tight">
          {t('landing.howTitle')}
        </h2>
        <ol className="mt-10 grid gap-6 md:grid-cols-3">
          {(['one', 'two', 'three'] as const).map((k, i) => (
            <li key={k} className="card p-6">
              <span className="brand-gradient grid size-10 place-items-center rounded-full font-bold text-white">{i + 1}</span>
              <h3 className="mt-4 font-semibold">{t(`landing.how.${k}.title`)}</h3>
              <p className="text-muted mt-1.5 text-sm">{t(`landing.how.${k}.body`)}</p>
            </li>
          ))}
        </ol>
      </section>

      <section className="mx-auto max-w-6xl px-4 pb-20 sm:px-6" aria-labelledby="security-title">
        <div className="brand-gradient rounded-3xl p-8 text-white sm:p-12">
          <h2 id="security-title" className="text-3xl font-bold tracking-tight">
            {t('landing.securityTitle')}
          </h2>
          <ul className="mt-8 grid gap-5 sm:grid-cols-2">
            {SECURITY.map((k) => (
              <li key={k} className="flex gap-3 text-white/90">
                <ShieldCheck className="mt-0.5 size-5 shrink-0" aria-hidden /> {t(`landing.security.${k}`)}
              </li>
            ))}
          </ul>
          {!user && (
            <ButtonLink to="/register" size="lg" className="mt-10 bg-white text-[#3b3fb8] hover:bg-white/90">
              {t('landing.ctaPrimary')}
            </ButtonLink>
          )}
        </div>
      </section>
    </>
  )
}
