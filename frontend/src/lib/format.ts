import i18n from '@/i18n'

function locale(): string {
  return i18n.language === 'en' ? 'en-US' : 'tr-TR'
}

const currencyCache = new Map<string, Intl.NumberFormat>()

/** Para birimiyle biçimlendirir: ₺1.250,50 / $1,250.50. Bilinmeyen kodlarda düz sayıya düşer. */
export function formatMoney(amount: number, currency: string, opts: { signed?: boolean } = {}): string {
  const key = `${locale()}|${currency}`
  let fmt = currencyCache.get(key)
  if (!fmt) {
    try {
      // narrowSymbol: en-US varsayılanı TL için "TRY 9,000.00" yazar; her dilde ₺ / $ / € gösterilsin
      fmt = new Intl.NumberFormat(locale(), { style: 'currency', currency, currencyDisplay: 'narrowSymbol', minimumFractionDigits: 2 })
    } catch {
      fmt = new Intl.NumberFormat(locale(), { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    }
    currencyCache.set(key, fmt)
  }
  const text = fmt.format(Math.abs(amount))
  if (!opts.signed) return amount < 0 ? `−${text}` : text
  return `${amount < 0 ? '−' : '+'}${text}`
}

export function formatNumber(value: number, fractionDigits = 2): string {
  return new Intl.NumberFormat(locale(), {
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  }).format(value)
}

export function formatDate(iso: string, style: 'short' | 'long' = 'short'): string {
  return new Intl.DateTimeFormat(locale(), {
    dateStyle: style === 'long' ? 'long' : 'medium',
    timeZone: 'Europe/Istanbul',
  }).format(new Date(iso))
}

export function formatDateTime(iso: string): string {
  return new Intl.DateTimeFormat(locale(), {
    dateStyle: 'medium',
    timeStyle: 'short',
    timeZone: 'Europe/Istanbul',
  }).format(new Date(iso))
}

export function formatTime(iso: string): string {
  return new Intl.DateTimeFormat(locale(), { timeStyle: 'short', timeZone: 'Europe/Istanbul' }).format(new Date(iso))
}

/** "2026-05" → "May 2026" / "Mayıs 2026" (short: "May") */
export function formatMonth(yearMonth: string, short = false): string {
  const [y, m] = yearMonth.split('-').map(Number)
  return new Intl.DateTimeFormat(locale(), { month: short ? 'short' : 'long', year: short ? undefined : 'numeric' }).format(
    new Date(Date.UTC(y ?? 1970, (m ?? 1) - 1, 15)),
  )
}

/** İstanbul saatine göre YYYY-MM-DD (gün gruplama için). */
export function dayKey(iso: string): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'Europe/Istanbul' }).format(new Date(iso))
}

/**
 * Kullanıcının yazdığı tutarı ayrıştırır. Hem "1.250,50" (TR) hem "1,250.50" (EN) hem de "1250.5" kabul edilir:
 * son ayırıcı, ardından 1-2 hane geliyorsa ondalık kabul edilir.
 */
export function parseAmount(input: string): number | null {
  const cleaned = input.replace(/[\s₺$€]/g, '')
  if (!cleaned) return null
  if (!/^[\d.,]+$/.test(cleaned)) return null
  const lastSep = Math.max(cleaned.lastIndexOf(','), cleaned.lastIndexOf('.'))
  let normalized: string
  if (lastSep >= 0 && cleaned.length - lastSep - 1 <= 2 && cleaned.length - lastSep - 1 > 0) {
    normalized = cleaned.slice(0, lastSep).replace(/[.,]/g, '') + '.' + cleaned.slice(lastSep + 1)
  } else if (lastSep === cleaned.length - 1) {
    normalized = cleaned.slice(0, -1).replace(/[.,]/g, '')
  } else {
    normalized = cleaned.replace(/[.,]/g, '')
  }
  const value = Number(normalized)
  return Number.isFinite(value) ? Math.round(value * 100) / 100 : null
}

export function initials(name: string): string {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0]?.toLocaleUpperCase('tr-TR'))
    .join('')
}
