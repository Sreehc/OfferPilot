import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App as AntApp, Button, Form, Input, List, Modal, Popconfirm, Select, Space, Tag } from 'antd'
import { addFavoriteApi, batchRemoveFavoriteApi, createFavoriteTagApi, deleteFavoriteTagApi, fetchFavoriteListApi, fetchFavoriteStatsApi, fetchFavoriteTagsApi, removeFavoriteApi, updateFavoriteTagApi } from '@/api/modules/favorites'
import { getErrorMessage } from '@/api/client'
import { DataTableCard, formatDateTime, normalizeRecords, pickText, targetTypeLabel } from '@/modules/common'
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
  const createTag = useMutation({ mutationFn: createFavoriteTagApi, onSuccess: () => { message.success('分组已创建'); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '创建分组失败')) })
  const removeTag = useMutation({ mutationFn: (tagId: number) => deleteFavoriteTagApi(tagId), onSuccess: () => { message.success('分组已删除'); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '删除分组失败')) })
  const changeTag = useMutation({ mutationFn: ({ favoriteId, tagId }: { favoriteId: number; tagId: number | null }) => updateFavoriteTagApi(favoriteId, tagId), onSuccess: () => { message.success('分组已更新'); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '更新分组失败')) })
  const remove = useMutation({ mutationFn: (id: number) => removeFavoriteApi(id), onSuccess: () => { message.success('已取消收藏'); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '删除失败')) })
  const batchRemove = useMutation({ mutationFn: (ids: number[]) => batchRemoveFavoriteApi(ids), onSuccess: () => { message.success('已批量删除'); setSelected([]); invalidate() }, onError: (error) => message.error(getErrorMessage(error, '批量删除失败')) })
  const tagRows = normalizeRecords(tags.data)
  const tagOptions = tagRows.map((tag) => ({ value: Number(tag.id), label: pickText(tag, ['name']) }))
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
      actions={<Space wrap><Button type="primary" onClick={() => setFavoriteOpen(true)}>新增收藏</Button><Button onClick={() => setTagOpen(true)}>管理分组</Button><Popconfirm title="批量删除选中收藏？" description="此操作不可恢复。" disabled={!selected.length} okText="删除" okButtonProps={{ danger: true }} cancelText="取消" onConfirm={() => batchRemove.mutate(selected.map(Number))}><Button danger disabled={!selected.length} loading={batchRemove.isPending}>批量删除</Button></Popconfirm></Space>}
    >
      <DataTableCard
        title="收藏列表"
        data={list.data}
        loading={list.isLoading}
        error={list.error}
        onRetry={() => list.refetch()}
        emptyTitle="还没有收藏，在题目或知识页面点收藏即可加入"
        rowSelection={{ selectedRowKeys: selected, onChange: setSelected }}
        columns={[
          { title: '标题', render: (_, row) => pickText(row, ['title', 'targetTitle', 'name'], '收藏项') },
          { title: '类型', render: (_, row) => <Tag color="blue">{targetTypeLabel(pickText(row, ['targetType', 'type']))}</Tag> },
          { title: '分组', render: (_, row) => <Select size="small" style={{ width: 130 }} allowClear placeholder="未分组" value={row.tagId ? Number(row.tagId) : undefined} options={tagOptions} onChange={(tagId) => changeTag.mutate({ favoriteId: Number(row.id), tagId: tagId ?? null })} /> },
          { title: '时间', render: (_, row) => formatDateTime(row.createTime || row.createdAt) },
          { title: '操作', render: (_, row) => <Popconfirm title="取消收藏？" okText="取消收藏" okButtonProps={{ danger: true }} cancelText="返回" onConfirm={() => remove.mutate(Number(row.id))}><Button danger size="small" loading={remove.isPending}>删除</Button></Popconfirm> }
        ]}
      />
      <Modal open={favoriteOpen} title="新增收藏" footer={null} onCancel={() => setFavoriteOpen(false)}>
        <Form layout="vertical" onFinish={(values) => add.mutate(values)}>
          <Form.Item label="目标类型" name="targetType" rules={[{ required: true }]}><Select options={[{ value: 'question', label: '题目' }, { value: 'knowledge', label: '知识' }, { value: 'application', label: '投递' }, { value: 'review', label: '复盘' }]} /></Form.Item>
          <Form.Item label="目标 ID" name="targetId" rules={[{ required: true }]} extra="一般无需手填——在题目/知识详情页点收藏会自动带入。此处用于手动补录。"><Input type="number" /></Form.Item>
          <Form.Item label="分组" name="tagId"><Select allowClear options={tagOptions} /></Form.Item>
          <Button type="primary" htmlType="submit" loading={add.isPending}>保存</Button>
        </Form>
      </Modal>
      <Modal open={tagOpen} title="管理分组" footer={null} onCancel={() => setTagOpen(false)}>
        <Form layout="inline" onFinish={(values) => createTag.mutate(values)} style={{ marginBottom: 16 }}>
          <Form.Item label="新分组" name="name" rules={[{ required: true, message: '请输入分组名称' }]}><Input placeholder="分组名称" /></Form.Item>
          <Button type="primary" htmlType="submit" loading={createTag.isPending}>新建</Button>
        </Form>
        <List
          dataSource={tagRows}
          locale={{ emptyText: '暂无分组' }}
          renderItem={(tag) => (
            <List.Item actions={[
              <Popconfirm key="del" title="删除该分组？" description="分组下的收藏不会被删除，仅解除分组。" okText="删除" okButtonProps={{ danger: true }} cancelText="取消" onConfirm={() => removeTag.mutate(Number(tag.id))}>
                <Button size="small" danger loading={removeTag.isPending}>删除</Button>
              </Popconfirm>
            ]}>
              {pickText(tag, ['name'], '分组')}
            </List.Item>
          )}
        />
      </Modal>
    </ModulePage>
  )
}
