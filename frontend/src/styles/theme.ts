import type { ThemeConfig } from 'antd'
import { theme } from 'antd'

const fontFamily = 'Noto Sans SC, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif'

export const offerPilotTokens = {
  light: {
    colorPrimary: '#3763f4',
    colorPrimaryStrong: '#244bd4',
    colorSuccess: '#2f8a7b',
    colorWarning: '#b87922',
    colorDanger: '#c55349',
    colorBg: '#f5f8ff',
    colorSurface: '#ffffff',
    colorSurfaceSoft: '#f4f7fd',
    colorCard: 'rgba(255,255,255,0.94)',
    colorText: '#10233a',
    colorTextSecondary: '#66778b',
    colorTextTertiary: '#92a0af',
    colorBorder: 'rgba(16,35,58,0.09)',
    colorBorderStrong: 'rgba(16,35,58,0.14)',
    shadow: '0 20px 60px rgba(32,40,53,0.08)',
    shadowSoft: '0 10px 28px rgba(32,40,53,0.06)',
    radius: 8,
    radiusLarge: 12,
    motionDurationFast: '160ms',
    motionDurationBase: '220ms',
    motionEase: 'cubic-bezier(0.16, 1, 0.3, 1)',
    focusRing: '0 0 0 3px rgba(55,99,244,0.22)'
  },
  dark: {
    colorPrimary: '#7ea2ff',
    colorPrimaryStrong: '#a9c2ff',
    colorSuccess: '#74c6b6',
    colorWarning: '#d29a39',
    colorDanger: '#ff7a6b',
    colorBg: '#0a1020',
    colorSurface: '#10192b',
    colorSurfaceSoft: '#141f34',
    colorCard: 'rgba(17,27,47,0.96)',
    colorText: '#edf4ff',
    colorTextSecondary: '#a9b8cf',
    colorTextTertiary: '#708198',
    colorBorder: 'rgba(190,204,226,0.12)',
    colorBorderStrong: 'rgba(190,204,226,0.2)',
    shadow: '0 26px 80px rgba(0,0,0,0.36)',
    shadowSoft: '0 14px 34px rgba(0,0,0,0.22)',
    radius: 8,
    radiusLarge: 12,
    motionDurationFast: '160ms',
    motionDurationBase: '220ms',
    motionEase: 'cubic-bezier(0.16, 1, 0.3, 1)',
    focusRing: '0 0 0 3px rgba(126,162,255,0.24)'
  }
} as const

const light = offerPilotTokens.light
const dark = offerPilotTokens.dark

export const lightTheme: ThemeConfig = {
  algorithm: theme.defaultAlgorithm,
  token: {
    colorPrimary: light.colorPrimary,
    colorSuccess: light.colorSuccess,
    colorWarning: light.colorWarning,
    colorError: light.colorDanger,
    colorInfo: light.colorPrimary,
    colorTextBase: light.colorText,
    colorText: light.colorText,
    colorTextSecondary: light.colorTextSecondary,
    colorTextTertiary: light.colorTextTertiary,
    colorBgBase: light.colorBg,
    colorBgLayout: light.colorBg,
    colorBgContainer: light.colorSurface,
    colorBgElevated: light.colorSurface,
    colorBorder: light.colorBorder,
    colorBorderSecondary: light.colorBorder,
    borderRadius: light.radius,
    borderRadiusLG: light.radiusLarge,
    boxShadow: light.shadow,
    boxShadowSecondary: light.shadowSoft,
    controlHeight: 38,
    fontFamily,
    wireframe: false
  },
  components: {
    Layout: { headerBg: 'rgba(255,255,255,0.84)', siderBg: light.colorSurface, bodyBg: light.colorBg },
    Card: { borderRadiusLG: light.radius, boxShadow: light.shadowSoft },
    Button: { borderRadius: light.radius, controlHeight: 38, primaryShadow: 'none' },
    Input: { borderRadius: light.radius, controlHeight: 38 },
    Menu: { itemBorderRadius: light.radius, itemSelectedBg: 'rgba(55,99,244,0.1)', itemSelectedColor: light.colorPrimary },
    Tabs: { itemSelectedColor: light.colorPrimary, inkBarColor: light.colorPrimary },
    Table: { headerBg: light.colorSurfaceSoft, borderColor: light.colorBorder },
    Modal: { borderRadiusLG: light.radiusLarge },
    Drawer: { colorBgElevated: light.colorSurface },
    Tag: { borderRadiusSM: 6 },
    Progress: { defaultColor: light.colorPrimary, remainingColor: light.colorSurfaceSoft },
    Notification: { borderRadiusLG: light.radiusLarge }
  }
}

export const darkTheme: ThemeConfig = {
  algorithm: theme.darkAlgorithm,
  token: {
    colorPrimary: dark.colorPrimary,
    colorSuccess: dark.colorSuccess,
    colorWarning: dark.colorWarning,
    colorError: dark.colorDanger,
    colorInfo: dark.colorPrimary,
    colorTextBase: dark.colorText,
    colorText: dark.colorText,
    colorTextSecondary: dark.colorTextSecondary,
    colorTextTertiary: dark.colorTextTertiary,
    colorBgBase: dark.colorBg,
    colorBgLayout: dark.colorBg,
    colorBgContainer: dark.colorSurface,
    colorBgElevated: dark.colorSurface,
    colorBorder: dark.colorBorder,
    colorBorderSecondary: dark.colorBorder,
    borderRadius: dark.radius,
    borderRadiusLG: dark.radiusLarge,
    boxShadow: dark.shadow,
    boxShadowSecondary: dark.shadowSoft,
    controlHeight: 38,
    fontFamily,
    wireframe: false
  },
  components: {
    Layout: { headerBg: 'rgba(13,20,36,0.86)', siderBg: dark.colorSurface, bodyBg: dark.colorBg },
    Card: { borderRadiusLG: dark.radius, boxShadow: dark.shadowSoft },
    Button: { borderRadius: dark.radius, controlHeight: 38, primaryShadow: 'none' },
    Input: { borderRadius: dark.radius, controlHeight: 38 },
    Menu: { itemBorderRadius: dark.radius, itemSelectedBg: 'rgba(126,162,255,0.14)', itemSelectedColor: dark.colorPrimary },
    Tabs: { itemSelectedColor: dark.colorPrimary, inkBarColor: dark.colorPrimary },
    Table: { headerBg: dark.colorSurfaceSoft, borderColor: dark.colorBorder },
    Modal: { borderRadiusLG: dark.radiusLarge },
    Drawer: { colorBgElevated: dark.colorSurface },
    Tag: { borderRadiusSM: 6 },
    Progress: { defaultColor: dark.colorPrimary, remainingColor: dark.colorSurfaceSoft },
    Notification: { borderRadiusLG: dark.radiusLarge }
  }
}
