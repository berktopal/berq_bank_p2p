/** Yalnızca uygulama içi yollara yönlendirmeye izin verir (open redirect koruması). */
export function safeNext(raw: string | null): string {
  if (!raw) return '/app'
  try {
    const decoded = decodeURIComponent(raw)
    return decoded.startsWith('/app') && !decoded.startsWith('//') ? decoded : '/app'
  } catch {
    return '/app'
  }
}
