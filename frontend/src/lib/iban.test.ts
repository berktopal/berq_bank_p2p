import { formatIban, hasValidChecksum, isTrIbanFormat, sanitizeIbanInput, shortIban } from './iban'

describe('iban', () => {
  it('validates mod-97 checksum like the backend', () => {
    expect(hasValidChecksum('TR330006100519786457841326')).toBe(true)
    expect(hasValidChecksum('TR340006100519786457841326')).toBe(false)
  })

  it('accepts pasted IBANs with spaces, dashes, lowercase or without TR', () => {
    expect(sanitizeIbanInput('tr33 0006-1005 1978 6457 8413 26')).toBe('TR330006100519786457841326')
    expect(sanitizeIbanInput('330006100519786457841326')).toBe('TR330006100519786457841326')
    expect(sanitizeIbanInput('TR33000610051978645784132699999')).toBe('TR330006100519786457841326')
  })

  it('formats and shortens for display', () => {
    expect(formatIban('TR330006100519786457841326')).toBe('TR33 0006 1005 1978 6457 8413 26')
    expect(shortIban('TR330006100519786457841326')).toBe('TR33 •••• 1326')
    expect(isTrIbanFormat('TR12')).toBe(false)
  })
})
