import type { ReactNode } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { ShieldCheck } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { toast } from 'sonner'
import type { z } from 'zod'
import { Button } from '@/components/ui/Button'
import { Field, PasswordInput } from '@/components/ui/Field'
import { Card, PageHeader, Segmented, Skeleton } from '@/components/ui/primitives'
import { useChangePassword, useProfile } from '@/features/auth/api'
import { useTheme, type ThemePreference } from '@/hooks/useTheme'
import { setLanguage, type Language } from '@/i18n'
import { ApiError } from '@/lib/api'
import { errorMessage, fieldMessage } from '@/lib/errors'
import { formatDate } from '@/lib/format'
import { changePasswordSchema } from '@/lib/validation'

function Section({ title, description, children }: { title: string; description?: string; children: ReactNode }) {
  return (
    <Card className="grid gap-6 p-5 sm:p-6 md:grid-cols-[16rem_1fr]">
      <div>
        <h2 className="font-semibold">{title}</h2>
        {description && <p className="text-muted mt-1 text-sm">{description}</p>}
      </div>
      <div>{children}</div>
    </Card>
  )
}

export function SettingsPage() {
  const { t, i18n } = useTranslation()
  const profile = useProfile()
  const { preference, setTheme } = useTheme()

  return (
    <>
      <PageHeader title={t('settings.title')} />
      <div className="flex flex-col gap-6">
        <Section title={t('settings.profile')} description={t('settings.profileBody')}>
          {profile.isPending ? (
            <Skeleton className="h-32 w-full" />
          ) : profile.data ? (
            <dl className="grid gap-4 sm:grid-cols-2">
              {[
                [t('auth.firstName'), profile.data.firstName],
                [t('auth.lastName'), profile.data.lastName],
                [t('auth.email'), profile.data.email],
                [t('auth.tckn'), profile.data.maskedTckn],
                [t('settings.memberSince'), formatDate(profile.data.createdAt, 'long')],
              ].map(([label, value]) => (
                <div key={label}>
                  <dt className="text-muted text-sm">{label}</dt>
                  <dd className="tabular mt-0.5 font-medium break-all">{value}</dd>
                </div>
              ))}
            </dl>
          ) : (
            <p className="text-danger text-sm">{errorMessage(t, profile.error)}</p>
          )}
        </Section>

        <Section title={t('settings.security')} description={t('settings.sessionNote')}>
          <ChangePasswordForm />
        </Section>

        <Section title={t('settings.preferences')}>
          <div className="flex flex-col gap-5">
            <div className="flex flex-col gap-2">
              <span className="text-sm font-medium">{t('settings.theme')}</span>
              <Segmented<ThemePreference>
                label={t('settings.theme')}
                value={preference}
                onChange={setTheme}
                options={[
                  { value: 'light', label: t('theme.light') },
                  { value: 'dark', label: t('theme.dark') },
                  { value: 'system', label: t('theme.system') },
                ]}
                className="self-start"
              />
            </div>
            <div className="flex flex-col gap-2">
              <span className="text-sm font-medium">{t('settings.language')}</span>
              <Segmented<Language>
                label={t('settings.language')}
                value={i18n.language === 'en' ? 'en' : 'tr'}
                onChange={setLanguage}
                options={[
                  { value: 'tr', label: t('language.tr') },
                  { value: 'en', label: t('language.en') },
                ]}
                className="self-start"
              />
            </div>
          </div>
        </Section>
      </div>
    </>
  )
}

type PasswordForm = z.infer<typeof changePasswordSchema>

function ChangePasswordForm() {
  const { t } = useTranslation()
  const change = useChangePassword()
  const { register, handleSubmit, reset, setError, formState } = useForm<PasswordForm>({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: { currentPassword: '', newPassword: '' },
  })

  return (
    <form
      noValidate
      className="flex max-w-md flex-col gap-5"
      onSubmit={handleSubmit((values) =>
        change.mutate(values, {
          onSuccess: () => {
            toast.success(t('settings.passwordChanged'))
            reset()
          },
          onError: (e) => {
            if (e instanceof ApiError && e.code === 'WRONG_CURRENT_PASSWORD') {
              setError('currentPassword', { message: 'errors.WRONG_CURRENT_PASSWORD' })
            } else {
              toast.error(errorMessage(t, e))
            }
          },
        }),
      )}
    >
      <Field label={t('settings.currentPassword')} error={fieldMessage(t, formState.errors.currentPassword?.message)}>
        {({ id, describedBy, invalid }) => (
          <PasswordInput id={id} autoComplete="current-password" aria-describedby={describedBy} aria-invalid={invalid} {...register('currentPassword')} />
        )}
      </Field>
      <Field label={t('settings.newPassword')} hint={t('auth.passwordHint')} error={fieldMessage(t, formState.errors.newPassword?.message)}>
        {({ id, describedBy, invalid }) => (
          <PasswordInput id={id} autoComplete="new-password" aria-describedby={describedBy} aria-invalid={invalid} {...register('newPassword')} />
        )}
      </Field>
      <Button type="submit" className="self-start" loading={change.isPending} icon={<ShieldCheck className="size-4" />}>
        {t('settings.changePassword')}
      </Button>
    </form>
  )
}
