import { useQuery } from '@tanstack/react-query'
import { Tag, Typography } from 'antd'
import { fetchLeaderboardApi } from '@/api/modules/community'
import { useAuthStore } from '@/features/auth/authStore'
import { DataTableCard, pickText } from '@/modules/common'
import { ModulePage } from '@/modules/common'

export function LeaderboardPage() {
  const currentUser = useAuthStore((state) => state.user)
  const query = useQuery({ queryKey: ['community', 'leaderboard'], queryFn: () => fetchLeaderboardApi().then((response) => response.data) })
  const isMe = (row: Record<string, any>) => Boolean(currentUser && (Number(row.userId ?? row.id) === Number(currentUser.id) || pickText(row, ['username']) === currentUser.username))
  return (
    <ModulePage
      title="排行榜"
      description="按社区贡献度排名。积分规则：被采纳回答 +10，获得赞同 +2，发起优质提问 +1。"
    >
      <DataTableCard
        title="贡献榜"
        data={query.data}
        loading={query.isLoading}
        error={query.error}
        onRetry={() => query.refetch()}
        emptyTitle="暂无排行数据"
        columns={[
          { title: '名次', width: 80, render: (_, _row, index) => <Tag color={index < 3 ? 'gold' : 'default'}>{index + 1}</Tag> },
          { title: '用户', render: (_, row) => <span>{pickText(row, ['nickname', 'username', 'name'], '用户')}{isMe(row) && <Tag color="blue" style={{ marginInlineStart: 8 }}>我</Tag>}</span> },
          { title: '积分', render: (_, row) => pickText(row, ['score', 'points'], '0') },
          { title: '被采纳', render: (_, row) => pickText(row, ['acceptedCount'], '0') },
          { title: '回答数', render: (_, row) => pickText(row, ['answerCount', 'answers'], '0') }
        ]}
      />
      <Typography.Paragraph className="muted-text" style={{ marginTop: 12 }}>
        持续输出高质量回答即可提升排名，被提问者采纳的回答积分最高。
      </Typography.Paragraph>
    </ModulePage>
  )
}
