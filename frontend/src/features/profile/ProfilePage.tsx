import { useCallback, useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import {
  UserIcon,
  PencilIcon,
  LogOutIcon,
  ChevronLeftIcon,
  UploadIcon,
  Trash2Icon,
} from 'lucide-react'
import { useAuth } from '@/auth/AuthContext'
import { profileApi } from './profileApi'
import { ThemeToggle } from '@/components/ThemeToggle'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent } from '@/components/ui/card'
import { Alert, AlertDescription } from '@/components/ui/alert'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { cn } from '@/lib/utils'

const displayNameSchema = z.object({
  displayName: z
    .string()
    .trim()
    .min(1, 'Display name is required')
    .max(50, 'Display name must be 50 characters or fewer')
    .refine(
      (v) => !/[\x00-\x1F\x7F]/.test(v),
      'Display name must not contain control characters',
    ),
})

type DisplayNameForm = z.infer<typeof displayNameSchema>

export function ProfilePage() {
  const { user, logout, refreshUser } = useAuth()

  const [avatarVersion, setAvatarVersion] = useState(0)
  const [avatarUrl, setAvatarUrl] = useState<string | null>(null)
  const [avatarError, setAvatarError] = useState<string | null>(null)
  const [avatarLoading, setAvatarLoading] = useState(false)

  const [editingName, setEditingName] = useState(false)
  const [nameError, setNameError] = useState<string | null>(null)
  const [nameSaving, setNameSaving] = useState(false)

  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [deleteLoading, setDeleteLoading] = useState(false)

  const fileInputRef = useRef<HTMLInputElement>(null)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<DisplayNameForm>({
    resolver: zodResolver(displayNameSchema),
    defaultValues: { displayName: user?.displayName ?? user?.username ?? '' },
  })

  useEffect(() => {
    if (!user?.hasAvatar) {
      setAvatarUrl(null)
      return
    }
    let objectUrl: string | null = null
    profileApi
      .fetchAvatarBlob()
      .then((blob) => {
        objectUrl = URL.createObjectURL(blob)
        setAvatarUrl(objectUrl)
      })
      .catch(() => {
        setAvatarUrl(null)
      })
    return () => {
      if (objectUrl) URL.revokeObjectURL(objectUrl)
    }
  }, [user?.hasAvatar, avatarVersion])

  const handleFileChange = useCallback(
    async (e: React.ChangeEvent<HTMLInputElement>) => {
      const file = e.target.files?.[0]
      if (!file) return
      e.target.value = ''

      setAvatarError(null)

      const allowedExtensions = /\.(png|jpe?g)$/i
      if (!allowedExtensions.test(file.name)) {
        setAvatarError('Only PNG and JPG files are allowed.')
        return
      }
      if (file.size > 1024 * 1024) {
        setAvatarError('File must be 1 MB or smaller.')
        return
      }

      setAvatarLoading(true)
      try {
        await profileApi.uploadAvatar(file)
        await refreshUser()
        setAvatarVersion((v) => v + 1)
      } catch {
        setAvatarError('Failed to upload avatar. Please try again.')
      } finally {
        setAvatarLoading(false)
      }
    },
    [refreshUser],
  )

  const handleRemoveAvatar = useCallback(async () => {
    setAvatarError(null)
    setAvatarLoading(true)
    try {
      await profileApi.deleteAvatar()
      await refreshUser()
      setAvatarVersion((v) => v + 1)
    } catch {
      setAvatarError('Failed to remove avatar. Please try again.')
    } finally {
      setAvatarLoading(false)
    }
  }, [refreshUser])

  const startEditName = () => {
    reset({ displayName: user?.displayName ?? user?.username ?? '' })
    setNameError(null)
    setEditingName(true)
  }

  const cancelEditName = () => {
    setEditingName(false)
    setNameError(null)
  }

  const onSaveName = handleSubmit(async (data) => {
    setNameError(null)
    setNameSaving(true)
    try {
      await profileApi.updateDisplayName(data.displayName)
      await refreshUser()
      setEditingName(false)
    } catch (err: unknown) {
      const axiosErr = err as { response?: { status?: number; data?: { message?: string } } }
      if (axiosErr.response?.status === 400) {
        setNameError(axiosErr.response.data?.message ?? 'Invalid display name.')
      } else {
        setNameError('Failed to save display name. Please try again.')
      }
    } finally {
      setNameSaving(false)
    }
  })

  const handleDeleteAccount = async () => {
    setDeleteLoading(true)
    try {
      await profileApi.deleteAccount()
      await logout()
    } catch {
      setDeleteLoading(false)
      setDeleteDialogOpen(false)
    }
  }

  const displayLabel = user?.displayName ?? user?.username ?? ''

  return (
    <div className="min-h-screen bg-background">
      <header className="border-b px-6 py-3 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Link to="/persons" className="flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-colors">
            <ChevronLeftIcon className="size-4" />
            Persons
          </Link>
          <span className="text-muted-foreground">/</span>
          <span className="text-base font-semibold">Profile</span>
        </div>
        <div className="flex items-center gap-2">
          <ThemeToggle />
          {displayLabel && (
            <span className="text-sm text-muted-foreground">{displayLabel}</span>
          )}
          <Button variant="ghost" size="sm" onClick={logout}>
            <LogOutIcon className="size-4 mr-1.5" />
            Logout
          </Button>
        </div>
      </header>

      <main className="p-6">
        <div className="max-w-lg mx-auto flex flex-col gap-6">
          <Card>
            <CardContent className="pt-6 flex flex-col gap-6">
              <section className="flex flex-col gap-3">
                <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wide">
                  Avatar
                </h2>
                <div className="flex items-center gap-4">
                  <div className="size-20 rounded-full overflow-hidden border bg-muted flex items-center justify-center flex-shrink-0">
                    {avatarUrl ? (
                      <img
                        src={avatarUrl}
                        alt="Avatar"
                        className="size-full object-cover"
                      />
                    ) : (
                      <UserIcon className="size-10 text-muted-foreground" />
                    )}
                  </div>
                  <div className="flex flex-col gap-2">
                    <input
                      ref={fileInputRef}
                      type="file"
                      accept=".png,.jpg,.jpeg"
                      className="hidden"
                      onChange={handleFileChange}
                    />
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={avatarLoading}
                      onClick={() => fileInputRef.current?.click()}
                    >
                      <UploadIcon className="size-4 mr-1.5" />
                      {user?.hasAvatar ? 'Replace avatar' : 'Upload avatar'}
                    </Button>
                    {user?.hasAvatar && (
                      <Button
                        variant="ghost"
                        size="sm"
                        disabled={avatarLoading}
                        onClick={handleRemoveAvatar}
                        className="text-destructive hover:text-destructive"
                      >
                        <Trash2Icon className="size-4 mr-1.5" />
                        Remove avatar
                      </Button>
                    )}
                  </div>
                </div>
                {avatarError && (
                  <Alert variant="destructive">
                    <AlertDescription>{avatarError}</AlertDescription>
                  </Alert>
                )}
              </section>

              <hr className="border-border" />

              <section className="flex flex-col gap-3">
                <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wide">
                  Display name
                </h2>
                {editingName ? (
                  <form onSubmit={onSaveName} className="flex flex-col gap-2">
                    <div className="flex flex-col gap-1.5">
                      <Label htmlFor="displayName">Display name</Label>
                      <Input
                        id="displayName"
                        autoFocus
                        disabled={nameSaving}
                        {...register('displayName')}
                        className={cn(errors.displayName && 'border-destructive')}
                      />
                      {errors.displayName && (
                        <p className="text-sm text-destructive">{errors.displayName.message}</p>
                      )}
                    </div>
                    {nameError && (
                      <Alert variant="destructive">
                        <AlertDescription>{nameError}</AlertDescription>
                      </Alert>
                    )}
                    <div className="flex gap-2">
                      <Button type="submit" size="sm" disabled={nameSaving}>
                        Save
                      </Button>
                      <Button
                        type="button"
                        variant="ghost"
                        size="sm"
                        disabled={nameSaving}
                        onClick={cancelEditName}
                      >
                        Cancel
                      </Button>
                    </div>
                  </form>
                ) : (
                  <div className="flex items-center gap-2">
                    <span className="text-sm">{displayLabel}</span>
                    <Button variant="ghost" size="sm" onClick={startEditName}>
                      <PencilIcon className="size-3.5 mr-1" />
                      Edit
                    </Button>
                  </div>
                )}
              </section>

              <hr className="border-border" />

              <section className="flex flex-col gap-1">
                <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wide">
                  Account
                </h2>
                <p className="text-sm text-muted-foreground">
                  Username: <span className="font-mono">{user?.username}</span>
                </p>
                <p className="text-sm text-muted-foreground">
                  Email: <span className="font-mono">{user?.email}</span>
                </p>
              </section>
            </CardContent>
          </Card>

          <div className="flex justify-end">
            <Button
              variant="ghost"
              size="sm"
              className="text-destructive hover:text-destructive"
              onClick={() => setDeleteDialogOpen(true)}
            >
              <Trash2Icon className="size-4 mr-1.5" />
              Delete account
            </Button>
          </div>
        </div>
      </main>

      <Dialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Delete account</DialogTitle>
            <DialogDescription>
              This will permanently delete your account. This cannot be undone.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setDeleteDialogOpen(false)}
              disabled={deleteLoading}
            >
              Cancel
            </Button>
            <Button
              variant="destructive"
              onClick={handleDeleteAccount}
              disabled={deleteLoading}
            >
              Delete
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
