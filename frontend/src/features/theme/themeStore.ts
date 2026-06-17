import { create } from 'zustand'
import { storage } from '@/utils/storage'

type ThemeMode = 'light' | 'dark'

interface ThemeState {
  mode: ThemeMode
  toggleTheme: () => void
  setTheme: (mode: ThemeMode) => void
}

const initialTheme = (): ThemeMode => storage.getTheme() || (window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light')

export const useThemeStore = create<ThemeState>((set) => ({
  mode: initialTheme(),
  toggleTheme: () => set((state) => {
    const mode = state.mode === 'dark' ? 'light' : 'dark'
    storage.setTheme(mode)
    document.documentElement.classList.toggle('dark', mode === 'dark')
    return { mode }
  }),
  setTheme: (mode) => {
    storage.setTheme(mode)
    document.documentElement.classList.toggle('dark', mode === 'dark')
    set({ mode })
  }
}))

const mode = useThemeStore.getState().mode
document.documentElement.classList.toggle('dark', mode === 'dark')
