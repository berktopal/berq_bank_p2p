import type { Account, AppNotification, Budget, Contact, Page, PaymentRequest, ScheduledTransfer, Transaction, User } from '@/lib/types'

/** MSW handler'larının paylaştığı, her testte sıfırlanan bellek içi "veritabanı". */
export const demoUser: User = { id: 1, firstName: 'Deniz', lastName: 'Yılmaz', email: 'demo@berqbank.dev' }

export const RECIPIENT_IBAN = 'TR330006100519786457841326'

export interface Db {
  user: User | null
  accounts: Account[]
  contacts: Contact[]
  transactions: Transaction[]
  transferRequests: { body: unknown; idempotencyKey: string | null }[]
  notifications: AppNotification[]
  paymentRequests: PaymentRequest[]
  schedules: ScheduledTransfer[]
  budgets: Budget[]
  calls: { method: string; path: string; body?: unknown }[]
}

export const db: Db = {
  user: null,
  accounts: [],
  contacts: [],
  transactions: [],
  transferRequests: [],
  notifications: [],
  paymentRequests: [],
  schedules: [],
  budgets: [],
  calls: [],
}

export function resetDb() {
  db.user = null
  db.accounts = [
    { id: 1, iban: 'TR200099907639784704692023', name: 'Vadesiz TL Hesabı', balance: 5000, currency: 'TRY', createdAt: '2026-01-01T09:00:00Z' },
    { id: 2, iban: 'TR660099901720084927441451', name: 'Dolar Hesabı', balance: 300, currency: 'USD', createdAt: '2026-01-01T09:00:00Z' },
  ]
  db.contacts = [
    { id: 7, nickname: 'Ayşe', iban: RECIPIENT_IBAN, ownerName: 'Ayşe K***', currency: 'TRY', createdAt: '2026-01-01T09:00:00Z' },
  ]
  db.transactions = []
  db.transferRequests = []
  db.notifications = []
  db.paymentRequests = []
  db.schedules = []
  db.budgets = []
  db.calls = []
}

export function paymentRequest(overrides: Partial<PaymentRequest> = {}): PaymentRequest {
  return {
    id: 31,
    direction: 'INCOMING',
    status: 'PENDING',
    amount: 250,
    currency: 'TRY',
    description: 'Konser bileti',
    counterpartyName: 'Ayşe Kaya',
    requesterIban: RECIPIENT_IBAN,
    createdAt: '2026-10-03T07:30:00Z',
    expiresAt: '2026-10-10T07:30:00Z',
    ...overrides,
  }
}

export function notification(overrides: Partial<AppNotification> = {}): AppNotification {
  return {
    id: 1,
    type: 'MONEY_RECEIVED',
    amount: 1500,
    currency: 'TRY',
    counterpartyName: 'Can Öztürk',
    transactionId: 100,
    read: false,
    createdAt: '2026-10-03T07:30:00Z',
    ...overrides,
  }
}

export function transaction(overrides: Partial<Transaction> = {}): Transaction {
  return {
    id: 100,
    reference: 'BQ7K2M9X4TQ1HZ8C',
    direction: 'OUTGOING',
    internal: false,
    amount: 250,
    currency: 'TRY',
    description: 'Kira',
    category: 'RENT',
    status: 'SUCCESS',
    createdAt: '2026-10-03T07:30:00Z',
    account: { id: 1, iban: 'TR200099907639784704692023', name: 'Vadesiz TL Hesabı' },
    counterparty: { name: 'Ayşe Kaya', iban: RECIPIENT_IBAN },
    balanceAfter: 4750,
    ...overrides,
  }
}

export function page<T>(content: T[], pageNo = 0, size = 20, total = content.length): Page<T> {
  return { content, page: pageNo, size, totalElements: total, totalPages: Math.max(1, Math.ceil(total / size)) }
}
