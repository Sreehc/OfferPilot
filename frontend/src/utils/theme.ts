export const readThemeToken = (name: string): string =>
  getComputedStyle(document.documentElement).getPropertyValue(name).trim()

export const readThemePalette = () => ({
  textPrimary: readThemeToken('--text-primary'),
  textSecondary: readThemeToken('--text-secondary'),
  textTertiary: readThemeToken('--text-tertiary'),
  border: readThemeToken('--bc-border'),
  borderStrong: readThemeToken('--bc-border-strong'),
  borderSubtle: readThemeToken('--bc-border-subtle'),
  surface: readThemeToken('--bc-surface'),
  surfaceCard: readThemeToken('--panel-bg'),
  surfaceMuted: readThemeToken('--panel-muted'),
  interactiveBg: readThemeToken('--interactive-bg'),
  interactiveHover: readThemeToken('--interactive-hover'),
  accent: readThemeToken('--bc-accent'),
  accentRgb: readThemeToken('--bc-accent-rgb'),
  amber: readThemeToken('--accent-amber'),
  cyan: readThemeToken('--accent-cyan'),
  cyanRgb: readThemeToken('--bc-cyan-rgb'),
  coral: readThemeToken('--accent-coral'),
  lime: readThemeToken('--accent-lime'),
  chartColors: [
    readThemeToken('--chart-1'),
    readThemeToken('--chart-2'),
    readThemeToken('--chart-3'),
    readThemeToken('--chart-4'),
    readThemeToken('--chart-5'),
    readThemeToken('--chart-6'),
    readThemeToken('--chart-7')
  ]
})
