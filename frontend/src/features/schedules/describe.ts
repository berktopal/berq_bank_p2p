import type { TFunction } from 'i18next'
import i18n from '@/i18n'
import { formatDate } from '@/lib/format'
import type { ScheduledTransfer } from '@/lib/types'

/** "Her ayın 3. günü", "Her Pazartesi", "12 Eki 2026 tarihinde bir kez" */
export function describeSchedule(t: TFunction, s: ScheduledTransfer): string {
  if (s.frequency === 'ONCE') return t('schedules.once', { date: formatDate(s.nextRunDate) })
  if (s.frequency === 'MONTHLY') return t('schedules.monthly', { day: s.dayOfMonth })
  const weekday = new Intl.DateTimeFormat(i18n.language === 'en' ? 'en-US' : 'tr-TR', { weekday: 'long', timeZone: 'UTC' }).format(
    new Date(`${s.nextRunDate}T12:00:00Z`),
  )
  return t('schedules.weekly', { day: weekday })
}
