import { isValidTckn, passwordStrength, registerSchema } from './validation'

describe('isValidTckn mirrors the backend algorithm', () => {
  it.each(['10000000146', '11111111110'])('accepts %s', (v) => expect(isValidTckn(v)).toBe(true))
  it.each(['12345678901', '01234567890', '1000000014', '10000000147'])('rejects %s', (v) => expect(isValidTckn(v)).toBe(false))
})

it('rates password strength', () => {
  expect(passwordStrength('abc')).toBe('weak')
  expect(passwordStrength('secret123')).toBe('weak')
  expect(passwordStrength('secretpass123')).toBe('medium')
  expect(passwordStrength('Secret123!long')).toBe('strong')
})

it('register schema reports i18n keys for invalid fields', () => {
  const result = registerSchema.safeParse({ firstName: '', lastName: 'X', tckn: '123', email: 'nope', password: 'short' })
  expect(result.success).toBe(false)
  // Alan başına ilk hata (formda gösterilen)
  const issues: Record<string, string> = {}
  for (const i of result.error!.issues) issues[String(i.path[0])] ??= i.message
  expect(issues).toMatchObject({
    firstName: 'validation.required',
    tckn: 'validation.tckn',
    email: 'validation.email',
    password: 'validation.password',
  })
})
