import { renderHook, act } from '@testing-library/react'
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { ThemeProvider, useTheme } from './ThemeContext'

let store: Record<string, string> = {}

const localStorageMock = {
  getItem: (key: string) => store[key] ?? null,
  setItem: (key: string, value: string) => { store[key] = value },
  removeItem: (key: string) => { delete store[key] },
  clear: () => { store = {} },
}

function mockMatchMedia(prefersDark: boolean) {
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: () => ({
      matches: prefersDark,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
    }),
  })
}

beforeEach(() => {
  store = {}
  Object.defineProperty(window, 'localStorage', { value: localStorageMock, writable: true })
  mockMatchMedia(false)
  document.documentElement.classList.remove('dark')
})

const wrapper = ({ children }: { children: React.ReactNode }) => (
  <ThemeProvider>{children}</ThemeProvider>
)

describe('useTheme', () => {
  it('defaults to auto when no stored preference', () => {
    const { result } = renderHook(() => useTheme(), { wrapper })
    expect(result.current.theme).toBe('auto')
  })

  it('reads stored theme from localStorage on mount', () => {
    store['theme'] = 'dark'
    const { result } = renderHook(() => useTheme(), { wrapper })
    expect(result.current.theme).toBe('dark')
  })

  it('setTheme persists to localStorage and updates state', () => {
    const { result } = renderHook(() => useTheme(), { wrapper })
    act(() => result.current.setTheme('light'))
    expect(store['theme']).toBe('light')
    expect(result.current.theme).toBe('light')
  })

  it('applies .dark class when theme is dark', () => {
    const { result } = renderHook(() => useTheme(), { wrapper })
    act(() => result.current.setTheme('dark'))
    expect(document.documentElement.classList.contains('dark')).toBe(true)
  })

  it('removes .dark class when theme is light', () => {
    document.documentElement.classList.add('dark')
    const { result } = renderHook(() => useTheme(), { wrapper })
    act(() => result.current.setTheme('light'))
    expect(document.documentElement.classList.contains('dark')).toBe(false)
  })

  it('resolvedTheme follows system preference in auto mode', () => {
    mockMatchMedia(true)
    const { result } = renderHook(() => useTheme(), { wrapper })
    expect(result.current.resolvedTheme).toBe('dark')
  })

  it('resolvedTheme is light when system is light in auto mode', () => {
    const { result } = renderHook(() => useTheme(), { wrapper })
    expect(result.current.resolvedTheme).toBe('light')
  })
})
