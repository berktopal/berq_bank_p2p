import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, toQuery } from '@/lib/api'
import type { Page, PaymentRequest, PaymentRequestStatus } from '@/lib/types'
import { accountKeys } from '@/features/accounts/api'
import { transactionKeys } from '@/features/transactions/api'

export const requestKeys = {
  all: ['payment-requests'] as const,
  list: (role: 'IN' | 'OUT', status?: PaymentRequestStatus) => ['payment-requests', role, status ?? 'ALL'] as const,
  pending: ['payment-requests', 'pending-count'] as const,
}

export function usePaymentRequests(role: 'IN' | 'OUT', status?: PaymentRequestStatus) {
  return useQuery({
    queryKey: requestKeys.list(role, status),
    queryFn: () => api.get<Page<PaymentRequest>>(`/payment-requests${toQuery({ role, status, size: 50 })}`),
  })
}

export function usePendingRequestCount() {
  return useQuery({
    queryKey: requestKeys.pending,
    queryFn: () => api.get<{ count: number }>('/payment-requests/pending-count').then((r) => r.count),
  })
}

export interface CreateRequestInput {
  toAccountId: number
  payerIban: string
  amount: number
  description?: string
}

export function useCreateRequest() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (input: CreateRequestInput) => api.post<PaymentRequest>('/payment-requests', input),
    onSuccess: () => qc.invalidateQueries({ queryKey: requestKeys.all }),
  })
}

/** Ödeme bir transferdir: bakiyeler ve hareketler de tazelenir. */
export function usePayRequest() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, fromAccountId }: { id: number; fromAccountId: number }) =>
      api.post<PaymentRequest>(`/payment-requests/${id}/pay`, { fromAccountId }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: requestKeys.all })
      void qc.invalidateQueries({ queryKey: accountKeys.all })
      void qc.invalidateQueries({ queryKey: transactionKeys.all })
      void qc.invalidateQueries({ queryKey: ['budgets'] })
    },
  })
}

export function useRespondRequest(action: 'decline' | 'cancel') {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => api.post<PaymentRequest>(`/payment-requests/${id}/${action}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: requestKeys.all }),
  })
}
