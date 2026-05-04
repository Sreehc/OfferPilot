import { ref, onMounted } from 'vue'

const THEME_KEY = 'bytecoach_theme'

type Theme = 'light' | 'dark'

const theme = ref<Theme>('light')

function applyTheme(t: Theme) {
  const root = document.documentElement
  if (t === 'dark') {
    root.classList.add('dark')
  } else {
    root.classList.remove('dark')
  }
  // Also set Element Plus dark class on body
  document.body.classList.toggle('dark', t === 'dark')
}

export function useTheme() {
  const toggleTheme = () => {
    theme.value = theme.value === 'light' ? 'dark' : 'light'
    localStorage.setItem(THEME_KEY, theme.value)
    applyTheme(theme.value)
  }

  const setTheme = (t: Theme) => {
    theme.value = t
    localStorage.setItem(THEME_KEY, t)
    applyTheme(t)
  }

  onMounted(() => {
    const saved = localStorage.getItem(THEME_KEY) as Theme | null
    if (saved) {
      theme.value = saved
    } else if (window.matchMedia('(prefers-color-scheme: dark)').matches) {
      theme.value = 'dark'
    }
    applyTheme(theme.value)
  })

  return {
    theme,
    toggleTheme,
    setTheme
  }
}
