<template>
  <div class="user-info-page" @click="closeDropdowns">
    <!-- 顶部固定导航栏（复用主页样式） -->
    <header class="fixed-header">
      <div class="header-left">
        <h1 class="header-headline" @click="router.push('/')">校园互助服务平台</h1>
      </div>
      <div class="header-right">
        <div class="user-info">{{ authStore.user?.name || '用户' }}</div>
        <div class="avatar-wrapper">
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
            <button class="dropdown-item" @click="handleMessages">消息</button>
            <div class="dropdown-divider"></div>
            <template v-if="authStore.user?.isAdmin">
              <button class="dropdown-item" @click="handleAdmin">管理后台</button>
            </template>
            <button class="dropdown-item logout-item" @click="handleLogout">退出登录</button>
          </div>
        </transition>
      </div>
    </header>

    <main class="main-content">
      <div class="info-container">
        <!-- 左侧：用户信息卡片 -->
        <div class="info-sidebar">
          <div class="user-card">
            <div class="avatar-large-wrapper">
              <img 
                :src="targetUserAvatar" 
                alt="用户头像"
                class="avatar-large"
              />
            </div>
            <h3 class="user-name">{{ targetUser?.name || '用户' }}</h3>
            <div class="user-stats">
              <div class="stat-item">
                <span class="stat-label">平均评分</span>
                <span class="stat-value">{{ userScoreInfo.averageScore ?? '暂无' }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">评价次数</span>
                <span class="stat-value">{{ userScoreInfo.scoreNum ?? 0 }}</span>
              </div>
            </div>
            <div class="user-basic-info">
              <div class="info-row">
                <span class="info-label">手机号</span>
                <span class="info-text">{{ targetUser?.phone || '未绑定' }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">身份</span>
                <span class="info-text">{{ targetUser?.isAdmin ? '管理员' : '普通用户' }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧：评价列表（分页） -->
        <div class="reviews-content">
          <div class="reviews-card">
            <div class="card-header">
              <h3 class="card-title">收到的评价</h3>
              <div class="score-filter">
                <span 
                  v-for="score in [0,1,2,3,4,5]" 
                  :key="score"
                  class="filter-chip"
                  :class="{ active: filterScore === score }"
                  @click="setFilterScore(score)"
                >
                  {{ score === 0 ? '全部' : `${score}星` }}
                </span>
              </div>
            </div>

            <div v-if="loadingReviews" class="loading-state">
              <div class="loading-spinner"></div>
              <span>加载评价中...</span>
            </div>
            <div v-else-if="reviews.length === 0" class="empty-state">
              <span>暂无评价</span>
            </div>
            <div v-else class="reviews-list">
              <div v-for="review in reviews" :key="review.id" class="review-item">
                <div class="review-header">
                  <div class="reviewer-info">
                    <img 
                      :src="getReviewerAvatar(review.reviewerId)" 
                      class="reviewer-avatar"
                      @error="handleAvatarError"
                    />
                    <span class="reviewer-name">{{ review.reviewerName || `用户${review.reviewerId}` }}</span>
                  </div>
                  <div class="review-score">
                    <span v-for="star in 5" :key="star" class="star" :class="{ filled: star <= review.score }">★</span>
                  </div>
                </div>
                <p class="review-content">{{ review.content || '没有填写评价内容' }}</p>
                <div class="review-time">{{ formatDate(review.createdAt) }}</div>
              </div>
            </div>

            <!-- 分页控件 -->
            <div v-if="totalPages > 1" class="pagination">
              <button class="page-btn" :disabled="currentPage === 0" @click="changePage(currentPage - 1)">上一页</button>
              <span class="page-info">{{ currentPage + 1 }} / {{ totalPages }}</span>
              <button class="page-btn" :disabled="currentPage >= totalPages - 1" @click="changePage(currentPage + 1)">下一页</button>
            </div>
          </div>
        </div>
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
import { useRouter, useRoute } from 'vue-router'
import AlertBox from './SmallComponents/AlertBox.vue'
import axios from 'axios'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()

// 页面状态
const showDropdown = ref(false)
const loadingReviews = ref(false)
const showAlert = ref(false)
const alertConfig = ref({ title: '', content: '', confirmText: '确定' })

// 用户数据
const targetUser = ref(null)
const userScoreInfo = ref({ averageScore: null, scoreNum: 0 })

// 评价数据
const reviews = ref([])
const currentPage = ref(0)
const pageSize = ref(5)
const totalPages = ref(0)
const totalElements = ref(0)
const filterScore = ref(0) // 0表示全部

// 头像缓存 (评价者头像)
const reviewerAvatars = ref({})

// 获取路由中的用户ID
const userId = computed(() => {
  const id = route.params.id
  return id ? parseInt(id) : null
})

// 当前登录用户头像
const displayAvatar = computed(() => {
  if (authStore.user?.avatarPath) {
    return `http://localhost:8080${authStore.user.avatarPath}`
  }
  return 'http://localhost:8080/api/files/default-avatar.png'
})

// 目标用户头像
const targetUserAvatar = computed(() => {
  if (targetUser.value?.avatarPath) {
    return `http://localhost:8080${targetUser.value.avatarPath}`
  }
  return 'http://localhost:8080/api/files/default-avatar.png'
})

// 显示通知
const showNotification = (title, content) => {
  alertConfig.value = { title, content, confirmText: '确定' }
  showAlert.value = true
}

// 获取用户基本信息
const fetchUserInfo = async () => {
  if (!userId.value) {
    showNotification('错误', '用户不存在')
    router.push('/')
    return
  }

  try {
    const response = await axios.get(`http://localhost:8080/users/${userId.value}`)
    if (response.data.code === 200 && response.data.data) {
      targetUser.value = response.data.data
      userScoreInfo.value = {
        averageScore: response.data.data.averageScore,
        scoreNum: response.data.data.scoreNum
      }
    } else {
      showNotification('获取失败', response.data.message || '用户不存在')
      router.push('/')
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
    showNotification('获取失败', '无法获取用户信息')
    router.push('/')
  }
}

// 获取用户收到的评价
const fetchUserReviews = async () => {
  if (!userId.value) return

  loadingReviews.value = true
  try {
    const params = new URLSearchParams({
      page: currentPage.value,
      size: pageSize.value,
      sortBy: 'createdAt',
      direction: 'desc'
    })
    
    // 添加评分筛选
    if (filterScore.value > 0) {
      params.append('score', filterScore.value)
    }

    const response = await axios.get(`http://localhost:8080/reviews/received/${userId.value}?${params}`)
    
    if (response.data.code === 200 && response.data.data) {
      const data = response.data.data
      reviews.value = data.content || []
      totalPages.value = data.totalPages || 0
      totalElements.value = data.totalElements || 0
      
      // 加载评价者的头像信息
      await fetchReviewerAvatars()
    }
  } catch (error) {
    console.error('获取评价失败:', error)
    showNotification('加载失败', '无法获取评价列表')
  } finally {
    loadingReviews.value = false
  }
}

// 获取评价者的头像
const fetchReviewerAvatars = async () => {
  const reviewerIds = [...new Set(reviews.value.map(r => r.reviewerId).filter(id => id))]
  
  for (const reviewerId of reviewerIds) {
    if (reviewerAvatars.value[reviewerId]) continue
    
    try {
      const response = await axios.get(`http://localhost:8080/users/${reviewerId}`)
      if (response.data.code === 200 && response.data.data) {
        reviewerAvatars.value[reviewerId] = response.data.data.avatarPath
      }
    } catch (error) {
      console.warn(`获取评价者${reviewerId}信息失败:`, error)
    }
  }
}

// 获取评价者头像URL
const getReviewerAvatar = (reviewerId) => {
  const avatarPath = reviewerAvatars.value[reviewerId]
  if (avatarPath) {
    return `http://localhost:8080${avatarPath}`
  }
  return 'http://localhost:8080/api/files/default-avatar.png'
}

// 头像加载失败处理
const handleAvatarError = (event) => {
  event.target.src = 'http://localhost:8080/api/files/default-avatar.png'
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  try {
    const date = new Date(dateStr)
    const now = new Date()
    const diff = now - date
    
    if (diff < 24 * 3600 * 1000) {
      return `今天 ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
    } else if (diff < 7 * 24 * 3600 * 1000) {
      const days = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
      return days[date.getDay()]
    } else {
      return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`
    }
  } catch {
    return dateStr
  }
}

// 设置评分筛选
const setFilterScore = (score) => {
  filterScore.value = score
  currentPage.value = 0
  fetchUserReviews()
}

// 分页切换
const changePage = (page) => {
  currentPage.value = page
  fetchUserReviews()
}

// 下拉菜单相关
const closeDropdowns = () => {
  showDropdown.value = false
}

const toggleDropdown = () => {
  if (authStore.isLoggedIn) showDropdown.value = !showDropdown.value
}

const goToProfile = () => {
  showDropdown.value = false
  router.push('/my/profile')
}

const handleMessages = () => {
  showDropdown.value = false
  router.push('/my/conversations')
}

const handleAdmin = () => {
  showNotification('功能开发中', '管理后台正在开发中')
  showDropdown.value = false
}

const handleLogout = () => {
  const username = authStore.user?.name || '用户'
  showNotification('退出登录', `再见，${username}！`)
  authStore.logout()
  showDropdown.value = false
  router.push('/')
}

const handleAlertConfirm = () => {}

// 监听路由参数变化
watch(() => route.params.id, (newId) => {
  if (newId) {
    currentPage.value = 0
    filterScore.value = 0
    reviews.value = []
    reviewerAvatars.value = {}
    fetchUserInfo()
    fetchUserReviews()
  }
}, { immediate: true })

// 页面挂载
onMounted(() => {
  document.addEventListener('click', closeDropdowns)
  
  if (userId.value) {
    fetchUserInfo()
    fetchUserReviews()
  } else {
    showNotification('错误', '用户不存在')
    router.push('/')
  }
})
</script>

<style scoped>
.user-info-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
}

/* ========== 头部样式 (复用HomePage) ========== */
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

.header-left {
  display: flex;
  align-items: center;
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

.avatar-wrapper {
  cursor: pointer;
  position: relative;
  display: flex;
  align-items: center;
  height: 100%;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid rgba(255, 255, 255, 0.3);
  transition: all 0.2s ease;
}

.avatar:hover {
  transform: scale(0.98);
}

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

/* ========== 主内容区 ========== */
.main-content {
  margin-top: 75px;
  flex: 1;
  padding: 40px;
  background-color: #f5f7fa;
}

.info-container {
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  gap: 30px;
  flex-wrap: wrap;
  min-height: calc(100vh - 155px);
  max-height: calc(100vh - 155px);
  overflow: hidden;
}

/* 左侧用户信息卡片 */
.info-sidebar {
  flex: 0 0 320px;
  min-width: 280px;
  height: 100%;
  overflow-y: auto;
}

.info-sidebar::-webkit-scrollbar {
  width: 6px;
}

.info-sidebar::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.info-sidebar::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.user-card {
  background: white;
  border-radius: 20px;
  padding: 30px 24px;
  text-align: center;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8ecf0;
}

.avatar-large-wrapper {
  width: 120px;
  height: 120px;
  margin: 0 auto 20px;
  border-radius: 50%;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.avatar-large {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-name {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 20px 0;
}

.user-stats {
  display: flex;
  justify-content: space-around;
  padding: 16px 0;
  margin-bottom: 20px;
  border-top: 1px solid #f0f2f5;
  border-bottom: 1px solid #f0f2f5;
}

.stat-item {
  text-align: center;
}

.stat-label {
  display: block;
  font-size: 13px;
  color: #8a9abb;
  margin-bottom: 6px;
}

.stat-value {
  display: block;
  font-size: 20px;
  font-weight: 700;
  color: #62055f;
}

.user-basic-info {
  text-align: left;
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  font-size: 14px;
  border-bottom: 1px solid #f5f5f5;
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  color: #8a9abb;
  font-weight: 500;
}

.info-text {
  color: #2c3e50;
}

/* 右侧评价列表 */
.reviews-content {
  flex: 1;
  min-width: 0;
  height: 100%;
  overflow: hidden;
}

.reviews-card {
  background: white;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8ecf0;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 2px solid #f0f2f5;
  flex-shrink: 0;
}

.card-title {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0;
}

.score-filter {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-chip {
  padding: 4px 12px;
  background: #f5f7fa;
  border-radius: 20px;
  font-size: 13px;
  color: #6c7a8e;
  cursor: pointer;
  transition: all 0.2s ease;
}

.filter-chip:hover {
  background: #ede8f5;
  color: #62055f;
}

.filter-chip.active {
  background: #62055f;
  color: white;
}

/* 评价列表容器 - 有最小/最大高度，无滚动条（内部滚动由浏览器控制） */
.reviews-list {
  flex: 1;
  overflow-y: auto;
  min-height: 300px;
  max-height: 500px;
  padding-right: 4px;
}

/* 隐藏滚动条但保持功能 */
.reviews-list::-webkit-scrollbar {
  width: 0;
  background: transparent;
}

.reviews-list {
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.review-item {
  padding: 20px 0;
  border-bottom: 1px solid #f0f2f5;
  transition: background 0.2s;
}

.review-item:last-child {
  border-bottom: none;
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.reviewer-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.reviewer-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  background-color: #f0f2f5;
}

.reviewer-name {
  font-weight: 600;
  color: #2c3e50;
  font-size: 14px;
}

.review-score {
  display: flex;
  gap: 2px;
}

.star {
  color: #d4d9e3;
  font-size: 16px;
  letter-spacing: 2px;
}

.star.filled {
  color: #f5b042;
}

.review-content {
  margin: 12px 0 10px;
  line-height: 1.5;
  color: #4a5a78;
  font-size: 14px;
  word-break: break-word;
}

.review-time {
  font-size: 12px;
  color: #9aa6b5;
  text-align: right;
}

/* 加载和空状态 */
.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #9aa6b5;
  gap: 12px;
  min-height: 300px;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 2px solid #e8ecf0;
  border-top-color: #62055f;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 分页控件 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  padding: 20px 0 8px;
  border-top: 1px solid #f0f2f5;
  margin-top: 16px;
  flex-shrink: 0;
}

.page-btn {
  padding: 6px 16px;
  background: #f5f7fa;
  border: 1px solid #e0e4e9;
  border-radius: 20px;
  font-size: 13px;
  color: #62055f;
  cursor: pointer;
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) {
  background: #ede8f5;
  border-color: #62055f;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  font-size: 13px;
  color: #6c7a8e;
}

/* 响应式 */
@media (max-width: 1024px) {
  .info-container {
    flex-direction: column;
    min-height: auto;
    max-height: none;
    overflow: visible;
  }
  
  .info-sidebar {
    flex: auto;
    height: auto;
    max-height: none;
  }
  
  .reviews-content {
    height: auto;
    max-height: 600px;
  }
  
  .reviews-list {
    max-height: 400px;
  }
  
  .fixed-header {
    padding: 0 20px;
  }
  
  .header-headline {
    font-size: 28px;
  }
}

@media (max-width: 768px) {
  .main-content {
    padding: 20px;
  }
  
  .info-sidebar {
    min-width: auto;
  }
  
  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .score-filter {
    width: 100%;
    justify-content: center;
  }
}
</style>