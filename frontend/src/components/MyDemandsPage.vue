<template>
  <div class="my-demand-page">
    <div class="page-container">
      <!-- 页面头部 -->
      <div class="page-header">
        <div>
          <h1 class="page-title">我的需求</h1>
          <p class="page-subtitle">查看和管理您发布的所有需求</p>
        </div>
        <button class="create-btn" @click="createDemand">
          <span>+</span> 发布新需求
        </button>
      </div>

      <!-- 搜索和筛选栏 -->
      <div class="filter-bar">
        <div class="filter-group">
          <label class="filter-label">状态</label>
          <select v-model="filters.status" class="filter-select" @change="handleFilterChange">
            <option value="">全部状态</option>
            <option value="PENDING">待接取</option>
            <option value="ACCEPTED">已接取</option>
            <option value="REJECTED">已拒绝</option>
            <option value="COMPLETED">已完成</option>
            <option value="CANCELLED">已取消</option>
            <option value="EXPIRED">已过期</option>
          </select>
        </div>
        <div class="filter-group">
          <label class="filter-label">分类</label>
          <select v-model="filters.category" class="filter-select" @change="handleFilterChange">
            <option value="">全部分类</option>
            <option value="快递代取">快递代取</option>
            <option value="学习辅导">学习辅导</option>
            <option value="二手交易">二手交易</option>
            <option value="活动组队">活动组队</option>
            <option value="其他">其他</option>
          </select>
        </div>
        <div class="filter-group search-group">
          <label class="filter-label">搜索</label>
          <div class="search-box">
            <input 
              type="text" 
              v-model="searchKeyword" 
              placeholder="搜索需求标题..."
              @keyup.enter="handleSearch"
            />
            <button class="search-btn" @click="handleSearch">🔍</button>
          </div>
        </div>
        <button class="reset-btn" @click="resetFilters">重置</button>
      </div>

      <!-- 统计 -->
      <div class="stats-bar">共 {{ totalElements }} 条需求</div>

      <!-- 加载中 -->
      <div v-if="loading" class="loading-state">
        <div class="loading-spinner"></div>
        <p>加载中...</p>
      </div>

      <!-- 空状态 -->
      <div v-else-if="demands.length === 0" class="empty-state">
        <p>{{ searchKeyword || filters.status || filters.category ? '暂无符合条件的需求' : '暂无发布的需求' }}</p>
        <button v-if="!searchKeyword && !filters.status && !filters.category" class="create-empty-btn" @click="createDemand">
          立即发布
        </button>
      </div>

      <!-- 需求列表 -->
      <div v-else class="demand-list">
        <div 
          v-for="demand in demands" 
          :key="demand.id" 
          class="demand-card"
          @click="viewDetail(demand.id)"
        >
          <div v-if="firstImageUrl(demand.pictureUrls)" class="demand-image" @click.stop="viewDetail(demand.id)">
            <img :src="getFullImageUrl(firstImageUrl(demand.pictureUrls))" :alt="demand.title" />
          </div>

          <div class="demand-info">
            <div class="demand-header">
              <div class="demand-title-row">
                <h3 class="demand-title">{{ demand.title }}</h3>
                <span class="demand-status" :class="getStatusClass(demand.status)">
                  {{ getStatusText(demand.status) }}
                </span>
              </div>
              <span class="demand-date">{{ formatDate(demand.createdAt) }}</span>
            </div>
            <p class="demand-desc">{{ truncate(demand.description) }}</p>
            <div class="demand-meta">
              <span class="meta-item">地点 {{ demand.location || '地点不限' }}</span>
              <span class="meta-item">酬金 {{ formatReward(demand.reward) }}</span>
              <span class="meta-item">分类 {{ demand.category || '未分类' }}</span>
            </div>
          </div>

          <div class="demand-actions" @click.stop>
            <button 
              v-if="demand.status === 'PENDING'" 
              class="action-btn edit-btn" 
              @click="editDemand(demand.id)"
            >
              编辑
            </button>
            <button 
              v-if="demand.status === 'PENDING'" 
              class="action-btn cancel-btn" 
              @click="cancelDemand(demand.id)"
            >
              取消
            </button>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <PaginationComponent
        v-if="totalPages > 1"
        :current-page="currentPage"
        :total-pages="totalPages"
        @page-change="changePage"
      />

      <AlertBox 
        v-model:visible="showAlert" 
        :title="alertConfig.title" 
        :content="alertConfig.content"
        :html-content="alertConfig.htmlContent" 
        :confirm-text="alertConfig.confirmText"
        @confirm="handleAlertConfirm" 
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import AlertBox from './SmallComponents/AlertBox.vue'
import PaginationComponent from './SmallComponents/PaginationComponent.vue'

const authStore = useAuthStore()
const router = useRouter()

const demands = ref([])
const loading = ref(false)
const totalElements = ref(0)
const totalPages = ref(0)
const currentPage = ref(0)
const pageSize = 10

const searchKeyword = ref('')
const filters = ref({ status: '', category: '' })

const showAlert = ref(false)
const alertConfig = ref({ title: '系统通知', content: '', htmlContent: '', confirmText: '确定' })

const showNotification = (title, content) => {
  alertConfig.value = { title, content, htmlContent: '', confirmText: '确定' }
  showAlert.value = true
}

const getFullImageUrl = (url) => `http://localhost:8080${url}`
const firstImageUrl = (urls) => {
  if (!urls) return null
  const list = urls.split(';').filter(u => u.trim())
  return list.length > 0 ? list[0] : null
}
const truncate = (text, len = 80) => !text ? '暂无描述' : (text.length > len ? text.slice(0, len) + '...' : text)
const formatReward = (reward) => (!reward && reward !== 0) ? '面议' : `¥${reward.toFixed(2)}`
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}/${d.getMonth() + 1}/${d.getDate()} ${d.getHours()}:${String(d.getMinutes()).padStart(2, '0')}`
}

const getStatusText = (status) => ({
  'PENDING': '待接取', 'ACCEPTED': '已接取', 'REJECTED': '已拒绝',
  'COMPLETED': '已完成', 'CANCELLED': '已取消', 'EXPIRED': '已过期'
}[status] || status)

const getStatusClass = (status) => {
  const map = {
    'PENDING': 'status-pending',
    'ACCEPTED': 'status-accepted',
    'REJECTED': 'status-rejected',
    'COMPLETED': 'status-completed',
    'CANCELLED': 'status-cancelled',
    'EXPIRED': 'status-expired'
  }
  return map[status] || ''
}

const fetchDemands = async () => {
  if (!authStore.isLoggedIn) { 
    router.push('/login')
    return 
  }
  loading.value = true
  try {
    let url = `http://localhost:8080/demands/search?publisherId=${authStore.user?.id}`
    if (searchKeyword.value) url += `&keyword=${encodeURIComponent(searchKeyword.value)}`
    if (filters.value.status) url += `&status=${filters.value.status}`
    if (filters.value.category) url += `&category=${encodeURIComponent(filters.value.category)}`
    url += `&page=${currentPage.value}&size=${pageSize}`
    
    const res = await fetch(url, { 
      headers: { 'Authorization': `Bearer ${authStore.token}` } 
    })
    const result = await res.json()
    
    if (result.code === 200 && result.data) {
      demands.value = result.data.content || []
      totalElements.value = result.data.totalElements
      totalPages.value = result.data.totalPages
    } else {
      throw new Error(result.message || '获取失败')
    }
  } catch (error) {
    console.error('获取失败:', error)
    showNotification('加载失败', error.message)
  } finally {
    loading.value = false
  }
}

const changePage = (page) => { 
  if (page >= 0 && page < totalPages.value) { 
    currentPage.value = page
    fetchDemands() 
  } 
}

const handleSearch = () => { 
  currentPage.value = 0
  fetchDemands() 
}

const handleFilterChange = () => { 
  currentPage.value = 0
  fetchDemands() 
}

const resetFilters = () => { 
  searchKeyword.value = ''
  filters.value = { status: '', category: '' }
  currentPage.value = 0
  fetchDemands() 
}

const viewDetail = (id) => router.push(`/demand/${id}`)
const editDemand = (id) => router.push(`/edit/demand/${id}`)
const createDemand = () => router.push('/create/demand')

const cancelDemand = async (id) => {
  if (!confirm('确定取消这个需求吗？')) return
  try {
    const res = await fetch(`http://localhost:8080/demands/${id}/status?status=CANCELLED`, {
      method: 'PATCH', 
      headers: { 'Authorization': `Bearer ${authStore.token}` }
    })
    const result = await res.json()
    if (result.code === 200) { 
      showNotification('操作成功', '需求已取消')
      fetchDemands() 
    } else {
      throw new Error(result.message)
    }
  } catch (error) { 
    showNotification('操作失败', error.message) 
  }
}

const handleAlertConfirm = () => { 
  if (router.currentRoute.value.path === '/my/demands') fetchDemands() 
}

onMounted(fetchDemands)
</script>

<style scoped>
.my-demand-page {
  min-height: 100vh;
  background: #f0f2f6;
  padding: 32px 24px;
}

.page-container {
  max-width: 1200px;
  margin: 0 auto;
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 28px;
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 6px 0;
  letter-spacing: -0.3px;
}

.page-subtitle {
  font-size: 14px;
  color: #6c757d;
  margin: 0;
}

.create-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  background: #62055f;
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.create-btn:hover {
  background: #7a0e76;
  transform: translateY(-1px);
}

/* 筛选栏 */
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: flex-end;
  background: #ffffff;
  padding: 20px 24px;
  border-radius: 16px;
  margin-bottom: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.filter-label {
  font-size: 12px;
  font-weight: 500;
  color: #6c757d;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.filter-select {
  padding: 8px 32px 8px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  font-size: 14px;
  background: #ffffff;
  color: #000000;
  cursor: pointer;
  outline: none;
  transition: all 0.2s;
}

.filter-select:focus {
  border-color: #62055f;
  box-shadow: 0 0 0 3px rgba(98, 5, 95, 0.1);
}

.search-group {
  min-width: 240px;
}

.search-box {
  display: flex;
  height: 38px;
}

.search-box input {
  flex: 1;
  padding: 0 12px;
  border: 1px solid #e2e8f0;
  border-right: none;
  border-radius: 10px 0 0 10px;
  font-size: 14px;
  outline: none;
  background: #ffffff;
  color: #000000;
}

.search-box input:focus {
  border-color: #62055f;
}

.search-btn {
  padding: 0 14px;
  background: #62055f;
  color: white;
  border: none;
  border-radius: 0 10px 10px 0;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.2s;
}

.search-btn:hover {
  background: #7a0e76;
}

.reset-btn {
  padding: 8px 20px;
  background: #f1f3f5;
  border: none;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 500;
  color: #495057;
  cursor: pointer;
  transition: all 0.2s;
}

.reset-btn:hover {
  background: #e9ecef;
  color: #212529;
}

/* 统计栏 */
.stats-bar {
  padding: 12px 0;
  font-size: 13px;
  color: #6c757d;
}

/* 加载状态 */
.loading-state {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 16px;
  color: #94a3b8;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #e2e8f0;
  border-top-color: #62055f;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 12px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 16px;
  color: #94a3b8;
}

.create-empty-btn {
  margin-top: 20px;
  padding: 10px 28px;
  background: #62055f;
  color: white;
  border: none;
  border-radius: 24px;
  cursor: pointer;
  font-size: 14px;
}

.create-empty-btn:hover {
  background: #7a0e76;
}

/* 需求列表 */
.demand-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.demand-card {
  display: flex;
  gap: 16px;
  background: white;
  border-radius: 16px;
  padding: 20px;
  transition: all 0.2s;
  cursor: pointer;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.demand-card:hover {
  transform: translateX(2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.demand-image {
  width: 100px;
  height: 100px;
  border-radius: 12px;
  overflow: hidden;
  flex-shrink: 0;
  background: #f5f5f5;
}

.demand-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.demand-image-placeholder {
  width: 100px;
  height: 100px;
  border-radius: 12px;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 36px;
  color: #ccc;
}

.demand-info {
  flex: 1;
  min-width: 0;
}

.demand-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 10px;
  flex-wrap: wrap;
  gap: 8px;
}

.demand-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.demand-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0;
}

.demand-status {
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 20px;
  font-weight: 600;
}

.status-pending { background: #fff8e7; color: #d97706; }
.status-accepted { background: #e8f0fe; color: #1e6f9f; }
.status-rejected { background: #fee9e6; color: #d9381e; }
.status-completed { background: #e6f7ec; color: #2b8c4a; }
.status-cancelled { background: #f5f5f5; color: #9e9e9e; }
.status-expired { background: #f5f5f5; color: #9e9e9e; }

.demand-date {
  font-size: 12px;
  color: #94a3b8;
}

.demand-desc {
  font-size: 13px;
  color: #64748b;
  margin: 0 0 10px 0;
  line-height: 1.5;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.demand-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.meta-item {
  font-size: 12px;
  color: #94a3b8;
}

.demand-actions {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  flex-shrink: 0;
}

.action-btn {
  padding: 6px 16px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}



.detail-btn:hover {
  background: #e9ecef;
}

.edit-btn {
  background: #e8edff;
  color: #3b5bdb;
}

.edit-btn:hover {
  background: #e0e7ff;
}

.cancel-btn {
  background: #fee9e6;
  color: #d9381e;
}

.cancel-btn:hover {
  background: #fde5e0;
}

/* 响应式 */
@media (max-width: 768px) {
  .my-demand-page {
    padding: 20px 16px;
  }

  .page-header {
    flex-direction: column;
    gap: 16px;
  }

  .create-btn {
    align-self: flex-start;
  }

  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-group {
    min-width: auto;
  }

  .demand-card {
    flex-direction: column;
  }

  .demand-image,
  .demand-image-placeholder {
    width: 100%;
    height: 160px;
  }

  .demand-actions {
    justify-content: flex-end;
    margin-top: 12px;
  }

  .demand-meta {
    gap: 12px;
  }
}
</style>