import { useQuery } from '@tanstack/react-query'
import { fetchLeaderboardApi } from '@/api/modules/community'
import { DataTableCard, pickText } from '@/modules/common'
import { ModulePage } from '@/modules/common'

export function LeaderboardPage() {
  const query = useQuery({ queryKey: ['community', 'leaderboard'], queryFn: () => fetchLeaderboardApi().then((response) => response.data) })
  return (
    <ModulePage title="排行榜" description="展示社区贡献度与活跃度。">
      <DataTableCard
        title="贡献榜"
        data={query.data}
        loading={query.isLoading}
        error={query.error}
        onRetry={() => query.refetch()}
        columns={[
          { title: '用户', render: (_, row) => pickText(row, ['nickname', 'username', 'name'], '用户') },
          { title: '贡献', render: (_, row) => pickText(row, ['score', 'points', 'answerCount'], '0') },
          { title: '采纳', render: (_, row) => pickText(row, ['acceptedCount'], '0') }
        ]}
      />
    </ModulePage>
  )
}
