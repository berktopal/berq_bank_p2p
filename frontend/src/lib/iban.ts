/** TR IBAN yardımcıları (backend: p2p_transfer.common.Iban). */

export const TR_IBAN_LENGTH = 26

export function normalizeIban(value: string): string {
  return value.replace(/\s+/g, '').toUpperCase()
}

/** "TR33 0006 1005 ..." şeklinde 4'lü gruplar. */
export function formatIban(value: string): string {
  return normalizeIban(value).replace(/(.{4})(?!$)/g, '$1 ')
}

export function isTrIbanFormat(value: string): boolean {
  return /^TR\d{24}$/.test(normalizeIban(value))
}

/** ISO 13616 mod-97 kontrolü; büyük sayılar için parça parça hesaplanır. */
export function hasValidChecksum(value: string): boolean {
  const iban = normalizeIban(value)
  if (!isTrIbanFormat(iban)) return false
  const rearranged = iban.slice(4) + iban.slice(0, 4)
  const numeric = rearranged.replace(/[A-Z]/g, (c) => String(c.charCodeAt(0) - 55))
  let remainder = 0
  for (const digit of numeric) {
    remainder = (remainder * 10 + Number(digit)) % 97
  }
  return remainder === 1
}

/**
 * Kullanıcının yazdığı/yapıştırdığı metni "TR" + en fazla 24 rakama indirger.
 * "TR" yazılmasa da kabul edilir; yapıştırılan boşluk ve tireler atılır.
 */
export function sanitizeIbanInput(value: string): string {
  const upper = value.toUpperCase().replace(/[\s-]/g, '')
  const digits = (upper.startsWith('TR') ? upper.slice(2) : upper).replace(/\D/g, '').slice(0, 24)
  return `TR${digits}`
}

/** Listelerde kısa gösterim: TR33 •••• 1326 */
export function shortIban(value: string): string {
  const iban = normalizeIban(value)
  return `${iban.slice(0, 4)} •••• ${iban.slice(-4)}`
}
