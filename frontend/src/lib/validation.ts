import { z } from 'zod'

/** Backend'deki @Tckn doğrulamasının aynısı: kullanıcı göndermeden önce hatayı görür. */
export function isValidTckn(value: string): boolean {
  if (!/^[1-9]\d{10}$/.test(value)) return false
  const d = [...value].map(Number) as number[]
  const odd = d[0]! + d[2]! + d[4]! + d[6]! + d[8]!
  const even = d[1]! + d[3]! + d[5]! + d[7]!
  const tenth = (((odd * 7 - even) % 10) + 10) % 10
  const eleventh = d.slice(0, 10).reduce((a, b) => a + b, 0) % 10
  return d[9] === tenth && d[10] === eleventh
}

/** AuthDtos.PASSWORD_PATTERN ile aynı kural. */
export const PASSWORD_REGEX = /^(?=.*\p{L})(?=.*\d).{8,72}$/u
export const NAME_REGEX = /^\p{L}[\p{L} .'-]*$/u

export type Strength = 'weak' | 'medium' | 'strong'
export function passwordStrength(pw: string): Strength {
  let score = 0
  if (pw.length >= 8) score++
  if (pw.length >= 12) score++
  if (/\d/.test(pw) && /\p{L}/u.test(pw)) score++
  if (/[^\p{L}\d]/u.test(pw)) score++
  if (/\p{Lu}/u.test(pw) && /\p{Ll}/u.test(pw)) score++
  return score >= 4 ? 'strong' : score >= 3 ? 'medium' : 'weak'
}

// Mesajlar i18n anahtarıdır; bileşen tarafında t() ile çevrilir.
export const loginSchema = z.object({
  email: z.string().trim().min(1, 'validation.required').email('validation.email'),
  password: z.string().min(1, 'validation.required'),
})

export const registerSchema = z.object({
  firstName: z.string().trim().min(1, 'validation.required').max(50, 'validation.name').regex(NAME_REGEX, 'validation.name'),
  lastName: z.string().trim().min(1, 'validation.required').max(50, 'validation.name').regex(NAME_REGEX, 'validation.name'),
  tckn: z.string().trim().refine(isValidTckn, 'validation.tckn'),
  email: z.string().trim().min(1, 'validation.required').email('validation.email').max(254, 'validation.email'),
  password: z.string().regex(PASSWORD_REGEX, 'validation.password'),
})

export const changePasswordSchema = z
  .object({
    currentPassword: z.string().min(1, 'validation.required'),
    newPassword: z.string().regex(PASSWORD_REGEX, 'validation.password'),
  })
  .refine((v) => v.currentPassword !== v.newPassword, { path: ['newPassword'], message: 'validation.passwordsDiffer' })
