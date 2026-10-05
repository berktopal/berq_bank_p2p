import { zodResolver } from '@hookform/resolvers/zod'
import { Sparkles } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate, useSearchParams } from 'react-router'
import { toast } from 'sonner'
import type { z } from 'zod'
import { Button } from '@/components/ui/Button'
import { Field, Input, PasswordInput } from '@/components/ui/Field'
import { errorMessage, fieldMessage } from '@/lib/errors'
import { safeNext } from '@/lib/navigation'
import { loginSchema } from '@/lib/validation'
import { AuthCard } from './AuthCard'
import { useLogin, usePublicConfig } from './api'

type LoginForm = z.infer<typeof loginSchema>

export function LoginPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const login = useLogin()
  const { data: config } = usePublicConfig()
  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors },
  } = useForm<LoginForm>({ resolver: zodResolver(loginSchema), defaultValues: { email: '', password: '' } })

  const onSubmit = handleSubmit((values) =>
    login.mutate(values, {
      onSuccess: (user) => {
        toast.success(t('auth.welcome', { name: user.firstName }))
        void navigate(safeNext(params.get('next')), { replace: true })
      },
    }),
  )

  const fillDemo = () => {
    if (!config?.demoEmail || !config.demoPassword) return
    setValue('email', config.demoEmail)
    setValue('password', config.demoPassword)
    void onSubmit()
  }

  return (
    <AuthCard title={t('auth.loginTitle')} subtitle={t('auth.loginSubtitle')}>
      <form onSubmit={onSubmit} noValidate className="flex flex-col gap-5">
        {login.isError && (
          <div role="alert" className="bg-danger-soft text-danger rounded-xl px-4 py-3 text-sm font-medium">
            {errorMessage(t, login.error)}
          </div>
        )}
        <Field label={t('auth.email')} error={fieldMessage(t, errors.email?.message)}>
          {({ id, describedBy, invalid }) => (
            <Input
              id={id}
              type="email"
              autoComplete="username"
              inputMode="email"
              aria-describedby={describedBy}
              aria-invalid={invalid}
              {...register('email')}
            />
          )}
        </Field>
        <Field label={t('auth.password')} error={fieldMessage(t, errors.password?.message)}>
          {({ id, describedBy, invalid }) => (
            <PasswordInput
              id={id}
              autoComplete="current-password"
              aria-describedby={describedBy}
              aria-invalid={invalid}
              {...register('password')}
            />
          )}
        </Field>
        <Button type="submit" size="lg" loading={login.isPending}>
          {t('auth.submitLogin')}
        </Button>
      </form>

      {config?.demo && (
        <div className="border-border bg-surface-2 mt-6 rounded-2xl border border-dashed p-4">
          <p className="flex items-center gap-2 font-semibold">
            <Sparkles className="text-primary size-4" aria-hidden />
            {t('auth.demoTitle')}
          </p>
          <p className="text-muted mt-1 text-sm">{t('auth.demoBody')}</p>
          <Button variant="secondary" size="sm" className="mt-3" onClick={fillDemo} disabled={login.isPending}>
            {t('auth.demoButton')}
          </Button>
        </div>
      )}

      <p className="text-muted mt-8 text-center text-sm">
        {t('auth.noAccount')}{' '}
        <Link to="/register" className="text-primary font-semibold hover:underline">
          {t('nav.register')}
        </Link>
      </p>
    </AuthCard>
  )
}
