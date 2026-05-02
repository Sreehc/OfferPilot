import type { Config } from 'tailwindcss'

export default {
  darkMode: 'class',
  content: ['./index.html', './src/**/*.{vue,ts,tsx}'],
  theme: {
    extend: {
      colors: {
        ink: '#172033',
        accent: '#2f4f9d',
        mist: '#eef2f7',
        fog: '#c8d2e1',
        shell: '#f5f7fb'
      },
      fontFamily: {
        display: ['Manrope', 'Avenir Next', 'sans-serif'],
        body: ['Manrope', 'Avenir Next', 'sans-serif']
      },
      boxShadow: {
        frame: '0 24px 80px rgba(16, 24, 43, 0.14)'
      }
    }
  },
  plugins: []
} satisfies Config
