import { QueryClient } from '@tanstack/react-query'
import { ApiError } from '@/lib/api'

export function createQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 30_000,
        refetchOnWindowFocus: true,
        // 4xx yanıtlarını tekrar denemek anlamsız; yalnızca ağ/sunucu hatalarında bir kez dene
        retry: (count, error) => !(error instanceof ApiError && error.status >= 400 && error.status < 500) && count < 1,
      },
      mutations: { retry: false },
    },
  })
}
