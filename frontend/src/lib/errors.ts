import type { TFunction } from 'i18next'
import { ApiError } from './api'
import { formatMoney, formatTime } from './format'

const KNOWN = new Set([
  'NETWORK_ERROR',
  'VALIDATION_FAILED',
  'MALFORMED_REQUEST',
  'UNAUTHENTICATED',
  'ACCESS_DENIED',
  'NOT_FOUND',
  'INTERNAL_ERROR',
  'INVALID_CREDENTIALS',
  'USER_LOCKED',
  'EMAIL_TAKEN',
  'TCKN_TAKEN',
  'WRONG_CURRENT_PASSWORD',
  'ACCOUNT_NOT_FOUND',
  'IBAN_NOT_FOUND',
  'ACCOUNT_LIMIT_REACHED',
  'SAME_ACCOUNT',
  'INSUFFICIENT_FUNDS',
  'CURRENCY_MISMATCH',
  'DAILY_LIMIT_EXCEEDED',
  'TRANSACTION_NOT_FOUND',
  'IDEMPOTENCY_CONFLICT',
  'CONTACT_NOT_FOUND',
  'CONTACT_EXISTS',
  'CONTACT_IS_SELF',
  'RATE_LIMITED',
  'REQUEST_NOT_FOUND',
  'REQUEST_NOT_PENDING',
  'REQUEST_SELF',
  'SCHEDULE_NOT_FOUND',
  'SCHEDULE_INVALID_DATES',
  'SCHEDULE_NOT_ACTIVE',
  'BUDGET_NOT_FOUND',
  'NOTIFICATION_NOT_FOUND',
] as const)

type KnownCode = typeof KNOWN extends Set<infer T> ? T : never

/** API hata kodunu kullanıcının dilinde, bağlamıyla (kilit saati, kalan limit…) birlikte metne çevirir. */
export function errorMessage(t: TFunction, error: unknown, currency = 'TRY'): string {
  if (!(error instanceof ApiError)) return t('errors.INTERNAL_ERROR')
  if (!KNOWN.has(error.code as KnownCode)) return t('errors.INTERNAL_ERROR')
  const code = error.code as KnownCode
  switch (code) {
    case 'USER_LOCKED': {
      const until = typeof error.data.lockedUntil === 'string' ? formatTime(error.data.lockedUntil) : '—'
      return t('errors.USER_LOCKED', { time: until })
    }
    case 'DAILY_LIMIT_EXCEEDED':
      return t('errors.DAILY_LIMIT_EXCEEDED', { remaining: formatMoney(Number(error.data.remaining ?? 0), currency) })
    case 'RATE_LIMITED':
      return t('errors.RATE_LIMITED', { seconds: Number(error.data.retryAfterSeconds ?? 60) })
    case 'ACCOUNT_LIMIT_REACHED':
      return t('errors.ACCOUNT_LIMIT_REACHED', { max: Number(error.data.max ?? 5) })
    default:
      return t(`errors.${code}`)
  }
}

/** Form hata mesajı: zod şemalarında i18n anahtarı ("validation.email"), sunucudan gelende hazır metin olur. */
export function fieldMessage(t: TFunction, message?: string): string | undefined {
  if (!message) return undefined
  return /^[a-zA-Z]+\.[\w.]+$/.test(message) ? t(message as never) : message
}

/** Bildirimlerde ve talimatlarda saklanan hata kodunu (ör. INSUFFICIENT_FUNDS) okunur metne çevirir. */
export function codeMessage(t: TFunction, code: string | undefined): string {
  return code && KNOWN.has(code as KnownCode) ? t(`errors.${code as KnownCode}` as never) : t('errors.INTERNAL_ERROR')
}
