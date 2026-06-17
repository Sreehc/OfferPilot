import { Button, Space, Tag } from 'antd'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { fetchCommunityQuestionsApi } from '@/api/modules/community'
import { DataTableCard, normalizeRecords, pickText, StatusTag } from '@/modules/common'
import { ModulePage } from '@/modules/common'

export function CommunityPage() {
  const query = useQuery({ queryKey: ['community', 'questions'], queryFn: () => fetchCommunityQuestionsApi({ pageNum: 1, pageSize: 20 }).then((response) => response.data) })
  const rows = normalizeRecords(query.data)
  return (
    <ModulePage
      title="社区"
      description="问题、回答、排行榜和协作内容。"
      metrics={[
        { label: '问题数', value: rows.length, hint: '当前列表' },
        { label: '待回答', value: rows.filter((row) => !row.acceptedAnswerId).length, hint: '需要参与' },
        { label: '已解决', value: rows.filter((row) => row.acceptedAnswerId).length, hint: '已有采纳' },
        { label: '状态', value: query.isFetching ? '刷新中' : '已同步', hint: '社区列表' }
      ]}
      actions={<Space><Link to="/community/submit"><Button type="primary">发起提问</Button></Link><Link to="/community/leaderboard"><Button>排行榜</Button></Link></Space>}
    >
      <DataTableCard
        title="社区问题"
        data={query.data}
        loading={query.isLoading}
        error={query.error}
        onRetry={() => query.refetch()}
        columns={[
          { title: '标题', render: (_, row) => <Link to={`/community/question/${row.id}`}>{pickText(row, ['title', 'name'], '社区问题')}</Link> },
          { title: '分类', render: (_, row) => <Tag>{pickText(row, ['categoryName', 'tag'])}</Tag> },
          { title: '状态', render: (_, row) => <StatusTag value={row.acceptedAnswerId ? 'SOLVED' : pickText(row, ['status'], 'OPEN')} /> },
          { title: '回答', render: (_, row) => pickText(row, ['answerCount', 'answers'], '0') }
        ]}
      />
    </ModulePage>
  )
}
