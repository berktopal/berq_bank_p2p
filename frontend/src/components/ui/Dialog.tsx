import { useEffect, useId, useRef, type ReactNode } from 'react'
import { X } from 'lucide-react'
import { useTranslation } from 'react-i18next'

interface DialogProps {
  open: boolean
  onClose: () => void
  title: ReactNode
  description?: ReactNode
  children?: ReactNode
  footer?: ReactNode
}

/**
 * Native <dialog> + showModal(): odak tuzağı, Esc ile kapanma ve arka planın etkisizleşmesi
 * tarayıcı tarafından sağlanır; ek kütüphane gerekmez.
 */
export function Dialog({ open, onClose, title, description, children, footer }: DialogProps) {
  const { t } = useTranslation()
  const ref = useRef<HTMLDialogElement>(null)
  const titleId = useId()
  const descId = useId()

  useEffect(() => {
    const dialog = ref.current
    if (!dialog) return
    if (open && !dialog.open) dialog.showModal?.()
    if (!open && dialog.open) dialog.close?.()
  }, [open])

  return (
    <dialog
      ref={ref}
      onClose={onClose}
      onCancel={(e) => {
        e.preventDefault()
        onClose()
      }}
      onClick={(e) => {
        // Arka plana tıklayınca kapat
        if (e.target === ref.current) onClose()
      }}
      aria-labelledby={titleId}
      aria-describedby={description ? descId : undefined}
      className="bg-surface text-text border-border m-auto w-[calc(100%-2rem)] max-w-md rounded-2xl border p-0 shadow-2xl"
    >
      {open && (
        <div className="flex flex-col gap-5 p-6">
          <div className="flex items-start justify-between gap-4">
            <div>
              <h2 id={titleId} className="text-lg font-semibold">
                {title}
              </h2>
              {description && (
                <p id={descId} className="text-muted mt-1 text-sm">
                  {description}
                </p>
              )}
            </div>
            <button
              type="button"
              onClick={onClose}
              className="text-muted hover:bg-surface-2 hover:text-text -m-2 rounded-lg p-2"
              aria-label={t('common.close')}
            >
              <X className="size-5" />
            </button>
          </div>
          {children}
          {footer && <div className="flex justify-end gap-2">{footer}</div>}
        </div>
      )}
    </dialog>
  )
}
