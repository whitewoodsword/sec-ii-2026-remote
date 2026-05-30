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
// 联系对方
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
        // 获取到对话信息，跳转到会话详情页
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


// 接取订单
const handleAccept = async () => {
  // TODO: 实现接取订单功能
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

onMounted(() => {
  fetchDemand()
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
              @click="window.open('http://localhost:8080' + url, '_blank')"
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
          <template v-else-if="canAccept">
            
            <button class="action-btn accept-btn" @click="handleAccept">
              📋 接取订单
            </button>
          </template>

          <!-- 未登录提示 -->
          <div v-else-if="!authStore.isLoggedIn && demand.status === 'PENDING'" class="login-tip">
            <span>请先</span>
            <button class="login-link" @click="router.push('/login')">登录</button>
            <span>后联系发布者或接取订单</span>
          </div>

          <button class="action-btn contact-btn" @click="handleContact">
              💬 联系TA
            </button>
        </div>
      </div>

      <!-- 错误状态 -->
      <div v-else class="error-state">
        <p>需求不存在或已被删除</p>
        <button class="back-home-btn" @click="router.push('/demands')">返回列表</button>
      </div>
    </div>

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
  background: #faf9fb;
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
}
</style>