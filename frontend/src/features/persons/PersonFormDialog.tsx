import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Alert, AlertDescription } from '@/components/ui/alert'
import type { PersonRequest, PersonResponse } from '@/types/person'

const schema = z.object({
  firstName: z.string().min(1, 'Required').max(100),
  lastName: z.string().min(1, 'Required').max(100),
  email: z.string().email('Invalid email').max(150),
  phoneNumber: z.string().max(20).optional(),
  street: z.string().max(200).optional(),
  city: z.string().max(100).optional(),
  postalCode: z.string().max(10).optional(),
  country: z.string().max(100).optional(),
  dateOfBirth: z.string().optional(),
  active: z.boolean().optional(),
})

type FormData = z.infer<typeof schema>

interface Props {
  open: boolean
  person?: PersonResponse
  onSubmit: (data: PersonRequest) => Promise<void>
  onClose: () => void
  error?: string | null
  isPending: boolean
}

function Field({
  label,
  id,
  error,
  children,
}: {
  label: string
  id: string
  error?: string
  children: React.ReactNode
}) {
  return (
    <div className="flex flex-col gap-1">
      <Label htmlFor={id}>{label}</Label>
      {children}
      {error && <p className="text-xs text-destructive">{error}</p>}
    </div>
  )
}

export function PersonFormDialog({ open, person, onSubmit, onClose, error, isPending }: Props) {
  const isEdit = !!person
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormData>({ resolver: zodResolver(schema) })

  useEffect(() => {
    if (open) {
      reset(
        person
          ? {
              firstName: person.firstName,
              lastName: person.lastName,
              email: person.email,
              phoneNumber: person.phoneNumber ?? '',
              street: person.street ?? '',
              city: person.city ?? '',
              postalCode: person.postalCode ?? '',
              country: person.country ?? '',
              dateOfBirth: person.dateOfBirth ?? '',
              active: person.active,
            }
          : {
              firstName: '',
              lastName: '',
              email: '',
              phoneNumber: '',
              street: '',
              city: '',
              postalCode: '',
              country: '',
              dateOfBirth: '',
              active: true,
            }
      )
    }
  }, [open, person, reset])

  const handleClose = () => {
    reset()
    onClose()
  }

  return (
    <Dialog open={open} onOpenChange={(v) => !v && handleClose()}>
      <DialogContent className="sm:max-w-lg max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{isEdit ? 'Edit person' : 'New person'}</DialogTitle>
        </DialogHeader>

        <form
          id="person-form"
          onSubmit={handleSubmit(onSubmit)}
          className="grid grid-cols-2 gap-3"
        >
          {error && (
            <div className="col-span-2">
              <Alert variant="destructive">
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            </div>
          )}

          <Field label="First name *" id="firstName" error={errors.firstName?.message}>
            <Input id="firstName" aria-invalid={!!errors.firstName} {...register('firstName')} />
          </Field>

          <Field label="Last name *" id="lastName" error={errors.lastName?.message}>
            <Input id="lastName" aria-invalid={!!errors.lastName} {...register('lastName')} />
          </Field>

          <div className="col-span-2">
            <Field label="Email *" id="email" error={errors.email?.message}>
              <Input id="email" type="email" aria-invalid={!!errors.email} {...register('email')} />
            </Field>
          </div>

          <Field label="Phone" id="phoneNumber" error={errors.phoneNumber?.message}>
            <Input id="phoneNumber" {...register('phoneNumber')} />
          </Field>

          <Field label="Date of birth" id="dateOfBirth" error={errors.dateOfBirth?.message}>
            <Input id="dateOfBirth" type="date" {...register('dateOfBirth')} />
          </Field>

          <div className="col-span-2">
            <Field label="Street" id="street" error={errors.street?.message}>
              <Input id="street" {...register('street')} />
            </Field>
          </div>

          <Field label="City" id="city" error={errors.city?.message}>
            <Input id="city" {...register('city')} />
          </Field>

          <Field label="Postal code" id="postalCode" error={errors.postalCode?.message}>
            <Input id="postalCode" {...register('postalCode')} />
          </Field>

          <div className="col-span-2">
            <Field label="Country" id="country" error={errors.country?.message}>
              <Input id="country" {...register('country')} />
            </Field>
          </div>

          <div className="col-span-2 flex items-center gap-2">
            <input
              id="active"
              type="checkbox"
              className="h-4 w-4 rounded border-input accent-primary"
              {...register('active')}
            />
            <Label htmlFor="active">Active</Label>
          </div>
        </form>

        <DialogFooter>
          <Button variant="outline" onClick={handleClose} disabled={isPending}>
            Cancel
          </Button>
          <Button type="submit" form="person-form" disabled={isPending}>
            {isPending ? 'Saving…' : isEdit ? 'Save changes' : 'Create'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
