export interface PersonRequest {
  firstName: string
  lastName: string
  email: string
  phoneNumber?: string
  street?: string
  city?: string
  postalCode?: string
  country?: string
  dateOfBirth?: string
  active?: boolean
}

export interface PersonResponse {
  id: number
  firstName: string
  lastName: string
  email: string
  phoneNumber?: string
  street?: string
  city?: string
  postalCode?: string
  country?: string
  dateOfBirth?: string
  active: boolean
  createdAt: string
  updatedAt: string
  version: number
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
  first: boolean
  last: boolean
  empty: boolean
}
