import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { personsApi } from './personsApi'
import type { PersonRequest } from '@/types/person'

export function usePersonsList(page: number, size: number) {
  return useQuery({
    queryKey: ['persons', page, size],
    queryFn: () => personsApi.list(page, size),
  })
}

export function usePersonSearch(email: string) {
  return useQuery({
    queryKey: ['persons', 'search', email],
    queryFn: () => personsApi.searchByEmail(email),
    enabled: email.length > 0,
    retry: false,
  })
}

export function useCreatePerson() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: PersonRequest) => personsApi.create(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['persons'] }),
  })
}

export function useUpdatePerson(id: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: PersonRequest) => personsApi.update(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['persons'] }),
  })
}

export function useDeletePerson() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => personsApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['persons'] }),
  })
}
