import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/lib/api'
import type { Category, Frequency, ScheduledTransfer } from '@/lib/types'

export const scheduleKeys = { all: ['scheduled-transfers'] as const }

export function useSchedules() {
  return useQuery({ queryKey: scheduleKeys.all, queryFn: () => api.get<ScheduledTransfer[]>('/scheduled-transfers') })
}

export interface CreateScheduleInput {
  fromAccountId: number
  toIban: string
  amount: number
  description?: string
  category?: Category
  frequency: Frequency
  startDate: string
  endDate?: string
}

export function useCreateSchedule() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (input: CreateScheduleInput) => api.post<ScheduledTransfer>('/scheduled-transfers', input),
    onSuccess: () => qc.invalidateQueries({ queryKey: scheduleKeys.all }),
  })
}

export function useSetScheduleStatus() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, status }: { id: number; status: 'ACTIVE' | 'PAUSED' }) =>
      api.patch<ScheduledTransfer>(`/scheduled-transfers/${id}`, { status }),
    onSuccess: () => qc.invalidateQueries({ queryKey: scheduleKeys.all }),
  })
}

export function useCancelSchedule() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => api.delete(`/scheduled-transfers/${id}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: scheduleKeys.all }),
  })
}
