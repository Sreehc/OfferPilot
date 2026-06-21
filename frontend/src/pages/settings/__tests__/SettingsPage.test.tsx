import { describe, expect, it, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '@/test/renderWithProviders'
import { SettingsPage } from '@/pages/settings/SettingsPage'
import {
  checkProviderConfigsApi,
  fetchProviderConfigsApi,
  updateProviderConfigsApi
} from '@/api/modules/settings'

vi.mock('@/api/modules/auth', () => ({
  fetchCurrentUserApi: vi.fn(() => Promise.resolve({
    data: {
      id: 1,
      username: 'offerpilot',
      nickname: 'OfferPilot 用户',
      email: 'user@example.com',
      emailVerified: true,
      role: 'USER',
      status: 'ACTIVE'
    }
  })),
  fetchDevicesApi: vi.fn(() => Promise.resolve({ data: [] })),
  fetchLoginLogsApi: vi.fn(() => Promise.resolve({ data: [] })),
  fetchTwoFactorStatusApi: vi.fn(() => Promise.resolve({ data: { enabled: false, recoveryCodesRemaining: 0 } })),
  uploadAvatarApi: vi.fn(() => Promise.resolve({ data: '' })),
  sendEmailVerificationCodeApi: vi.fn(() => Promise.resolve({ data: undefined })),
  verifyEmailCodeApi: vi.fn(() => Promise.resolve({ data: undefined })),
  setupTwoFactorApi: vi.fn(() => Promise.resolve({ data: {} })),
  enableTwoFactorApi: vi.fn(() => Promise.resolve({ data: undefined })),
  disableTwoFactorApi: vi.fn(() => Promise.resolve({ data: undefined })),
  revokeDeviceApi: vi.fn(() => Promise.resolve({ data: undefined })),
  revokeAllDevicesApi: vi.fn(() => Promise.resolve({ data: undefined })),
  exportMyDataApi: vi.fn(() => Promise.resolve({ data: new Blob() }))
}))

vi.mock('@/api/modules/settings', () => ({
  fetchProviderConfigsApi: vi.fn(),
  updateProviderConfigsApi: vi.fn(),
  checkProviderConfigsApi: vi.fn()
}))

const providerRows = [
  {
    scope: 'llm',
    label: '主模型',
    description: '配置通用问答、训练建议和 agent 任务使用的主 LLM。',
    enabled: true,
    configured: true,
    status: 'ready',
    statusMessage: '配置完整。',
    providerName: 'OpenAI',
    baseUrl: 'https://api.openai.com/v1',
    model: 'gpt-4o-mini',
    apiKeyMasked: 'sk-****abcd',
    lastCheckedAt: '2026-06-21T10:00:00',
    lastCheckStatus: 'ready',
    lastCheckMessage: '主模型 探测成功，可正常访问模型目录。'
  }
]

describe('SettingsPage provider settings', () => {
  it('edits provider config with masked secret and triggers check separately', async () => {
    vi.mocked(fetchProviderConfigsApi).mockResolvedValue({ data: providerRows } as any)
    vi.mocked(updateProviderConfigsApi).mockResolvedValue({
      data: [{ ...providerRows[0], baseUrl: 'https://gateway.example.com/v1', model: 'gpt-4.1-mini' }]
    } as any)
    vi.mocked(checkProviderConfigsApi).mockResolvedValue({ data: providerRows } as any)

    const user = userEvent.setup()
    renderWithProviders(<SettingsPage />, { route: '/settings?tab=providers' })

    await user.click(await screen.findByRole('tab', { name: '服务配置' }))

    expect((await screen.findAllByText('主模型')).length).toBeGreaterThan(0)
    expect(screen.getAllByText('sk-****abcd').length).toBeGreaterThan(0)

    await user.click(screen.getByRole('button', { name: '编辑主模型配置' }))
    expect(screen.queryByDisplayValue('sk-****abcd')).not.toBeInTheDocument()

    await user.clear(screen.getByLabelText('Base URL'))
    await user.type(screen.getByLabelText('Base URL'), 'https://gateway.example.com/v1')
    await user.clear(screen.getByLabelText('模型'))
    await user.type(screen.getByLabelText('模型'), 'gpt-4.1-mini')
    await user.type(screen.getByLabelText('API Key'), 'new-secret')
    await user.click(screen.getByRole('button', { name: '保存配置' }))

    await waitFor(() => {
      expect(updateProviderConfigsApi).toHaveBeenCalledWith([
        expect.objectContaining({
          scope: 'llm',
          enabled: true,
          providerName: 'OpenAI',
          baseUrl: 'https://gateway.example.com/v1',
          model: 'gpt-4.1-mini',
          apiKey: 'new-secret'
        })
      ])
    })

    await user.click(screen.getByRole('button', { name: '重新检测' }))

    await waitFor(() => {
      expect(checkProviderConfigsApi).toHaveBeenCalledTimes(1)
    })
  })
})
