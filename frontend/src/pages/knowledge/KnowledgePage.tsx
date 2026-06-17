import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App as AntApp, Button, Card, Form, Input, List, Modal, Popconfirm, Select, Space, Upload } from 'antd'
import { deleteKnowledgeDocApi, fetchKnowledgeDocsApi, reindexKnowledgeDocApi, searchKnowledgeApi, uploadKnowledgeDocApi } from '@/api/modules/knowledge'
import { fetchCategoriesApi } from '@/api/modules/category'
import { getErrorMessage } from '@/api/client'
import { DataTableCard, formatDateTime, normalizeRecords, pickText, StatusTag } from '@/modules/common'
import { ModulePage } from '@/modules/common'

export function KnowledgePage() {
  const { message } = AntApp.useApp()
  const queryClient = useQueryClient()
  const [searchOpen, setSearchOpen] = useState(false)
  const [searchResult, setSearchResult] = useState<unknown>(null)
  const [filters, setFilters] = useState({ keyword: '', categoryId: undefined as number | undefined, status: undefined as string | undefined })
  const [uploadCategoryId, setUploadCategoryId] = useState<number | undefined>()
  const query = useQuery({ queryKey: ['knowledge', filters], queryFn: () => fetchKnowledgeDocsApi({ pageNum: 1, pageSize: 20, ...filters }).then((response) => response.data) })
  const categories = useQuery({ queryKey: ['categories'], queryFn: () => fetchCategoriesApi().then((response) => response.data) })
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['knowledge'] })
  const upload = useMutation({ mutationFn: (file: File) => uploadKnowledgeDocApi(file, uploadCategoryId), onSuccess: () => { message.success('文档已上传'); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '上传失败')) })
  const reindex = useMutation({ mutationFn: (id: number) => reindexKnowledgeDocApi(id), onSuccess: () => { message.success('已加入重索引任务'); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '重索引失败')) })
  const remove = useMutation({ mutationFn: (id: number) => deleteKnowledgeDocApi(id), onSuccess: () => { message.success('文档已删除'); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '删除失败')) })
  const search = useMutation({ mutationFn: (keyword: string) => searchKnowledgeApi(keyword), onSuccess: (response) => setSearchResult(response.data), onError: (error) => message.error(getErrorMessage(error, '搜索失败')) })
  const rows = normalizeRecords(query.data)
  const categoryRows = normalizeRecords(categories.data)
  return (
    <ModulePage
      title="知识库"
      description="搜索、上传、重索引和题目上下文的统一入口。"
      metrics={[
        { label: '文档数', value: rows.length, hint: '当前列表' },
        { label: '搜索', value: search.isPending ? '进行中' : '可用', hint: '语义检索' },
        { label: '重索引', value: reindex.isPending ? '运行中' : '空闲', hint: '后台任务' },
        { label: '状态', value: query.isFetching ? '刷新中' : '已同步', hint: '文档列表' }
      ]}
      actions={<Space wrap><Input.Search allowClear placeholder="搜索文档" onSearch={(keyword) => setFilters((current) => ({ ...current, keyword }))} /><Select allowClear placeholder="分类" style={{ width: 140 }} options={categoryRows.map((item) => ({ value: item.id, label: pickText(item, ['name']) }))} onChange={(categoryId) => { setUploadCategoryId(categoryId); setFilters((current) => ({ ...current, categoryId })) }} /><Select allowClear placeholder="索引状态" style={{ width: 130 }} options={[{ value: 'READY', label: 'READY' }, { value: 'INDEXING', label: 'INDEXING' }, { value: 'FAILED', label: 'FAILED' }]} onChange={(status) => setFilters((current) => ({ ...current, status }))} /><Upload showUploadList={false} beforeUpload={(file) => { upload.mutate(file); return Upload.LIST_IGNORE }}><Button type="primary" loading={upload.isPending}>上传文档</Button></Upload><Button onClick={() => setSearchOpen(true)}>搜索知识库</Button></Space>}
    >
      <DataTableCard
        title="文档列表"
        data={query.data}
        loading={query.isLoading}
        error={query.error}
        onRetry={() => query.refetch()}
        columns={[
          { title: '文档', render: (_, row) => pickText(row, ['title', 'fileName', 'name'], '知识文档') },
          { title: '分类', render: (_, row) => pickText(row, ['categoryName', 'category']) },
          { title: '状态', render: (_, row) => <StatusTag value={pickText(row, ['status', 'indexStatus'])} /> },
          { title: '更新时间', render: (_, row) => formatDateTime(row.updateTime || row.createTime) },
          { title: '操作', render: (_, row) => <Space><Popconfirm title="重索引文档" description="该文档会进入后台重索引队列。" onConfirm={() => reindex.mutate(Number(row.id || row.docId))}><Button size="small" loading={reindex.isPending}>重索引</Button></Popconfirm><Popconfirm title="删除文档" description="此操作不可恢复。" onConfirm={() => remove.mutate(Number(row.id || row.docId))}><Button danger size="small" loading={remove.isPending}>删除</Button></Popconfirm></Space> }
        ]}
      />
      <Modal open={searchOpen} title="搜索知识库" onCancel={() => setSearchOpen(false)} footer={null}>
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
