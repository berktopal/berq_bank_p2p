import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, toQuery } from '@/lib/api'
import type { Page, Transaction, TransactionFilters, TransferInput } from '@/lib/types'
import { accountKeys } from '@/features/accounts/api'

export const transactionKeys = {
  all: ['transactions'] as const,
  list: (filters: TransactionFilters) => ['transactions', 'list', filters] as const,
  detail: (id: number) => ['transactions', 'detail', id] as const,
}

export function useTransactions(filters: TransactionFilters) {
  return useQuery({
    queryKey: transactionKeys.list(filters),
    queryFn: ({ signal }) => api.get<Page<Transaction>>(`/transactions${toQuery(filters)}`, { signal }),
    // Filtre değişirken eski sonuçlar ekranda kalır, liste "zıplamaz"
    placeholderData: keepPreviousData,
  })
}

export function useTransaction(id: number) {
  return useQuery({
    queryKey: transactionKeys.detail(id),
    queryFn: () => api.get<Transaction>(`/transactions/${id}`),
    enabled: Number.isFinite(id),
  })
}

export function exportUrl(filters: TransactionFilters): string {
  // Dışa aktarım sayfalama dışındaki tüm filtreleri kullanır
  const { accountId, direction, from, to, category, q } = filters
  return `/api/transactions/export${toQuery({ accountId, direction, from, to, category, q })}`
}

/**
 * Para gönderir. idempotencyKey çağıran tarafta onay ekranına girilirken bir kez üretilir;
 * çift tıklama veya ağ hatası sonrası tekrar denemede aynı anahtar gönderildiği için para iki kez çıkmaz.
 */
export function useTransfer() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ input, idempotencyKey }: { input: TransferInput; idempotencyKey: string }) =>
      api.post<Transaction>('/transactions/transfer', input, { headers: { 'Idempotency-Key': idempotencyKey } }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: accountKeys.all })
      void qc.invalidateQueries({ queryKey: transactionKeys.all })
      void qc.invalidateQueries({ queryKey: ['analytics'] })
    },
  })
}
