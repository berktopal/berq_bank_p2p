import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, toQuery } from '@/lib/api'
import type { Account, AccountLookup, Currency } from '@/lib/types'

export const accountKeys = {
  all: ['accounts'] as const,
  lookup: (iban: string) => ['account-lookup', iban] as const,
}

export function useAccounts() {
  return useQuery({ queryKey: accountKeys.all, queryFn: () => api.get<Account[]>('/accounts') })
}

/** IBAN → maskeli alıcı adı. Yalnızca tam biçimli IBAN için çalışır. */
export function useAccountLookup(iban: string | null) {
  return useQuery({
    queryKey: accountKeys.lookup(iban ?? ''),
    queryFn: ({ signal }) => api.get<AccountLookup>(`/accounts/lookup${toQuery({ iban })}`, { signal }),
    enabled: Boolean(iban),
    retry: false,
    staleTime: 60_000,
  })
}

export function useOpenAccount() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (input: { name: string; currency: Currency }) => api.post<Account>('/accounts', input),
    onSuccess: () => qc.invalidateQueries({ queryKey: accountKeys.all }),
  })
}

export function useRenameAccount() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, name }: { id: number; name: string }) => api.patch<Account>(`/accounts/${id}`, { name }),
    onSuccess: () => qc.invalidateQueries({ queryKey: accountKeys.all }),
  })
}

/** Para birimi bazında toplam bakiye (farklı para birimleri toplanmaz). */
export function totalsByCurrency(accounts: Account[]): { currency: string; total: number }[] {
  const map = new Map<string, number>()
  for (const a of accounts) map.set(a.currency, (map.get(a.currency) ?? 0) + a.balance)
  return [...map.entries()].map(([currency, total]) => ({ currency, total: Math.round(total * 100) / 100 }))
}
