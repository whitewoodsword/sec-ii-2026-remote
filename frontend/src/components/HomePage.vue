<template>
  <div class="home-page">
    <!-- 顶部固定导航栏 -->
    <header class="fixed-header">
      <div class="header-left">
        <h1 class="header-headline" @click="router.push('/')">校园互助服务平台</h1>
      </div>
      
      <!-- 搜索区域 -->
      <div class="header-search">
        <div class="search-box">
          <input 
            type="text" 
            v-model="searchKeyword" 
            placeholder="搜索需求、服务..."
            @keyup.enter="handleSearch"
          />
          <button v-if="searchKeyword" class="search-clear" @click="clearSearch">✕</button>
        </div>
      </div>

      <div class="header-right">
        <template v-if="!authStore.isLoggedIn">
          <button class="header-btn" @click="handleLogin">登录 / 注册</button>
        </template>
        <template v-else>
          <div class="user-info">{{ authStore.user?.name || '用户' }}</div>
          <div class="avatar-wrapper" @click.stop="toggleDropdown">
            <img 
              :src="displayAvatar" 
              alt="用户头像"
              class="avatar"
              :class="{ 'avatar-active': showDropdown }"
            />
            <!-- 未读消息红点 -->
            <span v-if="totalUnreadCount > 0" class="avatar-badge"></span>
          </div>
          <transition name="dropdown">
            <div v-if="showDropdown" class="dropdown-menu">
              <button class="dropdown-item" @click="goToProfile">个人主页</button>
              <div class="dropdown-divider"></div>
              <button class="dropdown-item message-item" @click="goToMessages">
                消息
                <span v-if="totalUnreadCount > 0" class="message-badge">{{ totalUnreadCount > 99 ? '99+' : totalUnreadCount }}</span>
              </button>
              <div class="dropdown-divider"></div>
              <template v-if="authStore.user?.isAdmin">
                <button class="dropdown-item" @click="handleAdmin">管理后台</button>
              </template>
              <button class="dropdown-item logout-item" @click="handleLogout">退出登录</button>
            </div>
          </transition>
        </template>
      </div>
    </header>

    <main class="main-content">
      <div class="content-container">
        <!-- 左侧：发布入口 + 分类筛选 (sticky) -->
        <aside class="left-sidebar">
          <!-- 发布快捷入口 -->
          <div class="publish-card">
            <p class="card-desc">快速发布您的需求，找到合适的帮手</p>
            <button class="publish-btn" @click="goToPublish">+ 发布新需求</button>
          </div>

          <!-- 分类筛选栏 -->
          <div class="category-card">
            <h3 class="card-title">需求分类</h3>
            <div class="category-list">
              <button 
                v-for="cat in categories" 
                :key="cat.value"
                class="category-btn"
                :class="{ active: selectedCategory === cat.value }"
                @click="selectCategory(cat.value)"
              >
                {{ cat.label }}
              </button>
            </div>
          </div>
        </aside>

        <!-- 中间：智能推荐需求流（正常滚动） -->
        <div class="demands-feed">
          <div class="feed-header">
            <h3 class="feed-title">推荐需求</h3>
            <div class="sort-options">
              <span 
                v-for="sort in sortOptions" 
                :key="sort.value"
                class="sort-btn"
                :class="{ active: currentSort === sort.value }"
                @click="changeSort(sort.value)"
              >
                {{ sort.label }}
              </span>
            </div>
          </div>

          <div v-if="loadingDemands" class="loading-state">
            <div class="loading-spinner"></div>
            <span>加载中...</span>
          </div>
          <div v-else-if="demands.length === 0" class="empty-state">
            <span>暂无需求，试试其他分类吧</span>
          </div>
          <div v-else class="demands-list">
            <div v-for="demand in demands" :key="demand.id" class="demand-card" @click="router.push(`/demand/${demand.id}`)">
              <!-- 左侧图片区域 -->
              <div class="demand-image" v-if="getFirstImageUrl(demand.pictureUrls)">
                <img :src="getFirstImageUrl(demand.pictureUrls)" :alt="demand.title" @error="handleImageError" />
              </div>
              
              <!-- 右侧内容区域 -->
              <div class="demand-content">
                <div class="demand-header">
                  <h4 class="demand-title">{{ demand.title }}</h4>
                  <span class="demand-category">{{ demand.category }}</span>
                </div>
                <p class="demand-desc">{{ demand.description || '暂无描述' }}</p>
                <div class="demand-meta">
                  <div class="meta-item">
                    <span class="meta-label">报酬</span>
                    <span class="meta-value reward">¥{{ demand.reward?.toFixed(2) || '面议' }}</span>
                  </div>
                  <div class="meta-item">
                    <span class="meta-label">地点</span>
                    <span class="meta-value">{{ demand.location || '未指定' }}</span>
                  </div>
                </div>
                <div class="demand-footer">
                  <span class="demand-time">{{ formatRelativeTime(demand.createdAt) }}</span>
                  <span v-if="demand.isOwn" class="my-demand-badge">我的需求</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 分页 -->
          <div v-if="totalPages > 1" class="pagination-btn">
            <button 
              class="page-btn" 
              :disabled="currentPage === 0" 
              @click="changePage(currentPage - 1)"
            >上一页</button>
            <span class="page-info">{{ currentPage + 1 }} / {{ totalPages }}</span>
            <button 
              class="page-btn" 
              :disabled="currentPage >= totalPages - 1" 
              @click="changePage(currentPage + 1)"
            >下一页</button>
          </div>
        </div>

        <!-- 右侧：订单看板 + 信用分 (sticky, 订单卡片最大高度滚动) -->
        <aside class="right-sidebar">
          <!-- 进行中订单卡片 - 固定最大高度，滚动条美化 -->
          <div class="orders-card">
            <h3 class="card-title">待处理订单</h3>
            <div v-if="loadingOrders" class="loading-mini">
              <div class="loading-spinner small"></div>
            </div>
            <div v-else-if="activeOrders.length === 0" class="empty-orders">
              <span>暂无待处理的订单</span>
            </div>
            <div v-else class="orders-list">
              <div v-for="order in activeOrders" :key="order.id" class="order-item">
                <div class="order-info" @click="goToOrderDetail(order.id)">
                  <div class="order-title">订单 #{{ order.id }}</div>
                  <div class="order-status" :class="getStatusClass(order.status)">
                    {{ getStatusText(order.status) }}
                  </div>
                </div>
                <div class="order-demand-info" v-if="order.demandTitle">
                  {{ order.demandTitle }}
                </div>
              </div>
            </div>
          </div>

          <!-- 个人信用分看板 -->
          <div class="credit-card">
            <h3 class="card-title">我的信用</h3>
            <div class="credit-score-large">
              <span class="score-number">{{ userCredit.averageScore !== null ? userCredit.averageScore.toFixed(1) : '暂无' }}</span>
              <span class="score-label">信用分</span>
            </div>
            <div class="credit-stats">
              <div class="stat-row">
                <span>评价次数</span>
                <strong>{{ userCredit.scoreNum || 0 }}</strong>
              </div>
              <div class="stat-row">
                <span>完成订单</span>
                <strong>{{ completedOrdersCount }}</strong>
              </div>
            </div>
          </div>
        </aside>
      </div>
    </main>

    <!-- 通知组件 -->
    <AlertBox
      v-model:visible="showAlert"
      :title="alertConfig.title"
      :content="alertConfig.content"
      :confirm-text="alertConfig.confirmText"
      @confirm="handleAlertConfirm"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useAuthStore } from '../stores/auth'
import { useRouter } from 'vue-router'
import AlertBox from './SmallComponents/AlertBox.vue'
import axios from 'axios'

const authStore = useAuthStore()
const router = useRouter()

// UI状态
const showDropdown = ref(false)
const searchKeyword = ref('')
const selectedCategory = ref('')
const currentSort = ref('time')   // 默认最新发布
const currentPage = ref(0)
const pageSize = ref(12)
const loadingDemands = ref(false)
const loadingOrders = ref(false)

// 数据
const demands = ref([])
const totalPages = ref(0)
const totalElements = ref(0)
const activeOrders = ref([])
const completedOrdersCount = ref(0)
const userCredit = ref({ averageScore: null, scoreNum: 0 })
const totalUnreadCount = ref(0)

// 轮询定时器
let pollingInterval = null

// 通知
const showAlert = ref(false)
const alertConfig = ref({ title: '', content: '', confirmText: '确定' })

// 分类选项
const categories = [
  { label: '全部', value: '' },
  { label: '快递代取', value: '快递代取' },
  { label: '学习辅导', value: '学习辅导' },
  { label: '二手交易', value: '二手交易' },
  { label: '活动组队', value: '活动组队' },
  { label: '其他', value: '其他' }
]

// 排序选项
const sortOptions = [
  { label: '最新发布', value: 'time' },
  { label: '信用优先', value: 'credit' },
  { label: '报酬最高', value: 'reward' }
]

// 头像
const displayAvatar = computed(() => {
  if (authStore.user?.avatarPath) {
    return `http://localhost:8080${authStore.user.avatarPath}`
  }
  return 'http://localhost:8080/api/files/default-avatar.png'
})

// 获取需求的第一张图片URL
const getFirstImageUrl = (pictureUrls) => {
  if (!pictureUrls) return null
  try {
    // pictureUrls 可能是 JSON 字符串数组或逗号分隔的字符串
    let urls = []
    if (typeof pictureUrls === 'string') {
      // 尝试解析 JSON
      if (pictureUrls.startsWith('[')) {
        urls = JSON.parse(pictureUrls)
      } else if (pictureUrls.includes(',')) {
        urls = pictureUrls.split(',')
      } else {
        urls = [pictureUrls]
      }
    } else if (Array.isArray(pictureUrls)) {
      urls = pictureUrls
    }
    
    if (urls.length > 0 && urls[0]) {
      // 如果是相对路径，加上 base URL
      const firstUrl = urls[0]
      if (firstUrl.startsWith('/')) {
        return `http://localhost:8080${firstUrl}`
      }
      if (firstUrl.startsWith('http')) {
        return firstUrl
      }
      return `http://localhost:8080/${firstUrl}`
    }
  } catch (e) {
    console.error('解析图片URL失败:', e)
  }
  return null
}

// 图片加载失败时的处理
const handleImageError = (event) => {
  event.target.src = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="100" height="100" viewBox="0 0 24 24" fill="none" stroke="%23999" stroke-width="1" stroke-linecap="round" stroke-linejoin="round"%3E%3Crect x="2" y="2" width="20" height="20" rx="2.18" ry="2.18"%3E%3C/rect%3E%3Ccircle cx="8.5" cy="8.5" r="1.5"%3E%3C/circle%3E%3Cpolyline points="21 15 16 10 5 21"%3E%3C/polyline%3E%3C/svg%3E'
}

// 显示通知
const showNotification = (title, content) => {
  alertConfig.value = { title, content, confirmText: '确定' }
  showAlert.value = true
}

// 获取未读消息总数
const fetchTotalUnreadCount = async () => {
  if (!authStore.isLoggedIn) return
  try {
    const response = await axios.get(`http://localhost:8080/messages/unread/count/${authStore.user.id}`)
    if (response.data.code === 200 && response.data.data) {
      totalUnreadCount.value = response.data.data.unreadCount || 0
    }
  } catch (error) {
    console.error('获取未读消息数失败:', error)
  }
}

// 获取需求列表（支持排序）
const fetchDemands = async () => {
  loadingDemands.value = true
  try {
    const params = new URLSearchParams({
      page: currentPage.value,
      size: pageSize.value,
      status: 'PENDING'
    })
    if (selectedCategory.value) params.append('category', selectedCategory.value)
    if (searchKeyword.value) params.append('keyword', searchKeyword.value)
    
    // 排序逻辑：根据currentSort构建sort参数
    if (currentSort.value === 'time') {
      params.append('sort', 'createdAt,desc')
    } else if (currentSort.value === 'credit') {
      params.append('sort', 'publisherCredit,desc')
    } else if (currentSort.value === 'reward') {
      params.append('sort', 'reward,desc')
    }

    const response = await axios.get(`http://localhost:8080/demands/search?${params}`)
    if (response.data.code === 200 && response.data.data) {
      const data = response.data.data
      demands.value = data.content.map(d => ({
        ...d,
        isOwn: authStore.user?.id === d.publisherId,
        publisherName: d.publisherName || '用户',
        publisherCredit: d.publisherCredit || null
      }))
      totalPages.value = data.totalPages || 0
      totalElements.value = data.totalElements || 0
    }
  } catch (error) {
    console.error('获取需求失败:', error)
    showNotification('加载失败', '无法获取需求列表')
  } finally {
    loadingDemands.value = false
  }
}

// 获取进行中订单（作为承接人）
const fetchActiveOrders = async () => {
  if (!authStore.isLoggedIn) return
  loadingOrders.value = true
  try {
    // 获取用户作为承接人的已接受订单（ACCEPTED状态）
    const response = await axios.get(`http://localhost:8080/orders/acceptor/${authStore.user.id}`, {
      params: { page: 0, size: 20, status: 'ACCEPTED' }
    })
    if (response.data.code === 200 && response.data.data) {
      activeOrders.value = response.data.data.content || []
    }
    // 获取已完成订单数量
    const completedRes = await axios.get(`http://localhost:8080/orders/user/${authStore.user.id}`, {
      params: { page: 0, size: 1, status: 'COMPLETED' }
    })
    if (completedRes.data.code === 200 && completedRes.data.data) {
      completedOrdersCount.value = completedRes.data.data.totalElements || 0
    }
  } catch (error) {
    console.error('获取订单失败:', error)
  } finally {
    loadingOrders.value = false
  }
}

// 获取用户信用信息
const fetchUserCredit = async () => {
  if (!authStore.isLoggedIn) return
  try {
    const response = await axios.get(`http://localhost:8080/users/${authStore.user.id}`)
    if(response.data.code === 200){
      if (response.data.data) {
        userCredit.value = {
          averageScore: response.data.data.averageScore,
          scoreNum: response.data.data.scoreNum
        }
      }
    }else{
      console.error('获取用户信息失败:', response.data.message)
    }
  } catch (error) {
    console.error('获取信用信息失败:', error)
  }
}

// 启动轮询
const startPolling = () => {
  if (pollingInterval) clearInterval(pollingInterval)
  pollingInterval = setInterval(() => {
    if (authStore.isLoggedIn) {
      fetchTotalUnreadCount()
      fetchActiveOrders()   // 订单状态可能变化，也轮询一下
    }
  }, 5000) // 每5秒轮询一次
}

// 停止轮询
const stopPolling = () => {
  if (pollingInterval) {
    clearInterval(pollingInterval)
    pollingInterval = null
  }
}

// 路由跳转
const goToPublish = () => router.push('/create/demand')
const goToProfile = () => { showDropdown.value = false; router.push('/my/profile') }
const goToMessages = () => { 
  showDropdown.value = false
  totalUnreadCount.value = 0
  router.push('/my/conversations') 
}
const goToOrderDetail = (orderId) => router.push(`/order/${orderId}`)

// 辅助方法
const formatRelativeTime = (isoString) => {
  if (!isoString) return ''
  const date = new Date(isoString)
  const now = new Date()
  const diff = now - date
  if (diff < 24 * 3600 * 1000) {
    return `今天 ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
  }
  if (diff < 48 * 3600 * 1000) {
    return `昨天 ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
  }
  return `${date.getMonth() + 1}/${date.getDate()}`
}

const getStatusClass = (status) => {
  const map = { ACCEPTED: 'accepted', IN_PROGRESS: 'progress', COMPLETED: 'completed' }
  return map[status] || ''
}

const getStatusText = (status) => {
  const map = { ACCEPTED: '待进行', IN_PROGRESS: '进行中', COMPLETED: '已完成' }
  return map[status] || status
}

const selectCategory = (cat) => { selectedCategory.value = cat; currentPage.value = 0; fetchDemands() }
const changeSort = (sort) => { currentSort.value = sort; currentPage.value = 0; fetchDemands() }
const changePage = (page) => { currentPage.value = page; fetchDemands() }
const handleSearch = () => { currentPage.value = 0; fetchDemands() }
const clearSearch = () => { searchKeyword.value = ''; fetchDemands() }

// 退出登录
const handleLogout = () => {
  const username = authStore.user?.name || '用户'
  showNotification('退出登录', `再见，${username}！`)
  authStore.logout()
  showDropdown.value = false
  totalUnreadCount.value = 0
  stopPolling()
  router.push('/')
}

const handleLogin = () => router.push('/login')
const handleAdmin = () => showNotification('功能开发中', '管理后台正在开发中')
const toggleDropdown = () => { if (authStore.isLoggedIn) showDropdown.value = !showDropdown.value }
const closeDropdown = () => { showDropdown.value = false }
const handleAlertConfirm = () => {}

// 监听点击关闭下拉菜单
onMounted(() => {
  document.addEventListener('click', closeDropdown)
  fetchDemands()
  if (authStore.isLoggedIn) {
    fetchActiveOrders()
    fetchUserCredit()
    fetchTotalUnreadCount()
    startPolling()
  }
})

onUnmounted(() => {
  document.removeEventListener('click', closeDropdown)
  stopPolling()
})

// 监听登录状态变化
watch(() => authStore.isLoggedIn, (isLoggedIn) => {
  if (isLoggedIn) {
    fetchActiveOrders()
    fetchUserCredit()
    fetchTotalUnreadCount()
    startPolling()
  } else {
    activeOrders.value = []
    userCredit.value = { averageScore: null, scoreNum: 0 }
    totalUnreadCount.value = 0
    stopPolling()
  }
})
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
}

/* 头部样式 */
.fixed-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 75px;
  background-color: #62055f;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 40px;
  z-index: 1000;
}

.header-headline {
  font-size: 50px;
  font-weight: 600;
  color: #ffff;
  margin: 0;
  cursor: pointer;
}

.header-search {
  flex: 1;
  max-width: 400px;
  margin: 0 40px;
}

.search-box {
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 40px;
  padding: 0 16px;
  transition: all 0.2s;
}

.search-box:focus-within {
  background: rgba(255, 255, 255, 0.25);
}

.search-box input {
  flex: 1;
  background: transparent;
  border: none;
  padding: 10px 0;
  color: white;
  font-size: 14px;
  outline: none;
}

.search-box input::placeholder {
  color: rgba(255, 255, 255, 0.6);
}

.search-clear {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  font-size: 14px;
  padding: 4px 8px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
  position: relative;
}

.user-info {
  color: white;
  font-size: 14px;
  font-weight: 500;
}

/* 头像包装器 - 用于红点定位 */
.avatar-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  cursor: pointer;
  border: 2px solid rgba(255, 255, 255, 0.3);
  transition: all 0.2s;
}

.avatar:hover {
  transform: scale(0.98);
}

/* 头像小红点 */
.avatar-badge {
  position: absolute;
  top: 0;
  right: 0;
  width: 12px;
  height: 12px;
  background-color: #ff4444;
  border-radius: 50%;
  border: 2px solid #62055f;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.6; transform: scale(1.1); }
}

.header-btn {
  background: transparent;
  border: none;
  color: white;
  padding: 8px 16px;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.2s;
}

/* 下拉菜单 */
.dropdown-menu {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  min-width: 180px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
  overflow: hidden;
  z-index: 1001;
}

.dropdown-item {
  display: block;
  width: 100%;
  padding: 12px 16px;
  text-align: left;
  background: white;
  border: none;
  cursor: pointer;
  font-size: 14px;
  color: #333;
  transition: background 0.2s;
  position: relative;
}

.dropdown-item:hover {
  background: #f5f5f5;
}

/* 消息菜单项 - 用于显示数字角标 */
.message-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.message-badge {
  background-color: #dc1010;
  color: white;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 6px;
  border-radius: 20px;
  min-width: 20px;
  text-align: center;
  margin-left: 12px;
}

.dropdown-divider {
  height: 1px;
  background: #eee;
  margin: 4px 0;
}

.logout-item {
  color: #e31829;
}

.dropdown-enter-active,
.dropdown-leave-active {
  transition: opacity 0.2s, transform 0.2s;
}

.dropdown-enter-from {
  opacity: 0;
  transform: translateY(-8px);
}

/* 主内容区 */
.main-content {
  margin-top: 75px;
  flex: 1;
  padding: 30px 40px;
}

.content-container {
  max-width: 1400px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 260px 1fr 300px;
  gap: 24px;
  align-items: start;
}

/* 左侧边栏 - 使用 sticky 固定 */
.left-sidebar,
.right-sidebar {
  position: sticky;
  top: 75px; /* 头部高度75px + 留白30px */
  align-self: start;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* 右侧边栏也使用 sticky */
.right-sidebar {
  top: 80px;
}

.publish-card,
.category-card,
.orders-card,
.credit-card {
  background: white;
  border-radius: 16px;
  padding: 15px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border: 1px solid #e8ecf0;
  display: flex;
  flex-direction: column;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 12px 0;
  padding-bottom: 10px;
  border-bottom: 2px solid #f0f2f5;
}

.card-desc {
  font-size: 13px;
  color: #6c7a8e;
  margin-bottom: 16px;
}

.publish-btn {
  width: 100%;
  padding: 12px;
  background: #62055f;
  color: white;
  border: none;
  border-radius: 40px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.publish-btn:hover {
  background: #7a0e76;
}

.category-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.category-btn {
  text-align: left;
  padding: 10px 12px;
  background: transparent;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  color: #3a4a6e;
  cursor: pointer;
  transition: all 0.2s;
}

.category-btn:hover {
  background: #f5f2f7;
  color: #62055f;
}

.category-btn.active {
  background: #ede8f5;
  color: #62055f;
  font-weight: 500;
}

/* 需求流 */
.demands-feed {
  background: transparent;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.feed-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.feed-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0;
}

.sort-options {
  display: flex;
  gap: 8px;
}

.sort-btn {
  font-size: 13px;
  color: #7a8aa3;
  cursor: pointer;
  padding: 4px 10px;
  border-radius: 20px;
  transition: all 0.2s;
}

.sort-btn:hover {
  color: #62055f;
  background: #f5f2f7;
}

.sort-btn.active {
  color: #62055f;
  background: #ede8f5;
  font-weight: 500;
}

.demands-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 需求卡片 - 新增图片布局 */
.demand-card {
  background: white;
  border-radius: 16px;
  padding: 16px;
  border: 1px solid #e8ecf0;
  transition: box-shadow 0.2s;
  cursor: pointer;
  display: flex;
  gap: 16px;
}

.demand-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

/* 左侧图片区域 */
.demand-image {
  flex-shrink: 0;
  width: 100px;
  height: 100px;
  border-radius: 12px;
  overflow: hidden;
  background-color: #f5f5f5;
}

.demand-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.demand-image-placeholder {
  flex-shrink: 0;
  width: 100px;
  height: 100px;
  border-radius: 12px;
  background-color: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ccc;
}

.placeholder-icon {
  font-size: 32px;
}

/* 右侧内容区域 */
.demand-content {
  flex: 1;
  min-width: 0; /* 防止溢出 */
}

.demand-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  flex-wrap: wrap;
  gap: 8px;
}

.demand-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0;
}

.demand-category {
  font-size: 12px;
  padding: 4px 10px;
  background: #f0f2f5;
  border-radius: 20px;
  color: #5a6e8a;
  white-space: nowrap;
}

.demand-desc {
  font-size: 14px;
  color: #4a5a78;
  margin: 8px 0 12px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.demand-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 14px;
}

.meta-item {
  display: flex;
  align-items: baseline;
  gap: 6px;
  font-size: 13px;
}

.meta-label {
  color: #8a9abb;
}

.meta-value.reward {
  color: #e67e22;
  font-weight: 600;
}

.demand-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f0f2f5;
}

.demand-time {
  font-size: 12px;
  color: #9aa6b5;
}

.my-demand-badge {
  font-size: 11px;
  padding: 2px 8px;
  background: #f0f2f5;
  border-radius: 12px;
  color: #62055f;
}

/* 右侧订单卡片 - 固定最大高度，内容滚动 + 滚动条美化 */
.orders-card {
  max-height: 150px;
  min-height: 150px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.orders-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-right: 4px;
  /* 滚动条美化 */
  scrollbar-width: thin;
  scrollbar-color: #c1c1c1 #f1f1f1;
}

.orders-list::-webkit-scrollbar {
  width: 6px;
}

.orders-list::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 10px;
}

.orders-list::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 10px;
}

.orders-list::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

.order-item {
  padding: 12px 0;
  border-bottom: 1px solid #f0f2f5;
  cursor: pointer;
  transition: background 0.2s;
}

.order-item:hover {
  background: #fafafc;
}

.order-item:last-child {
  border-bottom: none;
}

.order-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.order-title {
  font-size: 14px;
  font-weight: 500;
  color: #1a1a2e;
}

.order-status {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 20px;
  background: #f0f2f5;
}

.order-status.accepted { background: #e8f4fd; color: #3498db; }
.order-status.progress { background: #fee8e0; color: #e67e22; }

.order-demand-info {
  font-size: 12px;
  color: #7a8aa3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-orders {
  text-align: center;
  padding: 32px 0;
  color: #9aa6b5;
  font-size: 13px;
}

/* 信用看板 */
.credit-score-large {
  text-align: center;
  margin: 16px 0;
}

.score-number {
  font-size: 42px;
  font-weight: 700;
  color: #62055f;
}

.score-label {
  display: block;
  font-size: 13px;
  color: #8a9abb;
  margin-top: 4px;
}

.credit-stats {
  background: #f8f9fc;
  border-radius: 12px;
  padding: 12px;
  margin: 12px 0;
}

.stat-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  padding: 6px 0;
  color: #4a5a78;
}

/* 加载和空状态 */
.loading-state,
.empty-state {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 16px;
  color: #9aa6b5;
}

.loading-mini {
  display: flex;
  justify-content: center;
  padding: 24px 0;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 2px solid #e8ecf0;
  border-top-color: #62055f;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 12px;
}

.loading-spinner.small {
  width: 20px;
  height: 20px;
  margin: 0;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.pagination-btn {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  padding: 16px 0;
}

.page-btn {
  padding: 6px 14px;
  background: transparent;
  border: 1px solid #e8ecf0;
  border-radius: 8px;
  font-size: 13px;
  color: #62055f;
  cursor: pointer;
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) {
  background: #f5f2f7;
  border-color: #62055f;
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-info {
  font-size: 13px;
  color: #666;
}

/* 响应式 */
@media (max-width: 1024px) {
  .content-container {
    grid-template-columns: 220px 1fr 280px;
    gap: 16px;
  }
  .fixed-header {
    padding: 0 20px;
  }
  .header-search {
    max-width: 280px;
  }
  .left-sidebar,
  .right-sidebar {
    top: 95px;
  }
}

@media (max-width: 768px) {
  .content-container {
    grid-template-columns: 1fr;
  }
  .left-sidebar, .right-sidebar {
    position: static;
    order: 1;
  }
  .demands-feed {
    order: 2;
  }
  .header-headline {
    font-size: 24px;
  }
  .main-content {
    padding: 20px;
  }
  .fixed-header {
    padding: 0 16px;
  }
  .header-search {
    margin: 0 12px;
    max-width: 200px;
  }
  
  /* 移动端卡片布局调整 */
  .demand-card {
    flex-direction: column;
  }
  
  .demand-image,
  .demand-image-placeholder {
    width: 100%;
    height: 160px;
  }
}
</style>