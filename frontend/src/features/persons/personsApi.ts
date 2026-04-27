import { api } from '@/lib/api'
import type { Page, PersonRequest, PersonResponse } from '@/types/person'

export const personsApi = {
  list: (page: number, size: number) =>
    api
      .get<Page<PersonResponse>>('/api/persons', {
        params: { page, size, sort: 'lastName,asc' },
      })
      .then((r) => r.data),

  getById: (id: number) =>
    api.get<PersonResponse>(`/api/persons/${id}`).then((r) => r.data),

  searchByEmail: (email: string) =>
    api
      .get<PersonResponse>('/api/persons/search', { params: { email } })
      .then((r) => r.data),

  create: (data: PersonRequest) =>
    api.post<PersonResponse>('/api/persons', data).then((r) => r.data),

  update: (id: number, data: PersonRequest) =>
    api.put<PersonResponse>(`/api/persons/${id}`, data).then((r) => r.data),

  remove: (id: number) => api.delete(`/api/persons/${id}`),
}
