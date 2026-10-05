import { useState } from 'react'
import { AlertTriangle, CalendarClock, Pause, Play, Plus, Trash2 } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { toast } from 'sonner'
import { Button, ButtonLink } from '@/components/ui/Button'
import { Dialog } from '@/components/ui/Dialog'
import { Avatar, Badge, Card, EmptyState, PageHeader, Skeleton } from '@/components/ui/primitives'
import { cn } from '@/lib/cn'
import { codeMessage, errorMessage } from '@/lib/errors'
import { formatDate, formatMoney } from '@/lib/format'
import type { ScheduledTransfer, ScheduleStatus } from '@/lib/types'
import { useCancelSchedule, useSchedules, useSetScheduleStatus } from './api'
import { describeSchedule } from './describe'

const STATUS_TONE: Record<ScheduleStatus, 'success' | 'warning' | 'neutral'> = {
  ACTIVE: 'success',
  PAUSED: 'warning',
  COMPLETED: 'neutral',
  CANCELLED: 'neutral',
}

export function SchedulesPage() {
  const { t } = useTranslation()
  const schedules = useSchedules()
  const setStatus = useSetScheduleStatus()
  const cancel = useCancelSchedule()
  const [cancelling, setCancelling] = useState<ScheduledTransfer | null>(null)

  return (
    <>
      <PageHeader
        title={t('schedules.title')}
        subtitle={t('schedules.subtitle')}
        actions={
          <ButtonLink to="/app/transfer?when=RECURRING" icon={<Plus className="size-4" />}>
            {t('schedules.new')}
          </ButtonLink>
        }
      />

      {schedules.isPending ? (
        <div className="grid gap-4 md:grid-cols-2">
          {[0, 1].map((i) => (
            <Skeleton key={i} className="h-44 rounded-2xl" />
          ))}
        </div>
      ) : schedules.isError ? (
        <Card>
          <EmptyState icon={<CalendarClock />} title={errorMessage(t, schedules.error)} />
        </Card>
      ) : schedules.data.length === 0 ? (
        <Card>
          <EmptyState
            icon={<CalendarClock />}
            title={t('schedules.empty')}
            hint={t('schedules.emptyHint')}
            action={<ButtonLink to="/app/transfer?when=RECURRING">{t('schedules.new')}</ButtonLink>}
          />
        </Card>
      ) : (
        <ul className="grid gap-4 md:grid-cols-2">
          {schedules.data.map((s) => {
            const editable = s.status === 'ACTIVE' || s.status === 'PAUSED'
            return (
              <li key={s.id}>
                <Card className={cn('flex h-full flex-col gap-4 p-5', !editable && 'opacity-70')}>
                  <div className="flex items-start gap-3">
                    <Avatar name={s.toName} />
                    <div className="min-w-0 flex-1">
                      <p className="truncate font-semibold">{s.toName}</p>
                      <p className="text-muted truncate text-sm">
                        {[s.description, t(`categories.${s.category}`)].filter(Boolean).join(' · ')}
                      </p>
                    </div>
                    <Badge tone={STATUS_TONE[s.status]}>{t(`schedules.status.${s.status}`)}</Badge>
                  </div>

                  <div className="flex items-end justify-between gap-3">
                    <div>
                      <p className="tabular text-2xl font-bold">{formatMoney(s.amount, s.currency)}</p>
                      <p className="text-muted text-sm">{describeSchedule(t, s)}</p>
                    </div>
                    <div className="text-muted text-right text-xs">
                      {editable && <p className="text-text text-sm font-medium">{t('schedules.next', { date: formatDate(s.nextRunDate) })}</p>}
                      {s.endDate && <p>{t('schedules.ends', { date: formatDate(s.endDate) })}</p>}
                      {s.runCount > 0 && <p>{t('schedules.runs', { count: s.runCount })}</p>}
                    </div>
                  </div>

                  {s.lastError && (
                    <p className="bg-warning-soft text-warning flex items-start gap-2 rounded-xl px-3 py-2 text-sm" role="status">
                      <AlertTriangle className="mt-0.5 size-4 shrink-0" aria-hidden />
                      {t('schedules.lastError', { reason: codeMessage(t, s.lastError) })}
                    </p>
                  )}

                  {editable && (
                    <div className="mt-auto flex gap-2">
                      <Button
                        size="sm"
                        variant="secondary"
                        icon={s.status === 'ACTIVE' ? <Pause className="size-4" /> : <Play className="size-4" />}
                        loading={setStatus.isPending && setStatus.variables?.id === s.id}
                        onClick={() =>
                          setStatus.mutate(
                            { id: s.id, status: s.status === 'ACTIVE' ? 'PAUSED' : 'ACTIVE' },
                            {
                              onSuccess: (updated) =>
                                toast.success(updated.status === 'PAUSED' ? t('schedules.paused') : t('schedules.resumed')),
                              onError: (e) => toast.error(errorMessage(t, e)),
                            },
                          )
                        }
                      >
                        {s.status === 'ACTIVE' ? t('schedules.pause') : t('schedules.resume')}
                      </Button>
                      <Button size="sm" variant="ghost" icon={<Trash2 className="size-4" />} onClick={() => setCancelling(s)}>
                        {t('schedules.cancel')}
                      </Button>
                    </div>
                  )}
                </Card>
              </li>
            )
          })}
        </ul>
      )}

      <Dialog
        open={cancelling !== null}
        onClose={() => setCancelling(null)}
        title={t('schedules.cancelConfirm')}
        description={cancelling ? `${cancelling.toName} · ${formatMoney(cancelling.amount, cancelling.currency)}` : undefined}
        footer={
          <>
            <Button variant="secondary" onClick={() => setCancelling(null)}>
              {t('common.cancel')}
            </Button>
            <Button
              variant="danger"
              loading={cancel.isPending}
              onClick={() =>
                cancelling &&
                cancel.mutate(cancelling.id, {
                  onSuccess: () => {
                    toast.success(t('schedules.cancelled'))
                    setCancelling(null)
                  },
                  onError: (e) => toast.error(errorMessage(t, e)),
                })
              }
            >
              {t('schedules.cancel')}
            </Button>
          </>
        }
      />
    </>
  )
}
