import { useEffect } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { toast } from 'sonner'
import { api } from '@/lib/api'
import type { AppNotification, NotificationType } from '@/lib/types'
import { notificationLink, notificationText } from './text'

/** Hangi bildirim hangi önbelleği bayatlatır: ekrandaki veri sayfa yenilenmeden güncellenir. */
const AFFECTS: Record<NotificationType, string[][]> = {
  MONEY_RECEIVED: [['accounts'], ['transactions'], ['analytics']],
  PAYMENT_REQUEST_RECEIVED: [['payment-requests']],
  PAYMENT_REQUEST_PAID: [['payment-requests'], ['accounts'], ['transactions'], ['analytics']],
  PAYMENT_REQUEST_DECLINED: [['payment-requests']],
  PAYMENT_REQUEST_CANCELLED: [['payment-requests']],
  SCHEDULED_TRANSFER_EXECUTED: [['scheduled-transfers'], ['accounts'], ['transactions'], ['analytics'], ['budgets']],
  SCHEDULED_TRANSFER_FAILED: [['scheduled-transfers']],
  BUDGET_WARNING: [['budgets']],
  BUDGET_EXCEEDED: [['budgets']],
}

const RECONNECT_MS = 5_000

/**
 * Sunucudan canlı bildirimleri (Server-Sent Events) dinler. Bağlantı koparsa bekleyip yeniden bağlanır;
 * önce oturumu kontrol eder ki oturum düştüyse sonsuz 401 döngüsüne girilmesin (api istemcisi girişe yönlendirir).
 */
export function useNotificationStream(enabled: boolean) {
  const qc = useQueryClient()
  const { t } = useTranslation()
  const navigate = useNavigate()

  useEffect(() => {
    if (!enabled || typeof EventSource === 'undefined') return
    let source: EventSource | null = null
    let timer: ReturnType<typeof setTimeout> | undefined
    let disposed = false

    const connect = () => {
      source = new EventSource('/api/notifications/stream')
      source.addEventListener('notification', (event) => {
        const n = JSON.parse((event as MessageEvent<string>).data) as AppNotification
        void qc.invalidateQueries({ queryKey: ['notifications'] })
        AFFECTS[n.type]?.forEach((queryKey) => void qc.invalidateQueries({ queryKey }))
        toast(notificationText(t, n), {
          id: `notification-${n.id}`,
          action: { label: t('common.seeAll'), onClick: () => void navigate(notificationLink(n)) },
        })
      })
      source.onerror = () => {
        source?.close()
        if (disposed) return
        timer = setTimeout(() => {
          // Oturum düştüyse 401 → api istemcisi kullanıcıyı girişe yönlendirir ve bu bileşen kalkar
          void api.get('/auth/me').then(() => !disposed && connect(), () => undefined)
        }, RECONNECT_MS)
      }
    }

    connect()
    return () => {
      disposed = true
      clearTimeout(timer)
      source?.close()
    }
  }, [enabled, qc, t, navigate])
}
