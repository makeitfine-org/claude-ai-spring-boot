import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { PlusIcon, SearchIcon, LogOutIcon, ChevronLeftIcon, ChevronRightIcon } from 'lucide-react'
import { useAuth } from '@/auth/AuthContext'
import {
  usePersonsList,
  usePersonSearch,
  useCreatePerson,
  useUpdatePerson,
  useDeletePerson,
} from './usePersons'
import { PersonFormDialog } from './PersonFormDialog'
import { ConfirmDeleteDialog } from './ConfirmDeleteDialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Alert, AlertDescription } from '@/components/ui/alert'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import type { PersonRequest, PersonResponse } from '@/types/person'

const PAGE_SIZES = [10, 20, 50]

export function PersonsPage() {
  const { logout } = useAuth()
  const navigate = useNavigate()

  const [page, setPage] = useState(0)
  const [size, setSize] = useState(20)
  const [searchEmail, setSearchEmail] = useState('')
  const [debouncedEmail, setDebouncedEmail] = useState('')

  const [formOpen, setFormOpen] = useState(false)
  const [editTarget, setEditTarget] = useState<PersonResponse | undefined>()
  const [formError, setFormError] = useState<string | null>(null)

  const [deleteTarget, setDeleteTarget] = useState<PersonResponse | undefined>()

  const listQuery = usePersonsList(page, size)
  const searchQuery = usePersonSearch(debouncedEmail)

  const createMutation = useCreatePerson()
  const updateMutation = useUpdatePerson(editTarget?.id ?? 0)
  const deleteMutation = useDeletePerson()

  const isSearching = debouncedEmail.length > 0
  const persons = isSearching
    ? searchQuery.data
      ? [searchQuery.data]
      : []
    : listQuery.data?.content ?? []

  const totalPages = isSearching ? 1 : listQuery.data?.totalPages ?? 1
  const totalElements = isSearching
    ? searchQuery.data
      ? 1
      : 0
    : listQuery.data?.totalElements ?? 0

  const isLoading = isSearching ? searchQuery.isLoading : listQuery.isLoading
  const queryError = isSearching ? searchQuery.error : listQuery.error

  let emailTimer: ReturnType<typeof setTimeout>
  const handleSearchChange = (value: string) => {
    setSearchEmail(value)
    clearTimeout(emailTimer)
    emailTimer = setTimeout(() => {
      setDebouncedEmail(value.trim())
      setPage(0)
    }, 400)
  }

  const openCreate = () => {
    setEditTarget(undefined)
    setFormError(null)
    setFormOpen(true)
  }

  const openEdit = (person: PersonResponse) => {
    setEditTarget(person)
    setFormError(null)
    setFormOpen(true)
  }

  const handleFormSubmit = async (data: PersonRequest) => {
    setFormError(null)
    try {
      if (editTarget) {
        await updateMutation.mutateAsync(data)
      } else {
        await createMutation.mutateAsync(data)
      }
      setFormOpen(false)
    } catch {
      setFormError('Failed to save. Please check your input and try again.')
    }
  }

  const handleDelete = async () => {
    if (!deleteTarget) return
    try {
      await deleteMutation.mutateAsync(deleteTarget.id)
      setDeleteTarget(undefined)
    } catch {
      setDeleteTarget(undefined)
    }
  }

  const handleLogout = () => {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="min-h-screen bg-background">
      <header className="border-b px-6 py-3 flex items-center justify-between">
        <h1 className="text-base font-semibold">Persons</h1>
        <Button variant="ghost" size="sm" onClick={handleLogout}>
          <LogOutIcon className="size-4 mr-1.5" />
          Logout
        </Button>
      </header>

      <main className="p-6 flex flex-col gap-4">
        <div className="flex items-center gap-3 flex-wrap">
          <Button size="sm" onClick={openCreate}>
            <PlusIcon className="size-4 mr-1" />
            New person
          </Button>

          <div className="relative flex-1 min-w-48 max-w-xs">
            <SearchIcon className="absolute left-2.5 top-1/2 -translate-y-1/2 size-3.5 text-muted-foreground" />
            <Input
              className="pl-8"
              placeholder="Search by email…"
              value={searchEmail}
              onChange={(e) => handleSearchChange(e.target.value)}
            />
          </div>

          <div className="flex items-center gap-2 ml-auto text-sm text-muted-foreground">
            <span>Rows:</span>
            {PAGE_SIZES.map((s) => (
              <button
                key={s}
                onClick={() => { setSize(s); setPage(0) }}
                className={`px-1.5 rounded ${size === s ? 'font-semibold text-foreground' : 'hover:text-foreground'}`}
              >
                {s}
              </button>
            ))}
          </div>
        </div>

        {queryError && (
          <Alert variant="destructive">
            <AlertDescription>
              {isSearching ? 'No person found with that email.' : 'Failed to load persons.'}
            </AlertDescription>
          </Alert>
        )}

        <div className="rounded-xl border overflow-hidden">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>First name</TableHead>
                <TableHead>Last name</TableHead>
                <TableHead>Email</TableHead>
                <TableHead>Phone</TableHead>
                <TableHead>City</TableHead>
                <TableHead>Active</TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading && (
                <TableRow>
                  <TableCell colSpan={7} className="text-center text-muted-foreground py-8">
                    Loading…
                  </TableCell>
                </TableRow>
              )}
              {!isLoading && persons.length === 0 && (
                <TableRow>
                  <TableCell colSpan={7} className="text-center text-muted-foreground py-8">
                    No persons found.
                  </TableCell>
                </TableRow>
              )}
              {persons.map((person) => (
                <TableRow key={person.id}>
                  <TableCell>{person.firstName}</TableCell>
                  <TableCell>{person.lastName}</TableCell>
                  <TableCell className="max-w-48 truncate">{person.email}</TableCell>
                  <TableCell>{person.phoneNumber ?? '—'}</TableCell>
                  <TableCell>{person.city ?? '—'}</TableCell>
                  <TableCell>
                    <Badge variant={person.active ? 'default' : 'outline'}>
                      {person.active ? 'Active' : 'Inactive'}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="inline-flex gap-1.5">
                      <Button size="xs" variant="outline" onClick={() => openEdit(person)}>
                        Edit
                      </Button>
                      <Button
                        size="xs"
                        variant="destructive"
                        onClick={() => setDeleteTarget(person)}
                      >
                        Delete
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>

        {!isSearching && (
          <div className="flex items-center justify-between text-sm text-muted-foreground">
            <span>
              {totalElements} person{totalElements !== 1 ? 's' : ''}
            </span>
            <div className="flex items-center gap-2">
              <Button
                size="icon-sm"
                variant="outline"
                disabled={page === 0}
                onClick={() => setPage((p) => p - 1)}
              >
                <ChevronLeftIcon />
              </Button>
              <span>
                {page + 1} / {Math.max(totalPages, 1)}
              </span>
              <Button
                size="icon-sm"
                variant="outline"
                disabled={page + 1 >= totalPages}
                onClick={() => setPage((p) => p + 1)}
              >
                <ChevronRightIcon />
              </Button>
            </div>
          </div>
        )}
      </main>

      <PersonFormDialog
        open={formOpen}
        person={editTarget}
        onSubmit={handleFormSubmit}
        onClose={() => setFormOpen(false)}
        error={formError}
        isPending={createMutation.isPending || updateMutation.isPending}
      />

      <ConfirmDeleteDialog
        open={!!deleteTarget}
        name={deleteTarget ? `${deleteTarget.firstName} ${deleteTarget.lastName}` : ''}
        onConfirm={handleDelete}
        onClose={() => setDeleteTarget(undefined)}
        isPending={deleteMutation.isPending}
      />
    </div>
  )
}
