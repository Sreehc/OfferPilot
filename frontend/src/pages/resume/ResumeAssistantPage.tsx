import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App as AntApp, Button, Card, Empty, Popconfirm, Progress, Space, Tag, Typography, Upload } from 'antd'
import {
  fetchResumeDetailApi,
  fetchResumeListApi,
  fetchResumeProjectQuestionsApi,
  fetchResumeScoreApi,
  fetchResumeVersionsApi,
  restoreResumeVersionApi,
  retryResumeParseApi,
  uploadResumeApi
} from '@/api/modules/resume'
import { getErrorMessage } from '@/api/client'
import type { AnyRecord } from '@/api/types'
import { formatDateTime, getRecordId, labelOf, normalizeRecords, pickArray, pickText, StatusTag } from '@/modules/common'
import { ModulePage } from '@/modules/common'

const { Paragraph, Text, Title } = Typography

function getResumeId(record: AnyRecord | undefined) {
  if (!record) return ''
  return String(record.id ?? record.resumeId ?? record.fileId ?? '')
}

function asScore(value: unknown) {
  const score = Number(value)
  return Number.isFinite(score) ? Math.max(0, Math.min(100, Math.round(score))) : undefined
}

function buildSuggestionItems(active: AnyRecord | undefined, scoreData: unknown) {
  return [
    ...pickArray<AnyRecord>(active, ['suggestions', 'improvements', 'nextActions']),
    ...pickArray<AnyRecord>(scoreData as AnyRecord | undefined, ['suggestions', 'items'])
  ]
}

function suggestionTitle(item: AnyRecord, index: number) {
  return pickText(item, ['title', 'name', 'summary', 'content'], `建议 ${index + 1}`)
}

function suggestionContent(item: AnyRecord) {
  const title = suggestionTitle(item, 0)
  const content = pickText(item, ['content', 'description', 'detail', 'summary'], '')
  return content && content !== title ? content : ''
}

export function ResumeAssistantPage() {
  const { message } = AntApp.useApp()
  const queryClient = useQueryClient()
  const [detailId, setDetailId] = useState<string | null>(null)
  const list = useQuery({ queryKey: ['resume'], queryFn: () => fetchResumeListApi().then((response) => response.data) })
  const rows = useMemo(() => normalizeRecords<AnyRecord>(list.data), [list.data])
  const activeListItem = rows.find((row) => getResumeId(row) === detailId) || rows[0]
  const selectedId = detailId || getResumeId(activeListItem) || null
  const detail = useQuery({
    queryKey: ['resume', selectedId],
    queryFn: () => selectedId ? fetchResumeDetailApi(selectedId).then((response) => response.data) : Promise.resolve(null),
    enabled: Boolean(selectedId)
  })
  const questions = useQuery({
    queryKey: ['resume', selectedId, 'questions'],
    queryFn: () => selectedId ? fetchResumeProjectQuestionsApi(selectedId).then((response) => response.data) : Promise.resolve(null),
    enabled: Boolean(selectedId)
  })
  const score = useQuery({
    queryKey: ['resume', selectedId, 'score'],
    queryFn: () => selectedId ? fetchResumeScoreApi(selectedId).then((response) => response.data) : Promise.resolve(null),
    enabled: Boolean(selectedId)
  })
  const versions = useQuery({
    queryKey: ['resume', selectedId, 'versions'],
    queryFn: () => selectedId ? fetchResumeVersionsApi(selectedId).then((response) => response.data) : Promise.resolve(null),
    enabled: Boolean(selectedId)
  })

  useEffect(() => {
    if (detailId || rows.length === 0) return
    const firstId = getResumeId(rows[0])
    if (firstId) setDetailId(firstId)
  }, [detailId, rows])

  useEffect(() => {
    if (!detailId || rows.length === 0) return
    if (rows.some((row) => getResumeId(row) === detailId)) return
    const firstId = getResumeId(rows[0])
    setDetailId(firstId || null)
  }, [detailId, rows])

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['resume'] })
  }
  const upload = useMutation({
    mutationFn: (file: File) => uploadResumeApi(file),
    onSuccess: () => { message.success('简历已上传'); invalidate() },
    onError: (error) => message.error(getErrorMessage(error, '上传失败'))
  })
  const retry = useMutation({
    mutationFn: (id: string) => retryResumeParseApi(id),
    onSuccess: () => { message.success('已重新解析'); invalidate() },
    onError: (error) => message.error(getErrorMessage(error, '重试失败'))
  })
  const restore = useMutation({
    mutationFn: (versionId: string) => restoreResumeVersionApi(versionId),
    onSuccess: () => { message.success('已恢复到该版本'); invalidate() },
    onError: (error) => message.error(getErrorMessage(error, '恢复失败'))
  })
  const refreshCurrent = () => {
    void detail.refetch()
    void score.refetch()
    void questions.refetch()
    void versions.refetch()
  }

  const active = (detail.data || activeListItem) as AnyRecord | undefined
  const scoreValue = asScore((score.data as AnyRecord | undefined)?.score ?? active?.score ?? activeListItem?.score)
  const versionRows = normalizeRecords<AnyRecord>(versions.data)
  const questionRows = normalizeRecords<AnyRecord>(questions.data)
  const suggestions = buildSuggestionItems(active, score.data)
  const resumeTitle = pickText(active, ['resumeTitle', 'title', 'fileName'], pickText(activeListItem, ['resumeTitle', 'title', 'fileName'], '请选择简历'))
  const summary = pickText(active, ['summary', 'profileSummary', 'selfIntro', 'interviewResumeText'], '暂无简历摘要，上传或重新解析后可生成。')
  const skills = pickText(active, ['skills', 'techStack', 'skillTags'], '暂无技能标签')

  return (
    <ModulePage
      title="简历助手"
      description="上传、解析、评分、优化建议和版本恢复统一放在这里。"
      metrics={[
        { label: '简历数', value: rows.length, hint: '当前列表' },
        { label: '评分', value: scoreValue ?? '-', hint: '系统评分' },
        { label: '版本', value: versionRows.length, hint: '可恢复历史' },
        { label: '状态', value: list.isFetching ? '刷新中' : '已同步', hint: '简历数据' }
      ]}
      actions={(
        <Upload showUploadList={false} beforeUpload={(file) => { upload.mutate(file); return Upload.LIST_IGNORE }}>
          <Button type="primary" loading={upload.isPending}>上传简历</Button>
        </Upload>
      )}
      state={{
        loading: list.isLoading,
        error: list.error,
        onRetry: () => { void list.refetch() },
        empty: !list.isLoading && !list.error && rows.length === 0,
        emptyTitle: '暂无简历',
        emptyDescription: '上传第一份简历后即可查看评分、优化建议和项目追问。'
      }}
    >
      <section className="resume-workbench" role="region" aria-label="简历工作台">
        <section className="resume-version-panel surface-card" role="region" aria-label="版本列表">
          <div className="resume-section-head">
            <Text className="resume-eyebrow">版本列表</Text>
            <Text type="secondary">{rows.length} 份简历</Text>
          </div>
          <div className="resume-version-list">
            {rows.map((row) => {
              const rowId = getResumeId(row)
              const selected = rowId === selectedId
              return (
                <button
                  key={rowId || getRecordId(row)}
                  type="button"
                  className={selected ? 'resume-version-card is-active' : 'resume-version-card'}
                  onClick={() => rowId && setDetailId(rowId)}
                  aria-pressed={selected}
                >
                  <span className="resume-version-title">{pickText(row, ['resumeTitle', 'title'], pickText(row, ['fileName'], '简历'))}</span>
                  <span className="resume-version-meta">{formatDateTime(row.updateTime || row.createTime || row.createdAt)}</span>
                  <span className="resume-version-footer">
                    <StatusTag value={pickText(row, ['status', 'parseStatus'], 'READY')} />
                    <span>{pickText(row, ['score'], '-')} 分</span>
                  </span>
                </button>
              )
            })}
          </div>
        </section>

        <div className="resume-main-panel">
          <section className="resume-hero surface-card" role="region" aria-label="简历摘要">
            <div className="resume-hero-main">
              <Text className="resume-eyebrow">当前简历</Text>
              <Title level={2}>当前简历：{resumeTitle}</Title>
              <Paragraph type="secondary">{summary}</Paragraph>
              <Space wrap size={[8, 8]}>
                <Tag color="blue">{skills}</Tag>
                <Tag>{labelOf(pickText(active, ['status', 'parseStatus'], 'READY'))}</Tag>
                <Tag>{formatDateTime(active?.updateTime || active?.createTime || active?.createdAt)}</Tag>
              </Space>
            </div>
            <div className="resume-score-card" role="region" aria-label="AI 评分">
              <Text type="secondary">AI 评分</Text>
              <strong>{scoreValue ?? '-'}</strong>
              <Progress percent={scoreValue ?? 0} showInfo={false} status={scoreValue !== undefined && scoreValue < 70 ? 'exception' : 'normal'} />
              <Text type="secondary">{pickText(score.data as AnyRecord | undefined, ['summary', 'comment'], '评分完成后会展示投递准备度。')}</Text>
            </div>
          </section>

          <div className="resume-insight-grid">
            <section role="region" aria-label="优化建议">
              <Card title="优化建议" className="surface-card resume-insight-card" loading={Boolean(selectedId) && score.isLoading}>
                {suggestions.length === 0 ? (
                  <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无优化建议" />
                ) : (
                  <div className="resume-suggestion-list">
                    {suggestions.map((item, index) => (
                      <article key={`${suggestionTitle(item, index)}-${index}`} className="resume-suggestion-item">
                        <Text strong>{suggestionTitle(item, index)}</Text>
                        {suggestionContent(item) && <Paragraph type="secondary">{suggestionContent(item)}</Paragraph>}
                      </article>
                    ))}
                  </div>
                )}
              </Card>
            </section>

            <section role="region" aria-label="项目追问">
              <Card title="项目追问" className="surface-card resume-insight-card" loading={Boolean(selectedId) && questions.isLoading}>
                {questionRows.length === 0 ? (
                  <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无项目追问" />
                ) : (
                  <div className="resume-question-list">
                    {questionRows.map((item, index) => (
                      <div key={getRecordId(item, String(index))} className="resume-question-item">
                        <Text type="secondary">追问 {index + 1}</Text>
                        <Text strong>{pickText(item, ['question', 'title', 'content'], '项目追问')}</Text>
                      </div>
                    ))}
                  </div>
                )}
              </Card>
            </section>
          </div>

          <section role="region" aria-label="版本历史">
            <Card title="版本历史" className="surface-card" loading={Boolean(selectedId) && versions.isLoading}>
              {versionRows.length === 0 ? (
                <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无历史版本" />
              ) : (
                <div className="resume-history-list">
                  {versionRows.map((item) => {
                    const versionId = String(item.id ?? item.versionId ?? '')
                    return (
                      <div key={versionId || getRecordId(item)} className="resume-history-item">
                        <div>
                          <Text strong>{pickText(item, ['versionName', 'name', 'title'], '版本')}</Text>
                          <div className="muted-text">{formatDateTime(item.updateTime || item.createTime || item.createdAt)}</div>
                        </div>
                        <Popconfirm
                          title="恢复到该版本？"
                          description="当前内容将被该版本覆盖。"
                          okText="恢复"
                          cancelText="取消"
                          onConfirm={() => versionId && restore.mutate(versionId)}
                        >
                          <Button size="small" loading={restore.isPending}>恢复版本</Button>
                        </Popconfirm>
                      </div>
                    )
                  })}
                </div>
              )}
            </Card>
          </section>
        </div>

        <aside className="resume-action-panel surface-card">
          <Text className="resume-eyebrow">快捷操作</Text>
          <Button block onClick={() => selectedId && retry.mutate(selectedId)} loading={retry.isPending} disabled={!selectedId}>重新解析</Button>
          <Button block onClick={refreshCurrent} disabled={!selectedId}>刷新当前简历</Button>
          <Text type="secondary">页面内 AI 助手属于第二阶段，当前优先完成简历资产工作台。</Text>
        </aside>
      </section>
    </ModulePage>
  )
}
