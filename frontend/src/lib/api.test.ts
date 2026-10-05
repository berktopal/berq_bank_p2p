import { http, HttpResponse } from 'msw'
import { server } from '@/test/server'
import { api, ApiError, onUnauthorized } from './api'

describe('api client', () => {
  it('attaches the CSRF token from the cookie to state-changing requests', async () => {
    let header: string | null = null
    server.use(
      http.post('/api/echo', ({ request }) => {
        header = request.headers.get('X-XSRF-TOKEN')
        return HttpResponse.json({ ok: true })
      }),
    )
    await api.post('/echo', { a: 1 })
    expect(header).toBe('test-csrf-token')
  })

  it('refreshes the token and retries once on 403', async () => {
    let calls = 0
    server.use(
      http.post('/api/echo', () => {
        calls += 1
        return calls === 1 ? HttpResponse.json({ code: 'ACCESS_DENIED' }, { status: 403 }) : HttpResponse.json({ ok: true })
      }),
    )
    await expect(api.post('/echo')).resolves.toEqual({ ok: true })
    expect(calls).toBe(2)
  })

  it('turns problem details into ApiError with code and field errors', async () => {
    server.use(
      http.post('/api/echo', () =>
        HttpResponse.json({ status: 400, code: 'VALIDATION_FAILED', detail: 'x', errors: { amount: 'too small' } }, { status: 400 }),
      ),
    )
    const error = await api.post('/echo').catch((e: unknown) => e)
    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ status: 400, code: 'VALIDATION_FAILED', fieldErrors: { amount: 'too small' } })
  })

  it('notifies listeners on 401 unless the call is silent', async () => {
    server.use(http.get('/api/secret', () => HttpResponse.json({ code: 'UNAUTHENTICATED' }, { status: 401 })))
    const listener = vi.fn()
    const off = onUnauthorized(listener)
    await api.get('/secret', { silentUnauthorized: true }).catch(() => {})
    expect(listener).not.toHaveBeenCalled()
    await api.get('/secret').catch(() => {})
    expect(listener).toHaveBeenCalledTimes(1)
    off()
  })

  it('reports network failures with a dedicated code', async () => {
    server.use(http.get('/api/down', () => HttpResponse.error()))
    await expect(api.get('/down')).rejects.toMatchObject({ code: 'NETWORK_ERROR', status: 0 })
  })
})
