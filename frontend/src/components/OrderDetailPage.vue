<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import AlertBox from './SmallComponents/AlertBox.vue'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()

const orderId = ref(route.params.id)

// 订单数据
const order = ref(null)
// 相关用户信息
const publisher = ref(null)
const acceptor = ref(null)
// 需求信息
const demand = ref(null)

// 加载状态
const fetching = ref(true)
const updating = ref(false)

// 截止时间编辑
const editingDeadline = ref(false)
const newDeadline = ref('')

// 状态更新相关
const showStatusDialog = ref(false)
const selectedStatus = ref('')
const statusNote = ref('')

// 取消订单相关
const showCancelDialog = ref(false)
const cancelReason = ref('')

// 通知框
const showAlert = ref(false)
const alertConfig = ref({
  title: '系统通知',
  content: '',
  htmlContent: '',
  confirmText: '确定'
})

// 订单状态映射
const statusMap = {
  'ACCEPTED': { label: '已接单', color: '#1890ff', bg: '#e6f7ff' },
  'IN_PROGRESS': { label: '进行中', color: '#faad14', bg: '#fff7e6' },
  'COMPLETED': { label: '已完成', color: '#52c41a', bg: '#f6ffed' },
  'CANCELLED': { label: '已取消', color: '#ff4d4f', bg: '#fff1f0' },
  'DISPUTED': { label: '争议中', color: '#eb2f96', bg: '#fff0f6' }
}

// 可更新的状态列表（接单者视角）
const availableStatuses = [
  { value: 'IN_PROGRESS', label: '开始进行' },
  { value: 'COMPLETED', label: '标记完成' }
]

// 计算当前用户角色
const userRole = computed(() => {
  if (!order.value || !authStore.user) return null
  if (order.value.publisherId === authStore.user.id) return 'publisher'
  if (order.value.acceptorId === authStore.user.id) return 'acceptor'
  return null
})

// 是否可以编辑截止时间（仅接单者且订单未完成/取消）
const canEditDeadline = computed(() => {
  if (userRole.value !== 'acceptor') return false
  const status = order.value?.status
  return status === 'ACCEPTED' || status === 'IN_PROGRESS'
})

// 是否可以更新状态（仅接单者且订单未完成/取消）
const canUpdateStatus = computed(() => {
  if (userRole.value !== 'acceptor') return false
  const status = order.value?.status
  return status === 'ACCEPTED' || status === 'IN_PROGRESS'
})

// 是否可以取消订单（接单者或发布者，且订单未完成/取消）
const canCancel = computed(() => {
  if (!order.value) return false
  const status = order.value?.status
  if (status === 'COMPLETED' || status === 'CANCELLED') return false
  return userRole.value === 'acceptor' || userRole.value === 'publisher'
})

// 显示通知
const showNotification = (title, content, isHtml = false) => {
  alertConfig.value = {
    title: title,
    content: isHtml ? '' : content,
    htmlContent: isHtml ? content : '',
    confirmText: '确定'
  }
  showAlert.value = true
}

// 获取订单详情
const fetchOrderDetail = async () => {
  fetching.value = true
  
  try {
    // 获取订单信息
    const orderResponse = await fetch(`http://localhost:8080/orders/${orderId.value}`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${authStore.token}`
      }
    })
    const orderResult = await orderResponse.json()
    
    if (orderResult.code === 200 && orderResult.data) {
      order.value = orderResult.data
      
      // 并行获取相关用户和需求信息
      const [publisherRes, acceptorRes, demandRes] = await Promise.all([
        fetch(`http://localhost:8080/users/${order.value.publisherId}`, {
          headers: { 'Authorization': `Bearer ${authStore.token}` }
        }),
        fetch(`http://localhost:8080/users/${order.value.acceptorId}`, {
          headers: { 'Authorization': `Bearer ${authStore.token}` }
        }),
        fetch(`http://localhost:8080/demands/${order.value.demandId}`, {
          headers: { 'Authorization': `Bearer ${authStore.token}` }
        })
      ])
      
      const publisherData = await publisherRes.json()
      const acceptorData = await acceptorRes.json()
      const demandData = await demandRes.json()
      
      if (publisherData.code === 200) publisher.value = publisherData.data
      if (acceptorData.code === 200) acceptor.value = acceptorData.data
      if (demandData.code === 200) demand.value = demandData.data
      
      // 初始化截止时间编辑值
      if (order.value.updatedAt) {
        newDeadline.value = formatDateTime(order.value.updatedAt)
      }
    } else {
      throw new Error(orderResult.message || '获取订单失败')
    }
  } catch (error) {
    console.error('获取订单详情失败:', error)
    showNotification('加载失败', error.message || '网络错误，请重试')
    router.back()
  } finally {
    fetching.value = false
  }
}

// 格式化日期时间
const formatDateTime = (dateTimeStr) => {
  if (!dateTimeStr) return ''
  const date = new Date(dateTimeStr)
  return date.toISOString().slice(0, 16)
}

// 格式化显示日期
const formatDisplayDate = (dateTimeStr) => {
  if (!dateTimeStr) return '未设置'
  const date = new Date(dateTimeStr)
  return date.toLocaleString('zh-CN')
}

// 获取状态样式
const getStatusStyle = (status) => {
  const style = statusMap[status] || { label: status, color: '#666', bg: '#f5f5f5' }
  return {
    color: style.color,
    backgroundColor: style.bg
  }
}

// 保存截止时间
const saveDeadline = async () => {
  if (!newDeadline.value) {
    showNotification('提示', '请选择截止时间')
    return
  }
  
  updating.value = true
  
  try {
    // 更新订单备注作为截止时间信息（可根据实际API调整）
    const note = `截止时间：${new Date(newDeadline.value).toLocaleString('zh-CN')}`
    const response = await fetch(`http://localhost:8080/orders/${orderId.value}/note?userId=${authStore.user.id}&note=${encodeURIComponent(note)}`, {
      method: 'PATCH',
      headers: {
        'Authorization': `Bearer ${authStore.token}`
      }
    })
    
    const result = await response.json()
    
    if (result.code === 200) {
      showNotification('成功', '截止时间已更新')
      editingDeadline.value = false
      await fetchOrderDetail()
    } else {
      throw new Error(result.message || '更新失败')
    }
  } catch (error) {
    console.error('更新截止时间失败:', error)
    showNotification('更新失败', error.message || '网络错误，请重试')
  } finally {
    updating.value = false
  }
}

// 打开状态更新对话框
const openStatusDialog = () => {
  selectedStatus.value = ''
  statusNote.value = ''
  showStatusDialog.value = true
}

// 更新订单状态
const updateOrderStatus = async () => {
  if (!selectedStatus.value) {
    showNotification('提示', '请选择要更新的状态')
    return
  }
  
  updating.value = true
  
  try {
    const response = await fetch(`http://localhost:8080/orders/${orderId.value}/status?userId=${authStore.user.id}&status=${selectedStatus.value}&note=${encodeURIComponent(statusNote.value || '')}`, {
      method: 'PATCH',
      headers: {
        'Authorization': `Bearer ${authStore.token}`
      }
    })
    
    const result = await response.json()
    
    if (result.code === 200) {
      showNotification('成功', `订单状态已更新为「${availableStatuses.find(s => s.value === selectedStatus.value)?.label || selectedStatus.value}」`)
      showStatusDialog.value = false
      await fetchOrderDetail()
    } else {
      throw new Error(result.message || '更新失败')
    }
  } catch (error) {
    console.error('更新订单状态失败:', error)
    showNotification('更新失败', error.message || '网络错误，请重试')
  } finally {
    updating.value = false
  }
}

// 打开取消订单对话框
const openCancelDialog = () => {
  cancelReason.value = ''
  showCancelDialog.value = true
}

// 取消订单
const cancelOrder = async () => {
  updating.value = true
  
  try {
    const response = await fetch(`http://localhost:8080/orders/${orderId.value}/cancel?userId=${authStore.user.id}&reason=${encodeURIComponent(cancelReason.value || '用户主动取消')}`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${authStore.token}`
      }
    })
    
    const result = await response.json()
    
    if (result.code === 200) {
      showNotification('成功', '订单已取消')
      showCancelDialog.value = false
      await fetchOrderDetail()
    } else {
      throw new Error(result.message || '取消失败')
    }
  } catch (error) {
    console.error('取消订单失败:', error)
    showNotification('取消失败', error.message || '网络错误，请重试')
  } finally {
    updating.value = false
  }
}

// 联系对方
const handleContact = async () => {
  try{
    const response = await fetch(`http://localhost:8080/conversations?user1Id=${authStore.user.id}&user2Id=${publisher.value.id}`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${authStore.token}`
      }
    })

    if(response.status === 200){
      // const result = await response.json()
      router.push(`/my/conversations`)
    } else {
       showNotification('操作失败', error.message || '网络错误，无法创建会话')
    }
  } catch (error) {
    console.error('联系对方失败:', error)
    showNotification('操作失败', error.message || '网络错误，请重试')
  }
  


}

// 返回上一页
const goBack = () => {
  router.back()
}

// 跳转到需求详情
const goToDemand = () => {
  if (demand.value) {
    router.push(`/demand/${demand.value.id}`)
  }
}

// 跳转到用户主页
const goToUserProfile = (userId) => {
  router.push(`/user/${userId}`)
}

onMounted(() => {
  if (!authStore.isLoggedIn) {
    showNotification('请先登录', '您需要登录后才能查看订单详情')
    router.push('/login')
    return
  }
  fetchOrderDetail()
})
</script>

<template>
  <div class="order-detail-page">
    <div class="order-detail-container">
      <!-- 头部 -->
      <div class="page-header">
        <button class="back-btn" @click="goBack">
          <span class="back-icon">←</span> 返回
        </button>
        <h1 class="page-title">订单详情</h1>
        <div class="placeholder"></div>
      </div>

      <!-- 加载中 -->
      <div v-if="fetching" class="loading-state">
        <div class="loading-spinner"></div>
        <p>加载中...</p>
      </div>

      <!-- 订单内容 -->
      <div v-else-if="order" class="order-content">
        <!-- 订单状态卡片 -->
        <div class="status-card" :style="{ backgroundColor: getStatusStyle(order.status).backgroundColor }">
          <div class="status-badge" :style="{ color: getStatusStyle(order.status).color }">
            {{ getStatusStyle(order.status).label }}
          </div>
          <div class="order-id">订单号：{{ order.id }}</div>
        </div>

        <!-- 基本信息 -->
        <div class="info-section">
          <h3 class="section-title">基本信息</h3>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">原始需求</span>
              <span class="info-value demand-link" @click="goToDemand">
                {{ demand?.title || `需求 #${order.demandId}` }}
                <span class="link-icon"></span>
              </span>
            </div>
            <div class="info-item">
              <span class="info-label">创建时间</span>
              <span class="info-value">{{ formatDisplayDate(order.createdAt) }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">更新时间</span>
              <span class="info-value">{{ formatDisplayDate(order.updatedAt) }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">完成时间</span>
              <span class="info-value">{{ order.completedAt ? formatDisplayDate(order.completedAt) : '未完成' }}</span>
            </div>
            <div class="info-item" v-if="order.latestRequesterNote">
              <span class="info-label">最新备注</span>
              <span class="info-value">{{ order.latestRequesterNote }}</span>
            </div>
            <div class="info-item" v-if="order.commentId">
              <span class="info-label">关联评价</span>
              <span class="info-value">评价 ID: {{ order.commentId }}</span>
            </div>
          </div>
        </div>

        <!-- 参与方信息 -->
        <div class="info-section">
          <h3 class="section-title">参与方信息</h3>
          <div class="participant-grid">
            <div class="participant-card">
              <div class="participant-header">
                <span class="participant-role">发布者</span>
                <button class="contact-btn-small" @click="goToUserProfile(order.publisherId)">
                  查看主页
                </button>
              </div>
              <div class="participant-info">
                <div class="participant-name">
                  {{ publisher?.name || `用户 #${order.publisherId}` }}
                </div>
                <div class="participant-phone" v-if="publisher?.phone">
                  电话：{{ publisher.phone }}
                </div>
              </div>
            </div>
            <div class="participant-card">
              <div class="participant-header">
                <span class="participant-role">接单者</span>
                <button class="contact-btn-small" @click="goToUserProfile(order.acceptorId)">
                  查看主页
                </button>
              </div>
              <div class="participant-info">
                <div class="participant-name">
                  {{ acceptor?.name || `用户 #${order.acceptorId}` }}
                </div>
                <div class="participant-phone" v-if="acceptor?.phone">
                  电话：{{ acceptor.phone }}
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 接单者专属操作区域 -->
        <div v-if="userRole === 'acceptor'" class="action-section">
          <h3 class="section-title">订单操作</h3>
          
          <!-- 截止时间编辑 -->
          <div class="action-card" v-if="canEditDeadline">
            <div class="action-header">
              <span class="action-icon">⏰</span>
              <span class="action-title">截止时间</span>
            </div>
            <div v-if="!editingDeadline" class="action-content">
              <div class="deadline-display">
                <span class="deadline-label">当前截止时间：</span>
                <span class="deadline-value">{{ order.latestRequesterNote?.includes('截止时间') ? order.latestRequesterNote : '未设置' }}</span>
              </div>
              <button class="action-btn outline-btn" @click="editingDeadline = true" :disabled="updating">
                编辑
              </button>
            </div>
            <div v-else class="action-content editing">
              <input type="datetime-local" v-model="newDeadline" class="datetime-input" />
              <div class="edit-actions">
                <button class="action-btn confirm-btn" @click="saveDeadline" :disabled="updating">
                  {{ updating ? '保存中...' : '保存' }}
                </button>
                <button class="action-btn cancel-btn" @click="editingDeadline = false" :disabled="updating">
                  取消
                </button>
              </div>
            </div>
          </div>

          <!-- 状态更新 -->
          <div class="action-card" v-if="canUpdateStatus">
            <div class="action-header">
              <span class="action-icon">🔄</span>
              <span class="action-title">更新状态</span>
            </div>
            <div class="action-content">
              <p class="action-hint">当前状态：{{ getStatusStyle(order.status).label }}</p>
              <button class="action-btn primary-btn" @click="openStatusDialog" :disabled="updating">
                更新订单状态
              </button>
            </div>
          </div>

          <!-- 联系按钮 -->
          <div class="action-card">
            <div class="action-header">
              <span class="action-icon">💬</span>
              <span class="action-title">联系发布者</span>
            </div>
            <div class="action-content">
              <p class="action-hint">如有问题，可通过平台联系发布者</p>
              <button class="action-btn primary-btn" @click="handleContact">
                联系 {{ publisher?.name || '发布者' }}
              </button>
            </div>
          </div>
        </div>

        <!-- 发布者专属操作区域 -->
        <div v-if="userRole === 'publisher'" class="action-section">
          <h3 class="section-title">订单操作</h3>
          
          <!-- 联系按钮 -->
          <div class="action-card">
            <div class="action-header">
              <span class="action-icon">💬</span>
              <span class="action-title">联系接单者</span>
            </div>
            <div class="action-content">
              <p class="action-hint">如有问题，可通过平台联系接单者</p>
              <button class="action-btn primary-btn" @click="handleContact">
                联系 {{ acceptor?.name || '接单者' }}
              </button>
            </div>
          </div>
        </div>

        <!-- 取消订单按钮（双方都可见，符合条件的订单） -->
        <div v-if="canCancel" class="cancel-section">
          <button class="cancel-order-btn" @click="openCancelDialog" :disabled="updating">
            取消订单
          </button>
        </div>
      </div>

      <!-- 订单不存在 -->
      <div v-else class="empty-state">
        <p>订单不存在或已被删除</p>
        <button class="back-home-btn" @click="goBack">返回上一页</button>
      </div>
    </div>

    <!-- 状态更新对话框 -->
    <div v-if="showStatusDialog" class="dialog-overlay" @click.self="showStatusDialog = false">
      <div class="dialog-container">
        <div class="dialog-header">
          <h3>更新订单状态</h3>
          <button class="dialog-close" @click="showStatusDialog = false">×</button>
        </div>
        <div class="dialog-body">
          <div class="form-group">
            <label class="form-label">选择新状态</label>
            <select v-model="selectedStatus" class="form-select">
              <option value="">请选择</option>
              <option v-for="status in availableStatuses" :key="status.value" :value="status.value">
                {{ status.label }}
              </option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">备注说明（可选）</label>
            <textarea v-model="statusNote" class="form-textarea" rows="3" placeholder="请输入备注信息..."></textarea>
          </div>
        </div>
        <div class="dialog-footer">
          <button class="dialog-btn cancel" @click="showStatusDialog = false">取消</button>
          <button class="dialog-btn confirm" @click="updateOrderStatus" :disabled="updating">
            {{ updating ? '处理中...' : '确认更新' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 取消订单对话框 -->
    <div v-if="showCancelDialog" class="dialog-overlay" @click.self="showCancelDialog = false">
      <div class="dialog-container">
        <div class="dialog-header">
          <h3>取消订单</h3>
          <button class="dialog-close" @click="showCancelDialog = false">×</button>
        </div>
        <div class="dialog-body">
          <p class="warning-text">取消订单后无法恢复，请谨慎操作！</p>
          <div class="form-group">
            <label class="form-label">取消原因（可选）</label>
            <textarea v-model="cancelReason" class="form-textarea" rows="3" placeholder="请输入取消原因..."></textarea>
          </div>
        </div>
        <div class="dialog-footer">
          <button class="dialog-btn cancel" @click="showCancelDialog = false">返回</button>
          <button class="dialog-btn danger" @click="cancelOrder" :disabled="updating">
            {{ updating ? '处理中...' : '确认取消' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 通知框 -->
    <AlertBox
      v-model:visible="showAlert"
      :title="alertConfig.title"
      :content="alertConfig.content"
      :html-content="alertConfig.htmlContent"
      :confirm-text="alertConfig.confirmText"
      @confirm="showAlert = false"
    />
  </div>
</template>

<style scoped>
.order-detail-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #62055f 0%, #8b1a86 100%);
  padding: 40px 20px;
}

.order-detail-container {
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
  padding: 24px 32px;
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
  padding: 60px;
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
  to {
    transform: rotate(360deg);
  }
}

/* 订单内容 */
.order-content {
  padding: 0 32px 32px;
}

/* 状态卡片 */
.status-card {
  margin: -1px -32px 24px;
  padding: 20px 32px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e5e4e7;
}

.status-badge {
  font-size: 18px;
  font-weight: 600;
}

.order-id {
  color: #666;
  font-size: 14px;
}

/* 信息区块 */
.info-section {
  margin-bottom: 28px;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 20px;
}

.info-section:last-of-type {
  border-bottom: none;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0 0 16px 0;
  padding-left: 8px;
  border-left: 3px solid #62055f;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 12px;
  color: #999;
}

.info-value {
  font-size: 14px;
  color: #333;
}

.demand-link {
  color: #62055f;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.demand-link:hover {
  text-decoration: underline;
}

.link-icon {
  font-size: 12px;
}

/* 参与方卡片 */
.participant-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.participant-card {
  background: #faf9fb;
  border-radius: 16px;
  padding: 16px;
}

.participant-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.participant-role {
  font-size: 14px;
  font-weight: 600;
  color: #62055f;
}

.contact-btn-small {
  padding: 4px 12px;
  background: white;
  border: 1px solid #62055f;
  border-radius: 16px;
  font-size: 12px;
  color: #62055f;
  cursor: pointer;
  transition: all 0.2s ease;
}

.contact-btn-small:hover {
  background: #62055f;
  color: white;
}

.participant-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.participant-name {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.participant-phone {
  font-size: 13px;
  color: #666;
}

/* 操作区域 */
.action-section {
  margin-top: 8px;
}

.action-card {
  background: #faf9fb;
  border-radius: 16px;
  padding: 16px;
  margin-bottom: 16px;
}

.action-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e5e4e7;
}

.action-icon {
  font-size: 18px;
}

.action-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.action-content {
  padding-left: 28px;
}

.action-hint {
  font-size: 13px;
  color: #999;
  margin-bottom: 12px;
}

.deadline-display {
  margin-bottom: 12px;
  font-size: 14px;
}

.deadline-label {
  color: #666;
}

.deadline-value {
  color: #333;
  font-weight: 500;
}

.datetime-input {
  padding: 10px 12px;
  border: 1px solid #e5e4e7;
  border-radius: 10px;
  font-size: 14px;
  width: 100%;
  margin-bottom: 12px;
  outline: none;
  background-color: #ffffff;
  color: #000;
}

.datetime-input:focus {
  border-color: #62055f;
  box-shadow: 0 0 0 2px rgba(98, 5, 95, 0.1);
}

.edit-actions {
  display: flex;
  gap: 12px;
}

.action-btn {
  padding: 8px 20px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
}

.action-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.primary-btn {
  background: #62055f;
  color: white;
}

.primary-btn:hover:not(:disabled) {
  background: #7a0e76;
  transform: translateY(-1px);
}

.outline-btn {
  background: white;
  border: 1px solid #62055f;
  color: #62055f;
}

.outline-btn:hover:not(:disabled) {
  background: #f0eaf0;
}

.confirm-btn {
  background: #52c41a;
  color: white;
}

.confirm-btn:hover:not(:disabled) {
  background: #389e0d;
}

.cancel-btn {
  background: #f5f5f5;
  color: #666;
  border: 1px solid #e5e4e7;
}

.cancel-btn:hover:not(:disabled) {
  background: #e5e4e7;
}

/* 取消订单区域 */
.cancel-section {
  margin-top: 24px;
  text-align: center;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.cancel-order-btn {
  padding: 10px 24px;
  background: white;
  border: 1px solid #ff4d4f;
  border-radius: 24px;
  font-size: 14px;
  color: #ff4d4f;
  cursor: pointer;
  transition: all 0.2s ease;
}

.cancel-order-btn:hover:not(:disabled) {
  background: #fff1f0;
  border-color: #ff7875;
  color: #ff7875;
}

.cancel-order-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 空状态 */
.empty-state {
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

/* 对话框 */
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.dialog-container {
  background: white;
  border-radius: 20px;
  width: 90%;
  max-width: 480px;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e5e4e7;
  background: #faf9fb;
}

.dialog-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.dialog-close {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
}

.dialog-body {
  padding: 20px;
}

.warning-text {
  color: #ff4d4f;
  font-size: 14px;
  margin-bottom: 16px;
  padding: 8px 12px;
  background: #fff1f0;
  border-radius: 8px;
}

.form-group {
  margin-bottom: 16px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 8px;
}

.form-select {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e5e4e7;
  border-radius: 10px;
  font-size: 14px;
  outline: none;
}

.form-select:focus {
  border-color: #62055f;
}

.form-textarea {
  width: 100%;
  padding: 10px 2px;
  border: 1px solid #e5e4e7;
  border-radius: 10px;
  font-size: 14px;
  resize: vertical;
  font-family: inherit;
  outline: none;
  background-color: #ffffff;
  color: #000;
}

.form-textarea:focus {
  border-color: #62055f;
}

.dialog-footer {
  display: flex;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid #e5e4e7;
  background: #faf9fb;
}

.dialog-btn {
  flex: 1;
  padding: 10px;
  border: none;
  border-radius: 24px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.dialog-btn.cancel {
  background: #f5f5f5;
  color: #666;
}

.dialog-btn.confirm {
  background: #62055f;
  color: white;
}

.dialog-btn.danger {
  background: #ff4d4f;
  color: white;
}

.dialog-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 响应式 */
@media (max-width: 640px) {
  .order-detail-container {
    border-radius: 16px;
  }
  
  .page-header {
    padding: 16px 20px;
  }
  
  .order-content {
    padding: 0 20px 20px;
  }
  
  .status-card {
    margin: -1px -20px 20px;
    padding: 16px 20px;
  }
  
  .page-title {
    font-size: 20px;
  }
  
  .info-grid,
  .participant-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  
  .action-content {
    padding-left: 0;
  }
}
</style>