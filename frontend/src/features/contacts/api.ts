import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/lib/api'
import type { Contact } from '@/lib/types'

export const contactKeys = { all: ['contacts'] as const }

export function useContacts() {
  return useQuery({ queryKey: contactKeys.all, queryFn: () => api.get<Contact[]>('/contacts') })
}

export function useCreateContact() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (input: { nickname: string; iban: string }) => api.post<Contact>('/contacts', input),
    onSuccess: () => qc.invalidateQueries({ queryKey: contactKeys.all }),
  })
}

export function useRenameContact() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, nickname }: { id: number; nickname: string }) => api.patch<Contact>(`/contacts/${id}`, { nickname }),
    onSuccess: () => qc.invalidateQueries({ queryKey: contactKeys.all }),
  })
}

/** İyimser silme: liste anında güncellenir, hata olursa geri alınır. */
export function useDeleteContact() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => api.delete(`/contacts/${id}`),
    onMutate: async (id) => {
      await qc.cancelQueries({ queryKey: contactKeys.all })
      const previous = qc.getQueryData<Contact[]>(contactKeys.all)
      qc.setQueryData<Contact[]>(contactKeys.all, (list) => list?.filter((c) => c.id !== id))
      return { previous }
    },
    onError: (_e, _id, context) => {
      if (context?.previous) qc.setQueryData(contactKeys.all, context.previous)
    },
    onSettled: () => qc.invalidateQueries({ queryKey: contactKeys.all }),
  })
}
