import { api } from '@/lib/api'
import type { UserProfile } from '@/types/auth'

export const profileApi = {
  updateDisplayName: (displayName: string) =>
    api.patch<UserProfile>('/api/users/me', { displayName }).then((r) => r.data),

  uploadAvatar: (file: File) => {
    const fd = new FormData()
    fd.append('file', file)
    return api.put('/api/users/me/avatar', fd)
  },

  deleteAvatar: () => api.delete('/api/users/me/avatar'),

  deleteAccount: () => api.delete('/api/users/me'),

  fetchAvatarBlob: () =>
    api.get<Blob>('/api/users/me/avatar', { responseType: 'blob' }).then((r) => r.data),
}
