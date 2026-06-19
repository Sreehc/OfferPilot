import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App as AntApp, Button, Card, Drawer, Form, Input, List, Modal, Popconfirm, Select, Space, Typography, Upload } from 'antd'
import { deleteKnowledgeDocApi, fetchKnowledgeDocDetailApi, fetchKnowledgeDocsApi, reindexKnowledgeDocApi, searchKnowledgeApi, uploadKnowledgeDocApi } from '@/api/modules/knowledge'
import { fetchCategoriesApi } from '@/api/modules/category'
import { getErrorMessage } from '@/api/client'
import { useAuthStore } from '@/features/auth/authStore'
import { DataTableCard, formatDateTime, normalizeRecords, pickText, StatusTag } from '@/modules/common'
import { ModulePage } from '@/modules/common'

export function KnowledgePage() {
  const { message } = AntApp.useApp()
  const queryClient = useQueryClient()
  const isAdmin = useAuthStore((state) => state.user?.role) === 'ADMIN'
  const [searchOpen, setSearchOpen] = useState(false)
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
  const search = useMutation({ mutationFn: (keyword: string) => searchKnowledgeApi(keyword), onSuccess: (response) => setSearchResult(response.data), onError: (error) => message.error(getErrorMessage(error, '搜索失败')) })
  const rows = normalizeRecords(query.data)
  const categoryRows = normalizeRecords(categories.data)
  const readerDoc = reader.data as any
  return (
    <ModulePage
      title="知识库"
      description="点击文档可阅读全文，支持上传与语义搜索。"
      metrics={[
        { label: '文档数', value: rows.length, hint: '当前列表' },
        { label: '搜索', value: search.isPending ? '进行中' : '可用', hint: '语义检索' },
        { label: '重索引', value: reindex.isPending ? '运行中' : '空闲', hint: '后台任务' },
        { label: '状态', value: query.isFetching ? '刷新中' : '已同步', hint: '文档列表' }
      ]}
      actions={<Space wrap><Input.Search allowClear placeholder="搜索文档标题" onSearch={(keyword) => setFilters((current) => ({ ...current, keyword }))} /><Select allowClear placeholder="分类" style={{ width: 140 }} options={categoryRows.map((item) => ({ value: item.id, label: pickText(item, ['name']) }))} onChange={(categoryId) => { setUploadCategoryId(categoryId); setFilters((current) => ({ ...current, categoryId })) }} /><Select allowClear placeholder="索引状态" style={{ width: 130 }} options={[{ value: 'READY', label: '可用' }, { value: 'INDEXING', label: '索引中' }, { value: 'FAILED', label: '失败' }]} onChange={(status) => setFilters((current) => ({ ...current, status }))} /><Upload showUploadList={false} beforeUpload={(file) => { upload.mutate(file); return Upload.LIST_IGNORE }}><Button type="primary" loading={upload.isPending}>上传文档</Button></Upload><Button onClick={() => setSearchOpen(true)}>语义搜索内容</Button></Space>}
    >
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
      <Drawer open={Boolean(readerId)} onClose={() => setReaderId(null)} width={560} title={pickText(readerDoc, ['title', 'fileName', 'name'], '文档详情')} loading={reader.isLoading}>
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
      <Modal open={searchOpen} title="语义搜索内容" onCancel={() => setSearchOpen(false)} footer={null}>
        <Form layout="vertical" onFinish={({ keyword }) => search.mutate(keyword)}>
          <Form.Item label="关键词" name="keyword" rules={[{ required: true }]}><Input.Search enterButton="搜索" loading={search.isPending} /></Form.Item>
        </Form>
        {searchResult ? (
          <Card size="small" title="搜索结果" className="surface-card">
            <List
              dataSource={normalizeRecords(searchResult)}
              locale={{ emptyText: '没有匹配结果' }}
              renderItem={(item) => <List.Item><List.Item.Meta title={pickText(item, ['title', 'name', 'fileName'], '知识条目')} description={pickText(item, ['summary', 'content', 'description'], '-')} /></List.Item>}
            />
          </Card>
        ) : null}
      </Modal>
    </ModulePage>
  )
}
