import { useState } from 'react'
import { Button, Input, Select, Space, Tag } from 'antd'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { fetchCommunityQuestionsApi } from '@/api/modules/community'
import { DataTableCard, formatDateTime, getTotal, normalizeRecords, pickText, StatusTag } from '@/modules/common'
import { ModulePage } from '@/modules/common'

export function CommunityPage() {
  const [filters, setFilters] = useState({ keyword: '', status: undefined as string | undefined, sort: 'latest' })
  const query = useQuery({ queryKey: ['community', 'questions', filters], queryFn: () => fetchCommunityQuestionsApi({ pageNum: 1, pageSize: 20, ...filters }).then((response) => response.data) })
  const rows = normalizeRecords(query.data)
  return (
    <ModulePage
      title="社区"
      description="提问、回答、采纳，与同行交流求职经验。"
      metrics={[
        { label: '问题数', value: getTotal(query.data, rows.length), hint: '全部匹配' },
        { label: '待回答', value: rows.filter((row) => !row.acceptedAnswerId).length, hint: '当前页' },
        { label: '已解决', value: rows.filter((row) => row.acceptedAnswerId).length, hint: '当前页' },
        { label: '状态', value: query.isFetching ? '刷新中' : '已同步', hint: '社区列表' }
      ]}
      actions={<Space wrap>
        <Input.Search allowClear placeholder="搜索问题" style={{ width: 200 }} onSearch={(keyword) => setFilters((current) => ({ ...current, keyword }))} />
        <Select style={{ width: 120 }} value={filters.status ?? 'all'} options={[{ value: 'all', label: '全部' }, { value: 'open', label: '待解决' }, { value: 'solved', label: '已解决' }]} onChange={(value) => setFilters((current) => ({ ...current, status: value === 'all' ? undefined : value }))} />
        <Select style={{ width: 120 }} value={filters.sort} options={[{ value: 'latest', label: '最新' }, { value: 'hot', label: '热门' }]} onChange={(sort) => setFilters((current) => ({ ...current, sort }))} />
        <Link to="/community/submit"><Button type="primary">发起提问</Button></Link>
        <Link to="/community/leaderboard"><Button>排行榜</Button></Link>
      </Space>}
    >
      <DataTableCard
        title="社区问题"
        data={query.data}
        loading={query.isLoading}
        error={query.error}
        onRetry={() => query.refetch()}
        emptyTitle="还没有相关问题，来发起第一个提问"
        columns={[
          { title: '标题', render: (_, row) => <Link to={`/community/question/${row.id}`}>{pickText(row, ['title', 'name'], '社区问题')}</Link> },
          { title: '分类', render: (_, row) => <Tag>{pickText(row, ['categoryName', 'tag'])}</Tag> },
          { title: '状态', render: (_, row) => <StatusTag value={row.acceptedAnswerId ? 'SOLVED' : pickText(row, ['status'], 'OPEN')} /> },
          { title: '回答', render: (_, row) => pickText(row, ['answerCount', 'answers'], '0') },
          { title: '最后活跃', render: (_, row) => formatDateTime(row.lastActiveTime || row.updateTime || row.createTime) }
        ]}
      />
    </ModulePage>
  )
}
