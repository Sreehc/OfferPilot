<template>
  <div class="space-y-4">
    <div class="flex items-center gap-3">
      <el-input v-model="keyword" placeholder="搜索用户名/昵称" clearable size="large" class="max-w-xs" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-select v-model="roleFilter" placeholder="角色筛选" clearable size="large" class="w-32" @change="handleSearch">
        <el-option label="全部" value="" />
        <el-option label="普通用户" value="USER" />
        <el-option label="管理员" value="ADMIN" />
      </el-select>
      <el-button :loading="loading" type="primary" size="large" class="action-button" @click="handleSearch">搜索</el-button>
    </div>

    <el-table v-loading="loading" :data="users" stripe class="w-full" :header-cell-style="{ background: 'var(--el-bg-color-page)' }">
      <el-table-column label="用户" min-width="180">
        <template #default="{ row }">
          <div class="flex items-center gap-2">
            <el-avatar :size="28" :src="row.avatar">{{ (row.nickname || row.username || '?')[0] }}</el-avatar>
            <div>
              <div class="text-sm font-semibold text-ink">{{ row.nickname || row.username }}</div>
              <div class="text-xs text-slate-400">{{ row.username }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="角色" width="100">
        <template #default="{ row }">
          <el-tag :type="row.role === 'ADMIN' ? 'warning' : 'info'" size="small">{{ row.role === 'ADMIN' ? '管理员' : '用户' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '正常' : '封禁' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="注册时间" min-width="140">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="最后登录" min-width="140">
        <template #default="{ row }">{{ formatTime(row.lastLoginTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button v-if="row.status === 1" size="small" type="danger" plain @click="handleBan(row)">封禁</el-button>
          <el-button v-else size="small" type="success" plain @click="handleUnban(row)">解封</el-button>
          <el-button size="small" type="primary" plain @click="handleDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="totalPages > 1" class="flex justify-center pt-2">
      <el-pagination :current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="handlePageChange" />
    </div>

    <!-- Edit dialog -->
    <el-dialog v-model="editVisible" title="编辑用户" width="400px">
      <el-form label-position="top">
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="editForm.role" class="w-full">
            <el-option label="普通用户" value="USER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button :loading="editSaving" type="primary" class="action-button" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- Detail dialog -->
    <el-dialog v-model="detailVisible" title="用户详情" width="500px">
      <div v-if="detail" class="space-y-3">
        <div class="grid grid-cols-2 gap-3">
          <div><span class="text-xs text-slate-400">用户名</span><p class="font-semibold text-ink">{{ detail.username }}</p></div>
          <div><span class="text-xs text-slate-400">昵称</span><p class="font-semibold text-ink">{{ detail.nickname }}</p></div>
          <div><span class="text-xs text-slate-400">邮箱</span><p class="font-semibold text-ink">{{ detail.email || '-' }}</p></div>
          <div><span class="text-xs text-slate-400">注册时间</span><p class="font-semibold text-ink">{{ formatTime(detail.createTime) }}</p></div>
        </div>
        <div class="rule-divider"></div>
        <h4 class="text-sm font-semibold text-ink">学习统计</h4>
        <div class="grid grid-cols-3 gap-3">
          <div class="metric-card text-center p-3"><p class="metric-label">面试次数</p><p class="metric-value text-lg">{{ detail.interviewCount }}</p></div>
          <div class="metric-card text-center p-3"><p class="metric-label">错题数</p><p class="metric-value text-lg">{{ detail.wrongCount }}</p></div>
          <div class="metric-card text-center p-3"><p class="metric-label">复习次数</p><p class="metric-value text-lg">{{ detail.reviewCount }}</p></div>
          <div class="metric-card text-center p-3"><p class="metric-label">社区提问</p><p class="metric-value text-lg">{{ detail.communityQuestions }}</p></div>
          <div class="metric-card text-center p-3"><p class="metric-label">社区回答</p><p class="metric-value text-lg">{{ detail.communityAnswers }}</p></div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { banUserApi, fetchAdminUsersApi, fetchUserDetailApi, unbanUserApi, updateAdminUserApi } from '@/api/admin'
import type { AdminUserDetail } from '@/api/admin'
import type { UserInfo } from '@/types/api'

const users = ref<UserInfo[]>([])
const loading = ref(false)
const keyword = ref('')
const roleFilter = ref('')
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const totalPages = ref(0)

const editVisible = ref(false)
const editSaving = ref(false)
const editUserId = ref(0)
const editForm = reactive({ nickname: '', role: 'USER' })

const detailVisible = ref(false)
const detail = ref<AdminUserDetail | null>(null)

const loadUsers = async () => {
  loading.value = true
  try {
    const res = await fetchAdminUsersApi({ keyword: keyword.value || undefined, role: roleFilter.value || undefined, pageNum: pageNum.value, pageSize: pageSize.value })
    users.value = res.data.records
    total.value = res.data.total
    totalPages.value = res.data.totalPages
  } catch { ElMessage.error('用户列表加载失败') } finally { loading.value = false }
}

const handleSearch = () => { pageNum.value = 1; void loadUsers() }
const handlePageChange = (p: number) => { pageNum.value = p; void loadUsers() }

const handleEdit = (row: UserInfo) => {
  editUserId.value = row.id
  editForm.nickname = row.nickname || ''
  editForm.role = row.role || 'USER'
  editVisible.value = true
}

const saveEdit = async () => {
  editSaving.value = true
  try {
    await updateAdminUserApi(editUserId.value, { nickname: editForm.nickname, role: editForm.role })
    ElMessage.success('用户信息已更新')
    editVisible.value = false
    await loadUsers()
  } catch { ElMessage.error('更新失败') } finally { editSaving.value = false }
}

const handleBan = async (row: UserInfo) => {
  await ElMessageBox.confirm(`确认封禁用户「${row.username}」？该用户将被踢出所有设备。`, '封禁确认', { type: 'warning' })
  try { await banUserApi(row.id); ElMessage.success('已封禁'); await loadUsers() } catch { ElMessage.error('封禁失败') }
}

const handleUnban = async (row: UserInfo) => {
  try { await unbanUserApi(row.id); ElMessage.success('已解封'); await loadUsers() } catch { ElMessage.error('解封失败') }
}

const handleDetail = async (row: UserInfo) => {
  try {
    const res = await fetchUserDetailApi(row.id)
    detail.value = res.data
    detailVisible.value = true
  } catch { ElMessage.error('加载详情失败') }
}

const formatTime = (v?: string) => {
  if (!v) return '-'
  return new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(new Date(v))
}

onMounted(loadUsers)
</script>
