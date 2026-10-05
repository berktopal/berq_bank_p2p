import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/lib/api'
import type { Budget, Category, Currency } from '@/lib/types'

export const budgetKeys = { all: ['budgets'] as const }

export function useBudgets() {
  return useQuery({ queryKey: budgetKeys.all, queryFn: () => api.get<Budget[]>('/budgets') })
}

export function useUpsertBudget() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (input: { category: Category; currency: Currency; monthlyLimit: number }) =>
      api.put<Budget>('/budgets', input),
    onSuccess: () => qc.invalidateQueries({ queryKey: budgetKeys.all }),
  })
}

export function useDeleteBudget() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => api.delete(`/budgets/${id}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: budgetKeys.all }),
  })
}

/** Bu transfer yapılırsa bütçe durumu ne olur? (Onay ekranındaki uyarı için) */
export function budgetImpact(budgets: Budget[] | undefined, category: Category, currency: string, amount: number) {
  const budget = budgets?.find((b) => b.category === category && b.currency === currency)
  if (!budget) return null
  const after = budget.spent + amount
  const status = after >= budget.monthlyLimit ? 'EXCEEDED' : after >= budget.monthlyLimit * 0.8 ? 'WARNING' : 'OK'
  return { budget, after, status } as const
}
