import { useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query'
import { api, ApiError } from '@/lib/api'
import type { Profile, PublicConfig, User } from '@/lib/types'

export const authKeys = {
  me: ['me'] as const,
  profile: ['profile'] as const,
  config: ['public-config'] as const,
}

/** Oturumdaki kullanıcı; giriş yapılmamışsa null. */
export function useMe() {
  return useQuery({
    queryKey: authKeys.me,
    queryFn: async () => {
      try {
        return await api.get<User>('/auth/me', { silentUnauthorized: true })
      } catch (e) {
        if (e instanceof ApiError && e.status === 401) return null
        throw e
      }
    },
    staleTime: Infinity,
    retry: false,
  })
}

export function usePublicConfig() {
  return useQuery({
    queryKey: authKeys.config,
    queryFn: () => api.get<PublicConfig>('/public/config'),
    staleTime: Infinity,
  })
}

export interface LoginInput {
  email: string
  password: string
}

export interface RegisterInput {
  firstName: string
  lastName: string
  tckn: string
  email: string
  password: string
}

/**
 * Oturum değiştiğinde (giriş, çıkış, oturumun sunucuda düşmesi) önceki kullanıcının verisi önbellekte kalmamalı.
 * qc.clear() kullanılmaz: 'me' sorgusunu da silip yeniden yaratır ve ona abone bileşenler güncellemeyi kaçırır.
 */
export function resetSession(qc: QueryClient, user: User | null) {
  qc.removeQueries({ predicate: (q) => q.queryKey[0] !== authKeys.me[0] })
  qc.setQueryData(authKeys.me, user)
}

function useSignedIn() {
  const qc = useQueryClient()
  return (user: User) => resetSession(qc, user)
}

export function useLogin() {
  const signedIn = useSignedIn()
  return useMutation({
    mutationFn: (input: LoginInput) => api.post<User>('/auth/login', input, { silentUnauthorized: true }),
    onSuccess: signedIn,
  })
}

export function useRegister() {
  const signedIn = useSignedIn()
  return useMutation({
    mutationFn: (input: RegisterInput) => api.post<User>('/auth/register', input),
    onSuccess: signedIn,
  })
}

export function useLogout() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: () => api.post<void>('/auth/logout'),
    onSettled: () => resetSession(qc, null),
  })
}

export function useProfile() {
  return useQuery({ queryKey: authKeys.profile, queryFn: () => api.get<Profile>('/profile') })
}

export function useChangePassword() {
  return useMutation({
    mutationFn: (input: { currentPassword: string; newPassword: string }) => api.post<void>('/profile/password', input),
  })
}
