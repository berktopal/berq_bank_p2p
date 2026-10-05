/**
 * Backend ile konuşan tek nokta.
 * - Oturum HttpOnly çerezde (BQSESSION) taşınır; JS ona dokunmaz.
 * - Durum değiştiren isteklere XSRF-TOKEN çerezindeki değer X-XSRF-TOKEN başlığıyla eklenir.
 * - Hatalar RFC 9457 Problem Details olarak gelir ve ApiError'a dönüştürülür.
 */

const CSRF_COOKIE = 'XSRF-TOKEN'
const CSRF_HEADER = 'X-XSRF-TOKEN'

export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly fieldErrors: Record<string, string>
  readonly data: Record<string, unknown>

  constructor(status: number, code: string, message: string, data: Record<string, unknown> = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.data = data
    this.fieldErrors = (data.errors as Record<string, string> | undefined) ?? {}
  }
}

export const NETWORK_ERROR = 'NETWORK_ERROR'

type UnauthorizedListener = () => void
const unauthorizedListeners = new Set<UnauthorizedListener>()

/** Oturum düştüğünde (401) uygulamanın tepki vermesi için. */
export function onUnauthorized(listener: UnauthorizedListener): () => void {
  unauthorizedListeners.add(listener)
  return () => unauthorizedListeners.delete(listener)
}

function readCookie(name: string): string | undefined {
  return document.cookie
    .split('; ')
    .find((c) => c.startsWith(`${name}=`))
    ?.slice(name.length + 1)
}

let csrfRequest: Promise<void> | null = null

async function ensureCsrfToken(force = false): Promise<string | undefined> {
  if (force || !readCookie(CSRF_COOKIE)) {
    // Aynı anda gelen istekler tek bir token isteğini paylaşır
    csrfRequest ??= fetch('/api/auth/csrf', { credentials: 'same-origin' })
      .then(() => undefined)
      .finally(() => {
        csrfRequest = null
      })
    await csrfRequest
  }
  const raw = readCookie(CSRF_COOKIE)
  return raw ? decodeURIComponent(raw) : undefined
}

export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  body?: unknown
  headers?: Record<string, string>
  signal?: AbortSignal
  /** 401 yanıtında global "oturum düştü" akışını tetikleme (ör. /auth/me, /auth/login). */
  silentUnauthorized?: boolean
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}, retried = false): Promise<T> {
  const method = options.method ?? 'GET'
  const headers = new Headers(options.headers)
  headers.set('Accept', 'application/json')
  if (options.body !== undefined) headers.set('Content-Type', 'application/json')
  if (method !== 'GET') {
    const token = await ensureCsrfToken(retried)
    if (token) headers.set(CSRF_HEADER, token)
  }

  let response: Response
  try {
    response = await fetch(`/api${path}`, {
      method,
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
      credentials: 'same-origin',
      signal: options.signal,
    })
  } catch (e) {
    if (e instanceof DOMException && e.name === 'AbortError') throw e
    throw new ApiError(0, NETWORK_ERROR, 'Network error')
  }

  if (response.ok) {
    if (response.status === 204) return undefined as T
    const text = await response.text()
    return (text ? JSON.parse(text) : undefined) as T
  }

  const error = await toApiError(response)
  // Token süresi dolmuş/dönmüş olabilir: bir kez taze token ile tekrar dene
  if (error.status === 403 && method !== 'GET' && !retried) {
    return apiRequest<T>(path, options, true)
  }
  if (error.status === 401 && !options.silentUnauthorized) {
    unauthorizedListeners.forEach((l) => l())
  }
  throw error
}

async function toApiError(response: Response): Promise<ApiError> {
  let body: Record<string, unknown> = {}
  try {
    body = (await response.json()) as Record<string, unknown>
  } catch {
    // gövde JSON değil (ör. proxy hatası)
  }
  const code = typeof body.code === 'string' ? body.code : `HTTP_${response.status}`
  const detail = typeof body.detail === 'string' ? body.detail : response.statusText
  return new ApiError(response.status, code, detail, body)
}

export function toQuery(params: object): string {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') search.set(key, String(value))
  })
  const s = search.toString()
  return s ? `?${s}` : ''
}

export const api = {
  get: <T>(path: string, options?: Omit<RequestOptions, 'method' | 'body'>) => apiRequest<T>(path, options),
  post: <T>(path: string, body?: unknown, options?: Omit<RequestOptions, 'method' | 'body'>) =>
    apiRequest<T>(path, { ...options, method: 'POST', body }),
  put: <T>(path: string, body?: unknown) => apiRequest<T>(path, { method: 'PUT', body }),
  patch: <T>(path: string, body?: unknown) => apiRequest<T>(path, { method: 'PATCH', body }),
  delete: <T = void>(path: string) => apiRequest<T>(path, { method: 'DELETE' }),
}
