import { describe, expect, it, vi } from 'vitest'
import { Route, Routes } from 'react-router-dom'
import { screen, within } from '@testing-library/react'
import { ResumeAssistantPage } from '@/pages/resume/ResumeAssistantPage'
import { renderWithProviders } from '@/test/renderWithProviders'
import {
  fetchResumeDetailApi,
  fetchResumeListApi,
  fetchResumeProjectQuestionsApi,
  fetchResumeScoreApi,
  fetchResumeVersionsApi
} from '@/api/modules/resume'

vi.mock('@/api/modules/resume', () => ({
  uploadResumeApi: vi.fn(),
  fetchResumeListApi: vi.fn(),
  fetchLatestResumeApi: vi.fn(),
  fetchResumeDetailApi: vi.fn(),
  updateResumeApi: vi.fn(),
  fetchResumeProjectQuestionsApi: vi.fn(),
  fetchResumeIntroApi: vi.fn(),
  fetchInterviewResumeApi: vi.fn(),
  retryResumeParseApi: vi.fn(),
  fetchResumeScoreApi: vi.fn(),
  fetchResumeVersionsApi: vi.fn(),
  restoreResumeVersionApi: vi.fn()
}))

function renderResumePage() {
  return renderWithProviders(
    <Routes>
      <Route path="/resume" element={<ResumeAssistantPage />} />
    </Routes>,
    { route: '/resume' }
  )
}

describe('ResumeAssistantPage', () => {
  it('shows a resume workspace with versions, summary, score, suggestions and project questions in one screen', async () => {
    vi.mocked(fetchResumeListApi).mockResolvedValue({
      code: 0,
      message: 'ok',
      data: {
        records: [
          { id: 7, resumeTitle: 'Java 后端冲刺版', fileName: 'java-backend.pdf', score: 82, status: 'parsed', updateTime: '2026-06-20T08:00:00Z' },
          { id: 6, resumeTitle: '通用后端版', fileName: 'backend.pdf', score: 74, status: 'parsed', updateTime: '2026-06-18T08:00:00Z' }
        ]
      }
    })
    vi.mocked(fetchResumeDetailApi).mockResolvedValue({
      code: 0,
      message: 'ok',
      data: {
        id: 7,
        resumeTitle: 'Java 后端冲刺版',
        fileName: 'java-backend.pdf',
        status: 'parsed',
        summary: '聚焦 Spring Boot、Redis 缓存治理和高并发接口优化。',
        skills: 'Java, Spring Boot, Redis, MySQL',
        selfIntro: '3 年 Java 后端经验，主导过高并发缓存平台。',
        updateTime: '2026-06-20T08:00:00Z'
      }
    })
    vi.mocked(fetchResumeScoreApi).mockResolvedValue({
      code: 0,
      message: 'ok',
      data: {
        score: 82,
        level: 'ready',
        summary: '项目表达完整，但量化结果还可以更明确。',
        suggestions: [
          { title: '补充量化指标', content: '在 Redis 项目中补充 QPS、延迟和命中率变化。' },
          { title: '压缩技术栈堆叠', content: '把工具罗列改成问题、方案、结果。' }
        ]
      }
    })
    vi.mocked(fetchResumeProjectQuestionsApi).mockResolvedValue({
      code: 0,
      message: 'ok',
      data: {
        records: [
          { id: 1, question: 'Redis 缓存穿透如何在项目中落地？' },
          { id: 2, question: '接口 P99 延迟是如何压下来的？' }
        ]
      }
    })
    vi.mocked(fetchResumeVersionsApi).mockResolvedValue({
      code: 0,
      message: 'ok',
      data: {
        records: [
          { versionId: 21, versionName: '面试冲刺版 v3', createTime: '2026-06-20T08:00:00Z' },
          { versionId: 20, versionName: '通用投递版 v2', createTime: '2026-06-18T08:00:00Z' }
        ]
      }
    })

    renderResumePage()

    const workspace = await screen.findByRole('region', { name: '简历工作台' })
    expect(within(workspace).getByText('Java 后端冲刺版')).toBeInTheDocument()
    expect(within(workspace).getByText('82')).toBeInTheDocument()
    expect(within(workspace).getByText('聚焦 Spring Boot、Redis 缓存治理和高并发接口优化。')).toBeInTheDocument()

    const versions = await screen.findByRole('region', { name: '版本列表' })
    expect(within(versions).getByText('通用后端版')).toBeInTheDocument()

    const summary = await screen.findByRole('region', { name: '简历摘要' })
    expect(within(summary).getByText('Java, Spring Boot, Redis, MySQL')).toBeInTheDocument()

    const suggestions = await screen.findByRole('region', { name: '优化建议' })
    expect(within(suggestions).getByText('补充量化指标')).toBeInTheDocument()
    expect(within(suggestions).getByText('在 Redis 项目中补充 QPS、延迟和命中率变化。')).toBeInTheDocument()

    const questions = await screen.findByRole('region', { name: '项目追问' })
    expect(within(questions).getByText('Redis 缓存穿透如何在项目中落地？')).toBeInTheDocument()

    const history = await screen.findByRole('region', { name: '版本历史' })
    expect(within(history).getByText('面试冲刺版 v3')).toBeInTheDocument()
    expect(within(history).getAllByRole('button', { name: '恢复版本' })[0]).toBeInTheDocument()

    expect(screen.queryByText(/Copilot/i)).not.toBeInTheDocument()
  })
})
