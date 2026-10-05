import type { ReactNode } from 'react'
import { ShieldCheck, Zap, BarChart3 } from 'lucide-react'
import { useTranslation } from 'react-i18next'

/** Giriş/kayıt sayfalarının ortak iki sütunlu düzeni: solda marka paneli, sağda form. */
export function AuthCard({ title, subtitle, children }: { title: string; subtitle: string; children: ReactNode }) {
  const { t } = useTranslation()
  return (
    <div className="mx-auto grid max-w-6xl gap-10 px-4 py-10 sm:px-6 lg:grid-cols-2 lg:py-16">
      <section
        aria-hidden
        className="brand-gradient relative hidden overflow-hidden rounded-3xl p-10 text-white lg:flex lg:flex-col lg:justify-between"
      >
        <div className="absolute -top-24 -right-24 size-72 rounded-full bg-white/10" />
        <div className="absolute -bottom-32 -left-16 size-80 rounded-full bg-white/10" />
        <div className="relative">
          <p className="text-sm font-semibold tracking-wide text-white/80 uppercase">{t('landing.badge')}</p>
          <p className="mt-4 text-3xl leading-tight font-bold">{t('landing.title')}</p>
        </div>
        <ul className="relative mt-10 space-y-4 text-white/90">
          <li className="flex gap-3">
            <Zap className="size-5 shrink-0" /> {t('landing.features.instant.body')}
          </li>
          <li className="flex gap-3">
            <ShieldCheck className="size-5 shrink-0" /> {t('landing.security.locking')}
          </li>
          <li className="flex gap-3">
            <BarChart3 className="size-5 shrink-0" /> {t('landing.features.insights.body')}
          </li>
        </ul>
      </section>
      <section className="mx-auto w-full max-w-md self-center">
        <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">{title}</h1>
        <p className="text-muted mt-2">{subtitle}</p>
        <div className="mt-8">{children}</div>
      </section>
    </div>
  )
}
