import type { ThemeConfig } from 'antd'
import { theme } from 'antd'

export const lightTheme: ThemeConfig = {
  algorithm: theme.defaultAlgorithm,
  token: {
    colorPrimary: '#3763f4',
    colorSuccess: '#2f8a7b',
    colorWarning: '#b87922',
    colorError: '#c55349',
    colorInfo: '#3763f4',
    colorTextBase: '#10233a',
    colorBgBase: '#f5f8ff',
    borderRadius: 8,
    fontFamily: 'Noto Sans SC, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
    wireframe: false
  },
  components: {
    Layout: { headerBg: 'rgba(255,255,255,0.84)', siderBg: '#ffffff', bodyBg: '#f5f8ff' },
    Card: { borderRadiusLG: 8 },
    Button: { borderRadius: 8, controlHeight: 36 },
    Table: { headerBg: '#f4f7fd', borderColor: 'rgba(16,35,58,0.08)' }
  }
}

export const darkTheme: ThemeConfig = {
  algorithm: theme.darkAlgorithm,
  token: {
    colorPrimary: '#7ea2ff',
    colorSuccess: '#74c6b6',
    colorWarning: '#d29a39',
    colorError: '#ff7a6b',
    colorInfo: '#7ea2ff',
    colorTextBase: '#edf4ff',
    colorBgBase: '#0a1020',
    borderRadius: 8,
    fontFamily: lightTheme.token?.fontFamily,
    wireframe: false
  },
  components: {
    Layout: { headerBg: 'rgba(13,20,36,0.86)', siderBg: '#10192b', bodyBg: '#0a1020' },
    Card: { borderRadiusLG: 8 },
    Table: { headerBg: '#141f34', borderColor: 'rgba(190,204,226,0.12)' }
  }
}
