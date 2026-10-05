import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import { db, demoUser, page, RECIPIENT_IBAN, transaction } from './fixtures'
import type { ScheduledTransfer } from '@/lib/types'

const problem = (status: number, code: string, extra: Record<string, unknown> = {}) =>
  HttpResponse.json({ status, code, detail: code, ...extra }, { status })

const requireUser = () => (db.user ? null : problem(401, 'UNAUTHENTICATED'))

export const handlers = [
  http.get('/api/auth/csrf', () => HttpResponse.json({ headerName: 'X-XSRF-TOKEN', token: 'test-csrf-token' })),
  http.get('/api/public/config', () => HttpResponse.json({ demo: false })),
  http.get('/api/auth/me', () => requireUser() ?? HttpResponse.json(db.user)),
  http.post('/api/auth/login', async ({ request }) => {
    const body = (await request.json()) as { email: string; password: string }
    if (body.email === demoUser.email && body.password === 'Demo1234') {
      db.user = demoUser
      return HttpResponse.json(demoUser)
    }
    return problem(401, 'INVALID_CREDENTIALS')
  }),
  http.post('/api/auth/logout', () => {
    db.user = null
    return new HttpResponse(null, { status: 204 })
  }),
  http.get('/api/accounts', () => requireUser() ?? HttpResponse.json(db.accounts)),
  http.get('/api/accounts/lookup', ({ request }) => {
    const iban = new URL(request.url).searchParams.get('iban')
    if (iban === RECIPIENT_IBAN) return HttpResponse.json({ iban, ownerName: 'Ayşe K***', currency: 'TRY', ownAccount: false })
    return problem(404, 'IBAN_NOT_FOUND')
  }),
  http.get('/api/contacts', () => requireUser() ?? HttpResponse.json(db.contacts)),
  http.get('/api/transactions', () => requireUser() ?? HttpResponse.json(page(db.transactions))),
  http.post('/api/transactions/transfer', async ({ request }) => {
    const body = (await request.json()) as { fromAccountId: number; amount: number; toIban: string }
    db.transferRequests.push({ body, idempotencyKey: request.headers.get('Idempotency-Key') })
    const from = db.accounts.find((a) => a.id === body.fromAccountId)!
    if (body.amount > from.balance) return problem(422, 'INSUFFICIENT_FUNDS', { balance: from.balance })
    from.balance -= body.amount
    const tx = transaction({ id: 500 + db.transferRequests.length, amount: body.amount, balanceAfter: from.balance })
    db.transactions.unshift(tx)
    return HttpResponse.json(tx, { status: 201 })
  }),
  // ---------- v3 ----------
  http.get('/api/notifications/unread-count', () =>
    requireUser() ?? HttpResponse.json({ count: db.notifications.filter((n) => !n.read).length }),
  ),
  http.get('/api/notifications', () => requireUser() ?? HttpResponse.json(page(db.notifications))),
  http.post('/api/notifications/read-all', () => {
    db.notifications.forEach((n) => (n.read = true))
    db.calls.push({ method: 'POST', path: '/notifications/read-all' })
    return new HttpResponse(null, { status: 204 })
  }),
  http.post('/api/notifications/:id/read', ({ params }) => {
    const n = db.notifications.find((x) => x.id === Number(params.id))
    if (n) n.read = true
    return new HttpResponse(null, { status: 204 })
  }),
  http.get('/api/payment-requests/pending-count', () =>
    requireUser() ??
    HttpResponse.json({ count: db.paymentRequests.filter((r) => r.direction === 'INCOMING' && r.status === 'PENDING').length }),
  ),
  http.get('/api/payment-requests', ({ request }) => {
    const url = new URL(request.url)
    const role = url.searchParams.get('role')
    const status = url.searchParams.get('status')
    const list = db.paymentRequests.filter(
      (r) => (role === 'IN' ? r.direction === 'INCOMING' : r.direction === 'OUTGOING') && (!status || r.status === status),
    )
    return requireUser() ?? HttpResponse.json(page(list))
  }),
  http.post('/api/payment-requests/:id/pay', async ({ params, request }) => {
    const body = await request.json()
    db.calls.push({ method: 'POST', path: `/payment-requests/${String(params.id)}/pay`, body })
    const r = db.paymentRequests.find((x) => x.id === Number(params.id))!
    r.status = 'PAID'
    return HttpResponse.json(r)
  }),
  http.post('/api/payment-requests/:id/decline', ({ params }) => {
    const r = db.paymentRequests.find((x) => x.id === Number(params.id))!
    r.status = 'DECLINED'
    db.calls.push({ method: 'POST', path: `/payment-requests/${String(params.id)}/decline` })
    return HttpResponse.json(r)
  }),
  http.get('/api/budgets', () => requireUser() ?? HttpResponse.json(db.budgets)),
  http.put('/api/budgets', async ({ request }) => {
    const body = (await request.json()) as { category: 'FOOD'; currency: string; monthlyLimit: number }
    db.calls.push({ method: 'PUT', path: '/budgets', body })
    const b = { id: 9, ...body, spent: 0, remaining: body.monthlyLimit, percent: 0, status: 'OK' as const }
    db.budgets.push(b)
    return HttpResponse.json(b)
  }),
  http.get('/api/scheduled-transfers', () => requireUser() ?? HttpResponse.json(db.schedules)),
  http.post('/api/scheduled-transfers', async ({ request }) => {
    const body = (await request.json()) as { amount: number; frequency: 'MONTHLY'; startDate: string; description?: string }
    db.calls.push({ method: 'POST', path: '/scheduled-transfers', body })
    const s: ScheduledTransfer = {
      id: 77,
      from: { id: 1, iban: 'TR200099907639784704692023', name: 'Vadesiz TL Hesabı' },
      toIban: RECIPIENT_IBAN,
      toName: 'Ayşe Kaya',
      amount: body.amount,
      currency: 'TRY',
      description: body.description,
      category: 'RENT',
      frequency: body.frequency,
      dayOfMonth: Number(body.startDate.slice(8, 10)),
      nextRunDate: body.startDate,
      status: 'ACTIVE',
      runCount: 0,
      createdAt: '2026-10-03T07:30:00Z',
    }
    db.schedules.push(s)
    return HttpResponse.json(s, { status: 201 })
  }),
]

export const server = setupServer(...handlers)
