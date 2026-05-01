import { useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  PlusIcon,
  SearchIcon,
  LogOutIcon,
  ChevronLeftIcon,
  ChevronRightIcon,
  ChevronUpIcon,
  ChevronDownIcon,
} from 'lucide-react'
import { useAuth } from '@/auth/AuthContext'
import {
  usePersonsList,
  useCreatePerson,
  useUpdatePerson,
  useDeletePerson,
} from './usePersons'
import { PersonFormDialog } from './PersonFormDialog'
import { ConfirmDeleteDialog } from './ConfirmDeleteDialog'
import { ThemeToggle } from '@/components/ThemeToggle'
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

type SortField = 'firstName' | 'lastName' | 'email' | 'phoneNumber' | 'city' | 'active'
type SortDir = 'asc' | 'desc'

interface SortableHeadProps {
  field: SortField
  currentField: SortField
  currentDir: SortDir
  onSort: (field: SortField) => void
  children: React.ReactNode
  className?: string
}

function SortableHead({ field, currentField, currentDir, onSort, children, className }: SortableHeadProps) {
  const active = field === currentField
  return (
    <TableHead className={className}>
      <button
        className="flex items-center gap-1 hover:text-foreground transition-colors"
        onClick={() => onSort(field)}
      >
        {children}
        {active ? (
          currentDir === 'asc' ? (
            <ChevronUpIcon className="size-3.5" />
          ) : (
            <ChevronDownIcon className="size-3.5" />
          )
        ) : (
          <ChevronUpIcon className="size-3.5 opacity-0 group-hover:opacity-40" />
        )}
      </button>
    </TableHead>
  )
}

export function PersonsPage() {
  const { logout, user } = useAuth()

  const [page, setPage] = useState(0)
  const [size, setSize] = useState(20)
  const [sortField, setSortField] = useState<SortField>('lastName')
  const [sortDir, setSortDir] = useState<SortDir>('asc')
  const [searchValue, setSearchValue] = useState('')
  const [debouncedQ, setDebouncedQ] = useState('')
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  const [formOpen, setFormOpen] = useState(false)
  const [editTarget, setEditTarget] = useState<PersonResponse | undefined>()
  const [formError, setFormError] = useState<string | null>(null)

  const [deleteTarget, setDeleteTarget] = useState<PersonResponse | undefined>()

  const sort = `${sortField},${sortDir}`
  const listQuery = usePersonsList(page, size, sort, debouncedQ || undefined)

  const createMutation = useCreatePerson()
  const updateMutation = useUpdatePerson(editTarget?.id ?? 0)
  const deleteMutation = useDeletePerson()

  const persons = listQuery.data?.content ?? []
  const totalPages = listQuery.data?.totalPages ?? 1
  const totalElements = listQuery.data?.totalElements ?? 0

  const handleSearchChange = (value: string) => {
    setSearchValue(value)
    if (debounceRef.current) clearTimeout(debounceRef.current)
    debounceRef.current = setTimeout(() => {
      setDebouncedQ(value.trim())
      setPage(0)
    }, 400)
  }

  const handleSort = (field: SortField) => {
    if (field === sortField) {
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'))
    } else {
      setSortField(field)
      setSortDir('asc')
    }
    setPage(0)
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
  }

  const sortProps = { currentField: sortField, currentDir: sortDir, onSort: handleSort }

  return (
    <div className="min-h-screen bg-background">
      <header className="border-b px-6 py-3 flex items-center justify-between">
        <h1 className="text-base font-semibold">Persons</h1>
        <div className="flex items-center gap-2">
          <ThemeToggle />
          {(user?.displayName ?? user?.username) && (
            <span className="text-sm text-muted-foreground">
              {user?.displayName ?? user?.username}
            </span>
          )}
          <Link to="/profile">
            <Button variant="ghost" size="sm">Profile</Button>
          </Link>
          <Button variant="ghost" size="sm" onClick={handleLogout}>
            <LogOutIcon className="size-4 mr-1.5" />
            Logout
          </Button>
        </div>
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
              placeholder="Search name, email, phone, city…"
              value={searchValue}
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

        {listQuery.error && (
          <Alert variant="destructive">
            <AlertDescription>Failed to load persons.</AlertDescription>
          </Alert>
        )}

        <div className="rounded-xl border overflow-hidden">
          <Table>
            <TableHeader>
              <TableRow>
                <SortableHead field="firstName" {...sortProps}>First name</SortableHead>
                <SortableHead field="lastName" {...sortProps}>Last name</SortableHead>
                <SortableHead field="email" {...sortProps}>Email</SortableHead>
                <SortableHead field="phoneNumber" {...sortProps}>Phone</SortableHead>
                <SortableHead field="city" {...sortProps}>City</SortableHead>
                <SortableHead field="active" {...sortProps}>Active</SortableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {listQuery.isLoading && (
                <TableRow>
                  <TableCell colSpan={7} className="text-center text-muted-foreground py-8">
                    Loading…
                  </TableCell>
                </TableRow>
              )}
              {!listQuery.isLoading && persons.length === 0 && (
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
