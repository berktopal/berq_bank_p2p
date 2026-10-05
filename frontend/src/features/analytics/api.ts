import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { api, toQuery } from '@/lib/api'
import type { AnalyticsSummary } from '@/lib/types'

export function useAnalytics(accountId: number | undefined, months: number) {
  return useQuery({
    queryKey: ['analytics', accountId, months],
    queryFn: () => api.get<AnalyticsSummary>(`/analytics/summary${toQuery({ accountId, months })}`),
    enabled: accountId !== undefined,
    placeholderData: keepPreviousData,
  })
}
