<template>
  <div class="favorites-page space-y-5">
    <!-- Stats Bar -->
    <section class="shell-section-card p-4 sm:p-5">
      <div class="favorites-header">
        <div class="min-w-0">
          <h1 class="text-xl font-semibold tracking-[-0.03em] text-ink sm:text-2xl">我的收藏</h1>
          <p class="mt-2 text-sm text-secondary">收藏的资料、题目和回答都集中在这里。</p>
        </div>
      </div>
      <div class="favorites-stats mt-4">
        <div class="favorites-stat">
          <span>全部收藏</span>
          <strong>{{ stats.total }}</strong>
        </div>
        <div class="favorites-stat">
          <span>知识资料</span>
          <strong>{{ stats.knowledgeCount }}</strong>
        </div>
        <div class="favorites-stat">
          <span>题目</span>
          <strong>{{ stats.questionCount }}</strong>
        </div>
        <div class="favorites-stat">
          <span>社区回答</span>
          <strong>{{ stats.communityCount }}</strong>
        </div>
        <div class="favorites-stat">
          <span>今日新增</span>
          <strong>{{ stats.todayCount }}</strong>
        </div>
      </div>
    </section>

    <div class="favorites-workspace">
      <!-- Main List -->
      <section class="shell-section-card overflow-hidden">
        <!-- Batch toolbar -->
        <div
          v-if="selectedIds.size > 0"
          class="favorites-batch-bar border-b border-slate-200/70 px-5 py-3 dark:border-slate-700/70"
        >
          <span class="text-sm text-secondary">已选 {{ selectedIds.size }} 项</span>
          <el-popconfirm
            title="确认批量取消收藏？"
            confirm-button-text="确认"
            cancel-button-text="取消"
            @confirm="handleBatchRemove"
          >
            <template #reference>
              <button type="button" class="text-sm font-semibold text-coral">批量取消收藏</button>
            </template>
          </el-popconfirm>
          <button type="button" class="text-sm text-secondary" @click="selectedIds.clear()">取消选择</button>
        </div>

        <!-- List -->
        <div v-if="favorites.length === 0 && !loading" class="p-5">
          <EmptyState
            icon="inbox"
            :title="EMPTY_STATE_COPY.favorites.title"
            :description="EMPTY_STATE_COPY.favorites.description"
          />
        </div>

        <div v-else class="favorites-list">
          <article v-for="fav in favorites" :key="fav.id" class="favorites-row">
            <div class="flex items-start gap-3">
              <input
                type="checkbox"
                :checked="selectedIds.has(fav.id)"
                class="favorites-checkbox mt-1"
                @change="toggleSelect(fav.id)"
              />
              <div class="min-w-0 flex-1">
                <div class="flex flex-wrap items-center gap-2">
                  <span class="hard-chip">{{ targetTypeLabel(fav.targetType) }}</span>
                  <span v-if="fav.tagName" class="detail-pill">{{ fav.tagName }}</span>
                  <span v-if="fav.categoryName" class="detail-pill">{{ fav.categoryName }}</span>
                </div>
                <h4 class="mt-2 text-base font-semibold leading-7 text-ink">{{ fav.title }}</h4>
                <p class="mt-1 text-sm leading-6 text-secondary line-clamp-2">
                  {{ fav.summary || '这条收藏还没有补充摘要，先打开原内容继续查看重点。' }}
                </p>
                <div class="mt-3 flex flex-wrap items-center gap-3">
                  <span class="text-xs text-tertiary">{{ formatTime(fav.createTime) }}</span>
                  <RouterLink :to="getTargetLink(fav)" class="hard-button-secondary text-xs !min-h-[30px] !px-3">
                    去查看
                  </RouterLink>
                  <el-popconfirm
                    title="确认取消收藏？"
                    confirm-button-text="确认"
                    cancel-button-text="取消"
                    @confirm="handleRemove(fav.id)"
                  >
                    <template #reference>
                      <button type="button" class="text-xs font-semibold text-coral">取消收藏</button>
                    </template>
                  </el-popconfirm>
                </div>
              </div>
            </div>
          </article>
        </div>

        <!-- Pagination -->
        <div v-if="totalPages > 1" class="border-t border-slate-200/70 px-5 py-5 dark:border-slate-700/70">
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="total"
            layout="prev, pager, next"
            @current-change="handlePageChange"
          />
        </div>
      </section>

      <!-- Sidebar -->
      <aside class="favorites-side space-y-4">
        <!-- Search & Filter -->
        <section class="shell-section-card p-5 sm:p-6">
          <h3 class="text-lg font-semibold tracking-[-0.03em] text-ink">筛选</h3>
          <div class="mt-4 grid gap-3">
            <el-input v-model="keyword" clearable placeholder="搜索标题" size="large" @keyup.enter="handleSearch" />
            <el-select v-model="filterType" clearable placeholder="类型" size="large" @change="handleSearch">
              <el-option label="全部" value="" />
              <el-option label="知识资料" value="knowledge" />
              <el-option label="题目" value="question" />
              <el-option label="社区" value="community" />
            </el-select>
            <el-select v-model="filterTagId" clearable placeholder="分组" size="large" @change="handleSearch">
              <el-option label="全部分组" :value="null" />
              <el-option v-for="tag in tags" :key="tag.id" :label="`${tag.name}（${tag.count}）`" :value="tag.id" />
            </el-select>
          </div>
          <div class="mt-4 grid grid-cols-2 gap-3">
            <el-button
              :loading="loading"
              type="primary"
              size="large"
              class="action-button !min-h-11"
              @click="handleSearch"
            >
              搜索
            </el-button>
            <el-button size="large" class="hard-button-secondary !min-h-11" @click="handleReset"> 重置 </el-button>
          </div>
        </section>

        <!-- Tag Management -->
        <section class="shell-section-card p-5 sm:p-6">
          <h3 class="text-lg font-semibold tracking-[-0.03em] text-ink">收藏分组</h3>
          <p class="mt-2 text-sm leading-6 text-secondary">给收藏分组，方便考前集中复习。</p>
          <div class="mt-4 flex gap-2">
            <el-input
              v-model="newTagName"
              clearable
              placeholder="新分组名称"
              size="large"
              @keyup.enter="handleCreateTag"
            />
            <el-button type="primary" size="large" :disabled="!newTagName.trim()" @click="handleCreateTag">
              新增
            </el-button>
          </div>
          <div v-if="tags.length" class="mt-4 space-y-2">
            <div
              v-for="tag in tags"
              :key="tag.id"
              class="flex items-center justify-between gap-2 rounded-xl px-3 py-2 transition hover:bg-[var(--interactive-hover)]"
            >
              <span class="text-sm text-ink"
                >{{ tag.name }} <span class="text-xs text-tertiary">（{{ tag.count }}）</span></span
              >
              <button type="button" class="text-xs font-semibold text-coral" @click="handleDeleteTag(tag.id)">
                删除
              </button>
            </div>
          </div>
          <div v-else class="mt-4 text-sm text-tertiary">{{ EMPTY_STATE_COPY.favoriteGroups.description }}</div>
        </section>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import EmptyState from '@/components/EmptyState.vue'
import { EMPTY_STATE_COPY, ERROR_COPY } from '@/constants/productCopy'
import {
  batchRemoveFavoriteApi,
  createFavoriteTagApi,
  deleteFavoriteTagApi,
  fetchFavoriteListApi,
  fetchFavoriteStatsApi,
  fetchFavoriteTagsApi,
  removeFavoriteApi
} from '@/api/favorites'
import type { FavoriteItem, FavoriteStats, FavoriteTagItem } from '@/types/api'

const loading = ref(false)
const favorites = ref<FavoriteItem[]>([])
const stats = reactive<FavoriteStats>({
  total: 0,
  knowledgeCount: 0,
  questionCount: 0,
  communityCount: 0,
  todayCount: 0
})
const tags = ref<FavoriteTagItem[]>([])
const selectedIds = ref(new Set<number>())

const keyword = ref('')
const filterType = ref('')
const filterTagId = ref<number | null>(null)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const totalPages = ref(0)

const newTagName = ref('')

const loadStats = async () => {
  try {
    const { data } = await fetchFavoriteStatsApi()
    Object.assign(stats, data)
  } catch {
    // silent
  }
}

const loadTags = async () => {
  try {
    const { data } = await fetchFavoriteTagsApi()
    tags.value = data
  } catch {
    tags.value = []
  }
}

const loadFavorites = async () => {
  loading.value = true
  try {
    const { data } = await fetchFavoriteListApi({
      targetType: filterType.value || undefined,
      tagId: filterTagId.value ?? undefined,
      keyword: keyword.value || undefined,
      pageNum: currentPage.value,
      pageSize: pageSize.value
    })
    favorites.value = data.records
    total.value = data.total
    totalPages.value = data.totalPages
  } catch {
    ElMessage.error(ERROR_COPY.favoritesLoadFailed)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  selectedIds.value.clear()
  void loadFavorites()
}

const handleReset = () => {
  keyword.value = ''
  filterType.value = ''
  filterTagId.value = null
  currentPage.value = 1
  selectedIds.value.clear()
  void loadFavorites()
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  selectedIds.value.clear()
  void loadFavorites()
}

const handleRemove = async (id: number) => {
  try {
    await removeFavoriteApi(id)
    ElMessage.success('已取消收藏')
    await Promise.all([loadFavorites(), loadStats()])
  } catch {
    ElMessage.error(ERROR_COPY.favoriteRemoveFailed)
  }
}

const handleBatchRemove = async () => {
  if (selectedIds.value.size === 0) return
  try {
    await batchRemoveFavoriteApi([...selectedIds.value])
    ElMessage.success('已批量取消收藏')
    selectedIds.value.clear()
    await Promise.all([loadFavorites(), loadStats()])
  } catch {
    ElMessage.error(ERROR_COPY.favoriteBatchRemoveFailed)
  }
}

const toggleSelect = (id: number) => {
  if (selectedIds.value.has(id)) {
    selectedIds.value.delete(id)
  } else {
    selectedIds.value.add(id)
  }
}

const handleCreateTag = async () => {
  const name = newTagName.value.trim()
  if (!name) return
  try {
    await createFavoriteTagApi({ name })
    newTagName.value = ''
    ElMessage.success('分组已创建')
    await loadTags()
  } catch {
    ElMessage.error(ERROR_COPY.favoriteTagCreateFailed)
  }
}

const handleDeleteTag = async (tagId: number) => {
  try {
    await deleteFavoriteTagApi(tagId)
    ElMessage.success('分组已删除')
    await loadTags()
  } catch {
    ElMessage.error(ERROR_COPY.favoriteTagDeleteFailed)
  }
}

const targetTypeLabel = (type: string) => {
  if (type === 'knowledge') return '资料'
  if (type === 'question') return '题目'
  if (type === 'community') return '社区'
  return type
}

const getTargetLink = (fav: FavoriteItem) => {
  if (fav.targetType === 'knowledge') return '/knowledge'
  if (fav.targetType === 'question') return '/question'
  if (fav.targetType === 'community') return `/community/question/${fav.targetId}`
  return '/dashboard'
}

const formatTime = (value?: string) => {
  if (!value) return '刚刚'
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

onMounted(async () => {
  await Promise.all([loadStats(), loadTags(), loadFavorites()])
})
</script>

<style scoped>
.favorites-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px 16px;
}

.favorites-workspace {
  display: grid;
  gap: 18px;
}

.favorites-stats {
  display: grid;
  gap: 10px;
}

.favorites-stat {
  display: grid;
  gap: 4px;
  min-width: 108px;
  padding: 9px 12px;
  border-radius: 16px;
  background: var(--panel-muted);
}

.favorites-stat span {
  color: var(--bc-ink-secondary);
  font-size: 12px;
  line-height: 1.4;
}

.favorites-stat strong {
  color: var(--bc-ink);
  font-size: 1rem;
  font-weight: 700;
  letter-spacing: -0.03em;
}

.favorites-side {
  min-width: 0;
}

.favorites-batch-bar {
  display: flex;
  align-items: center;
  gap: 14px;
}

.favorites-list {
  display: flex;
  flex-direction: column;
}

.favorites-row {
  padding: 18px 20px;
}

.favorites-row + .favorites-row {
  border-top: 1px solid rgba(148, 163, 184, 0.16);
}

.favorites-checkbox {
  width: 18px;
  height: 18px;
  accent-color: var(--bc-accent);
  cursor: pointer;
  flex-shrink: 0;
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

@media (max-width: 767px) {
  .favorites-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (min-width: 768px) {
  .favorites-stats {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (min-width: 1200px) {
  .favorites-workspace {
    grid-template-columns: minmax(0, 1.45fr) 320px;
    align-items: start;
  }

  .favorites-stats {
    grid-template-columns: repeat(5, minmax(0, 1fr));
  }

  .favorites-side {
    position: sticky;
    top: 88px;
  }
}
</style>
