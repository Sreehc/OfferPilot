<template>
  <div class="space-y-4">
    <section class="shell-section-card p-5">
      <div class="flex items-center justify-between gap-3">
        <div>
          <p class="section-kicker">内容审核</p>
          <h3 class="mt-3 text-2xl font-semibold tracking-[-0.03em] text-ink">待审核 {{ total }} 条</h3>
        </div>
        <el-button :loading="loading" type="primary" size="large" class="action-button" @click="loadPending">刷新</el-button>
      </div>
    </section>

    <section class="shell-section-card p-5">
      <el-table v-loading="loading" :data="items" stripe class="w-full" :header-cell-style="{ background: 'var(--el-bg-color-page)' }">
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.type === 'question' ? 'primary' : 'success'" size="small">{{ row.type === 'question' ? '提问' : '回答' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="标题/内容" min-width="300">
          <template #default="{ row }">
            <div>
              <p class="text-sm font-semibold text-ink">{{ row.title || '(回答)' }}</p>
              <p class="mt-1 line-clamp-2 text-xs text-secondary">{{ row.content }}</p>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="作者" width="100">
          <template #default="{ row }">{{ row.username || '-' }}</template>
        </el-table-column>
        <el-table-column label="提交时间" min-width="140">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="success" :loading="actionId === `approve-${row.id}`" @click="handleApprove(row)">通过</el-button>
            <el-button size="small" type="danger" plain @click="openRejectDialog(row)">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <div v-if="totalPages > 1" class="flex justify-center pt-2">
      <el-pagination :current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="handlePageChange" />
    </div>

    <el-dialog v-model="rejectVisible" title="审核拒绝" width="400px">
      <el-input v-model="rejectReason" type="textarea" :rows="3" placeholder="拒绝原因（可选）" />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button :loading="rejectLoading" type="danger" @click="handleReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, ref } from 'vue'
import { approveContentApi, fetchPendingContentApi, rejectContentApi } from '@/api/admin'
import type { AdminContentReviewItem } from '@/api/admin'

const items = ref<AdminContentReviewItem[]>([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const totalPages = ref(0)
const actionId = ref('')

const rejectVisible = ref(false)
const rejectReason = ref('')
const rejectLoading = ref(false)
const rejectItem = ref<AdminContentReviewItem | null>(null)

const loadPending = async () => {
  loading.value = true
  try {
    const res = await fetchPendingContentApi({ pageNum: pageNum.value, pageSize: pageSize.value })
    items.value = res.data.records
    total.value = res.data.total
    totalPages.value = res.data.totalPages
  } catch { ElMessage.error('加载失败') } finally { loading.value = false }
}

const handlePageChange = (p: number) => { pageNum.value = p; void loadPending() }

const handleApprove = async (item: AdminContentReviewItem) => {
  actionId.value = `approve-${item.id}`
  try { await approveContentApi(item.id); ElMessage.success('已通过'); await loadPending() } catch { ElMessage.error('操作失败') } finally { actionId.value = '' }
}

const openRejectDialog = (item: AdminContentReviewItem) => {
  rejectItem.value = item
  rejectReason.value = ''
  rejectVisible.value = true
}

const handleReject = async () => {
  if (!rejectItem.value) return
  rejectLoading.value = true
  try {
    await rejectContentApi(rejectItem.value.id, rejectReason.value || undefined)
    ElMessage.success('已拒绝')
    rejectVisible.value = false
    await loadPending()
  } catch { ElMessage.error('操作失败') } finally { rejectLoading.value = false }
}

const formatTime = (v?: string) => {
  if (!v) return '-'
  return new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(new Date(v))
}

onMounted(loadPending)
</script>
