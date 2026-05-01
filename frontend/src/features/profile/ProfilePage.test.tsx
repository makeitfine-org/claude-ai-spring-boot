import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'

// Module mocks — must come before any imports that reference them
vi.mock('@/auth/AuthContext')
vi.mock('./profileApi')

import { useAuth } from '@/auth/AuthContext'
import { profileApi } from './profileApi'
import { ProfilePage } from './ProfilePage'
import { ThemeProvider } from '@/contexts/ThemeContext'
import type { UserProfile } from '@/types/auth'

// Browser APIs not available in jsdom
Object.defineProperty(URL, 'createObjectURL', {
  writable: true,
  value: vi.fn(() => 'blob:mock-url'),
})
Object.defineProperty(URL, 'revokeObjectURL', {
  writable: true,
  value: vi.fn(),
})

const baseUser: UserProfile = {
  sub: 'user-123',
  username: 'testuser',
  displayName: 'Test User',
  email: 'test@example.com',
  hasAvatar: false,
}

function makeAuthMock(overrides: Partial<UserProfile> = {}) {
  return {
    user: { ...baseUser, ...overrides },
    logout: vi.fn().mockResolvedValue(undefined),
    refreshUser: vi.fn().mockResolvedValue(undefined),
    isAuthenticated: true,
    isLoading: false,
  }
}

function renderProfilePage() {
  return render(
    <ThemeProvider>
      <MemoryRouter>
        <ProfilePage />
      </MemoryRouter>
    </ThemeProvider>
  )
}

/** Simulate a file being selected in the (display:none) file input. */
function uploadFile(file: File) {
  const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement
  // fireEvent.change bypasses userEvent's visibility check on hidden inputs
  fireEvent.change(fileInput, { target: { files: [file] } })
  return fileInput
}

// ---------------------------------------------------------------------------
// AvatarUploader
// ---------------------------------------------------------------------------
describe('ProfilePage – AvatarUploader', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(useAuth).mockReturnValue(makeAuthMock())
    vi.mocked(profileApi.uploadAvatar).mockResolvedValue(undefined as never)
    vi.mocked(profileApi.deleteAvatar).mockResolvedValue(undefined as never)
    vi.mocked(profileApi.fetchAvatarBlob).mockResolvedValue(new Blob(['img'], { type: 'image/png' }))
  })

  it('file input accepts only .png, .jpg, .jpeg', () => {
    renderProfilePage()
    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement
    expect(fileInput).not.toBeNull()
    expect(fileInput.accept).toBe('.png,.jpg,.jpeg')
  })

  it('shows error for disallowed file type without calling uploadAvatar', async () => {
    renderProfilePage()
    const badFile = new File(['data'], 'image.gif', { type: 'image/gif' })
    uploadFile(badFile)
    await waitFor(() => {
      expect(screen.getByText('Only PNG and JPG files are allowed.')).toBeInTheDocument()
    })
    expect(profileApi.uploadAvatar).not.toHaveBeenCalled()
  })

  it('shows error for file > 1 MB without calling uploadAvatar', async () => {
    renderProfilePage()
    // 1 025 × 1024 bytes = 1 025 KB > 1 MB
    const largeFile = new File(
      [new Uint8Array(1025 * 1024)],
      'big.jpg',
      { type: 'image/jpeg' },
    )
    uploadFile(largeFile)
    await waitFor(() => {
      expect(screen.getByText('File must be 1 MB or smaller.')).toBeInTheDocument()
    })
    expect(profileApi.uploadAvatar).not.toHaveBeenCalled()
  })

  it('calls uploadAvatar and refreshUser on valid file selection', async () => {
    renderProfilePage()
    const validFile = new File(['img'], 'photo.png', { type: 'image/png' })
    uploadFile(validFile)
    await waitFor(() => {
      expect(profileApi.uploadAvatar).toHaveBeenCalledWith(validFile)
    })
    expect(vi.mocked(useAuth)().refreshUser).toHaveBeenCalled()
  })

  it('does not show Remove avatar button when user has no avatar', () => {
    renderProfilePage()
    expect(screen.queryByRole('button', { name: /remove avatar/i })).not.toBeInTheDocument()
  })

  it('shows Remove avatar button when user has avatar', () => {
    vi.mocked(useAuth).mockReturnValue(makeAuthMock({ hasAvatar: true }))
    renderProfilePage()
    expect(screen.getByRole('button', { name: /remove avatar/i })).toBeInTheDocument()
  })

  it('calls deleteAvatar and refreshUser when Remove avatar is clicked', async () => {
    vi.mocked(useAuth).mockReturnValue(makeAuthMock({ hasAvatar: true }))
    renderProfilePage()
    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: /remove avatar/i }))
    await waitFor(() => {
      expect(profileApi.deleteAvatar).toHaveBeenCalled()
    })
    expect(vi.mocked(useAuth)().refreshUser).toHaveBeenCalled()
  })
})

// ---------------------------------------------------------------------------
// DisplayNameEditor
// ---------------------------------------------------------------------------
describe('ProfilePage – DisplayNameEditor', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(useAuth).mockReturnValue(makeAuthMock())
    vi.mocked(profileApi.updateDisplayName).mockResolvedValue(undefined as never)
    vi.mocked(profileApi.fetchAvatarBlob).mockResolvedValue(new Blob())
  })

  it('clicking Edit shows text input pre-populated with current display name', async () => {
    renderProfilePage()
    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: /edit/i }))
    const input = screen.getByLabelText(/display name/i)
    expect(input).toBeInTheDocument()
    expect(input).toHaveValue('Test User')
  })

  it('clicking Cancel hides the form without calling the API', async () => {
    renderProfilePage()
    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: /edit/i }))
    await user.clear(screen.getByLabelText(/display name/i))
    await user.type(screen.getByLabelText(/display name/i), 'Something Else')
    await user.click(screen.getByRole('button', { name: /cancel/i }))
    // Input should be gone — back to read-only view
    expect(screen.queryByLabelText(/display name/i)).not.toBeInTheDocument()
    // Edit button returns
    expect(screen.getByRole('button', { name: /edit/i })).toBeInTheDocument()
    expect(profileApi.updateDisplayName).not.toHaveBeenCalled()
  })

  it('saving a valid name calls updateDisplayName with the new value', async () => {
    renderProfilePage()
    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: /edit/i }))
    const input = screen.getByLabelText(/display name/i)
    await user.clear(input)
    await user.type(input, 'Alice')
    await user.click(screen.getByRole('button', { name: /^save$/i }))
    await waitFor(() => {
      expect(profileApi.updateDisplayName).toHaveBeenCalledWith('Alice')
    })
  })

  it('shows inline error when display name is empty on save', async () => {
    renderProfilePage()
    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: /edit/i }))
    await user.clear(screen.getByLabelText(/display name/i))
    await user.click(screen.getByRole('button', { name: /^save$/i }))
    await waitFor(() => {
      expect(screen.getByText(/display name is required/i)).toBeInTheDocument()
    })
    expect(profileApi.updateDisplayName).not.toHaveBeenCalled()
  })

  it('shows inline error when display name exceeds 50 characters', async () => {
    renderProfilePage()
    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: /edit/i }))
    const input = screen.getByLabelText(/display name/i)
    await user.clear(input)
    await user.type(input, 'A'.repeat(51))
    await user.click(screen.getByRole('button', { name: /^save$/i }))
    await waitFor(() => {
      expect(
        screen.getByText(/display name must be 50 characters or fewer/i),
      ).toBeInTheDocument()
    })
    expect(profileApi.updateDisplayName).not.toHaveBeenCalled()
  })
})
