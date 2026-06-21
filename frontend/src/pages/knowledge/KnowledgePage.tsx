import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { App as AntApp, Button, Card, Drawer, Empty, Input, Popconfirm, Select, Space, Tag, Typography, Upload } from 'antd'
import { deleteKnowledgeDocApi, fetchKnowledgeDocDetailApi, fetchKnowledgeDocsApi, reindexKnowledgeDocApi, searchKnowledgeApi, uploadKnowledgeDocApi } from '@/api/modules/knowledge'
import { fetchCategoriesApi } from '@/api/modules/category'
import { getErrorMessage } from '@/api/client'
import { useAuthStore } from '@/features/auth/authStore'
import { DataTableCard, formatDateTime, normalizeRecords, pickNumber, pickText, StatusTag } from '@/modules/common'
import { ModulePage } from '@/modules/common'

export function KnowledgePage() {
  const { message } = AntApp.useApp()
  const queryClient = useQueryClient()
  const isAdmin = useAuthStore((state) => state.user?.role) === 'ADMIN'
  const [searchKeyword, setSearchKeyword] = useState('')
  const [submittedKeyword, setSubmittedKeyword] = useState('')
  const [searchResult, setSearchResult] = useState<unknown>(null)
  const [readerId, setReaderId] = useState<number | null>(null)
  const [filters, setFilters] = useState({ keyword: '', categoryId: undefined as number | undefined, status: undefined as string | undefined })
  const [uploadCategoryId, setUploadCategoryId] = useState<number | undefined>()
  const query = useQuery({ queryKey: ['knowledge', filters], queryFn: () => fetchKnowledgeDocsApi({ pageNum: 1, pageSize: 20, ...filters }).then((response) => response.data) })
  const categories = useQuery({ queryKey: ['categories'], queryFn: () => fetchCategoriesApi().then((response) => response.data) })
  const reader = useQuery({ queryKey: ['knowledge', 'doc', readerId], queryFn: () => readerId ? fetchKnowledgeDocDetailApi(readerId).then((response) => response.data) : Promise.resolve(null), enabled: Boolean(readerId) })
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['knowledge'] })
  const upload = useMutation({ mutationFn: (file: File) => uploadKnowledgeDocApi(file, uploadCategoryId), onSuccess: () => { message.success('文档已上传'); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '上传失败')) })
  const reindex = useMutation({ mutationFn: (id: number) => reindexKnowledgeDocApi(id), onSuccess: () => { message.success('已加入重索引任务'); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '重索引失败')) })
  const remove = useMutation({ mutationFn: (id: number) => deleteKnowledgeDocApi(id), onSuccess: () => { message.success('文档已删除'); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '删除失败')) })
  const search = useMutation({
    mutationFn: (keyword: string) => searchKnowledgeApi(keyword),
    onSuccess: (response, keyword) => {
      setSubmittedKeyword(keyword)
      setSearchResult(response.data)
    },
    onError: (error) => message.error(getErrorMessage(error, '搜索失败'))
  })
  const rows = normalizeRecords(query.data)
  const categoryRows = normalizeRecords(categories.data)
  const searchRows = normalizeRecords(searchResult)
  const readerDoc = reader.data as any
  const runSearch = () => {
    const keyword = searchKeyword.trim()
    if (!keyword) {
      message.warning('请输入检索问题')
      return
    }
    search.mutate(keyword)
  }
  const uploadButton = (label = '上传文档') => (
    <Upload showUploadList={false} beforeUpload={(file) => { upload.mutate(file); return Upload.LIST_IGNORE }}>
      <Button type={label === '上传文档' ? 'primary' : 'default'} loading={upload.isPending}>{label}</Button>
    </Upload>
  )

  return (
    <ModulePage
      title="知识库"
      description="用 AI 检索把资料摘要、命中片段和来源引用拆开呈现，便于继续追问和复盘。"
      metrics={[
        { label: '文档数', value: rows.length, hint: '当前列表' },
        { label: '搜索', value: search.isPending ? '进行中' : '可用', hint: '语义检索' },
        { label: '重索引', value: reindex.isPending ? '运行中' : '空闲', hint: '后台任务' },
        { label: '状态', value: query.isFetching ? '刷新中' : '已同步', hint: '文档列表' }
      ]}
      actions={<Space wrap><Input.Search allowClear placeholder="搜索文档标题" onSearch={(keyword) => setFilters((current) => ({ ...current, keyword }))} /><Select allowClear placeholder="分类" style={{ width: 140 }} options={categoryRows.map((item) => ({ value: item.id, label: pickText(item, ['name']) }))} onChange={(categoryId) => { setUploadCategoryId(categoryId); setFilters((current) => ({ ...current, categoryId })) }} /><Select allowClear placeholder="索引状态" style={{ width: 130 }} options={[{ value: 'READY', label: '可用' }, { value: 'INDEXING', label: '索引中' }, { value: 'FAILED', label: '失败' }]} onChange={(status) => setFilters((current) => ({ ...current, status }))} />{uploadButton()}</Space>}
    >
      <Card className="surface-card knowledge-search-card">
        <section role="region" aria-label="AI 知识检索" className="knowledge-search-panel">
          <div className="knowledge-search-header">
            <div>
              <Typography.Text className="knowledge-search-eyebrow">AI RETRIEVAL</Typography.Text>
              <Typography.Title level={3}>AI 知识检索</Typography.Title>
              <Typography.Paragraph className="muted-text">
                输入面试问题或关键词，先看摘要，再定位命中片段和来源文档。
              </Typography.Paragraph>
            </div>
          </div>
          <div className="knowledge-search-box">
            <Input
              aria-label="AI 检索问题"
              value={searchKeyword}
              placeholder="例如：ReentrantLock 和 synchronized 怎么答"
              onChange={(event) => setSearchKeyword(event.target.value)}
              onPressEnter={runSearch}
            />
            <Button type="primary" loading={search.isPending} disabled={!searchKeyword.trim()} onClick={runSearch}>检索知识库</Button>
          </div>
          {searchResult ? (
            searchRows.length ? (
              <div className="knowledge-search-results">
                {searchRows.map((item, index) => {
                  const docId = pickText(item, ['docId', 'documentId', 'knowledgeId', 'id'], '')
                  const sourceTitle = pickText(item, ['sourceTitle', 'documentTitle', 'docTitle', 'fileName', 'title'], '知识来源')
                  const title = pickText(item, ['title', 'chunkTitle', 'name', 'fileName'], sourceTitle)
                  const summary = pickText(item, ['summary', 'answer', 'abstract', 'description'], '')
                  const snippet = pickText(item, ['snippet', 'hitSnippet', 'chunkText', 'content', 'text'], '')
                  const categoryName = pickText(item, ['categoryName', 'category'], '')
                  const score = pickNumber(item, ['score', 'similarity', 'confidence'], NaN)
                  const scorePercent = pickNumber(item, ['scorePercent'], NaN)
                  const displayScorePercent = Number.isFinite(scorePercent)
                    ? scorePercent
                    : Number.isFinite(score) ? Math.round(score * 100) : NaN
                  const sourcePath = docId && docId !== '-' ? `/knowledge?docId=${docId}` : '/knowledge'
                  const followUpPath = submittedKeyword ? `/chat?source=knowledge&query=${encodeURIComponent(submittedKeyword)}` : '/chat?source=knowledge'

                  return (
                    <article className="knowledge-result-card" key={docId || title || index}>
                      <div className="knowledge-result-head">
                        <div>
                          <Typography.Title level={4}>{title}</Typography.Title>
                          <Typography.Text className="muted-text">来源：{sourceTitle}</Typography.Text>
                        </div>
                        <Space size={6} wrap>
                          {categoryName && categoryName !== '-' ? <Tag>{categoryName}</Tag> : null}
                          {Number.isFinite(displayScorePercent) ? <Tag color="blue">匹配 {displayScorePercent}%</Tag> : null}
                        </Space>
                      </div>
                      {summary && summary !== '-' ? (
                        <div className="knowledge-result-summary">
                          <Typography.Text strong>摘要</Typography.Text>
                          <Typography.Paragraph>{summary}</Typography.Paragraph>
                        </div>
                      ) : null}
                      {snippet && snippet !== '-' ? (
                        <div className="knowledge-result-snippet">
                          <Typography.Text strong>命中片段</Typography.Text>
                          <Typography.Paragraph>{snippet}</Typography.Paragraph>
                        </div>
                      ) : null}
                      <Space wrap>
                        <Link to={sourcePath}><Button>打开来源</Button></Link>
                        <Link to={followUpPath}><Button type="primary">继续追问 AI</Button></Link>
                      </Space>
                    </article>
                  )
                })}
              </div>
            ) : (
              <div className="knowledge-search-empty">
                <Empty
                  image={Empty.PRESENTED_IMAGE_SIMPLE}
                  description={(
                    <Space orientation="vertical" size={4}>
                      <Typography.Text strong>没有匹配结果</Typography.Text>
                      <Typography.Text type="secondary">可以换一个关键词，或上传相关资料后重试。</Typography.Text>
                    </Space>
                  )}
                />
                {uploadButton('上传文档')}
              </div>
            )
          ) : (
            <div className="knowledge-search-placeholder">
              <Typography.Text strong>可检索简历、题目和面试复盘相关资料</Typography.Text>
              <Typography.Text className="muted-text">结果会展示摘要、命中片段、来源引用和继续追问入口。</Typography.Text>
            </div>
          )}
        </section>
      </Card>
      <DataTableCard
        title="文档列表"
        data={query.data}
        loading={query.isLoading}
        error={query.error}
        onRetry={() => query.refetch()}
        emptyTitle="暂无文档，点击右上角上传第一份资料"
        columns={[
          { title: '文档', render: (_, row) => <Button type="link" style={{ padding: 0 }} onClick={() => setReaderId(Number(row.id || row.docId))}>{pickText(row, ['title', 'fileName', 'name'], '知识文档')}</Button> },
          { title: '分类', render: (_, row) => pickText(row, ['categoryName', 'category']) },
          { title: '状态', render: (_, row) => <StatusTag value={pickText(row, ['status', 'indexStatus'])} /> },
          { title: '更新时间', render: (_, row) => formatDateTime(row.updateTime || row.createTime) },
          { title: '操作', render: (_, row) => <Space>{isAdmin && <Popconfirm title="重索引文档" description="该文档会进入后台重索引队列。" onConfirm={() => reindex.mutate(Number(row.id || row.docId))}><Button size="small" loading={reindex.isPending}>重索引</Button></Popconfirm>}<Popconfirm title="删除文档" description="此操作不可恢复。" onConfirm={() => remove.mutate(Number(row.id || row.docId))}><Button danger size="small" loading={remove.isPending}>删除</Button></Popconfirm></Space> }
        ]}
      />
      <Drawer open={Boolean(readerId)} onClose={() => setReaderId(null)} size="large" title={pickText(readerDoc, ['title', 'fileName', 'name'], '文档详情')} loading={reader.isLoading}>
        <Space orientation="vertical" style={{ width: '100%' }} size={12}>
          <Space wrap>
            <StatusTag value={pickText(readerDoc, ['status', 'indexStatus'])} />
            <span className="muted-text">{pickText(readerDoc, ['categoryName', 'category'], '未分类')}</span>
            <span className="muted-text">{formatDateTime(readerDoc?.updateTime || readerDoc?.createTime)}</span>
          </Space>
          <Typography.Paragraph style={{ whiteSpace: 'pre-wrap' }}>
            {pickText(readerDoc, ['content', 'text', 'body', 'summary'], readerDoc ? '该文档暂无可显示的正文内容' : '')}
          </Typography.Paragraph>
        </Space>
      </Drawer>
    </ModulePage>
  )
}
