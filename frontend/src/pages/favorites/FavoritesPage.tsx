import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App as AntApp, Button, Form, Input, Modal, Select, Space, Tag } from 'antd'
import { addFavoriteApi, batchRemoveFavoriteApi, createFavoriteTagApi, fetchFavoriteListApi, fetchFavoriteStatsApi, fetchFavoriteTagsApi, removeFavoriteApi } from '@/api/modules/favorites'
import { getErrorMessage } from '@/api/client'
import { DataTableCard, formatDateTime, normalizeRecords, pickText } from '@/modules/common'
import { ModulePage } from '@/modules/common'

export function FavoritesPage() {
  const { message } = AntApp.useApp()
  const queryClient = useQueryClient()
  const [selected, setSelected] = useState<React.Key[]>([])
  const [favoriteOpen, setFavoriteOpen] = useState(false)
  const [tagOpen, setTagOpen] = useState(false)
  const list = useQuery({ queryKey: ['favorites', 'list'], queryFn: () => fetchFavoriteListApi({ pageNum: 1, pageSize: 20 }).then((response) => response.data) })
  const stats = useQuery({ queryKey: ['favorites', 'stats'], queryFn: () => fetchFavoriteStatsApi().then((response) => response.data) })
  const tags = useQuery({ queryKey: ['favorites', 'tags'], queryFn: () => fetchFavoriteTagsApi().then((response) => response.data) })
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['favorites'] })
  const add = useMutation({ mutationFn: addFavoriteApi, onSuccess: () => { message.success('已添加收藏'); setFavoriteOpen(false); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '添加收藏失败')) })
  const createTag = useMutation({ mutationFn: createFavoriteTagApi, onSuccess: () => { message.success('分组已创建'); setTagOpen(false); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '创建分组失败')) })
  const remove = useMutation({ mutationFn: (id: number) => removeFavoriteApi(id), onSuccess: () => { message.success('已取消收藏'); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '删除失败')) })
  const batchRemove = useMutation({ mutationFn: (ids: number[]) => batchRemoveFavoriteApi(ids), onSuccess: () => { message.success('已批量删除'); setSelected([]); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '批量删除失败')) })
  const tagRows = normalizeRecords(tags.data)
  return (
    <ModulePage
      title="收藏"
      description="管理题目、文章、岗位和复盘结论，支持分组和批量清理。"
      metrics={[
        { label: '收藏总数', value: (stats.data as any)?.total ?? normalizeRecords(list.data).length, hint: '当前账号' },
        { label: '分组', value: tagRows.length, hint: '自定义标签' },
        { label: '已选中', value: selected.length, hint: '可批量删除' },
        { label: '最近同步', value: list.isFetching ? '同步中' : '已完成', hint: '列表状态' }
      ]}
      actions={<Space><Button type="primary" onClick={() => setFavoriteOpen(true)}>新增收藏</Button><Button onClick={() => setTagOpen(true)}>新建分组</Button><Button danger disabled={!selected.length} loading={batchRemove.isPending} onClick={() => batchRemove.mutate(selected.map(Number))}>批量删除</Button></Space>}
    >
      <DataTableCard
        title="收藏列表"
        data={list.data}
        loading={list.isLoading}
        error={list.error}
        onRetry={() => list.refetch()}
        rowSelection={{ selectedRowKeys: selected, onChange: setSelected }}
        columns={[
          { title: '标题', render: (_, row) => pickText(row, ['title', 'targetTitle', 'name'], '收藏项') },
          { title: '类型', render: (_, row) => <Tag color="blue">{pickText(row, ['targetType', 'type'])}</Tag> },
          { title: '分组', render: (_, row) => pickText(row, ['tagName']) },
          { title: '时间', render: (_, row) => formatDateTime(row.createTime || row.createdAt) },
          { title: '操作', render: (_, row) => <Button danger size="small" loading={remove.isPending} onClick={() => remove.mutate(Number(row.id))}>删除</Button> }
        ]}
      />
      <Modal open={favoriteOpen} title="新增收藏" footer={null} onCancel={() => setFavoriteOpen(false)}>
        <Form layout="vertical" onFinish={(values) => add.mutate(values)}>
          <Form.Item label="目标类型" name="targetType" rules={[{ required: true }]}><Select options={[{ value: 'question', label: '题目' }, { value: 'knowledge', label: '知识' }, { value: 'application', label: '投递' }, { value: 'review', label: '复盘' }]} /></Form.Item>
          <Form.Item label="目标 ID" name="targetId" rules={[{ required: true }]}><Input type="number" /></Form.Item>
          <Form.Item label="分组" name="tagId"><Select allowClear options={tagRows.map((tag) => ({ value: tag.id, label: pickText(tag, ['name']) }))} /></Form.Item>
          <Button type="primary" htmlType="submit" loading={add.isPending}>保存</Button>
        </Form>
      </Modal>
      <Modal open={tagOpen} title="新建分组" footer={null} onCancel={() => setTagOpen(false)}>
        <Form layout="vertical" onFinish={(values) => createTag.mutate(values)}>
          <Form.Item label="分组名称" name="name" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item label="排序" name="sortOrder"><Input type="number" /></Form.Item>
          <Button type="primary" htmlType="submit" loading={createTag.isPending}>保存</Button>
        </Form>
      </Modal>
    </ModulePage>
  )
}
