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
          </div>
          <transition name="dropdown">
            <div v-if="showDropdown" class="dropdown-menu">
              <button class="dropdown-item" @click="goToProfile">个人主页</button>
              <div class="dropdown-divider"></div>
              <button class="dropdown-item" @click="goToMessages">消息</button>
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
        <!-- 左侧：发布入口 + 分类筛选 -->
        <aside class="left-sidebar">
          <!-- 发布快捷入口 -->
          <div class="publish-card">
            <h3 class="card-title">发布需求</h3>
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

        <!-- 中间：智能推荐需求流 -->
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
                <div class="meta-item">
                  <span class="meta-label">发布者</span>
                  <span class="meta-value credit" :class="getCreditClass(demand.publisherCredit)">
                    {{ demand.publisherName || '用户' }} 
                    <span class="credit-score">{{ demand.publisherCredit ? `(${demand.publisherCredit})` : '' }}</span>
                  </span>
                </div>
              </div>
              <div class="demand-footer">
                <span class="demand-time">{{ formatRelativeTime(demand.createdAt) }}</span>
                <button 
                  class="accept-btn" 
                  @click="handleAcceptDemand(demand)"
                  :disabled="!authStore.isLoggedIn || demand.isOwn"
                >
                  {{ demand.isOwn ? '我的需求' : '接单' }}
                </button>
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

        <!-- 右侧：订单看板 + 信用分 -->
        <aside class="right-sidebar">
          <!-- 进行中订单卡片 -->
          <div class="orders-card">
            <h3 class="card-title">进行中的订单</h3>
            <div v-if="loadingOrders" class="loading-mini">
              <div class="loading-spinner small"></div>
            </div>
            <div v-else-if="activeOrders.length === 0" class="empty-orders">
              <span>暂无进行中的订单</span>
            </div>
            <div v-else class="orders-list">
              <div v-for="order in activeOrders" :key="order.id" class="order-item">
                <div class="order-info">
                  <div class="order-title">{{ order.demandTitle || '订单' }}</div>
                  <div class="order-status" :class="getStatusClass(order.status)">
                    {{ getStatusText(order.status) }}
                  </div>
                </div>
                <div class="order-actions">
                  <button class="order-btn" @click="goToOrderDetail(order.id)">查看</button>
                  <button class="order-btn chat" @click="goToChat(order)">聊天</button>
                </div>
              </div>
            </div>
          </div>

          <!-- 个人信用分看板 -->
          <div class="credit-card">
            <h3 class="card-title">我的信用</h3>
            <div class="credit-score-large">
              <span class="score-number">{{ userCredit.averageScore || '暂无' }}</span>
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
            <button class="credit-detail-btn" @click="goToProfile">查看详情</button>
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
import { ref, computed, onMounted, watch } from 'vue'
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
const currentSort = ref('distance')
const currentPage = ref(0)
const pageSize = ref(10)
const loadingDemands = ref(false)
const loadingOrders = ref(false)

// 数据
const demands = ref([])
const totalPages = ref(0)
const totalElements = ref(0)
const activeOrders = ref([])
const completedOrdersCount = ref(0)
const userCredit = ref({ averageScore: null, scoreNum: 0 })

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
  { label: '距离优先', value: 'distance' },
  { label: '最新发布', value: 'time' },
  { label: '信用优先', value: 'credit' }
]

// 头像
const displayAvatar = computed(() => {
  if (authStore.user?.avatarPath) {
    return `http://localhost:8080${authStore.user.avatarPath}`
  }
  return 'http://localhost:8080/api/files/default-avatar.png'
})

// 显示通知
const showNotification = (title, content) => {
  alertConfig.value = { title, content, confirmText: '确定' }
  showAlert.value = true
}

// 获取需求列表
const fetchDemands = async () => {
  loadingDemands.value = true
  try {
    const params = new URLSearchParams({
      page: currentPage.value,
      size: pageSize.value,
      sortBy: getSortByField(),
      direction: currentSort.value === 'time' ? 'desc' : 'asc'
    })
    if (selectedCategory.value) params.append('category', selectedCategory.value)
    if (searchKeyword.value) params.append('keyword', searchKeyword.value)

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

// 获取排序字段
const getSortByField = () => {
  switch (currentSort.value) {
    case 'distance': return 'location'
    case 'time': return 'createdAt'
    case 'credit': return 'publisherCredit'
    default: return 'createdAt'
  }
}

// 获取进行中订单
const fetchActiveOrders = async () => {
  if (!authStore.isLoggedIn) return
  loadingOrders.value = true
  try {
    // 获取用户作为发布者或接单者的进行中订单
    const response = await axios.get(`http://localhost:8080/orders/user/${authStore.user.id}`, {
      params: { page: 0, size: 10, role: 'all', status: 'ACCEPTED,IN_PROGRESS' }
    })
    if (response.data.code === 200 && response.data.data) {
      activeOrders.value = response.data.data.content || []
    }
    console.log(response)
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
      return
    }
  } catch (error) {
    console.error('获取信用信息失败:', error)
  }
}

// 接单操作
const handleAcceptDemand = async (demand) => {
  if (!authStore.isLoggedIn) {
    router.push('/login')
    return
  }
  if (demand.isOwn) {
    showNotification('提示', '不能接自己的需求')
    return
  }
  
  try {
    const response = await axios.post(`http://localhost:8080/orders/create`, null, {
      params: { demandId: demand.id, userId: authStore.user.id }
    })
    if (response.data.code === 200 || response.data.code === 201) {
      showNotification('接单成功', '您已成功接单，请前往订单页面查看')
      fetchActiveOrders()
    }
  } catch (error) {
    console.error('接单失败:', error)
    const msg = error.response?.data?.message || '接单失败，请稍后重试'
    showNotification('接单失败', msg)
  }
}

// 路由跳转
const goToPublish = () => router.push('/demands/publish')
const goToProfile = () => { showDropdown.value = false; router.push('/my/profile') }
const goToMessages = () => { showDropdown.value = false; router.push('/my/conversations') }
const goToOrderDetail = (orderId) => router.push(`/orders/${orderId}`)
const goToChat = (order) => router.push(`/messages?orderId=${order.id}`)

// 辅助方法
const formatRelativeTime = (isoString) => {
  if (!isoString) return ''
  const date = new Date(isoString)
  const now = new Date()
  const diff = now - date
  if (diff < 24 * 3600 * 1000) {
    return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
  }
  return `${date.getMonth() + 1}/${date.getDate()}`
}

const getCreditClass = (credit) => {
  if (!credit) return ''
  if (credit >= 4.5) return 'high'
  if (credit >= 3.5) return 'medium'
  return 'low'
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
  }
})

// 监听登录状态变化
watch(() => authStore.isLoggedIn, (isLoggedIn) => {
  if (isLoggedIn) {
    fetchActiveOrders()
    fetchUserCredit()
  } else {
    activeOrders.value = []
    userCredit.value = { averageScore: null, scoreNum: 0 }
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

.search-icon {
  color: rgba(255, 255, 255, 0.7);
  font-size: 16px;
  margin-right: 8px;
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
  min-width: 160px;
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
}

.dropdown-item:hover {
  background: #f5f5f5;
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
  margin-top: 70px;
  flex: 1;
  padding: 30px 40px;
}

.content-container {
  max-width: 1400px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 260px 1fr 300px;
  gap: 24px;
}

/* 左侧边栏 */
.left-sidebar,
.right-sidebar {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.publish-card,
.category-card,
.orders-card,
.credit-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border: 1px solid #e8ecf0;
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

.demand-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  border: 1px solid #e8ecf0;
  transition: box-shadow 0.2s;
}

.demand-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.demand-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
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

.meta-value.credit {
  display: flex;
  align-items: center;
  gap: 4px;
}

.credit-score {
  font-size: 12px;
  color: #27ae60;
}

.credit-score.high { color: #27ae60; }
.credit-score.medium { color: #f39c12; }
.credit-score.low { color: #e74c3c; }

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

.accept-btn {
  padding: 6px 20px;
  background: #62055f;
  color: white;
  border: none;
  border-radius: 30px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.accept-btn:hover:not(:disabled) {
  background: #7a0e76;
  transform: translateY(-1px);
}

.accept-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

/* 右侧订单卡片 */
.orders-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-item {
  padding: 12px 0;
  border-bottom: 1px solid #f0f2f5;
}

.order-item:last-child {
  border-bottom: none;
}

.order-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
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

.order-actions {
  display: flex;
  gap: 12px;
}

.order-btn {
  padding: 5px 12px;
  background: transparent;
  border: 1px solid #ddd;
  border-radius: 20px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.order-btn.chat {
  border-color: #62055f;
  color: #62055f;
}

.order-btn.chat:hover {
  background: #62055f;
  color: white;
}

.empty-orders {
  text-align: center;
  padding: 24px 0;
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

.credit-detail-btn {
  width: 100%;
  padding: 10px;
  background: transparent;
  border: 1px solid #ddd;
  border-radius: 30px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}

.credit-detail-btn:hover {
  border-color: #62055f;
  color: #62055f;
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
  border: none;
  border-radius: 8px;
  font-size: 13px;
  color: #62055f;
  cursor: pointer;
}

.page-btn:disabled {
  opacity: 0.5;
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
}

@media (max-width: 768px) {
  .content-container {
    grid-template-columns: 1fr;
  }
  .left-sidebar, .right-sidebar {
    order: 1;
  }
  .demands-feed {
    order: 2;
  }
  .header-headline {
    font-size: 20px;
  }
  .main-content {
    padding: 20px;
  }
}
</style>