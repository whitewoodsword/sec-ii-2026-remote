<template>
  <div class="my-orders-page">
    <div class="page-container">
      <!-- 页面头部 -->
      <div class="page-header">
        <div>
          <h1 class="page-title">我的订单</h1>
          <p class="page-subtitle">查看和管理您的所有订单记录</p>
        </div>
      </div>

      <!-- 双栏布局 -->
      <div class="two-column-layout">
        <!-- 左侧：我的订单 -->
        <div class="orders-panel">
          <div class="panel-header">
            <h2 class="panel-title">我的订单</h2>
            <div class="filter-bar">
              <div class="filter-group">
                <label class="filter-label">身份</label>
                <select v-model="filters.role" class="filter-select" @change="resetAndSearch">
                  <option value="all">全部</option>
                  <option value="publisher">我发布的</option>
                  <option value="acceptor">我接受的</option>
                </select>
              </div>
              <div class="filter-group">
                <label class="filter-label">状态</label>
                <select v-model="filters.status" class="filter-select" @change="searchOrders">
                  <option value="">全部</option>
                  <option value="ACCEPTED">已接单</option>
                  <option value="IN_PROGRESS">进行中</option>
                  <option value="COMPLETED">已完成</option>
                  <option value="CANCELLED">已取消</option>
                  <option value="DISPUTED">争议中</option>
                </select>
              </div>
              <div class="filter-group">
                <label class="filter-label">时间</label>
                <select v-model="filters.timeRange" class="filter-select" @change="searchOrders">
                  <option value="">全部</option>
                  <option value="today">今天</option>
                  <option value="week">本周</option>
                  <option value="month">本月</option>
                  <option value="threeMonths">三个月内</option>
                </select>
              </div>
              <button class="reset-btn" @click="resetFilters">重置</button>
            </div>
          </div>

          <div v-if="ordersLoading" class="loading-state">
            <div class="loading-spinner"></div>
            <p>加载中...</p>
          </div>
          <div v-else-if="orders.length === 0" class="empty-state">
            <p>暂无订单记录</p>
          </div>
          <div v-else class="orders-list">
            <div 
              v-for="order in orders" 
              :key="order.id" 
              class="order-card"
              @click="viewOrderDetail(order)"
            >
              <div class="order-header">
                <div class="order-info">
                  <span class="order-role" :class="getRoleClass(order)">
                    {{ getRoleLabel(order) }}
                  </span>
                  <span class="order-status" :class="getStatusClass(order.status)">
                    {{ getStatusLabel(order.status) }}
                  </span>
                </div>
                <span class="order-date">{{ formatDate(order.createdAt) }}</span>
              </div>
              <div class="order-body">
                <div class="order-demand-info">
                  <h3 class="order-demand-title">需求 #{{ order.demandId }}</h3>
                  <p class="order-partner">{{ getPartnerLabel(order) }}：{{ getPartnerName(order) }}</p>
                </div>
                <div class="order-actions" @click.stop>
      
                  <button 
                    v-if="order.status === 'IN_PROGRESS'"
                    class="action-btn detail-btn"
                    @click="viewOrderDetail(order)"
                  >
                    查看详情
                  </button>
                  <button 
                    v-if="order.status === 'ACCEPTED' && order.acceptorId === authStore.user?.id"
                    class="action-btn start-btn"
                    @click="startOrder(order)"
                  >
                    开始服务
                  </button>
                </div>
              </div>
            </div>
          </div>

          <pagination
            v-if="ordersTotalPages > 0"
            :current-page="ordersPage"
            :total-pages="ordersTotalPages"
            @page-change="handleOrdersPageChange"
          />
        </div>

        <!-- 右侧：待评价订单 -->
        <div class="review-panel">
          <div class="panel-header">
            <div class="panel-title-wrapper">
              <h2 class="panel-title">待评价订单</h2>
              <span class="badge">{{ reviewOrdersTotal }}</span>
            </div>
          </div>

          <div v-if="reviewLoading" class="loading-state small">
            <div class="loading-spinner small"></div>
            <p>加载中...</p>
          </div>
          <div v-else-if="reviewOrders.length === 0" class="empty-state small">
            <p>暂无待评价订单</p>
          </div>
          <div v-else class="review-list">
            <div 
              v-for="order in reviewOrders" 
              :key="order.id" 
              class="review-card"
              @click="viewOrderDetail(order)"
            >
              <div class="review-info">
                <div>
                  <div class="review-demand-id">需求 #{{ order.demandId }}</div>
                  <div class="review-date">{{ formatDate(order.completedAt || order.updatedAt) }}</div>
                </div>
                <button class="review-action-btn" @click.stop="openReviewModal(order)">
                  写评价
                </button>
              </div>
            </div>
          </div>

          <Pagination
            v-if="reviewTotalPages > 0"
            :current-page="reviewPage"
            :total-pages="reviewTotalPages"
            @page-change="handleReviewPageChange"
          />
        </div>
      </div>
    </div>

    <!-- 评价弹窗 -->
    <div v-if="showReviewModal" class="modal-overlay" @click.self="closeReviewModal">
      <div class="modal-container">
        <div class="modal-header">
          <h3>发表评价</h3>
          <button class="modal-close" @click="closeReviewModal">&times;</button>
        </div>
        <div class="modal-body">
          <div class="rating-section">
            <label class="rating-label">评分</label>
            <div class="stars">
              <span 
                v-for="star in 5" 
                :key="star"
                class="star"
                :class="{ active: star <= reviewForm.rating }"
                @click="reviewForm.rating = star"
              >★</span>
            </div>
          </div>
          <div class="comment-section">
            <label class="comment-label">评价内容</label>
            <textarea
              v-model="reviewForm.content"
              class="comment-input"
              rows="4"
              placeholder="请分享您的服务体验..."
              maxlength="500"
            ></textarea>
            <span class="char-count">{{ reviewForm.content.length }}/500</span>
          </div>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="closeReviewModal">取消</button>
          <button class="submit-btn" @click="submitReview" :disabled="reviewSubmitting">
            {{ reviewSubmitting ? '提交中...' : '提交评价' }}
          </button>
        </div>
      </div>
    </div>

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
import { ref, reactive, onMounted } from 'vue'
import { useAuthStore } from '../stores/auth'
import { useRouter } from 'vue-router'
import AlertBox from './SmallComponents/AlertBox.vue'
import Pagination from './SmallComponents/Pagination.vue'

const authStore = useAuthStore()
const router = useRouter()

// 筛选条件
const filters = reactive({
  role: 'all',
  status: '',
  timeRange: ''
})

// 订单数据
const orders = ref([])
const ordersLoading = ref(false)
const ordersPage = ref(0)
const ordersTotalPages = ref(0)

// 待评价订单
const reviewOrders = ref([])
const reviewLoading = ref(false)
const reviewPage = ref(0)
const reviewTotalPages = ref(0)
const reviewOrdersTotal = ref(0)

// 评价弹窗
const showReviewModal = ref(false)
const reviewSubmitting = ref(false)
const currentOrderForReview = ref(null)
const reviewForm = reactive({ rating: 5, content: '' })

// 通知
const showAlert = ref(false)
const alertConfig = ref({ title: '系统通知', content: '', confirmText: '确定' })

const showNotification = (title, content) => {
  alertConfig.value = { title, content, confirmText: '确定' }
  showAlert.value = true
}

const handleAlertConfirm = () => { showAlert.value = false }

// 辅助函数
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

const getRoleLabel = (order) => {
  if (order.publisherId === authStore.user?.id) return '我发布的'
  if (order.acceptorId === authStore.user?.id) return '我接单的'
  return '未知'
}

const getRoleClass = (order) => order.publisherId === authStore.user?.id ? 'role-publisher' : 'role-acceptor'

const getPartnerLabel = (order) => order.publisherId === authStore.user?.id ? '接单方' : '发布方'

const getPartnerName = (order) => {
  return order.partnerName || (order.publisherId === authStore.user?.id ? `用户${order.acceptorId}` : `用户${order.publisherId}`)
}

const getStatusLabel = (status) => {
  const map = { 'ACCEPTED': '已接单', 'IN_PROGRESS': '进行中', 'COMPLETED': '已完成', 'CANCELLED': '已取消', 'DISPUTED': '争议中' }
  return map[status] || status
}

const getStatusClass = (status) => {
  const map = { 'ACCEPTED': 'status-accepted', 'IN_PROGRESS': 'status-progress', 'COMPLETED': 'status-completed', 'CANCELLED': 'status-cancelled', 'DISPUTED': 'status-disputed' }
  return map[status] || ''
}

const canReview = (order) => order.status === 'COMPLETED' && !order.commentId && (order.publisherId === authStore.user?.id || order.acceptorId === authStore.user?.id)

// API 调用
const searchOrders = async () => {
  ordersLoading.value = true
  try {
    const params = new URLSearchParams({ page: ordersPage.value, size: 10, sortBy: 'createdAt', direction: 'desc' })
    if (filters.status) params.append('status', filters.status)
    
    let url = ''
    if (filters.role === 'publisher') url = `/orders/publisher/${authStore.user.id}`
    else if (filters.role === 'acceptor') url = `/orders/acceptor/${authStore.user.id}`
    else { url = `/orders/user/${authStore.user.id}`; params.append('role', 'all') }
    
    const response = await fetch(`http://localhost:8080${url}?${params.toString()}`, {
      headers: { 'Authorization': `Bearer ${authStore.token}` }
    })
    const result = await response.json()
    
    if (result.code === 200 && result.data) {
      orders.value = result.data.content || []
      ordersTotalPages.value = result.data.totalPages || 0
    } else throw new Error(result.message || '获取订单失败')
  } catch (error) {
    console.error('获取订单失败:', error)
    showNotification('加载失败', error.message)
    orders.value = []
  } finally {
    ordersLoading.value = false
  }
}

// 替换原有的 fetchReviewOrders 函数
const fetchReviewOrders = async () => {
  reviewLoading.value = true
  try {
    // 获取所有已完成的订单（不分页获取更多，或者分页但需要处理）
    const params = new URLSearchParams({ 
      page: 0, 
      size: 100,  // 获取足够多的已完成订单
      status: 'COMPLETED', 
      sortBy: 'completedAt', 
      direction: 'desc' 
    })
    const response = await fetch(`http://localhost:8080/orders/user/${authStore.user.id}?${params.toString()}`, {
      headers: { 'Authorization': `Bearer ${authStore.token}` }
    })
    const result = await response.json()
    
    if (result.code === 200 && result.data) {
      const allCompletedOrders = result.data.content || []
      
      // 筛选出当前用户尚未评价的订单
      const unreviewedOrders = []
      for (const order of allCompletedOrders) {
        // 检查当前用户是否已经评价过这个订单
        const reviewCheckResponse = await fetch(
          `http://localhost:8080/reviews/user-order?orderId=${order.id}&userId=${authStore.user.id}`,
          {
            headers: { 'Authorization': `Bearer ${authStore.token}` }
          }
        )
        const reviewCheckResult = await reviewCheckResponse.json()
        
        // 如果返回404表示未评价，200表示已评价
        if (reviewCheckResult.code === 404) {
          unreviewedOrders.push(order)
        }
      }
      
      reviewOrders.value = unreviewedOrders
      reviewOrdersTotal.value = unreviewedOrders.length
      reviewTotalPages.value = Math.ceil(reviewOrdersTotal.value / 5)
    }
  } catch (error) {
    console.error('获取待评价订单失败:', error)
    reviewOrders.value = []
  } finally {
    reviewLoading.value = false
  }
}

const resetAndSearch = () => { ordersPage.value = 0; searchOrders() }
const resetFilters = () => { filters.role = 'all'; filters.status = ''; filters.timeRange = ''; resetAndSearch() }
const handleOrdersPageChange = (page) => { ordersPage.value = page; searchOrders() }
const handleReviewPageChange = (page) => { reviewPage.value = page; fetchReviewOrders() }

const viewOrderDetail = (order) => { router.push(`/order/${order.id}`) }

const startOrder = async (order) => {
  try {
    const response = await fetch(`http://localhost:8080/orders/${order.id}/status?userId=${authStore.user.id}&status=IN_PROGRESS`, {
      method: 'PATCH',
      headers: { 'Authorization': `Bearer ${authStore.token}` }
    })
    const result = await response.json()
    if (result.code === 200) {
      showNotification('成功', '订单状态已更新为进行中')
      searchOrders()
    } else throw new Error(result.message)
  } catch (error) {
    showNotification('操作失败', error.message)
  }
}

const openReviewModal = (order) => {
  currentOrderForReview.value = order
  reviewForm.rating = 5
  reviewForm.content = ''
  showReviewModal.value = true
}

const closeReviewModal = () => {
  showReviewModal.value = false
  currentOrderForReview.value = null
}

const submitReview = async () => {
  if (!reviewForm.content.trim()) {
    showNotification('提示', '请填写评价内容')
    return
  }
  
  reviewSubmitting.value = true
  try {
    const order = currentOrderForReview.value
    // 确定被评价者ID（评价对方，不是自己）
    const reviewedId = order.publisherId === authStore.user?.id 
      ? order.acceptorId   // 发布者评价接单者
      : order.publisherId  // 接单者评价发布者
    
    // 调用正确的 API：POST /reviews/create 使用 Query 参数
    const params = new URLSearchParams({
      orderId: order.id,
      userId: authStore.user.id,
      score: reviewForm.rating,
      content: reviewForm.content
    })
    
    const response = await fetch(`http://localhost:8080/reviews/create?${params.toString()}`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${authStore.token}`
      }
    })
    const result = await response.json()
    
    if (result.code === 200 || result.code === 201) {
      showNotification('评价成功', '感谢您的评价！')
      closeReviewModal()
      await searchOrders()
      await fetchReviewOrders()
    } else {
      throw new Error(result.message || '评价失败')
    }
  } catch (error) {
    console.error('评价失败:', error)
    showNotification('评价失败', error.message || '网络错误，请重试')
  } finally {
    reviewSubmitting.value = false
  }
}

onMounted(() => {
  if (!authStore.isLoggedIn) {
    showNotification('请先登录', '您需要登录后才能查看订单')
    router.push('/login')
    return
  }
  searchOrders()
  fetchReviewOrders()
})
</script>

<style scoped>
.my-orders-page {
  min-height: 100vh;
  background: #f0f2f6;
  padding: 32px 24px;
}

.page-container {
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
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

.two-column-layout {
  display: flex;
  gap: 28px;
}

.orders-panel {
  flex: 2;
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

.review-panel {
  flex: 1;
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

.panel-header {
  padding: 20px 24px;
  border-bottom: 1px solid #edf2f7;
  background: #ffffff;
}

.panel-title-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
}

.panel-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0;
}

.badge {
  background: #62055f;
  color: white;
  font-size: 12px;
  font-weight: 500;
  padding: 2px 10px;
  border-radius: 20px;
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  align-items: flex-end;
  margin-top: 16px;
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
  padding: 8px 28px 8px 12px;
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

.orders-list {
  padding: 8px 0;
}

.order-card {
  padding: 18px 24px;
  margin: 0;
  border-bottom: 1px solid #edf2f7;
  cursor: pointer;
  transition: all 0.2s;
}

.order-card:hover {
  background: #fafbfc;
  transform: translateX(2px);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.order-info {
  display: flex;
  gap: 12px;
  align-items: center;
}

.order-role {
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 20px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.role-publisher {
  background: #e8edff;
  color: #3b5bdb;
}

.role-acceptor {
  background: #e6f7ec;
  color: #2b8c4a;
}

.order-status {
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 20px;
  font-weight: 600;
}

.status-accepted { background: #fff8e7; color: #d97706; }
.status-progress { background: #e8f0fe; color: #1e6f9f; }
.status-completed { background: #e6f7ec; color: #2b8c4a; }
.status-cancelled { background: #fee9e6; color: #d9381e; }
.status-disputed { background: #fef3f2; color: #c0392b; }

.order-date {
  font-size: 12px;
  color: #94a3b8;
}

.order-body {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.order-demand-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 4px 0;
}

.order-partner {
  font-size: 13px;
  color: #64748b;
  margin: 0;
}

.order-actions {
  display: flex;
  gap: 10px;
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

.review-btn {
  background: #62055f;
  color: white;
}

.review-btn:hover {
  background: #7a0e76;
  transform: translateY(-1px);
}

.detail-btn, .start-btn {
  background: #f1f3f5;
  color: #495057;
}

.detail-btn:hover, .start-btn:hover {
  background: #e9ecef;
}

.review-list {
  padding: 8px 0;
}

.review-card {
  padding: 16px 20px;
  border-bottom: 1px solid #edf2f7;
  cursor: pointer;
  transition: background 0.2s;
}

.review-card:hover {
  background: #fafbfc;
}

.review-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.review-demand-id {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
  margin-bottom: 4px;
}

.review-date {
  font-size: 11px;
  color: #94a3b8;
}

.review-action-btn {
  padding: 6px 18px;
  background: #62055f;
  color: white;
  border: none;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.review-action-btn:hover {
  background: #7a0e76;
  transform: scale(0.98);
}

.loading-state, .empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #94a3b8;
}

.loading-state.small, .empty-state.small {
  padding: 40px 20px;
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

.loading-spinner.small {
  width: 28px;
  height: 28px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-container {
  background: white;
  border-radius: 20px;
  width: 90%;
  max-width: 500px;
  overflow: hidden;
  box-shadow: 0 20px 35px -8px rgba(0, 0, 0, 0.2);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px;
  border-bottom: 1px solid #edf2f7;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1a1a2e;
}

.modal-close {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #94a3b8;
  transition: color 0.2s;
}

.modal-close:hover {
  color: #475569;
}

.modal-body {
  padding: 24px;
}

.rating-section {
  margin-bottom: 24px;
}

.rating-label, .comment-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #334155;
  margin-bottom: 10px;
}

.stars {
  display: flex;
  gap: 6px;
}

.star {
  font-size: 30px;
  cursor: pointer;
  color: #cbd5e1;
  transition: color 0.2s;
}

.star.active {
  color: #f59e0b;
}

.comment-input {
  width: 100%;
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  font-size: 14px;
  resize: vertical;
  font-family: inherit;
  background: #ffffff;
  color: #000000;
  transition: all 0.2s;
}

.comment-input:focus {
  outline: none;
  border-color: #62055f;
  box-shadow: 0 0 0 3px rgba(98, 5, 95, 0.1);
}

.char-count {
  display: block;
  text-align: right;
  font-size: 11px;
  color: #94a3b8;
  margin-top: 6px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid #edf2f7;
}

.modal-footer .cancel-btn {
  padding: 8px 20px;
  background: #f1f3f5;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}

.modal-footer .submit-btn {
  padding: 8px 24px;
  background: #62055f;
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.modal-footer .submit-btn:hover:not(:disabled) {
  background: #7a0e76;
}

.modal-footer .submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 响应式 */
@media (max-width: 900px) {
  .two-column-layout {
    flex-direction: column;
  }
  
  .my-orders-page {
    padding: 24px 16px;
  }
}

@media (max-width: 640px) {
  .order-body {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .filter-bar {
    width: 100%;
  }
  
  .filter-group {
    flex: 1;
  }
  
  .filter-select {
    width: 100%;
  }
  
  .panel-header {
    padding: 16px;
  }
  
  .order-card {
    padding: 14px 16px;
  }
}
</style>