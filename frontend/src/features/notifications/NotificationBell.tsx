import { useEffect, useRef, useState } from 'react'
import { ArrowDownLeft, Bell, CalendarClock, CheckCheck, HandCoins, PiggyBank } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { Spinner } from '@/components/ui/Spinner'
import { cn } from '@/lib/cn'
import { formatDateTime } from '@/lib/format'
import type { AppNotification } from '@/lib/types'
import { useMarkAllRead, useMarkRead, useNotifications, useUnreadCount } from './api'
import { notificationLink, notificationText } from './text'

function NotificationIcon({ n }: { n: AppNotification }) {
  const Icon = n.type.startsWith('PAYMENT_REQUEST')
    ? HandCoins
    : n.type.startsWith('SCHEDULED')
      ? CalendarClock
      : n.type.startsWith('BUDGET')
        ? PiggyBank
        : ArrowDownLeft
  const tone =
    n.type === 'BUDGET_EXCEEDED' || n.type === 'SCHEDULED_TRANSFER_FAILED'
      ? 'bg-danger-soft text-danger'
      : n.type === 'BUDGET_WARNING'
        ? 'bg-warning-soft text-warning'
        : 'bg-primary-soft text-primary'
  return (
    <span className={cn('grid size-9 shrink-0 place-items-center rounded-full', tone)} aria-hidden>
      <Icon className="size-4" />
    </span>
  )
}

/** Üst çubuktaki zil: okunmamış sayısı + son bildirimler paneli. */
export function NotificationBell() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const panelRef = useRef<HTMLDivElement>(null)
  const buttonRef = useRef<HTMLButtonElement>(null)
  const unread = useUnreadCount()
  const list = useNotifications(open)
  const markRead = useMarkRead()
  const markAll = useMarkAllRead()
  const count = unread.data ?? 0

  // Dışarı tıklayınca ve Esc ile kapanır; odak düğmeye döner
  useEffect(() => {
    if (!open) return
    const onClick = (e: MouseEvent) => {
      if (!panelRef.current?.contains(e.target as Node) && !buttonRef.current?.contains(e.target as Node)) setOpen(false)
    }
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setOpen(false)
        buttonRef.current?.focus()
      }
    }
    document.addEventListener('mousedown', onClick)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('mousedown', onClick)
      document.removeEventListener('keydown', onKey)
    }
  }, [open])

  const openItem = (n: AppNotification) => {
    if (!n.read) markRead.mutate(n.id)
    setOpen(false)
    void navigate(notificationLink(n))
  }

  return (
    <div className="relative">
      <button
        ref={buttonRef}
        type="button"
        onClick={() => setOpen((o) => !o)}
        className="text-muted hover:text-text hover:bg-surface-2 relative inline-grid size-10 place-items-center rounded-xl transition"
        aria-label={count ? `${t('nav.notifications')}: ${t('notifications.unread', { count })}` : t('nav.notifications')}
        aria-expanded={open}
        aria-haspopup="true"
      >
        <Bell className="size-5" />
        {count > 0 && (
          <span className="bg-danger absolute top-1.5 right-1.5 grid min-w-4 place-items-center rounded-full px-1 text-[10px] leading-4 font-bold text-white">
            {count > 9 ? '9+' : count}
          </span>
        )}
      </button>

      {open && (
        <div
          ref={panelRef}
          role="region"
          aria-label={t('notifications.title')}
          className="card absolute right-0 z-50 mt-2 w-[min(24rem,calc(100vw-2rem))] overflow-hidden p-0 shadow-xl"
        >
          <div className="border-border flex items-center justify-between border-b px-4 py-3">
            <h2 className="font-semibold">{t('notifications.title')}</h2>
            {count > 0 && (
              <button
                type="button"
                onClick={() => markAll.mutate()}
                className="text-primary inline-flex items-center gap-1 text-sm font-semibold hover:underline"
              >
                <CheckCheck className="size-4" aria-hidden /> {t('notifications.markAllRead')}
              </button>
            )}
          </div>
          <div className="max-h-[26rem] overflow-y-auto">
            {list.isPending ? (
              <div className="text-primary grid place-items-center py-10">
                <Spinner />
              </div>
            ) : list.data?.content.length ? (
              <ul>
                {list.data.content.map((n) => (
                  <li key={n.id}>
                    <button
                      type="button"
                      onClick={() => openItem(n)}
                      className={cn(
                        'hover:bg-surface-2 flex w-full items-start gap-3 px-4 py-3 text-left transition',
                        !n.read && 'bg-primary-soft/40',
                      )}
                    >
                      <NotificationIcon n={n} />
                      <span className="min-w-0 flex-1">
                        <span className={cn('block text-sm', !n.read && 'font-semibold')}>{notificationText(t, n)}</span>
                        <span className="text-muted mt-0.5 block text-xs">{formatDateTime(n.createdAt)}</span>
                      </span>
                      {!n.read && <span className="bg-primary mt-2 size-2 shrink-0 rounded-full" aria-hidden />}
                    </button>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="text-muted px-4 py-10 text-center text-sm">{t('notifications.empty')}</p>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
