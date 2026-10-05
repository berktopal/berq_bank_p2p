import type { TFunction } from 'i18next'
import { codeMessage } from '@/lib/errors'
import { formatMoney } from '@/lib/format'
import type { AppNotification } from '@/lib/types'

/** Bildirim, kullanıcının dilinde ve para biçiminde cümleye dönüştürülür (sunucu metin saklamaz). */
export function notificationText(t: TFunction, n: AppNotification): string {
  const amount = n.amount !== undefined && n.currency ? formatMoney(n.amount, n.currency) : ''
  return t(`notifications.${n.type}`, {
    name: n.counterpartyName ?? '',
    amount,
    category: n.category ? t(`categories.${n.category}`) : '',
    reason: codeMessage(t, n.errorCode),
  })
}

/** Bildirime tıklayınca gidilecek sayfa. */
export function notificationLink(n: AppNotification): string {
  switch (n.type) {
    case 'PAYMENT_REQUEST_RECEIVED':
    case 'PAYMENT_REQUEST_CANCELLED':
      return '/app/requests?tab=IN'
    case 'PAYMENT_REQUEST_DECLINED':
      return '/app/requests?tab=OUT'
    case 'SCHEDULED_TRANSFER_FAILED':
      return '/app/scheduled'
    case 'BUDGET_WARNING':
    case 'BUDGET_EXCEEDED':
      return '/app/budgets'
    default:
      return n.transactionId ? `/app/transactions/${n.transactionId}` : '/app'
  }
}
