<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import AlertBox from './SmallComponents/AlertBox.vue'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()

const demandId = ref(route.params.id)
const demand = ref(null)
const loading = ref(false)
const statusUpdating = ref(false)
const publisherInfo = ref(null) // 添加发布者信息

// 图片预览相关状态
const isPreviewVisible = ref(false)
const previewImageUrl = ref('')
const currentImageIndex = ref(0)

// 通知框
const showAlert = ref(false)
const alertConfig = ref({
  title: '系统通知',
  content: '',
  htmlContent: '',
  confirmText: '确定'
})

const showNotification = (title, content, isHtml = false) => {
  alertConfig.value = {
    title: title,
    content: isHtml ? '' : content,
    htmlContent: isHtml ? content : '',
    confirmText: '确定'
  }
  showAlert.value = true
}

const handleAlertConfirm = () => {
  //donothing
  router.go(0) // 刷新当前页面以获取最新状态
}

// 解析图片URL
const pictureUrls = computed(() => {
  if (!demand.value?.pictureUrls) return []
  return demand.value.pictureUrls.split(';').filter(url => url.trim())
})

// 图片预览功能
const openImagePreview = (index) => {
  if (!pictureUrls.value.length) return
  currentImageIndex.value = index
  const fullUrl = 'http://localhost:8080' + pictureUrls.value[index]
  previewImageUrl.value = fullUrl
  isPreviewVisible.value = true
  // 禁止背景滚动
  document.body.style.overflow = 'hidden'
}

const closeImagePreview = () => {
  isPreviewVisible.value = false
  previewImageUrl.value = ''
  currentImageIndex.value = 0
  // 恢复背景滚动
  document.body.style.overflow = ''
}

const prevImage = () => {
  if (currentImageIndex.value > 0) {
    currentImageIndex.value--
    previewImageUrl.value = 'http://localhost:8080' + pictureUrls.value[currentImageIndex.value]
  }
}

const nextImage = () => {
  if (currentImageIndex.value < pictureUrls.value.length - 1) {
    currentImageIndex.value++
    previewImageUrl.value = 'http://localhost:8080' + pictureUrls.value[currentImageIndex.value]
  }
}

// 键盘事件处理
const handleKeydown = (e) => {
  if (!isPreviewVisible.value) return
  if (e.key === 'ArrowLeft') {
    prevImage()
  } else if (e.key === 'ArrowRight') {
    nextImage()
  } else if (e.key === 'Escape') {
    closeImagePreview()
  }
}

// 状态映射
const statusMap = {
  'PENDING': { text: '待接取', color: '#ff9800', bg: '#fff3e0' },
  'ACCEPTED': { text: '已接取', color: '#2196f3', bg: '#e3f2fd' },
  'REJECTED': { text: '已拒绝', color: '#f44336', bg: '#ffebee' },
  'COMPLETED': { text: '已完成', color: '#4caf50', bg: '#e8f5e9' },
  'CANCELLED': { text: '已取消', color: '#9e9e9e', bg: '#f5f5f5' },
  'EXPIRED': { text: '已过期', color: '#9e9e9e', bg: '#f5f5f5' }
}

const statusInfo = computed(() => {
  return statusMap[demand.value?.status] || { text: '未知', color: '#666', bg: '#f0f0f0' }
})

// 是否是发布者
const isPublisher = computed(() => {
  if (!demand.value || !authStore.isLoggedIn) return false
  return demand.value.publisherId === authStore.user?.id
})

// 是否可编辑（发布者且状态为待接取或被拒绝）
const canEdit = computed(() => {
  return isPublisher.value &&
      (demand.value?.status === 'PENDING' || demand.value?.status === 'REJECTED')
})

// 是否可取消（发布者且状态为待接取）
const canCancel = computed(() => {
  return isPublisher.value && demand.value?.status === 'PENDING'
})

// 是否可接取（非发布者、已登录、状态为待接取）
const canAccept = computed(() => {
  return authStore.isLoggedIn &&
      !isPublisher.value &&
      demand.value?.status === 'PENDING'
})

// 获取发布者信息
const fetchPublisherInfo = async () => {
  if (!demand.value?.publisherId) return
  try {
    const response = await fetch(`http://localhost:8080/users/${demand.value.publisherId}`, {
      headers: {
        'Authorization': authStore.token ? `Bearer ${authStore.token}` : ''
      }
    })
    const result = await response.json()
    if (result.code === 200) {
      publisherInfo.value = result.data
    }
  } catch (error) {
    console.error('获取发布者信息失败:', error)
  }
}

// 获取需求详情
const fetchDemand = async () => {
  loading.value = true
  try {
    const response = await fetch(`http://localhost:8080/demands/${demandId.value}`, {
      headers: {
        'Authorization': authStore.token ? `Bearer ${authStore.token}` : ''
      }
    })
    const result = await response.json()
    console.log('获取需求成功:', result)
    if (result.code === 200) {
      demand.value = result.data
      // 获取需求详情后，再获取发布者信息
      await fetchPublisherInfo()
    } else {
      throw new Error(result.message || '获取需求失败')
    }
  } catch (error) {
    console.error('获取需求失败:', error)
    showNotification('加载失败', error.message || '网络错误，请重试')
    setTimeout(() => router.push('/demands'), 1500)
  } finally {
    loading.value = false
  }
}

// 更新状态
const updateStatus = async (status) => {
  statusUpdating.value = true
  try {
    const response = await fetch(`http://localhost:8080/demands/${demandId.value}/status?status=${status}`, {
      method: 'PATCH',
      headers: {
        'Authorization': `Bearer ${authStore.token}`
      }
    })
    const result = await response.json()

    if (result.code === 200) {
      demand.value.status = status
      showNotification('操作成功', `需求已${statusMap[status].text}`)
    } else {
      throw new Error(result.message || '操作失败')
    }
  } catch (error) {
    console.error('更新状态失败:', error)
    showNotification('操作失败', error.message || '请稍后重试')
  } finally {
    statusUpdating.value = false
  }
}

// 取消需求
const handleCancel = () => {
  if (confirm('确定要取消这个需求吗？')) {
    updateStatus('CANCELLED')
  }
}

// 编辑需求
const handleEdit = () => {
  router.push(`/edit/demand/${demandId.value}`)
}

// 联系TA
const handleContact = async () => {
  try {
    const otherUserId = demand.value.publisherId;

    const response = await fetch(`http://localhost:8080/conversations?user1Id=${authStore.user.id}&user2Id=${otherUserId}`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${authStore.token}`
      }
    })

    if (response.status === 200 || response.status === 201) {
      const result = await response.json()

      if (result.code === 200 && result.data) {
        router.push(`/my/conversations`)
      }
    } else {
      const errorResult = await response.json()
      showNotification('操作失败', errorResult.message || '网络错误，无法创建会话')
    }
  } catch (error) {
    console.error('联系对方失败:', error)
    showNotification('操作失败', error.message || '网络错误，请重试')
  }
}

// 跳转到用户详情页
const goToUserProfile = (userId) => {
  router.push(`/user/${userId}`)
}

// 接取订单
const handleAccept = async () => {
  try{
    const response = await fetch(`http://localhost:8080/orders/create?demandId=${demandId.value}&userId=${authStore.user.id}`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${authStore.token}`
      },
    })
    const result = await response.json()
    if (result.code === 200) {
      showNotification('接取成功', '接取成功，请及时与该用户联系并完成订单')
    } else {
      console.log('接取失败:', result)
      showNotification('接取失败', (result.message || '请稍后重试'))
    }
  }catch (error) {
    console.error('接取订单失败:', error)
    showNotification('接取订单失败', error.message || '请稍后重试')
  }
}

// 返回
const goBack = () => {
  router.back()
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return '未设置'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

// 格式化酬金
const formatReward = (reward) => {
  if (!reward && reward !== 0) return '面议'
  return `¥${reward.toFixed(2)}`
}

// 获取头像URL
const getAvatarUrl = (avatarPath) => {
  if (!avatarPath) return '/default-avatar.png' // 默认头像路径
  if (avatarPath.startsWith('http')) return avatarPath
  return `http://localhost:8080${avatarPath}`
}

onMounted(() => {
  fetchDemand()
  // 添加键盘事件监听
  window.addEventListener('keydown', handleKeydown)
})

// 组件卸载时移除键盘事件监听
import { onUnmounted } from 'vue'
onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <div class="demand-detail-page">
    <div class="detail-container">
      <!-- 头部 -->
      <div class="page-header">
        <button class="back-btn" @click="goBack">
          <span class="back-icon">←</span> 返回
        </button>
        <h1 class="page-title">需求详情</h1>
        <div class="placeholder"></div>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="loading-state">
        <div class="loading-spinner"></div>
        <p>加载中...</p>
      </div>

      <!-- 内容 -->
      <div v-else-if="demand" class="detail-content">

        <!-- 状态栏 -->
        <div class="status-bar" :style="{ backgroundColor: statusInfo.bg }">
          <span class="status-dot" :style="{ backgroundColor: statusInfo.color }"></span>
          <span class="status-text" :style="{ color: statusInfo.color }">{{ statusInfo.text }}</span>
        </div>

        <!-- 标题 -->
        <h2 class="demand-title">{{ demand.title }}</h2>

        <!-- 基本信息 -->
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">分类</span>
            <span class="info-value">{{ demand.category }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">酬金</span>
            <span class="info-value reward">{{ formatReward(demand.reward) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">地点</span>
            <span class="info-value">{{ demand.location || '未填写' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">截止时间</span>
            <span class="info-value">{{ formatDate(demand.deadline) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">发布时间</span>
            <span class="info-value">{{ formatDate(demand.createdAt) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">更新时间</span>
            <span class="info-value">{{ formatDate(demand.updatedAt) }}</span>
          </div>
        </div>

        <div class="description-section">
          <h3 class="section-title">需求发布者</h3>
        </div>

        <div v-if="publisherInfo" class="publisher-card" @click="goToUserProfile(publisherInfo.id)">
          <div class="publisher-avatar">
            <img
                :src="getAvatarUrl(publisherInfo.avatarPath)"
                :alt="publisherInfo.name"
                @error="(e) => e.target.src = 'http://localhost:8080/api/files/default-avatar.png'"
            />
          </div>
          <div class="publisher-info">
            <div class="publisher-name">
              {{ publisherInfo.name }}
              <span v-if="publisherInfo.isAdmin" class="admin-badge">管理员</span>
              <span v-if="publisherInfo.isSuperAdmin" class="super-admin-badge">超级管理员</span>
            </div>
            <div class="publisher-stats">
              <span class="stat-item">
                <span class="stat-label">评分</span>
                <span class="stat-value">{{ publisherInfo.averageScore?.toFixed(1) || '暂无' }}</span>
              </span>
              <span class="stat-item">
                <span class="stat-label">评价数</span>
                <span class="stat-value">{{ publisherInfo.scoreNum || 0 }}</span>
              </span>
            </div>
          </div>
          <div class="publisher-arrow">›</div>
        </div>

        <!-- 需求描述 -->
        <div class="description-section">
          <h3 class="section-title">需求描述</h3>
          <div class="description-content">
            {{ demand.description || '暂无详细描述' }}
          </div>
        </div>

        <!-- 图片展示 -->
        <div v-if="pictureUrls.length > 0" class="images-section">
          <h3 class="section-title">相关图片 ({{ pictureUrls.length }})</h3>
          <div class="image-grid">
            <div
                v-for="(url, index) in pictureUrls"
                :key="index"
                class="image-item"
                @click="openImagePreview(index)"
            >
              <img :src="'http://localhost:8080' + url" :alt="`图片${index + 1}`" />
            </div>
          </div>
        </div>

        <!-- 操作按钮区域 -->
        <div class="action-buttons">
          <!-- 发布者操作按钮 -->
          <template v-if="isPublisher">
            <button
                v-if="canEdit"
                class="action-btn edit-btn"
                @click="handleEdit"
                :disabled="statusUpdating"
            >
              编辑需求
            </button>
            <button
                v-if="canCancel"
                class="action-btn cancel-btn"
                @click="handleCancel"
                :disabled="statusUpdating"
            >
              取消需求
            </button>
          </template>

          <!-- 非发布者操作按钮（已登录且可接取） -->
          <template v-else-if="canAccept && authStore.isLoggedIn">
            <button class="action-btn contact-btn" @click="handleContact">
              💬 联系TA
            </button>
            <button class="action-btn accept-btn" @click="handleAccept">
              📋 接取订单
            </button>
          </template>

          <!-- 未登录提示 -->
          <div v-else-if="!authStore.isLoggedIn && demand.status === 'PENDING'" class="login-tip">
            <button class="login-link" @click="router.push('/login')">请先登录后再联系发布者或接取订单</button>
          </div>
        </div>
      </div>

      <!-- 错误状态 -->
      <div v-else class="error-state">
        <p>需求不存在或已被删除</p>
        <button class="back-home-btn" @click="router.push('/demands')">返回列表</button>
      </div>
    </div>

    <!-- 图片预览模态框 -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="isPreviewVisible" class="image-preview-overlay" @click="closeImagePreview">
          <div class="preview-container" @click.stop>
            <!-- 关闭按钮 -->
            <button class="preview-close" @click="closeImagePreview">×</button>
            
            <!-- 上一张按钮 -->
            <button 
              v-if="pictureUrls.length > 1" 
              class="preview-nav prev" 
              @click="prevImage"
              :disabled="currentImageIndex === 0"
            >
              ‹
            </button>
            
            <!-- 图片 -->
            <div class="preview-image-wrapper">
              <img :src="previewImageUrl" :alt="`图片${currentImageIndex + 1}`" />
              <div v-if="pictureUrls.length > 1" class="image-counter">
                {{ currentImageIndex + 1 }} / {{ pictureUrls.length }}
              </div>
            </div>
            
            <!-- 下一张按钮 -->
            <button 
              v-if="pictureUrls.length > 1" 
              class="preview-nav next" 
              @click="nextImage"
              :disabled="currentImageIndex === pictureUrls.length - 1"
            >
              ›
            </button>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 通知框 -->
    <AlertBox
        v-model:visible="showAlert"
        :title="alertConfig.title"
        :content="alertConfig.content"
        :html-content="alertConfig.htmlContent"
        :confirm-text="alertConfig.confirmText"
        @confirm="handleAlertConfirm"
    />
  </div>
</template>

<style scoped>
.demand-detail-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #62055f 0%, #8b1a86 100%);
  padding: 40px 20px;
}

.detail-container {
  max-width: 900px;
  margin: 0 auto;
  background: white;
  border-radius: 24px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
  overflow: hidden;
}

/* 头部 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 32px;
  border-bottom: 1px solid #e5e4e7;
  background: #faf9fb;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: none;
  border: none;
  border-radius: 8px;
  color: #62055f;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.back-btn:hover {
  background: #f0eaf0;
}

.back-icon {
  font-size: 18px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #62055f;
  margin: 0;
}

.placeholder {
  width: 80px;
}

/* 加载状态 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px;
  color: #666;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #e5e4e7;
  border-top-color: #62055f;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 内容区域 */
.detail-content {
  padding: 32px;
}

/* 发布者卡片 */
.publisher-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 16px;
  margin-bottom: 24px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid rgba(98, 5, 95, 0.1);
}



.publisher-avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  overflow: hidden;
  background: #e5e4e7;
  flex-shrink: 0;
  border: none;
}

.publisher-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.publisher-info {
  flex: 1;
}

.publisher-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.admin-badge {
  font-size: 11px;
  padding: 2px 8px;
  background: #ff9800;
  color: white;
  border-radius: 12px;
  font-weight: 500;
}

.super-admin-badge {
  font-size: 11px;
  padding: 2px 8px;
  background: #f44336;
  color: white;
  border-radius: 12px;
  font-weight: 500;
}

.publisher-stats {
  display: flex;
  gap: 16px;
  font-size: 12px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.stat-label {
  color: #999;
}

.stat-value {
  color: #62055f;
  font-weight: 600;
}

.publisher-arrow {
  font-size: 24px;
  color: #62055f;
  font-weight: 300;
  opacity: 0.6;
  transition: opacity 0.2s ease;
}

.publisher-card:hover .publisher-arrow {
  opacity: 1;
  transform: translateX(2px);
}

/* 状态栏 */
.status-bar {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 16px;
  border-radius: 20px;
  margin-bottom: 20px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-text {
  font-size: 13px;
  font-weight: 500;
}

/* 标题 */
.demand-title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin: 0 0 24px 0;
  line-height: 1.4;
}

/* 信息网格 */
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  background: #f8f9fa;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 28px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.info-label {
  font-size: 12px;
  color: #999;
}

.info-value {
  font-size: 15px;
  color: #333;
  font-weight: 500;
}

.info-value.reward {
  color: #62055f;
  font-size: 18px;
  font-weight: 600;
}

/* 章节 */
.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0 0 16px 0;
  padding-bottom: 12px;
  border-bottom: 2px solid #f0f0f0;
}

.description-section {
  margin-bottom: 28px;
}

.description-content {
  line-height: 1.8;
  color: #555;
  white-space: pre-wrap;
  word-break: break-word;
}

/* 图片网格 */
.images-section {
  margin-bottom: 28px;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 16px;
}

.image-item {
  aspect-ratio: 1;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid #e5e4e7;
  transition: transform 0.2s ease;
}

.image-item:hover {
  transform: scale(1.02);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 操作按钮区域 */
.action-buttons {
  display: flex;
  gap: 16px;
  padding-top: 16px;
  border-top: 1px solid #e5e4e7;
  margin-top: 8px;
}

.action-btn {
  flex: 1;
  padding: 12px;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

/* 发布者按钮样式 */
.edit-btn {
  background: #62055f;
  color: white;
}

.edit-btn:hover {
  background: #7a0e76;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(98, 5, 95, 0.3);
}

.cancel-btn {
  background: #f5f5f5;
  color: #f44336;
  border: 1px solid #ffcdd2;
}

.cancel-btn:hover {
  background: #ffebee;
}

/* 非发布者按钮样式 */
.contact-btn {
  background: #f5f5f5;
  color: #62055f;
  border: 1px solid #e5e4e7;
}

.contact-btn:hover {
  background: #f0eaf0;
  transform: translateY(-1px);
}

.accept-btn {
  background: #62055f;
  color: white;
}

.accept-btn:hover {
  background: #7a0e76;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(98, 5, 95, 0.3);
}

/* 登录提示 */
.login-tip {
  flex: 1;
  text-align: center;
  padding: 12px;
  font-size: 14px;
  color: #666;
  background: #ffffff;
  border-radius: 12px;
}

.login-link {
  background: none;
  border: none;
  color: #62055f;
  font-weight: 600;
  cursor: pointer;
  margin: 0 4px;
  text-decoration: underline;
}

.login-link:hover {
  color: #7a0e76;
}

/* 错误状态 */
.error-state {
  text-align: center;
  padding: 60px 20px;
  color: #666;
}

.back-home-btn {
  margin-top: 20px;
  padding: 10px 24px;
  background: #62055f;
  color: white;
  border: none;
  border-radius: 24px;
  cursor: pointer;
}

/* 图片预览模态框样式 */
.image-preview-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.9);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-container {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-close {
  position: absolute;
  top: 20px;
  right: 30px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: white;
  font-size: 28px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  z-index: 10;
}

.preview-close:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: scale(1.1);
}

.preview-nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: white;
  font-size: 36px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  z-index: 10;
}

.preview-nav:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.3);
  transform: translateY(-50%) scale(1.1);
}

.preview-nav.prev {
  left: 30px;
}

.preview-nav.next {
  right: 30px;
}

.preview-nav:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.preview-image-wrapper {
  max-width: 90vw;
  max-height: 90vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-image-wrapper img {
  max-width: 100%;
  max-height: 90vh;
  object-fit: contain;
  border-radius: 8px;
}

.image-counter {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  padding: 6px 12px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  border-radius: 20px;
  font-size: 14px;
  pointer-events: none;
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 响应式 */
@media (max-width: 640px) {
  .detail-container {
    border-radius: 16px;
  }

  .page-header {
    padding: 16px 20px;
  }

  .detail-content {
    padding: 20px;
  }

  .page-title {
    font-size: 20px;
  }

  .demand-title {
    font-size: 20px;
  }

  .info-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .image-grid {
    grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  }

  .action-buttons {
    flex-direction: column;
  }

  .publisher-card {
    padding: 12px;
  }

  .publisher-avatar {
    width: 48px;
    height: 48px;
  }

  .publisher-name {
    font-size: 14px;
  }

  .publisher-stats {
    font-size: 11px;
    gap: 12px;
  }

  /* 移动端预览样式调整 */
  .preview-nav {
    width: 40px;
    height: 40px;
    font-size: 28px;
  }

  .preview-nav.prev {
    left: 10px;
  }

  .preview-nav.next {
    right: 10px;
  }

  .preview-close {
    top: 10px;
    right: 10px;
    width: 35px;
    height: 35px;
    font-size: 24px;
  }
}
</style>