<script setup>
import { computed, onMounted, ref, watch } from 'vue'

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

const categoryOptions = [
  { value: 'all', label: 'All' },
  { value: '\u5feb\u9012\u4ee3\u53d6', label: 'Delivery' },
  { value: '\u5b66\u4e60\u4e92\u52a9', label: 'Study Help' },
  { value: '\u6821\u56ed\u8dd1\u817f', label: 'Errand' },
]

const currentUser = computed(() => users.value.find((user) => user.id === currentUserId.value) ?? null)
const openRequestCount = computed(() => requests.value.filter((request) => request.status === 'OPEN').length)
const activeOrderCount = computed(() => orders.value.filter((order) => order.status !== 'COMPLETED').length)
const ownedRequestCount = computed(() => requests.value.filter((request) => request.ownedByViewer).length)
const notePlaceholder = computed(() => {
  const noteAction = orderDetail.value?.availableActions?.find((action) => action.requiresNote)
  return noteAction?.notePlaceholder ?? 'Enter a note when required'
})

function formatTime(value) {
  if (!value) {
    return 'Not set'
  }

  return new Intl.DateTimeFormat('en-CA', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

function statusTone(status) {
  switch (status) {
    case 'OPEN':
      return 'tone-open'
    case 'ORDER_CREATED':
    case 'PENDING_ACCEPTANCE':
      return 'tone-pending'
    case 'IN_PROGRESS':
      return 'tone-progress'
    case 'PENDING_CONFIRMATION':
      return 'tone-warning'
    case 'COMPLETED':
      return 'tone-completed'
    default:
      return 'tone-muted'
  }
}

function actionTone(code) {
  return code === 'REJECT_COMPLETION' ? 'ghost' : 'solid'
}

function setBanner(type, text) {
  banner.value = { type, text }
}

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
    throw new Error(payload.message ?? 'Request failed')
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
  if (!currentUserId.value) {
    return
  }

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
  if (!requestDetail.value) {
    return
  }

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
    setBanner('success', 'Application submitted')
    await refreshAll()
  } catch (error) {
    setBanner('error', error.message)
  } finally {
    loading.value = false
  }
}

async function chooseApplicant(applicationId) {
  if (!requestDetail.value) {
    return
  }

  loading.value = true
  try {
    const detail = await requestJson(
      `/api/orders/requests/${requestDetail.value.id}/applications/${applicationId}/select`,
      {
        method: 'POST',
        body: JSON.stringify({
          requesterId: currentUserId.value,
        }),
      },
    )

    requestDetail.value = detail
    setBanner('success', 'Provider selected and order created')
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
  if (!orderDetail.value) {
    return
  }

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
    setBanner('success', `Action completed: ${action.label}`)
    await Promise.all([loadRequests(), loadOrders()])
    await refreshRequestDetail(requestDetail.value?.id)
    await refreshOrderDetail(orderDetail.value.id)
  } catch (error) {
    setBanner('error', error.message)
  } finally {
    loading.value = false
  }
}

watch(category, () => {
  refreshAll({ preserveRequest: false, preserveOrder: true })
})

watch(currentUserId, (nextValue, previousValue) => {
  if (nextValue && nextValue !== previousValue) {
    refreshAll({ preserveRequest: false, preserveOrder: false })
  }
})

onMounted(async () => {
  await loadUsers()
  await refreshAll({ preserveRequest: false, preserveOrder: false })
})
</script>

<template>
  <div class="order-page">
    <div class="page-shell">
      <header class="masthead">
        <div class="headline">
          <p class="eyebrow">CampusHub · P4 Order Module</p>
          <h1>Order Workbench</h1>
          <p class="subtitle">
            Demo flow for application, requester confirmation, provider progress update,
            completion confirmation, and order history lookup.
          </p>
        </div>

        <div class="toolbar">
          <label class="field">
            <span>Viewer</span>
            <select v-model.number="currentUserId">
              <option v-for="user in users" :key="user.id" :value="user.id">
                {{ user.name }} · {{ user.roleLabel }}
              </option>
            </select>
          </label>

          <label class="field">
            <span>Category</span>
            <select v-model="category">
              <option v-for="option in categoryOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </label>

          <button class="refresh-button" :disabled="loading" @click="refreshAll()">
            {{ loading ? 'Refreshing...' : 'Refresh Workbench' }}
          </button>
        </div>

        <div class="stat-grid">
          <article class="stat-card">
            <span>Current user</span>
            <strong>{{ currentUser?.name ?? 'None' }}</strong>
            <p>{{ currentUser?.academy ?? 'Choose a demo user first' }}</p>
          </article>
          <article class="stat-card">
            <span>Open requests</span>
            <strong>{{ openRequestCount }}</strong>
            <p>Requests still available under the current filter</p>
          </article>
          <article class="stat-card">
            <span>Active orders</span>
            <strong>{{ activeOrderCount }}</strong>
            <p>Orders not yet completed</p>
          </article>
          <article class="stat-card">
            <span>Owned requests</span>
            <strong>{{ ownedRequestCount }}</strong>
            <p>Requests published by the current viewer</p>
          </article>
        </div>
      </header>

      <p v-if="banner.text" class="banner" :class="banner.type">
        {{ banner.text }}
      </p>

      <main class="workspace">
        <section class="panel">
          <div class="panel-head">
            <div>
              <p class="panel-kicker">Request Pool</p>
              <h2>Requests</h2>
            </div>
            <span class="panel-count">{{ requests.length }}</span>
          </div>

          <div class="stack">
            <button
              v-for="request in requests"
              :key="request.id"
              class="request-card"
              :class="{ selected: selectedRequestId === request.id }"
              @click="selectRequest(request.id)"
            >
              <div class="card-topline">
                <span class="category-chip">{{ request.category }}</span>
                <span class="status-chip" :class="statusTone(request.status)">
                  {{ request.statusLabel }}
                </span>
              </div>
              <h3>{{ request.title }}</h3>
              <p>{{ request.location }}</p>
              <div class="card-meta">
                <span>Requester: {{ request.requesterName }}</span>
                <span>Reward: {{ request.reward }}</span>
              </div>
              <div class="card-meta">
                <span>{{ formatTime(request.serviceTime) }}</span>
                <span>{{ request.applicationCount }} applications</span>
              </div>
              <div class="card-flags">
                <span v-if="request.ownedByViewer" class="flag">Owned by me</span>
                <span v-if="request.canApply" class="flag flag-action">Can apply</span>
              </div>
            </button>

            <div v-if="!requests.length" class="empty-state">
              No requests are visible under the current filter.
            </div>
          </div>
        </section>

        <section class="panel">
          <div class="panel-head">
            <div>
              <p class="panel-kicker">Request Detail</p>
              <h2>Selected Request</h2>
            </div>
            <span v-if="requestDetail" class="status-chip" :class="statusTone(requestDetail.status)">
              {{ requestDetail.statusLabel }}
            </span>
          </div>

          <div v-if="requestDetail" class="detail-body">
            <div class="detail-hero">
              <div>
                <h3>{{ requestDetail.title }}</h3>
                <p>Requester: {{ requestDetail.requesterName }}</p>
              </div>
              <button
                v-if="requestDetail.currentOrderId"
                class="outline-button"
                @click="selectOrder(requestDetail.currentOrderId)"
              >
                Open linked order
              </button>
            </div>

            <div class="meta-grid">
              <div>
                <span>Category</span>
                <strong>{{ requestDetail.category }}</strong>
              </div>
              <div>
                <span>Location</span>
                <strong>{{ requestDetail.location }}</strong>
              </div>
              <div>
                <span>Service time</span>
                <strong>{{ formatTime(requestDetail.serviceTime) }}</strong>
              </div>
              <div>
                <span>Reward</span>
                <strong>{{ requestDetail.reward }}</strong>
              </div>
            </div>

            <article class="story-card">
              <p>{{ requestDetail.description }}</p>
            </article>

            <article v-if="requestDetail.selectedProviderName" class="selection-card">
              <span>Selected provider</span>
              <strong>{{ requestDetail.selectedProviderName }}</strong>
              <p>The request is already linked to an order flow.</p>
            </article>

            <div v-if="requestDetail.canApply" class="apply-box">
              <label>
                <span>Application message</span>
                <textarea
                  v-model="applyMessage"
                  rows="3"
                  placeholder="Example: I will be nearby tonight and can do this on the way."
                ></textarea>
              </label>
              <button class="primary-button" :disabled="loading" @click="submitApplication">
                Submit application
              </button>
            </div>

            <div class="application-block">
              <div class="subhead">
                <h4>Applications</h4>
                <span>{{ requestDetail.applications.length }}</span>
              </div>

              <div v-if="requestDetail.applications.length" class="stack compact">
                <article
                  v-for="application in requestDetail.applications"
                  :key="application.id"
                  class="application-card"
                >
                  <div class="application-topline">
                    <div>
                      <strong>{{ application.applicantName }}</strong>
                      <p>{{ application.applicantAcademy }}</p>
                    </div>
                    <span class="status-chip" :class="statusTone(application.status)">
                      {{ application.statusLabel }}
                    </span>
                  </div>
                  <p class="application-message">{{ application.message }}</p>
                  <div class="application-footer">
                    <span>{{ formatTime(application.createdAt) }}</span>
                    <button
                      v-if="application.selectable"
                      class="primary-button"
                      :disabled="loading"
                      @click="chooseApplicant(application.id)"
                    >
                      Select provider
                    </button>
                  </div>
                </article>
              </div>

              <div v-else class="empty-inline">
                No application records yet.
              </div>
            </div>
          </div>

          <div v-else class="empty-state">
            Select a request from the left panel to inspect details.
          </div>
        </section>

        <section class="panel">
          <div class="panel-head">
            <div>
              <p class="panel-kicker">Order Board</p>
              <h2>My Orders</h2>
            </div>
            <span class="panel-count">{{ orders.length }}</span>
          </div>

          <div class="stack">
            <button
              v-for="order in orders"
              :key="order.id"
              class="order-card"
              :class="{ selected: selectedOrderId === order.id }"
              @click="selectOrder(order.id)"
            >
              <div class="card-topline">
                <span class="category-chip">{{ order.category }}</span>
                <span class="status-chip" :class="statusTone(order.status)">
                  {{ order.statusLabel }}
                </span>
              </div>
              <h3>{{ order.requestTitle }}</h3>
              <div class="card-meta">
                <span>Counterpart: {{ order.counterpartName }}</span>
                <span>{{ formatTime(order.updatedAt) }}</span>
              </div>
            </button>

            <div v-if="!orders.length" class="empty-state">
              No orders are linked to the current viewer.
            </div>
          </div>
        </section>

        <section class="panel panel-wide">
          <div class="panel-head">
            <div>
              <p class="panel-kicker">Order Flow</p>
              <h2>Order Detail</h2>
            </div>
            <span v-if="orderDetail" class="status-chip" :class="statusTone(orderDetail.status)">
              {{ orderDetail.statusLabel }}
            </span>
          </div>

          <div v-if="orderDetail" class="detail-body">
            <div class="detail-hero">
              <div>
                <h3>{{ orderDetail.requestTitle }}</h3>
                <p>{{ orderDetail.location }} · {{ orderDetail.reward }}</p>
              </div>
              <button class="outline-button" @click="selectRequest(orderDetail.requestId)">
                Back to request
              </button>
            </div>

            <div class="meta-grid">
              <div>
                <span>Requester</span>
                <strong>{{ orderDetail.requesterName }}</strong>
              </div>
              <div>
                <span>Provider</span>
                <strong>{{ orderDetail.providerName }}</strong>
              </div>
              <div>
                <span>Service time</span>
                <strong>{{ formatTime(orderDetail.serviceTime) }}</strong>
              </div>
              <div>
                <span>Last updated</span>
                <strong>{{ formatTime(orderDetail.updatedAt) }}</strong>
              </div>
            </div>

            <article class="story-card">
              <p>{{ orderDetail.requestDescription }}</p>
            </article>

            <article v-if="orderDetail.latestRequesterNote" class="selection-card warning-card">
              <span>Latest requester rejection note</span>
              <strong>{{ orderDetail.latestRequesterNote }}</strong>
            </article>

            <div v-if="orderDetail.availableActions.length" class="action-console">
              <div class="subhead">
                <h4>Available actions</h4>
                <span>{{ orderDetail.availableActions.length }}</span>
              </div>
              <textarea
                v-if="orderDetail.availableActions.some((action) => action.requiresNote)"
                v-model="actionNote"
                rows="3"
                :placeholder="notePlaceholder"
              ></textarea>
              <div class="action-row">
                <button
                  v-for="action in orderDetail.availableActions"
                  :key="action.code"
                  class="action-button"
                  :class="actionTone(action.code)"
                  :disabled="loading"
                  @click="runOrderAction(action)"
                >
                  {{ action.label }}
                </button>
              </div>
            </div>

            <div class="timeline">
              <div class="subhead">
                <h4>Status timeline</h4>
                <span>{{ orderDetail.timeline.length }}</span>
              </div>

              <div class="timeline-list">
                <article
                  v-for="entry in orderDetail.timeline"
                  :key="`${entry.happenedAt}-${entry.description}`"
                  class="timeline-row"
                >
                  <span class="timeline-dot"></span>
                  <div>
                    <div class="timeline-topline">
                      <strong>{{ entry.actorName }}</strong>
                      <span>{{ formatTime(entry.happenedAt) }}</span>
                    </div>
                    <p>{{ entry.description }}</p>
                  </div>
                </article>
              </div>
            </div>
          </div>

          <div v-else class="empty-state">
            Select an order to inspect its current state and timeline.
          </div>
        </section>
      </main>

      <footer class="footnote">
        This page is backed by the Spring Boot plus MySQL order repository and
        keeps requests, applications, orders, and timeline records in sync.
      </footer>
    </div>
  </div>
</template>

<style scoped>
.order-page {
  min-height: calc(100vh - 120px);
  padding: 24px 16px 40px;
  background:
    radial-gradient(circle at top left, rgba(184, 90, 43, 0.18), transparent 32%),
    radial-gradient(circle at bottom right, rgba(37, 97, 93, 0.12), transparent 28%),
    linear-gradient(180deg, #f6f1e6 0%, #efe7d8 100%);
}

.page-shell {
  width: min(1480px, 100%);
  margin: 0 auto;
}

.masthead {
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(73, 58, 40, 0.14);
  border-radius: 32px;
  padding: 28px;
  background:
    linear-gradient(130deg, rgba(255, 251, 244, 0.94) 0%, rgba(245, 235, 217, 0.92) 52%, rgba(236, 241, 236, 0.95) 100%);
  box-shadow: 0 20px 40px rgba(86, 58, 27, 0.12);
}

.masthead::after {
  content: "";
  position: absolute;
  inset: auto -40px -60px auto;
  width: 220px;
  height: 220px;
  border-radius: 999px;
  background: rgba(184, 90, 43, 0.08);
}

.headline {
  max-width: 760px;
}

.eyebrow {
  margin: 0 0 10px;
  color: #b85a2b;
  text-transform: uppercase;
  letter-spacing: 0.18em;
  font-size: 0.82rem;
  font-weight: 700;
}

.headline h1 {
  margin: 0;
  font-size: clamp(2.4rem, 4vw, 4.4rem);
  line-height: 0.98;
  color: #1f2622;
}

.subtitle {
  margin: 14px 0 0;
  color: #62594d;
  max-width: 680px;
  font-size: 1.02rem;
}

.toolbar {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 24px;
}

.field {
  display: grid;
  gap: 8px;
}

.field span {
  font-size: 0.88rem;
  color: #62594d;
}

.field select,
.apply-box textarea,
.action-console textarea {
  width: 100%;
  border: 1px solid rgba(73, 58, 40, 0.14);
  border-radius: 18px;
  padding: 0.85rem 1rem;
  background: rgba(255, 255, 255, 0.75);
  color: #1f2622;
}

.field select:focus,
.apply-box textarea:focus,
.action-console textarea:focus {
  outline: 2px solid rgba(184, 90, 43, 0.18);
  border-color: rgba(184, 90, 43, 0.35);
}

.refresh-button,
.primary-button,
.outline-button,
.action-button {
  border: 0;
  border-radius: 999px;
  padding: 0.9rem 1.25rem;
  transition: transform 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
}

.refresh-button,
.primary-button,
.action-button.solid {
  background: linear-gradient(135deg, #b85a2b 0%, #8f3d12 100%);
  color: #fffaf5;
  box-shadow: 0 16px 28px rgba(143, 61, 18, 0.18);
}

.outline-button,
.action-button.ghost {
  background: rgba(255, 255, 255, 0.75);
  color: #1f2622;
  border: 1px solid rgba(73, 58, 40, 0.14);
}

.refresh-button:hover,
.primary-button:hover,
.outline-button:hover,
.action-button:hover {
  transform: translateY(-1px);
}

.refresh-button:disabled,
.primary-button:disabled,
.outline-button:disabled,
.action-button:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  transform: none;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-top: 22px;
}

.stat-card {
  position: relative;
  z-index: 1;
  padding: 18px;
  border-radius: 22px;
  border: 1px solid rgba(73, 58, 40, 0.08);
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(12px);
}

.stat-card span {
  display: block;
  color: #62594d;
  font-size: 0.86rem;
}

.stat-card strong {
  display: block;
  margin-top: 8px;
  font-size: 1.8rem;
  color: #1f2622;
}

.stat-card p {
  margin: 6px 0 0;
  color: #62594d;
  font-size: 0.92rem;
}

.banner {
  margin: 18px 0 0;
  padding: 0.95rem 1.1rem;
  border-radius: 18px;
  border: 1px solid rgba(73, 58, 40, 0.14);
  background: rgba(255, 251, 243, 0.95);
}

.banner.success {
  color: #2f6e45;
  border-color: rgba(47, 110, 69, 0.22);
  background: rgba(238, 248, 240, 0.92);
}

.banner.error {
  color: #9d3424;
  border-color: rgba(157, 52, 36, 0.22);
  background: rgba(252, 240, 237, 0.95);
}

.workspace {
  display: grid;
  grid-template-columns: 1.02fr 1.2fr 0.92fr;
  gap: 16px;
  margin-top: 18px;
}

.panel {
  border: 1px solid rgba(73, 58, 40, 0.14);
  border-radius: 28px;
  padding: 20px;
  background: rgba(255, 252, 246, 0.92);
  box-shadow: 0 20px 40px rgba(86, 58, 27, 0.12);
}

.panel-wide {
  grid-column: 2 / 4;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.panel-kicker {
  margin: 0;
  color: #b85a2b;
  font-size: 0.75rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  font-weight: 700;
}

.panel-head h2 {
  margin: 5px 0 0;
  font-size: 1.35rem;
  color: #1f2622;
}

.panel-count {
  min-width: 38px;
  height: 38px;
  display: inline-grid;
  place-items: center;
  border-radius: 999px;
  background: rgba(37, 97, 93, 0.1);
  color: #25615d;
  font-weight: 700;
}

.stack {
  display: grid;
  gap: 12px;
}

.stack.compact {
  gap: 10px;
}

.request-card,
.order-card {
  text-align: left;
  border: 1px solid transparent;
  border-radius: 22px;
  padding: 16px;
  background: #fffaf0;
  box-shadow: inset 0 0 0 1px rgba(73, 58, 40, 0.06);
}

.request-card.selected,
.order-card.selected {
  border-color: rgba(184, 90, 43, 0.36);
  box-shadow: 0 18px 26px rgba(143, 61, 18, 0.12);
}

.card-topline,
.application-topline,
.timeline-topline,
.card-meta,
.card-flags,
.application-footer,
.detail-hero,
.subhead,
.action-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.request-card h3,
.order-card h3,
.detail-hero h3 {
  margin: 10px 0 6px;
  font-size: 1.08rem;
  color: #1f2622;
}

.request-card p,
.order-card p,
.application-card p,
.timeline-row p,
.selection-card p {
  margin: 0;
  color: #62594d;
}

.card-meta {
  margin-top: 8px;
  font-size: 0.9rem;
  color: #62594d;
}

.card-flags {
  margin-top: 12px;
  justify-content: flex-start;
  flex-wrap: wrap;
}

.flag,
.category-chip,
.status-chip {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  padding: 0.35rem 0.75rem;
  font-size: 0.82rem;
  white-space: nowrap;
}

.flag {
  background: rgba(37, 97, 93, 0.1);
  color: #25615d;
}

.flag-action,
.category-chip {
  background: rgba(184, 90, 43, 0.11);
  color: #8f3d12;
}

.status-chip {
  background: rgba(73, 58, 40, 0.08);
  color: #62594d;
}

.tone-open {
  background: rgba(37, 97, 93, 0.12);
  color: #25615d;
}

.tone-pending {
  background: rgba(184, 90, 43, 0.12);
  color: #8f3d12;
}

.tone-progress {
  background: rgba(37, 97, 93, 0.14);
  color: #1f5f54;
}

.tone-warning {
  background: rgba(163, 98, 22, 0.12);
  color: #a36216;
}

.tone-completed {
  background: rgba(47, 110, 69, 0.12);
  color: #2f6e45;
}

.tone-muted {
  background: rgba(73, 58, 40, 0.08);
  color: #62594d;
}

.detail-body {
  display: grid;
  gap: 16px;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.meta-grid > div,
.story-card,
.selection-card,
.apply-box,
.application-card,
.action-console,
.timeline-row {
  border-radius: 22px;
  border: 1px solid rgba(73, 58, 40, 0.09);
  background: rgba(255, 255, 255, 0.74);
}

.meta-grid > div {
  padding: 14px;
}

.meta-grid span {
  display: block;
  color: #62594d;
  font-size: 0.82rem;
}

.meta-grid strong {
  display: block;
  margin-top: 6px;
  color: #1f2622;
}

.story-card,
.selection-card,
.apply-box,
.action-console {
  padding: 16px;
}

.selection-card span {
  display: block;
  color: #62594d;
  font-size: 0.82rem;
}

.selection-card strong {
  display: block;
  margin-top: 8px;
  font-size: 1.08rem;
  color: #1f2622;
}

.warning-card {
  background: rgba(255, 247, 234, 0.9);
}

.apply-box {
  display: grid;
  gap: 12px;
}

.apply-box label,
.action-console {
  display: grid;
  gap: 10px;
}

.apply-box span {
  color: #62594d;
}

.application-block,
.timeline {
  display: grid;
  gap: 12px;
}

.subhead h4 {
  margin: 0;
  color: #1f2622;
}

.subhead span {
  color: #62594d;
  font-size: 0.9rem;
}

.application-card {
  padding: 14px;
}

.application-message {
  margin-top: 10px;
  line-height: 1.6;
}

.timeline-list {
  display: grid;
  gap: 10px;
}

.timeline-row {
  padding: 14px;
  display: grid;
  grid-template-columns: 18px 1fr;
  gap: 12px;
}

.timeline-dot {
  width: 12px;
  height: 12px;
  margin-top: 5px;
  border-radius: 999px;
  background: linear-gradient(135deg, #b85a2b 0%, #25615d 100%);
}

.action-console textarea {
  min-height: 96px;
}

.action-row {
  flex-wrap: wrap;
  justify-content: flex-start;
}

.empty-state,
.empty-inline {
  padding: 20px;
  border-radius: 22px;
  border: 1px dashed rgba(73, 58, 40, 0.18);
  color: #62594d;
  background: rgba(255, 255, 255, 0.52);
}

.footnote {
  margin-top: 18px;
  color: #62594d;
  font-size: 0.92rem;
  text-align: center;
}

@media (max-width: 1180px) {
  .workspace {
    grid-template-columns: 1fr 1fr;
  }

  .panel-wide {
    grid-column: 1 / -1;
  }

  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 860px) {
  .order-page {
    padding: 18px 12px 32px;
  }

  .toolbar,
  .workspace,
  .meta-grid,
  .stat-grid {
    grid-template-columns: 1fr;
  }

  .panel-wide {
    grid-column: auto;
  }

  .panel,
  .masthead {
    padding: 18px;
    border-radius: 24px;
  }

  .detail-hero,
  .card-topline,
  .card-meta,
  .application-topline,
  .application-footer,
  .timeline-topline {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
