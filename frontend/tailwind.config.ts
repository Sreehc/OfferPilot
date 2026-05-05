import type { Config } from 'tailwindcss'

export default {
  darkMode: 'class',
  content: ['./index.html', './src/**/*.{vue,ts,tsx}'],
  theme: {
    extend: {
      colors: {
        ink: 'rgb(var(--bc-ink-rgb) / <alpha-value>)',
        accent: 'rgb(var(--bc-accent-rgb) / <alpha-value>)',
        mist: '#dfeaf1',
        fog: '#8ca6bf',
        shell: 'var(--bc-shell)',
        void: 'var(--bc-void)',
        depth: 'var(--bc-depth)',
        amber: 'var(--bc-amber)',
        cyan: 'var(--bc-cyan)',
        coral: 'var(--bc-coral)',
        lime: 'var(--bc-lime)'
      },
      fontFamily: {
        display: ['Barlow Condensed', 'Noto Sans SC', 'sans-serif'],
        body: ['Noto Sans SC', 'Avenir Next', 'sans-serif'],
        mono: ['JetBrains Mono', 'ui-monospace', 'SFMono-Regular', 'monospace']
      },
      boxShadow: {
        frame: '0 24px 80px rgba(16, 24, 43, 0.14)'
      }
    }
  },
  plugins: []
} satisfies Config
