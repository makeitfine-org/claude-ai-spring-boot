import '@testing-library/jest-dom'

// ---------------------------------------------------------------------------
// localStorage — jsdom may report localStorage as not-a-function when the
// --localstorage-file flag is passed without a valid path.  Provide a simple
// in-memory fallback so any component that reads/writes localStorage works.
// ---------------------------------------------------------------------------
;(() => {
  const store: Record<string, string> = {}
  const mock = {
    getItem: (key: string) => store[key] ?? null,
    setItem: (key: string, value: string) => { store[key] = value },
    removeItem: (key: string) => { delete store[key] },
    clear: () => { Object.keys(store).forEach((k) => delete store[k]) },
    get length() { return Object.keys(store).length },
    key: (index: number) => Object.keys(store)[index] ?? null,
  }
  try {
    if (typeof window !== 'undefined' && typeof window.localStorage?.getItem !== 'function') {
      Object.defineProperty(window, 'localStorage', { value: mock, writable: true })
    }
  } catch {
    // already defined and not writable — ignore
  }
})()

// ---------------------------------------------------------------------------
// window.matchMedia — not implemented by jsdom
// ---------------------------------------------------------------------------
if (typeof window !== 'undefined' && typeof window.matchMedia !== 'function') {
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: () => ({
      matches: false,
      addEventListener: () => undefined,
      removeEventListener: () => undefined,
    }),
  })
}
