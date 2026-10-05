// Backend DTO'larının (p2p_transfer.*Dtos) TypeScript karşılıkları.

export type Currency = 'TRY' | 'USD' | 'EUR'

export interface User {
  id: number
  firstName: string
  lastName: string
  email: string
}

export interface Profile extends User {
  maskedTckn: string
  createdAt: string
}

export interface Account {
  id: number
  iban: string
  name: string
  balance: number
  currency: string
  createdAt: string
}

export interface AccountLookup {
  iban: string
  ownerName: string
  currency: string
  ownAccount: boolean
}

export const CATEGORIES = [
  'GENERAL',
  'RENT',
  'BILLS',
  'FOOD',
  'SHOPPING',
  'TRANSPORT',
  'HEALTH',
  'EDUCATION',
  'ENTERTAINMENT',
  'FAMILY',
  'SAVINGS',
] as const
export type Category = (typeof CATEGORIES)[number]

export type Direction = 'INCOMING' | 'OUTGOING'

export interface Transaction {
  id: number
  reference: string
  direction: Direction
  internal: boolean
  amount: number
  currency: string
  description?: string
  category: Category
  status: 'SUCCESS' | 'PENDING' | 'FAILED'
  createdAt: string
  account: { id: number; iban: string; name: string }
  counterparty: { name: string; iban: string }
  balanceAfter?: number
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface TransactionFilters {
  accountId?: number
  direction?: Direction
  from?: string
  to?: string
  category?: Category
  q?: string
  page?: number
  size?: number
}

export interface TransferInput {
  fromAccountId: number
  toIban: string
  amount: number
  description?: string
  category?: Category
}

export interface Contact {
  id: number
  nickname: string
  iban: string
  ownerName: string
  currency: string
  createdAt: string
}

export interface AnalyticsSummary {
  accountId: number
  currency: string
  since: string
  totalIncoming: number
  totalOutgoing: number
  net: number
  monthly: { month: string; incoming: number; outgoing: number }[]
  byCategory: { category: Category; total: number; count: number }[]
  topRecipients: { name: string; iban: string; total: number; count: number }[]
}

export interface PublicConfig {
  demo: boolean
  demoEmail?: string
  demoPassword?: string
}

// ---------- v3: bildirimler, para istekleri, talimatlar, bütçeler ----------

export type NotificationType =
  | 'MONEY_RECEIVED'
  | 'PAYMENT_REQUEST_RECEIVED'
  | 'PAYMENT_REQUEST_PAID'
  | 'PAYMENT_REQUEST_DECLINED'
  | 'PAYMENT_REQUEST_CANCELLED'
  | 'SCHEDULED_TRANSFER_EXECUTED'
  | 'SCHEDULED_TRANSFER_FAILED'
  | 'BUDGET_WARNING'
  | 'BUDGET_EXCEEDED'

export interface AppNotification {
  id: number
  type: NotificationType
  amount?: number
  currency?: string
  counterpartyName?: string
  category?: Category
  transactionId?: number
  paymentRequestId?: number
  scheduledTransferId?: number
  errorCode?: string
  read: boolean
  createdAt: string
}

export type PaymentRequestStatus = 'PENDING' | 'PAID' | 'DECLINED' | 'CANCELLED' | 'EXPIRED'

export interface PaymentRequest {
  id: number
  direction: Direction
  status: PaymentRequestStatus
  amount: number
  currency: string
  description?: string
  counterpartyName: string
  requesterIban: string
  requesterAccountId?: number
  transactionId?: number
  createdAt: string
  expiresAt: string
  respondedAt?: string
}

export type Frequency = 'ONCE' | 'WEEKLY' | 'MONTHLY'
export type ScheduleStatus = 'ACTIVE' | 'PAUSED' | 'COMPLETED' | 'CANCELLED'

export interface ScheduledTransfer {
  id: number
  from: { id: number; iban: string; name: string }
  toIban: string
  toName: string
  amount: number
  currency: string
  description?: string
  category: Category
  frequency: Frequency
  dayOfMonth: number
  nextRunDate: string
  endDate?: string
  status: ScheduleStatus
  lastRunAt?: string
  lastError?: string
  runCount: number
  createdAt: string
}

export interface Budget {
  id: number
  category: Category
  currency: string
  monthlyLimit: number
  spent: number
  remaining: number
  percent: number
  status: 'OK' | 'WARNING' | 'EXCEEDED'
}
