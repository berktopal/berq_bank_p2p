import type { InputHTMLAttributes } from 'react'
import { cn } from '@/lib/cn'
import { inputStyles } from '@/components/ui/Field'
import { sanitizeIbanInput } from '@/lib/iban'

interface IbanInputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'value' | 'onChange'> {
  /** Normalize edilmiş değer: "TR" + 0-24 rakam */
  value: string
  onChange: (iban: string) => void
}

/**
 * "TR" sabit önekli IBAN alanı. Kullanıcı yalnızca rakam yazar; yapıştırılan tam IBAN (boşluklu, "TR"li)
 * otomatik temizlenir ve 4'lü gruplar halinde gösterilir.
 */
export function IbanInput({ value, onChange, className, ...rest }: IbanInputProps) {
  const digits = value.startsWith('TR') ? value.slice(2) : value
  const display = digits.replace(/(.{4})(?!$)/g, '$1 ')
  return (
    <div className={cn('relative', className)}>
      <span className="text-muted pointer-events-none absolute inset-y-0 left-3.5 grid place-items-center font-semibold" aria-hidden>
        TR
      </span>
      <input
        {...rest}
        inputMode="numeric"
        autoComplete="off"
        spellCheck={false}
        value={display}
        onChange={(e) => onChange(sanitizeIbanInput(e.target.value))}
        onPaste={(e) => {
          e.preventDefault()
          onChange(sanitizeIbanInput(e.clipboardData.getData('text')))
        }}
        className={cn(inputStyles, 'tabular pl-11 tracking-wide')}
      />
    </div>
  )
}
