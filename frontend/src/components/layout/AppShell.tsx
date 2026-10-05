import { useState } from 'react'
import {
  ArrowLeftRight,
  BarChart3,
  CalendarClock,
  HandCoins,
  Home,
  LogOut,
  Menu,
  PiggyBank,
  ReceiptText,
  Send,
  Settings,
  Users,
  Wallet,
  type LucideIcon,
} from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Link, NavLink, Outlet, useNavigate } from 'react-router'
import { Dialog } from '@/components/ui/Dialog'
import { Avatar } from '@/components/ui/primitives'
import { useMe } from '@/features/auth/api'
import { NotificationBell } from '@/features/notifications/NotificationBell'
import { useNotificationStream } from '@/features/notifications/useNotificationStream'
import { usePendingRequestCount } from '@/features/requests/api'
import { cn } from '@/lib/cn'
import type { Resources } from '@/i18n/tr'
import { Logo } from './Logo'
import { LanguageToggle, ThemeToggle } from './Toggles'

type NavKey = keyof Resources['nav']
interface NavItem {
  to: string
  key: NavKey
  icon: LucideIcon
  end?: boolean
}

/** Kenar çubuğu grupları; mobilde aynı sıra "Daha fazla" menüsünde düz liste olur. */
const NAV_GROUPS: { title?: NavKey; items: NavItem[] }[] = [
  { items: [{ to: '/app', key: 'dashboard', icon: Home, end: true }] },
  {
    title: 'groupMoney',
    items: [
      { to: '/app/transfer', key: 'transfer', icon: Send },
      { to: '/app/requests', key: 'requests', icon: HandCoins },
      { to: '/app/scheduled', key: 'scheduled', icon: CalendarClock },
    ],
  },
  {
    title: 'groupTrack',
    items: [
      { to: '/app/transactions', key: 'transactions', icon: ReceiptText },
      { to: '/app/analytics', key: 'analytics', icon: BarChart3 },
      { to: '/app/budgets', key: 'budgets', icon: PiggyBank },
    ],
  },
  {
    title: 'groupAccount',
    items: [
      { to: '/app/accounts', key: 'accounts', icon: Wallet },
      { to: '/app/contacts', key: 'contacts', icon: Users },
      { to: '/app/settings', key: 'settings', icon: Settings },
    ],
  },
]
const NAV: NavItem[] = NAV_GROUPS.flatMap((g) => g.items)

const MOBILE_PRIMARY: NavKey[] = ['dashboard', 'accounts', 'transfer', 'transactions']

export function AppShell() {
  const { t } = useTranslation()
  const { data: user } = useMe()
  const navigate = useNavigate()
  const [moreOpen, setMoreOpen] = useState(false)
  const fullName = user ? `${user.firstName} ${user.lastName}` : ''
  const pending = usePendingRequestCount().data ?? 0
  useNotificationStream(Boolean(user))

  // Bekleyen gelen istek sayısı menüde rozet olarak görünür
  const badge = (key: NavKey) =>
    key === 'requests' && pending > 0 ? (
      <span className="bg-danger ml-auto grid min-w-5 place-items-center rounded-full px-1.5 text-[11px] leading-5 font-bold text-white">
        {pending}
      </span>
    ) : null

  // Çıkış /logout rotasında yapılır (bkz. LogoutPage)
  const handleLogout = () => {
    setMoreOpen(false)
    void navigate('/logout')
  }

  return (
    <div className="min-h-dvh lg:grid lg:grid-cols-[264px_1fr]">
      <a
        href="#main"
        className="bg-primary text-on-primary sr-only z-50 rounded-lg px-4 py-2 focus:not-sr-only focus:fixed focus:top-3 focus:left-3"
      >
        {t('common.skipToContent')}
      </a>

      {/* Masaüstü kenar çubuğu */}
      <div className="border-border bg-surface hidden border-r lg:block">
      <aside className="sticky top-0 flex h-dvh flex-col px-4 py-5">
        <Link to="/app" className="px-2" aria-label={t('nav.dashboard')}>
          <Logo />
        </Link>
        <nav aria-label={t('nav.mainMenu')} className="mt-6 flex flex-1 flex-col gap-4 overflow-y-auto">
          {NAV_GROUPS.map((group, i) => (
            <div key={group.title ?? i} className="flex flex-col gap-0.5">
              {group.title && (
                <p className="text-muted px-3 pb-1 text-[11px] font-semibold tracking-wider uppercase">{t(`nav.${group.title}`)}</p>
              )}
              {group.items.map(({ to, key, icon: Icon, end }) => (
                <NavLink
                  key={to}
                  to={to}
                  end={end}
                  className={({ isActive }) =>
                    cn(
                      'flex items-center gap-3 rounded-xl px-3 py-2 text-sm font-medium transition',
                      isActive ? 'bg-primary-soft text-primary' : 'text-muted hover:bg-surface-2 hover:text-text',
                    )
                  }
                >
                  <Icon className="size-5" aria-hidden />
                  {t(`nav.${key}`)}
                  {badge(key)}
                </NavLink>
              ))}
            </div>
          ))}
        </nav>
        <div className="border-border mt-4 flex items-center gap-3 border-t px-2 pt-4">
          <Avatar name={fullName || '?'} size="sm" />
          <div className="min-w-0 flex-1">
            <p className="truncate text-sm font-semibold">{fullName}</p>
            <p className="text-muted truncate text-xs">{user?.email}</p>
          </div>
          <button
            type="button"
            onClick={handleLogout}
            className="text-muted hover:bg-danger-soft hover:text-danger rounded-lg p-2"
            aria-label={t('nav.logout')}
            title={t('nav.logout')}
          >
            <LogOut className="size-5" />
          </button>
        </div>
      </aside>
      </div>

      <div className="flex min-w-0 flex-col">
        <header className="border-border bg-bg/80 sticky top-0 z-30 flex h-16 items-center justify-between gap-3 border-b px-4 backdrop-blur sm:px-6 lg:justify-end">
          <Link to="/app" className="lg:hidden" aria-label={t('nav.dashboard')}>
            <Logo />
          </Link>
          <div className="flex items-center gap-1">
            <LanguageToggle />
            <ThemeToggle />
            <NotificationBell />
          </div>
        </header>

        <main id="main" tabIndex={-1} className="mx-auto w-full max-w-6xl flex-1 px-4 pt-6 pb-28 outline-none sm:px-6 lg:pb-12">
          <Outlet />
        </main>
      </div>

      {/* Mobil alt navigasyon */}
      <nav
        aria-label={t('nav.mainMenu')}
        className="border-border bg-surface/95 fixed inset-x-0 bottom-0 z-40 grid grid-cols-5 border-t px-2 pt-2 pb-[max(0.5rem,env(safe-area-inset-bottom))] backdrop-blur lg:hidden"
      >
        {NAV.filter((n) => MOBILE_PRIMARY.includes(n.key)).map(({ to, key, icon: Icon, end }) =>
          key === 'transfer' ? (
            <NavLink key={to} to={to} className="flex flex-col items-center" aria-label={t('nav.transfer')}>
              <span className="brand-gradient -mt-6 grid size-14 place-items-center rounded-2xl text-white shadow-lg">
                <ArrowLeftRight className="size-6" aria-hidden />
              </span>
              <span className="mt-1 text-[11px] font-medium">{t('common.send')}</span>
            </NavLink>
          ) : (
            <NavLink
              key={to}
              to={to}
              end={end}
              className={({ isActive }) =>
                cn('flex flex-col items-center gap-1 py-1 text-[11px] font-medium', isActive ? 'text-primary' : 'text-muted')
              }
            >
              <Icon className="size-5" aria-hidden />
              {t(`nav.${key}`)}
            </NavLink>
          ),
        )}
        <button
          type="button"
          onClick={() => setMoreOpen(true)}
          className="text-muted flex flex-col items-center gap-1 py-1 text-[11px] font-medium"
          aria-haspopup="dialog"
        >
          <Menu className="size-5" aria-hidden />
          {t('nav.more')}
        </button>
      </nav>

      <Dialog open={moreOpen} onClose={() => setMoreOpen(false)} title={t('nav.more')}>
        <nav className="flex flex-col gap-1" aria-label={t('nav.more')}>
          {NAV.filter((n) => !MOBILE_PRIMARY.includes(n.key)).map(({ to, key, icon: Icon }) => (
            <Link
              key={to}
              to={to}
              onClick={() => setMoreOpen(false)}
              className="hover:bg-surface-2 flex items-center gap-3 rounded-xl px-3 py-3 font-medium"
            >
              <Icon className="text-muted size-5" aria-hidden />
              {t(`nav.${key}`)}
              {badge(key)}
            </Link>
          ))}
          <button
            type="button"
            onClick={handleLogout}
            className="text-danger hover:bg-danger-soft flex items-center gap-3 rounded-xl px-3 py-3 font-medium"
          >
            <LogOut className="size-5" aria-hidden />
            {t('nav.logout')}
          </button>
        </nav>
      </Dialog>
    </div>
  )
}
