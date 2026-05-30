<template>
  <div class="order-workbench-page" @click="closeDropdowns">
    <header class="fixed-header">
      <div class="header-left">
        <h1 class="header-headline" @click="goHome">校园互助服务平台</h1>
      </div>
      <div class="header-right">
        <div class="user-info">{{ currentUser?.name || '用户' }}</div>
        <div class="avatar-wrapper">
          <img 
            :src="currentUserAvatar" 
            alt="用户头像"
            class="avatar"
            :class="{ 'avatar-active': showDropdown }"
            @click="toggleDropdown"
          />
        </div>
        
        <transition name="dropdown">
          <div v-if="showDropdown" class="dropdown-menu">
            <button class="dropdown-item" @click="goToProfile">
              个人主页
            </button>
            <div class="dropdown-divider"></div>
            <button class="dropdown-item" @click="handleMessages">
              消息
            </button>
            <div class="dropdown-divider"></div>
            <button class="dropdown-item logout-item" @click="handleLogout">
              退出登录
            </button>
          </div>
        </transition>
      </div>
    </header>

    <main class="main-content">
      <div class="workbench-container">
        <!-- 页面标题区 -->
        <div class="page-header">
          <h1 class="page-title">订单工作台</h1>
          <p class="page-subtitle">需求发布 · 申请接单 · 订单管理 · 进度跟踪</p>
        </div>

        <!-- 筛选工具栏 -->
        <div class="toolbar-card">
          <div class="toolbar-left">
            <div class="filter-group">
              <label class="filter-label">查看用户</label>
              <select v-model.number="currentUserId" class="filter-select">
                <option v-for="user in users" :key="user.id" :value="user.id">
                  {{ user.name }} · {{ user.roleLabel }}
                </option>
              </select>
            </div>
            <div class="filter-group">
              <label class="filter-label">需求分类</label>
              <select v-model="category" class="filter-select">
                <option v-for="option in categoryOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
            </div>
          </div>
          <button class="refresh-btn" :disabled="loading" @click="refreshAll()">
            <span class="refresh-icon">⟳</span>
            {{ loading ? '刷新中...' : '刷新工作台' }}
          </button>
        </div>

        <!-- 统计卡片区 -->
        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-icon">👤</div>
            <div class="stat-info">
              <span class="stat-label">当前用户</span>
              <strong class="stat-value">{{ currentUser?.name ?? '未选择' }}</strong>
              <p class="stat-desc">{{ currentUser?.academy ?? '请选择演示用户' }}</p>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon">📋</div>
            <div class="stat-info">
              <span class="stat-label">公开需求</span>
              <strong class="stat-value">{{ openRequestCount }}</strong>
              <p class="stat-desc">当前筛选下可申请的需求</p>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon">🔄</div>
            <div class="stat-info">
              <span class="stat-label">进行中订单</span>
              <strong class="stat-value">{{ activeOrderCount }}</strong>
              <p class="stat-desc">尚未完成的订单</p>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon">📝</div>
            <div class="stat-info">
              <span class="stat-label">我的需求</span>
              <strong class="stat-value">{{ ownedRequestCount }}</strong>
              <p class="stat-desc">我发布的需求数量</p>
            </div>
          </div>
        </div>

        <!-- 通知横幅 -->
        <transition name="fade">
          <div v-if="banner.text" class="banner" :class="banner.type">
            <span class="banner-icon">{{ banner.type === 'success' ? '✓' : '⚠' }}</span>
            <span>{{ banner.text }}</span>
            <button class="banner-close" @click="banner = { type: '', text: '' }">×</button>
          </div>
        </transition>

        <!-- 主工作区：三列布局 -->
        <div class="workspace-grid">
          <!-- 左侧：需求列表 -->
          <div class="panel">
            <div class="panel-header">
              <div>
                <span class="panel-badge">需求池</span>
                <h3 class="panel-title">公开需求</h3>
              </div>
              <span class="panel-count">{{ requests.length }}</span>
            </div>

            <div class="request-list">
              <div
                v-for="request in requests"
                :key="request.id"
                class="request-item"
                :class="{ active: selectedRequestId === request.id }"
                @click="selectRequest(request.id)"
              >
                <div class="request-header">
                  <span class="category-tag">{{ request.category }}</span>
                  <span class="status-tag" :class="getStatusClass(request.status)">
                    {{ request.statusLabel }}
                  </span>
                </div>
                <h4 class="request-title">{{ request.title }}</h4>
                <div class="request-meta">
                  <span>📍 {{ request.location }}</span>
                  <span>💰 {{ request.reward }}</span>
                </div>
                <div class="request-meta">
                  <span>👤 {{ request.requesterName }}</span>
                  <span>⏰ {{ formatTime(request.serviceTime) }}</span>
                </div>
                <div class="request-tags">
                  <span v-if="request.ownedByViewer" class="tag tag-owner">我发布的</span>
                  <span v-if="request.canApply" class="tag tag-apply">可申请</span>
                  <span class="tag tag-applications">{{ request.applicationCount }} 份申请</span>
                </div>
              </div>

              <div v-if="!requests.length && !loading" class="empty-placeholder">
                <div class="empty-icon">📭</div>
                <p>暂无公开需求</p>
                <span>试试切换分类或稍后再来</span>
              </div>
            </div>
          </div>

          <!-- 中间：需求详情 -->
          <div class="panel">
            <div class="panel-header">
              <div>
                <span class="panel-badge">需求详情</span>
                <h3 class="panel-title">选中的需求</h3>
              </div>
              <span v-if="requestDetail" class="status-tag" :class="getStatusClass(requestDetail.status)">
                {{ requestDetail.statusLabel }}
              </span>
            </div>

            <div v-if="requestDetail" class="detail-content">
              <div class="detail-header">
                <div>
                  <h4 class="detail-title">{{ requestDetail.title }}</h4>
                  <p class="detail-requester">发布者：{{ requestDetail.requesterName }}</p>
                </div>
                <button 
                  v-if="requestDetail.currentOrderId"
                  class="link-btn"
                  @click="selectOrder(requestDetail.currentOrderId)"
                >
                  查看关联订单 →
                </button>
              </div>

              <div class="info-grid">
                <div class="info-item">
                  <span class="info-label">需求分类</span>
                  <strong>{{ requestDetail.category }}</strong>
                </div>
                <div class="info-item">
                  <span class="info-label">服务地点</span>
                  <strong>{{ requestDetail.location }}</strong>
                </div>
                <div class="info-item">
                  <span class="info-label">服务时间</span>
                  <strong>{{ formatTime(requestDetail.serviceTime) }}</strong>
                </div>
                <div class="info-item">
                  <span class="info-label">酬劳</span>
                  <strong>{{ requestDetail.reward }}</strong>
                </div>
              </div>

              <div class="description-box">
                <p>{{ requestDetail.description }}</p>
              </div>

              <div v-if="requestDetail.selectedProviderName" class="selected-provider">
                <span class="selected-icon">✓</span>
                <div>
                  <span class="selected-label">已选定服务者</span>
                  <strong>{{ requestDetail.selectedProviderName }}</strong>
                </div>
              </div>

              <!-- 申请表单 -->
              <div v-if="requestDetail.canApply" class="apply-section">
                <label class="form-label">申请留言</label>
                <textarea
                  v-model="applyMessage"
                  rows="3"
                  class="form-textarea"
                  placeholder="例：我正好有时间可以帮忙，请考虑我的申请..."
                ></textarea>
                <button class="btn-primary" :disabled="loading" @click="submitApplication">
                  {{ loading ? '提交中...' : '提交申请' }}
                </button>
              </div>

              <!-- 申请列表 -->
              <div class="applications-section">
                <div class="section-header">
                  <h5>申请列表</h5>
                  <span class="section-count">{{ requestDetail.applications?.length || 0 }}</span>
                </div>

                <div v-if="requestDetail.applications?.length" class="applications-list">
                  <div
                    v-for="application in requestDetail.applications"
                    :key="application.id"
                    class="application-item"
                  >
                    <div class="application-header">
                      <div>
                        <strong>{{ application.applicantName }}</strong>
                        <span class="applicant-academy">{{ application.applicantAcademy }}</span>
                      </div>
                      <span class="status-tag small" :class="getStatusClass(application.status)">
                        {{ application.statusLabel }}
                      </span>
                    </div>
                    <p class="application-message">{{ application.message }}</p>
                    <div class="application-footer">
                      <span class="application-time">{{ formatTime(application.createdAt) }}</span>
                      <button
                        v-if="application.selectable"
                        class="btn-outline small"
                        :disabled="loading"
                        @click="chooseApplicant(application.id)"
                      >
                        选定服务者
                      </button>
                    </div>
                  </div>
                </div>

                <div v-else class="empty-inline">
                  <span>暂无申请记录</span>
                </div>
              </div>
            </div>

            <div v-else class="empty-placeholder">
              <div class="empty-icon">📌</div>
              <p>未选中需求</p>
              <span>从左侧列表选择一个需求查看详情</span>
            </div>
          </div>

          <!-- 右侧：订单列表 -->
          <div class="panel">
            <div class="panel-header">
              <div>
                <span class="panel-badge">订单管理</span>
                <h3 class="panel-title">我的订单</h3>
              </div>
              <span class="panel-count">{{ orders.length }}</span>
            </div>

            <div class="order-list">
              <div
                v-for="order in orders"
                :key="order.id"
                class="order-item"
                :class="{ active: selectedOrderId === order.id }"
                @click="selectOrder(order.id)"
              >
                <div class="order-header">
                  <span class="category-tag small">{{ order.category }}</span>
                  <span class="status-tag" :class="getStatusClass(order.status)">
                    {{ order.statusLabel }}
                  </span>
                </div>
                <h4 class="order-title">{{ order.requestTitle }}</h4>
                <div class="order-meta">
                  <span>👥 {{ order.counterpartName }}</span>
                  <span>🕐 {{ formatTime(order.updatedAt) }}</span>
                </div>
              </div>

              <div v-if="!orders.length && !loading" class="empty-placeholder">
                <div class="empty-icon">📦</div>
                <p>暂无订单</p>
                <span>申请需求后会自动创建订单</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 底部：订单详情区 -->
        <div class="order-detail-panel">
          <div class="panel-header">
            <div>
              <span class="panel-badge">订单流水</span>
              <h3 class="panel-title">订单详情</h3>
            </div>
            <span v-if="orderDetail" class="status-tag" :class="getStatusClass(orderDetail.status)">
              {{ orderDetail.statusLabel }}
            </span>
          </div>

          <div v-if="orderDetail" class="order-detail-content">
            <div class="order-detail-header">
              <div>
                <h4 class="detail-title">{{ orderDetail.requestTitle }}</h4>
                <p>{{ orderDetail.location }} · {{ orderDetail.reward }}</p>
              </div>
              <button class="link-btn" @click="selectRequest(orderDetail.requestId)">
                ← 返回需求
              </button>
            </div>

            <div class="info-grid">
              <div class="info-item">
                <span class="info-label">需求方</span>
                <strong>{{ orderDetail.requesterName }}</strong>
              </div>
              <div class="info-item">
                <span class="info-label">服务方</span>
                <strong>{{ orderDetail.providerName }}</strong>
              </div>
              <div class="info-item">
                <span class="info-label">服务时间</span>
                <strong>{{ formatTime(orderDetail.serviceTime) }}</strong>
              </div>
              <div class="info-item">
                <span class="info-label">最后更新</span>
                <strong>{{ formatTime(orderDetail.updatedAt) }}</strong>
              </div>
            </div>

            <div class="description-box">
              <p>{{ orderDetail.requestDescription }}</p>
            </div>

            <!-- 拒绝备注 -->
            <div v-if="orderDetail.latestRequesterNote" class="warning-box">
              <span class="warning-icon">⚠</span>
              <div>
                <span class="warning-label">需求方备注</span>
                <strong>{{ orderDetail.latestRequesterNote }}</strong>
              </div>
            </div>

            <!-- 操作按钮区 -->
            <div v-if="orderDetail.availableActions?.length" class="actions-section">
              <div class="section-header">
                <h5>可执行操作</h5>
                <span>{{ orderDetail.availableActions.length }}</span>
              </div>
              
              <textarea
                v-if="orderDetail.availableActions.some((a) => a.requiresNote)"
                v-model="actionNote"
                rows="2"
                class="form-textarea"
                :placeholder="notePlaceholder"
              ></textarea>
              
              <div class="actions-group">
                <button
                  v-for="action in orderDetail.availableActions"
                  :key="action.code"
                  class="action-btn"
                  :class="{
                    'btn-primary': action.code !== 'REJECT_COMPLETION',
                    'btn-outline': action.code === 'REJECT_COMPLETION'
                  }"
                  :disabled="loading"
                  @click="runOrderAction(action)"
                >
                  {{ action.label }}
                </button>
              </div>
            </div>

            <!-- 时间线 -->
            <div class="timeline-section">
              <div class="section-header">
                <h5>状态时间线</h5>
                <span>{{ orderDetail.timeline?.length || 0 }}</span>
              </div>

              <div class="timeline-list">
                <div
                  v-for="(entry, idx) in orderDetail.timeline"
                  :key="idx"
                  class="timeline-item"
                >
                  <div class="timeline-dot"></div>
                  <div class="timeline-content">
                    <div class="timeline-header">
                      <strong>{{ entry.actorName }}</strong>
                      <span>{{ formatTime(entry.happenedAt) }}</span>
                    </div>
                    <p>{{ entry.description }}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div v-else class="empty-placeholder">
            <div class="empty-icon">📄</div>
            <p>未选中订单</p>
            <span>从右侧列表选择一个订单查看详情</span>
          </div>
        </div>
      </div>
    </main>

    <!-- 通知弹窗 -->
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

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import AlertBox from './SmallComponents/AlertBox.vue'

const router = useRouter()
const authStore = useAuthStore()

// ==================== 状态定义 ====================
const users = ref([])
const currentUserId = ref(null)
const category = ref('all')
const requests = ref([])
const requestDetail = ref(null)
const orders = ref([])
const orderDetail = ref(null)
const selectedRequestId = ref(null)
const selectedOrderId = ref(null)
const applyMessage = ref('')
const actionNote = ref('')
const loading = ref(false)
const banner = ref({ type: '', text: '' })

// 弹窗相关
const showAlert = ref(false)
const alertConfig = ref({
  title: '系统通知',
  content: '',
  htmlContent: '',
  confirmText: '确定'
})

const categoryOptions = [
  { value: 'all', label: '全部' },
  { value: '快递代取', label: '快递代取' },
  { value: '学习互助', label: '学习互助' },
  { value: '校园跑腿', label: '校园跑腿' },
]

// ==================== 计算属性 ====================
const currentUser = computed(() => users.value.find((user) => user.id === currentUserId.value) ?? null)

const currentUserAvatar = computed(() => {
  if (authStore.user?.avatarPath) {
    return 'http://localhost:8080' + authStore.user.avatarPath
  }
  return 'http://localhost:8080/api/files/default-avatar.png'
})

const openRequestCount = computed(() => requests.value.filter((request) => request.status === 'OPEN').length)
const activeOrderCount = computed(() => orders.value.filter((order) => order.status !== 'COMPLETED').length)
const ownedRequestCount = computed(() => requests.value.filter((request) => request.ownedByViewer).length)

const notePlaceholder = computed(() => {
  const noteAction = orderDetail.value?.availableActions?.find((action) => action.requiresNote)
  return noteAction?.notePlaceholder ?? '请输入备注信息（如有需要）'
})

// 下拉菜单状态
const showDropdown = ref(false)

// ==================== 辅助函数 ====================
function formatTime(value) {
  if (!value) return '未设置'
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

function getStatusClass(status) {
  const classMap = {
    'OPEN': 'status-open',
    'ORDER_CREATED': 'status-pending',
    'PENDING_ACCEPTANCE': 'status-pending',
    'IN_PROGRESS': 'status-progress',
    'PENDING_CONFIRMATION': 'status-warning',
    'COMPLETED': 'status-completed'
  }
  return classMap[status] || 'status-muted'
}

function showNotification(title, content, isHtml = false) {
  alertConfig.value = {
    title,
    content: isHtml ? '' : content,
    htmlContent: isHtml ? content : '',
    confirmText: '确定'
  }
  showAlert.value = true
}

function setBanner(type, text) {
  banner.value = { type, text }
  setTimeout(() => {
    if (banner.value.text === text) {
      banner.value = { type: '', text: '' }
    }
  }, 5000)
}

// ==================== API 请求 ====================
async function requestJson(path, options = {}) {
  const response = await fetch(path, {
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers ?? {}),
    },
    ...options,
  })

  if (!response.ok) {
    const payload = await response.json().catch(() => ({}))
    throw new Error(payload.message ?? '请求失败')
  }

  return response.json()
}

async function loadUsers() {
  users.value = await requestJson('/api/orders/users')
  if (!currentUserId.value && users.value.length > 0) {
    currentUserId.value = users.value[0].id
  }
}

async function loadRequests() {
  if (!currentUserId.value) {
    requests.value = []
    return
  }

  const params = new URLSearchParams({
    viewerId: String(currentUserId.value),
    category: category.value,
  })
  requests.value = await requestJson(`/api/orders/requests?${params.toString()}`)
}

async function loadOrders() {
  if (!currentUserId.value) {
    orders.value = []
    return
  }

  const params = new URLSearchParams({
    userId: String(currentUserId.value),
  })
  orders.value = await requestJson(`/api/orders?${params.toString()}`)
}

async function refreshRequestDetail(preferredId = selectedRequestId.value) {
  if (!currentUserId.value || requests.value.length === 0) {
    selectedRequestId.value = null
    requestDetail.value = null
    return
  }

  const targetId = requests.value.some((request) => request.id === preferredId)
    ? preferredId
    : requests.value[0].id

  selectedRequestId.value = targetId
  const params = new URLSearchParams({
    viewerId: String(currentUserId.value),
  })
  requestDetail.value = await requestJson(`/api/orders/requests/${targetId}?${params.toString()}`)
}

async function refreshOrderDetail(preferredId = selectedOrderId.value) {
  if (!currentUserId.value || orders.value.length === 0) {
    selectedOrderId.value = null
    orderDetail.value = null
    return
  }

  const targetId = orders.value.some((order) => order.id === preferredId)
    ? preferredId
    : orders.value[0].id

  selectedOrderId.value = targetId
  const params = new URLSearchParams({
    viewerId: String(currentUserId.value),
  })
  orderDetail.value = await requestJson(`/api/orders/${targetId}?${params.toString()}`)
}

async function refreshAll({ preserveRequest = true, preserveOrder = true } = {}) {
  if (!currentUserId.value) return

  loading.value = true
  banner.value = { type: '', text: '' }

  try {
    await Promise.all([loadRequests(), loadOrders()])
    await refreshRequestDetail(preserveRequest ? selectedRequestId.value : null)
    await refreshOrderDetail(preserveOrder ? selectedOrderId.value : null)
  } catch (error) {
    setBanner('error', error.message)
  } finally {
    loading.value = false
  }
}

async function selectRequest(requestId) {
  selectedRequestId.value = requestId
  try {
    const params = new URLSearchParams({
      viewerId: String(currentUserId.value),
    })
    requestDetail.value = await requestJson(`/api/orders/requests/${requestId}?${params.toString()}`)
  } catch (error) {
    setBanner('error', error.message)
  }
}

async function selectOrder(orderId) {
  selectedOrderId.value = orderId
  try {
    const params = new URLSearchParams({
      viewerId: String(currentUserId.value),
    })
    orderDetail.value = await requestJson(`/api/orders/${orderId}?${params.toString()}`)
  } catch (error) {
    setBanner('error', error.message)
  }
}

async function submitApplication() {
  if (!requestDetail.value) return

  loading.value = true
  try {
    await requestJson(`/api/orders/requests/${requestDetail.value.id}/applications`, {
      method: 'POST',
      body: JSON.stringify({
        applicantId: currentUserId.value,
        message: applyMessage.value,
      }),
    })
    applyMessage.value = ''
    setBanner('success', '申请已提交')
    await refreshAll()
  } catch (error) {
    setBanner('error', error.message)
  } finally {
    loading.value = false
  }
}

async function chooseApplicant(applicationId) {
  if (!requestDetail.value) return

  loading.value = true
  try {
    const detail = await requestJson(
      `/api/orders/requests/${requestDetail.value.id}/applications/${applicationId}/select`,
      {
        method: 'POST',
        body: JSON.stringify({
          requesterId: currentUserId.value,
        }),
      }
    )

    requestDetail.value = detail
    setBanner('success', '已选定服务者，订单已创建')
    await Promise.all([loadRequests(), loadOrders()])
    await refreshRequestDetail(requestDetail.value.id)
    await refreshOrderDetail(requestDetail.value.currentOrderId)
  } catch (error) {
    setBanner('error', error.message)
  } finally {
    loading.value = false
  }
}

async function runOrderAction(action) {
  if (!orderDetail.value) return

  loading.value = true
  try {
    orderDetail.value = await requestJson(`/api/orders/${orderDetail.value.id}/actions`, {
      method: 'POST',
      body: JSON.stringify({
        actorId: currentUserId.value,
        action: action.code,
        note: action.requiresNote ? actionNote.value : '',
      }),
    })

    actionNote.value = ''
    setBanner('success', `操作成功：${action.label}`)
    await Promise.all([loadRequests(), loadOrders()])
    await refreshRequestDetail(requestDetail.value?.id)
    await refreshOrderDetail(orderDetail.value.id)
  } catch (error) {
    setBanner('error', error.message)
  } finally {
    loading.value = false
  }
}

// ==================== UI 交互 ====================
function goHome() {
  router.push('/')
}

function goToProfile() {
  showDropdown.value = false
  router.push('/my/profile')
}

function handleMessages() {
  showDropdown.value = false
  router.push('/messages')
}

function handleLogout() {
  const username = authStore.user?.name || '用户'
  showNotification('退出登录', `再见，${username}！期待你的再次使用。`)
  authStore.logout()
  showDropdown.value = false
  router.push('/')
}

function toggleDropdown() {
  showDropdown.value = !showDropdown.value
}

function closeDropdowns() {
  showDropdown.value = false
}

function handleAlertConfirm() {
  console.log('通知框已关闭')
}

// ==================== 监听器 ====================
watch(category, () => {
  refreshAll({ preserveRequest: false, preserveOrder: true })
})

watch(currentUserId, (nextValue, previousValue) => {
  if (nextValue && nextValue !== previousValue) {
    refreshAll({ preserveRequest: false, preserveOrder: false })
  }
})

// ==================== 生命周期 ====================
onMounted(async () => {
  await loadUsers()
  await refreshAll({ preserveRequest: false, preserveOrder: false })
})
</script>

<style scoped>
.order-workbench-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #62055f 0%, #8b1a86 100%);
}

/* ========== 固定头部 ========== */
.fixed-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 70px;
  background-color: #62055f;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 40px;
  z-index: 1000;
}

.header-headline {
  font-size: 28px;
  font-weight: 600;
  color: #ffffff;
  margin: 0;
  cursor: pointer;
  transition: opacity 0.2s;
}

.header-headline:hover {
  opacity: 0.9;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  position: relative;
}

.user-info {
  color: #ffffff;
  font-size: 14px;
  font-weight: 500;
}

.avatar-wrapper {
  cursor: pointer;
  display: flex;
  align-items: center;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid rgba(255, 255, 255, 0.3);
  transition: all 0.2s;
}

.avatar:hover {
  transform: scale(0.98);
  border-color: rgba(255, 255, 255, 0.6);
}

.avatar-active {
  transform: scale(0.96);
}

/* 下拉菜单 */
.dropdown-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  min-width: 150px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  z-index: 1001;
}

.dropdown-item {
  display: block;
  width: 100%;
  padding: 12px 16px;
  border: none;
  background: white;
  text-align: left;
  cursor: pointer;
  font-size: 14px;
  color: #333;
  transition: background 0.2s;
}

.dropdown-item:hover {
  background-color: #f5f5f5;
}

.dropdown-divider {
  height: 1px;
  background-color: #e0e0e0;
  margin: 4px 0;
}

.logout-item {
  color: #e31829;
}

.logout-item:hover {
  background-color: #fff0f0;
}

.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.25s ease;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* ========== 主内容区 ========== */
.main-content {
  margin-top: 70px;
  flex: 1;
  padding: 30px 40px;
}

.workbench-container {
  max-width: 1600px;
  margin: 0 auto;
}

/* 页面标题 */
.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 32px;
  font-weight: 700;
  color: #ffffff;
  margin: 0 0 8px 0;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.page-subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  margin: 0;
}

/* 工具栏卡片 */
.toolbar-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  padding: 16px 24px;
  margin-bottom: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.toolbar-left {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-label {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.filter-select {
  padding: 8px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background: white;
  font-size: 14px;
  color: #333;
  cursor: pointer;
  outline: none;
  transition: border-color 0.2s;
}

.filter-select:focus {
  border-color: #62055f;
}

.refresh-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 20px;
  background: #62055f;
  border: none;
  border-radius: 24px;
  color: white;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.refresh-btn:hover:not(:disabled) {
  background: #7a0e76;
  transform: translateY(-1px);
}

.refresh-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.refresh-icon {
  font-size: 16px;
}

/* 统计卡片 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-icon {
  font-size: 32px;
}

.stat-info {
  flex: 1;
}

.stat-label {
  display: block;
  font-size: 12px;
  color: #888;
  margin-bottom: 4px;
}

.stat-value {
  display: block;
  font-size: 28px;
  font-weight: 700;
  color: #62055f;
  line-height: 1.2;
  margin-bottom: 4px;
}

.stat-desc {
  font-size: 11px;
  color: #999;
  margin: 0;
}

/* 通知横幅 */
.banner {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  border-radius: 12px;
  margin-bottom: 24px;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.banner.success {
  background: #e8f5e9;
  border-left: 4px solid #4caf50;
}

.banner.error {
  background: #ffebee;
  border-left: 4px solid #f44336;
}

.banner-icon {
  font-size: 18px;
  font-weight: 600;
}

.banner-close {
  margin-left: auto;
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #999;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 工作区三列布局 */
.workspace-grid {
  display: grid;
  grid-template-columns: 1fr 1.2fr 1fr;
  gap: 20px;
  margin-bottom: 24px;
}

/* 通用面板样式 */
.panel {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  background: #faf9fb;
}

.panel-badge {
  font-size: 11px;
  font-weight: 600;
  color: #62055f;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 4px 0 0 0;
}

.panel-count {
  background: #f0f0f0;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  color: #666;
}

/* 需求/订单列表 */
.request-list,
.order-list {
  flex: 1;
  max-height: 500px;
  overflow-y: auto;
  padding: 8px;
}

.request-item,
.order-item {
  padding: 14px;
  border-radius: 12px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s;
  background: white;
  border: 1px solid #f0f0f0;
}

.request-item:hover,
.order-item:hover {
  background: #faf9fb;
  border-color: #e0e0e0;
  transform: translateX(2px);
}

.request-item.active,
.order-item.active {
  background: linear-gradient(135deg, #faf5ff 0%, #f5eaf5 100%);
  border-color: #62055f;
  box-shadow: 0 2px 8px rgba(98, 5, 95, 0.1);
}

.request-header,
.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.request-title,
.order-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px 0;
  line-height: 1.4;
}

.request-meta,
.order-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #888;
  margin-bottom: 6px;
}

.request-tags {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

/* 标签样式 */
.category-tag {
  background: #f0e6f0;
  color: #62055f;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 500;
}

.category-tag.small {
  font-size: 10px;
}

.status-tag {
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 500;
}

.status-tag.small {
  font-size: 10px;
}

.status-open {
  background: #e3f2fd;
  color: #1976d2;
}

.status-pending {
  background: #fff3e0;
  color: #f57c00;
}

.status-progress {
  background: #e8eaf6;
  color: #3f51b5;
}

.status-warning {
  background: #ffebee;
  color: #e53935;
}

.status-completed {
  background: #e8f5e9;
  color: #43a047;
}

.status-muted {
  background: #f5f5f5;
  color: #9e9e9e;
}

.tag {
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 10px;
  font-weight: 500;
}

.tag-owner {
  background: #e3f2fd;
  color: #1976d2;
}

.tag-apply {
  background: #e8f5e9;
  color: #43a047;
}

.tag-applications {
  background: #f5f5f5;
  color: #757575;
}

/* 详情内容区 */
.detail-content,
.order-detail-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  max-height: 500px;
}

.detail-header,
.order-detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.detail-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0 0 4px 0;
}

.detail-requester {
  font-size: 13px;
  color: #888;
  margin: 0;
}

.link-btn {
  background: none;
  border: none;
  color: #62055f;
  font-size: 13px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background 0.2s;
}

.link-btn:hover {
  background: #f0eaf0;
}

/* 信息网格 */
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.info-item {
  background: #faf9fb;
  padding: 12px;
  border-radius: 12px;
}

.info-label {
  display: block;
  font-size: 11px;
  color: #888;
  margin-bottom: 4px;
}

.info-item strong {
  display: block;
  font-size: 14px;
  color: #333;
}

/* 描述框 */
.description-box {
  background: #faf9fb;
  padding: 16px;
  border-radius: 12px;
  margin-bottom: 20px;
}

.description-box p {
  margin: 0;
  font-size: 14px;
  color: #555;
  line-height: 1.6;
}

/* 选定服务者 */
.selected-provider {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #e8f5e9;
  padding: 12px 16px;
  border-radius: 12px;
  margin-bottom: 20px;
}

.selected-icon {
  width: 24px;
  height: 24px;
  background: #4caf50;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.selected-label {
  display: block;
  font-size: 11px;
  color: #2e7d32;
}

.selected-provider strong {
  display: block;
  font-size: 14px;
  color: #333;
}

/* 警告框 */
.warning-box {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff3e0;
  padding: 12px 16px;
  border-radius: 12px;
  margin-bottom: 20px;
}

.warning-icon {
  font-size: 20px;
}

.warning-label {
  display: block;
  font-size: 11px;
  color: #ef6c00;
}

.warning-box strong {
  display: block;
  font-size: 14px;
  color: #333;
}

/* 表单区域 */
.apply-section,
.actions-section {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #555;
  margin-bottom: 8px;
}

.form-textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
  outline: none;
  transition: border-color 0.2s;
}

.form-textarea:focus {
  border-color: #62055f;
}

/* 按钮 */
.btn-primary {
  background: #62055f;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 24px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  margin-top: 8px;
}

.btn-primary:hover:not(:disabled) {
  background: #7a0e76;
  transform: translateY(-1px);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-outline {
  background: transparent;
  border: 1px solid #e0e0e0;
  padding: 8px 16px;
  border-radius: 24px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-outline:hover:not(:disabled) {
  border-color: #62055f;
  color: #62055f;
  background: #faf5ff;
}

.btn-outline.small {
  padding: 4px 12px;
  font-size: 11px;
}

/* 申请列表 */
.applications-section,
.timeline-section {
  margin-top: 20px;
  border-top: 1px solid #f0f0f0;
  padding-top: 16px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.section-header h5 {
  font-size: 14px;
  font-weight: 600;
  color: #555;
  margin: 0;
}

.section-count {
  background: #f0f0f0;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  color: #888;
}

.applications-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.application-item {
  background: #faf9fb;
  padding: 12px;
  border-radius: 12px;
}

.application-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.application-header strong {
  font-size: 14px;
  color: #333;
}

.applicant-academy {
  font-size: 11px;
  color: #888;
  margin-left: 8px;
}

.application-message {
  font-size: 13px;
  color: #666;
  margin: 0 0 8px 0;
  line-height: 1.5;
}

.application-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.application-time {
  font-size: 11px;
  color: #999;
}

/* 操作按钮组 */
.actions-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 12px;
}

.action-btn {
  padding: 8px 20px;
  border-radius: 24px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

/* 时间线 */
.timeline-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.timeline-item {
  display: flex;
  gap: 12px;
}

.timeline-dot {
  width: 8px;
  height: 8px;
  background: #62055f;
  border-radius: 50%;
  margin-top: 6px;
  flex-shrink: 0;
}

.timeline-content {
  flex: 1;
}

.timeline-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}

.timeline-header strong {
  font-size: 13px;
  color: #333;
}

.timeline-header span {
  font-size: 11px;
  color: #999;
}

.timeline-content p {
  margin: 0;
  font-size: 12px;
  color: #888;
  line-height: 1.4;
}

/* 空状态 */
.empty-placeholder {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
  opacity: 0.5;
}

.empty-placeholder p {
  margin: 0 0 4px 0;
  font-size: 14px;
  font-weight: 500;
}

.empty-placeholder span {
  font-size: 12px;
}

.empty-inline {
  text-align: center;
  padding: 20px;
  color: #999;
  font-size: 13px;
}

/* 底部订单详情面板 */
.order-detail-panel {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

/* 滚动条样式 */
.request-list::-webkit-scrollbar,
.order-list::-webkit-scrollbar,
.detail-content::-webkit-scrollbar,
.order-detail-content::-webkit-scrollbar {
  width: 6px;
}

.request-list::-webkit-scrollbar-track,
.order-list::-webkit-scrollbar-track,
.detail-content::-webkit-scrollbar-track,
.order-detail-content::-webkit-scrollbar-track {
  background: #f0f0f0;
  border-radius: 3px;
}

.request-list::-webkit-scrollbar-thumb,
.order-list::-webkit-scrollbar-thumb,
.detail-content::-webkit-scrollbar-thumb,
.order-detail-content::-webkit-scrollbar-thumb {
  background: #c0c0c0;
  border-radius: 3px;
}

.request-list::-webkit-scrollbar-thumb:hover,
.order-list::-webkit-scrollbar-thumb:hover,
.detail-content::-webkit-scrollbar-thumb:hover,
.order-detail-content::-webkit-scrollbar-thumb:hover {
  background: #a0a0a0;
}

/* 响应式 */
@media (max-width: 1200px) {
  .workspace-grid {
    grid-template-columns: 1fr;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .main-content {
    padding: 20px;
  }

  .fixed-header {
    padding: 0 20px;
  }

  .header-headline {
    font-size: 20px;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }

  .toolbar-card {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-left {
    flex-direction: column;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>