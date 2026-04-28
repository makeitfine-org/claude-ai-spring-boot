import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { personsApi } from './personsApi'
import type { PersonRequest } from '@/types/person'

export function usePersonsList(page: number, size: number, sort: string, q?: string) {
  return useQuery({
    queryKey: ['persons', page, size, sort, q ?? ''],
    queryFn: () => personsApi.list(page, size, sort, q),
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
