import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'
import { lightTheme, offerPilotTokens } from './theme'

const globalCss = readFileSync('src/styles/global.css', 'utf8')

describe('OfferPilot light theme tokens', () => {
  it('exposes the semantic tokens required by the light visual system', () => {
    expect(offerPilotTokens.light).toMatchObject({
      colorPrimary: '#3763f4',
      colorPrimaryStrong: '#244bd4',
      colorSuccess: '#2f8a7b',
      colorWarning: '#b87922',
      colorDanger: '#c55349',
      colorBg: '#f5f8ff',
      colorSurface: '#ffffff',
      colorSurfaceSoft: '#f4f7fd',
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
      focusRing: '0 0 0 3px rgba(55,99,244,0.22)'
    })
  })

  it('maps light semantic tokens into Ant Design global and component tokens', () => {
    expect(lightTheme.token).toMatchObject({
      colorPrimary: offerPilotTokens.light.colorPrimary,
      colorSuccess: offerPilotTokens.light.colorSuccess,
      colorWarning: offerPilotTokens.light.colorWarning,
      colorError: offerPilotTokens.light.colorDanger,
      colorTextBase: offerPilotTokens.light.colorText,
      colorBgBase: offerPilotTokens.light.colorBg,
      colorBorder: offerPilotTokens.light.colorBorder,
      borderRadius: offerPilotTokens.light.radius,
      wireframe: false
    })

    expect(Object.keys(lightTheme.components || {}).sort()).toEqual([
      'Button',
      'Card',
      'Drawer',
      'Input',
      'Layout',
      'Menu',
      'Modal',
      'Notification',
      'Progress',
      'Table',
      'Tabs',
      'Tag'
    ])

    expect(lightTheme.components?.Button).toMatchObject({
      borderRadius: offerPilotTokens.light.radius,
      controlHeight: 38,
      primaryShadow: 'none'
    })
    expect(lightTheme.components?.Input).toMatchObject({
      borderRadius: offerPilotTokens.light.radius,
      controlHeight: 38
    })
    expect(lightTheme.components?.Table).toMatchObject({
      headerBg: offerPilotTokens.light.colorSurfaceSoft,
      borderColor: offerPilotTokens.light.colorBorder
    })
    expect(lightTheme.components?.Progress).toMatchObject({
      defaultColor: offerPilotTokens.light.colorPrimary
    })
  })

  it('defines accessible focus and reduced-motion CSS hooks', () => {
    expect(globalCss).toContain('--op-focus-ring: 0 0 0 3px rgba(55,99,244,0.22);')
    expect(globalCss).toContain(':focus-visible')
    expect(globalCss).toContain('box-shadow: var(--op-focus-ring);')
    expect(globalCss).toContain('@media (prefers-reduced-motion: reduce)')
    expect(globalCss).toContain('animation-duration: 0.01ms !important')
    expect(globalCss).toContain('transition-duration: 0.01ms !important')
  })
})
