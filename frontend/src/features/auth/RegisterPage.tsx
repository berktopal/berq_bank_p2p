import { zodResolver } from '@hookform/resolvers/zod'
import { useForm, useWatch } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate } from 'react-router'
import { toast } from 'sonner'
import type { z } from 'zod'
import { Button } from '@/components/ui/Button'
import { Field, Input, PasswordInput } from '@/components/ui/Field'
import { ApiError } from '@/lib/api'
import { cn } from '@/lib/cn'
import { errorMessage, fieldMessage } from '@/lib/errors'
import { passwordStrength, registerSchema } from '@/lib/validation'
import { AuthCard } from './AuthCard'
import { useRegister } from './api'

type RegisterForm = z.infer<typeof registerSchema>
const FIELDS = ['firstName', 'lastName', 'tckn', 'email', 'password'] as const

export function RegisterPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const registerUser = useRegister()
  const {
    register,
    handleSubmit,
    setError,
    control,
    formState: { errors },
  } = useForm<RegisterForm>({
    resolver: zodResolver(registerSchema),
    defaultValues: { firstName: '', lastName: '', tckn: '', email: '', password: '' },
  })
  const password = useWatch({ control, name: 'password' })
  const strength = password ? passwordStrength(password) : null

  const onSubmit = handleSubmit((values) =>
    registerUser.mutate(values, {
      onSuccess: (user) => {
        toast.success(t('auth.welcome', { name: user.firstName }))
        void navigate('/app', { replace: true })
      },
      onError: (error) => {
        // Sunucu tarafı alan hatalarını ilgili inputların altında göster
        if (error instanceof ApiError) {
          if (error.code === 'EMAIL_TAKEN') setError('email', { message: 'errors.EMAIL_TAKEN' })
          if (error.code === 'TCKN_TAKEN') setError('tckn', { message: 'errors.TCKN_TAKEN' })
          for (const field of FIELDS) {
            const msg = error.fieldErrors[field]
            if (msg) setError(field, { message: msg })
          }
        }
      },
    }),
  )

  const showBanner =
    registerUser.isError &&
    !(registerUser.error instanceof ApiError && ['EMAIL_TAKEN', 'TCKN_TAKEN', 'VALIDATION_FAILED'].includes(registerUser.error.code))

  return (
    <AuthCard title={t('auth.registerTitle')} subtitle={t('auth.registerSubtitle')}>
      <form onSubmit={onSubmit} noValidate className="flex flex-col gap-5">
        {showBanner && (
          <div role="alert" className="bg-danger-soft text-danger rounded-xl px-4 py-3 text-sm font-medium">
            {errorMessage(t, registerUser.error)}
          </div>
        )}
        <div className="grid gap-5 sm:grid-cols-2">
          <Field label={t('auth.firstName')} error={fieldMessage(t, errors.firstName?.message)}>
            {({ id, describedBy, invalid }) => (
              <Input id={id} autoComplete="given-name" aria-describedby={describedBy} aria-invalid={invalid} {...register('firstName')} />
            )}
          </Field>
          <Field label={t('auth.lastName')} error={fieldMessage(t, errors.lastName?.message)}>
            {({ id, describedBy, invalid }) => (
              <Input id={id} autoComplete="family-name" aria-describedby={describedBy} aria-invalid={invalid} {...register('lastName')} />
            )}
          </Field>
        </div>
        <Field label={t('auth.tckn')} error={fieldMessage(t, errors.tckn?.message)}>
          {({ id, describedBy, invalid }) => (
            <Input
              id={id}
              inputMode="numeric"
              maxLength={11}
              autoComplete="off"
              className="tabular tracking-wider"
              aria-describedby={describedBy}
              aria-invalid={invalid}
              {...register('tckn')}
            />
          )}
        </Field>
        <Field label={t('auth.email')} error={fieldMessage(t, errors.email?.message)}>
          {({ id, describedBy, invalid }) => (
            <Input
              id={id}
              type="email"
              inputMode="email"
              autoComplete="email"
              aria-describedby={describedBy}
              aria-invalid={invalid}
              {...register('email')}
            />
          )}
        </Field>
        <Field label={t('auth.password')} hint={t('auth.passwordHint')} error={fieldMessage(t, errors.password?.message)}>
          {({ id, describedBy, invalid }) => (
            <>
              <PasswordInput
                id={id}
                autoComplete="new-password"
                aria-describedby={describedBy}
                aria-invalid={invalid}
                {...register('password')}
              />
              {strength && (
                <div className="flex items-center gap-2" aria-live="polite">
                  <div className="flex flex-1 gap-1" aria-hidden>
                    {[0, 1, 2].map((i) => (
                      <span
                        key={i}
                        className={cn(
                          'h-1.5 flex-1 rounded-full',
                          i <= ['weak', 'medium', 'strong'].indexOf(strength)
                            ? { weak: 'bg-danger', medium: 'bg-warning', strong: 'bg-success' }[strength]
                            : 'bg-surface-2',
                        )}
                      />
                    ))}
                  </div>
                  <span className="text-muted text-xs font-medium">{t(`auth.passwordStrength.${strength}`)}</span>
                </div>
              )}
            </>
          )}
        </Field>
        <Button type="submit" size="lg" loading={registerUser.isPending}>
          {t('auth.submitRegister')}
        </Button>
        <p className="text-muted text-center text-xs">{t('auth.terms')}</p>
      </form>
      <p className="text-muted mt-8 text-center text-sm">
        {t('auth.haveAccount')}{' '}
        <Link to="/login" className="text-primary font-semibold hover:underline">
          {t('nav.login')}
        </Link>
      </p>
    </AuthCard>
  )
}
