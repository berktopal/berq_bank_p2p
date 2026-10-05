import { useId, useState, type InputHTMLAttributes, type ReactNode, type SelectHTMLAttributes } from 'react'
import { Eye, EyeOff } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { cn } from '@/lib/cn'

interface FieldProps {
  label: ReactNode
  hint?: ReactNode
  error?: string
  optional?: boolean
  className?: string
  children: (ids: { id: string; describedBy?: string; invalid: boolean }) => ReactNode
}

/** Etiket + ipucu + hata mesajını erişilebilir şekilde (aria-describedby) girişe bağlar. */
export function Field({ label, hint, error, optional, className, children }: FieldProps) {
  const { t } = useTranslation()
  const id = useId()
  const hintId = hint ? `${id}-hint` : undefined
  const errorId = error ? `${id}-error` : undefined
  const describedBy = [errorId, hintId].filter(Boolean).join(' ') || undefined
  return (
    <div className={cn('flex flex-col gap-1.5', className)}>
      <label htmlFor={id} className="text-sm font-medium">
        {label}
        {optional && <span className="text-muted font-normal"> ({t('common.optional')})</span>}
      </label>
      {children({ id, describedBy, invalid: Boolean(error) })}
      {error ? (
        <p id={errorId} className="text-danger text-sm" role="alert">
          {error}
        </p>
      ) : (
        hint && (
          <p id={hintId} className="text-muted text-sm">
            {hint}
          </p>
        )
      )}
    </div>
  )
}

export const inputStyles = cn(
  'h-11 w-full rounded-xl border border-border bg-surface px-3.5 text-base text-text sm:text-sm',
  'placeholder:text-muted/70 transition',
  'focus:border-primary focus:outline-none focus:ring-3 focus:ring-primary/20',
  'aria-[invalid=true]:border-danger aria-[invalid=true]:focus:ring-danger/20',
  'disabled:opacity-60',
)

export function Input({ className, ...props }: InputHTMLAttributes<HTMLInputElement>) {
  return <input className={cn(inputStyles, className)} {...props} />
}

export function Select({ className, children, ...props }: SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <select className={cn(inputStyles, 'appearance-none bg-no-repeat pr-9', 'select-chevron', className)} {...props}>
      {children}
    </select>
  )
}

export function PasswordInput(props: InputHTMLAttributes<HTMLInputElement>) {
  const { t } = useTranslation()
  const [visible, setVisible] = useState(false)
  return (
    <div className="relative">
      <Input {...props} type={visible ? 'text' : 'password'} className="pr-11" />
      <button
        type="button"
        onClick={() => setVisible((v) => !v)}
        className="text-muted hover:text-text absolute inset-y-0 right-0 grid w-11 place-items-center rounded-r-xl"
        aria-label={visible ? t('auth.hidePassword') : t('auth.showPassword')}
        aria-pressed={visible}
      >
        {visible ? <EyeOff className="size-5" /> : <Eye className="size-5" />}
      </button>
    </div>
  )
}
