import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

const globalCss = readFileSync('src/styles/global.css', 'utf8')

describe('responsive visual guardrails', () => {
  it('prevents global horizontal overflow across the required visual breakpoints', () => {
    expect(globalCss).toContain('html, body, #root { min-height: 100%; width: 100%; overflow-x: hidden; }')
    expect(globalCss).toContain('img, svg, canvas, video { max-width: 100%; }')
    expect(globalCss).toContain('.app-shell, .shell-content, .workspace-page { max-width: 100%; overflow-x: hidden; }')
  })

  it('lets Ant Design surfaces and repeated page containers shrink instead of forcing mobile overflow', () => {
    expect(globalCss).toContain('.ant-card, .ant-card-body, .ant-tabs, .ant-tabs-content, .ant-tabs-tabpane, .ant-space, .ant-list, .ant-list-item, .ant-descriptions, .ant-table-wrapper, .ant-form, .ant-alert { min-width: 0; max-width: 100%; }')
    expect(globalCss).toContain('.surface-card .ant-card-body { min-width: 0; overflow-wrap: anywhere; }')
    expect(globalCss).toContain('.metric-card .value { overflow-wrap: anywhere; font-size: clamp(22px, 4vw, 28px); }')
  })

  it('defines a narrow mobile pass for 390px screenshots and reachable core actions', () => {
    expect(globalCss).toContain('@media (max-width: 480px)')
    expect(globalCss).toContain('.shell-content { padding: 12px 12px 84px; }')
    expect(globalCss).toContain('.workspace-header .ant-space { width: 100%; }')
    expect(globalCss).toContain('.workspace-header .ant-btn { flex: 1 1 min(160px, 100%); }')
    expect(globalCss).toContain('.chat-streaming-status, .review-card-foot, .action-panel { align-items: stretch; flex-direction: column; }')
  })
})
