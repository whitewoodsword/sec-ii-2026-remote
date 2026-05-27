<script setup>
import { useAuthStore } from '../stores/auth'
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import AlertBox from './SmallComponents/AlertBox.vue'
import router from '../router'
import axios from 'axios'

const authStore = useAuthStore()
const BASE = 'http://localhost:8080'

const showDropdown = ref(false)
const showAlert = ref(false)
const alertConfig = ref({ title: '', content: '', htmlContent: '', confirmText: '确定' })

const showNotification = (title, content, isHtml = false) => {
  alertConfig.value = { title, content: isHtml ? '' : content, htmlContent: isHtml ? content : '', confirmText: '确定' }
  showAlert.value = true
}

const activeTab = ref('chat')
const conversations = ref([])
const selectedPartner = ref(null)
const messages = ref([])
const systemNotifications = ref([])
const newMessageText = ref('')
const loading = ref(false)
const messagesContainer = ref(null)

let pollTimer = null

const displayAvatar = computed(() => {
  if (authStore.user?.avatarPath) return BASE + authStore.user.avatarPath
  return 'https://picsum.photos/40/40?random=1'
})

const chatUnreadCount = computed(() =>
  conversations.value.reduce((sum, c) => sum + (c.unreadCount || 0), 0)
)

const systemUnreadCount = computed(() =>
  systemNotifications.value.filter(n => !n.read).length
)

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return d.toLocaleDateString('zh-CN') + ' ' + d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const formatConvTime = (timeStr) => {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  const now = new Date()
  const time = d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')

  if (d.toDateString() === now.toDateString()) return '今天 ' + time
  const yesterday = new Date(now)
  yesterday.setDate(yesterday.getDate() - 1)
  if (d.toDateString() === yesterday.toDateString()) return '昨天 ' + time
  if (d.getFullYear() === now.getFullYear()) return month + '/' + day + ' ' + time
  return d.getFullYear() + '/' + month + '/' + day + ' ' + time
}

const formatMsgTime = (timeStr) => {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  const now = new Date()
  const hour = d.getHours()
  const minute = String(d.getMinutes()).padStart(2, '0')
  const period = hour < 12 ? '上午' : hour < 13 ? '中午' : '下午'
  const displayHour = hour <= 12 ? hour : hour - 12
  const time = period + ' ' + displayHour + ':' + minute

  if (d.toDateString() === now.toDateString()) return time
  const yesterday = new Date(now)
  yesterday.setDate(yesterday.getDate() - 1)
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  if (d.toDateString() === yesterday.toDateString()) return '昨天 ' + time
  if (d.getFullYear() === now.getFullYear()) return month + '/' + day + ' ' + time
  return d.getFullYear() + '/' + month + '/' + day + ' ' + time
}

const shouldShowMsgTime = (msg, index) => {
  if (index === 0) return true
  const prev = messages.value[index - 1]
  const gap = new Date(msg.createdAt) - new Date(prev.createdAt)
  return gap > 5 * 60 * 1000
}

const fetchConversations = async () => {
  try {
    const res = await axios.get(BASE + '/messages/conversations', { params: { token: authStore.token } })
    if (res.data.success) conversations.value = res.data.data
  } catch (e) { /* silent */ }
}

const fetchNotifications = async () => {
  try {
    const res = await axios.get(BASE + '/messages/notifications', { params: { token: authStore.token } })
    if (res.data.success) systemNotifications.value = res.data.data
  } catch (e) { /* silent */ }
}

const fetchMessages = async (partnerId) => {
  try {
    const res = await axios.get(BASE + '/messages/conversation', {
      params: { token: authStore.token, withUserId: partnerId }
    })
    if (res.data.success) {
      messages.value = res.data.data
      await nextTick()
      scrollToBottom()
    }
  } catch (e) { /* silent */ }
}

const selectConversation = async (conv) => {
  selectedPartner.value = conv
  activeTab.value = 'chat'
  await fetchMessages(conv.userId)
  try {
    await axios.put(BASE + '/messages/read-conversation', {
      token: authStore.token,
      senderId: String(conv.userId)
    })
    await fetchConversations()
  } catch (e) { /* silent */ }
}

const sendMessage = async () => {
  const text = newMessageText.value.trim()
  if (!text || !selectedPartner.value) return
  try {
    const res = await axios.post(BASE + '/messages/send', {
      token: authStore.token,
      receiverId: String(selectedPartner.value.userId),
      content: text,
      type: 'CHAT'
    })
    if (res.data.success) {
      newMessageText.value = ''
      await fetchMessages(selectedPartner.value.userId)
      await fetchConversations()
    }
  } catch (e) {
    showNotification('发送失败', '消息发送失败，请重试')
  }
}

const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const handleLogout = () => {
  const username = authStore.user?.name || '用户'
  showNotification('退出登录', `再见，${username}！`)
  authStore.logout()
  showDropdown.value = false
  router.push('/')
}

const toggleDropdown = () => { if (authStore.isLoggedIn) showDropdown.value = !showDropdown.value }
const closeDropdowns = () => { showDropdown.value = false }
const goToProfile = () => { showDropdown.value = false; router.push('/my/profile') }

const refreshData = () => {
  fetchConversations()
  fetchNotifications()
}

watch(activeTab, () => {
  if (activeTab.value === 'system') {
    selectedPartner.value = null
  }
})

onMounted(() => {
  if (!authStore.isLoggedIn) { router.push('/login'); return }
  refreshData()
  pollTimer = setInterval(refreshData, 10000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<template>
  <div class="messages-page" @click="closeDropdowns">
    <header class="fixed-header">
      <div class="header-left">
        <h1 class="header-headline">校园互助服务平台</h1>
      </div>
      <div class="header-right">
        <template v-if="!authStore.isLoggedIn">
          <button class="header-btn" @click="router.push('/login')">登录 / 注册</button>
        </template>
        <template v-else>
          <div class="user-info">{{ authStore.user?.name || '用户' }}</div>
          <div class="avatar-wrapper" @click.stop="toggleDropdown">
            <img :src="displayAvatar" alt="用户头像" class="avatar" :class="{ 'avatar-active': showDropdown }" />
          </div>
          <transition name="dropdown">
            <div v-if="showDropdown" class="dropdown-menu">
              <button class="dropdown-item" @click="goToProfile">个人主页</button>
              <div class="dropdown-divider"></div>
              <button class="dropdown-item" @click="router.push('/messages')">消息</button>
              <div class="dropdown-divider"></div>
              <template v-if="authStore.user?.isAdmin">
                <button class="dropdown-item" @click.stop>管理后台</button>
              </template>
              <button class="dropdown-item logout-item" @click="handleLogout">退出登录</button>
            </div>
          </transition>
        </template>
      </div>
    </header>

    <main class="main-content">
      <div class="messages-container">
        <div class="sidebar">
          <div class="tab-bar">
            <button :class="{ active: activeTab === 'chat' }" @click="activeTab = 'chat'">
              私信
              <span v-if="chatUnreadCount > 0" class="tab-badge">{{ chatUnreadCount }}</span>
            </button>
            <button :class="{ active: activeTab === 'system' }" @click="activeTab = 'system'">
              系统通知
              <span v-if="systemUnreadCount > 0" class="tab-badge">{{ systemUnreadCount }}</span>
            </button>
          </div>

          <div v-if="activeTab === 'chat'" class="conversation-list">
            <div v-if="conversations.length === 0" class="empty-state">暂无私信对话<br>发布需求后可与服务方聊天</div>
            <div v-for="conv in conversations" :key="conv.userId"
                 class="conversation-item"
                 :class="{ active: selectedPartner?.userId === conv.userId }"
                 @click="selectConversation(conv)">
              <img :src="conv.avatarPath ? BASE + conv.avatarPath : 'https://picsum.photos/48/48?random=' + conv.userId"
                   class="conv-avatar" />
              <div class="conv-info">
                <div class="conv-name">{{ conv.userName }}</div>
                <div class="conv-preview">{{ conv.lastMessage || '暂无消息' }}</div>
              </div>
              <div class="conv-meta">
                <span class="conv-time">{{ formatConvTime(conv.lastTime) }}</span>
                <span v-if="conv.unreadCount > 0" class="unread-badge">{{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}</span>
              </div>
            </div>
          </div>

          <div v-if="activeTab === 'system'" class="notification-list">
            <div v-if="systemNotifications.length === 0" class="empty-state">暂无系统通知<br>订单状态变更时会收到通知</div>
            <div v-for="notif in systemNotifications" :key="notif.id"
                 class="notification-item" :class="{ unread: !notif.read }">
              <div class="notif-content">{{ notif.content }}</div>
              <div class="notif-time">{{ formatTime(notif.createdAt) }}</div>
            </div>
          </div>
        </div>

        <div class="chat-panel">
          <div v-if="!selectedPartner && activeTab === 'chat'" class="chat-placeholder">
            <div class="placeholder-icon">💬</div>
            <p>选择一位用户开始对话</p>
          </div>

          <div v-if="activeTab === 'system'" class="chat-placeholder">
            <div class="placeholder-icon">🔔</div>
            <p>系统通知会在此显示</p>
          </div>

          <div v-if="selectedPartner && activeTab === 'chat'" class="chat-view">
            <div class="chat-header-bar">
              <img :src="selectedPartner.avatarPath ? BASE + selectedPartner.avatarPath : 'https://picsum.photos/36/36?random=' + selectedPartner.userId"
                   class="chat-header-avatar" />
              <span class="chat-header-name">{{ selectedPartner.userName }}</span>
            </div>
            <div class="chat-messages" ref="messagesContainer">
              <div v-if="messages.length === 0" class="empty-chat">开始你们的对话吧</div>
              <template v-for="(msg, index) in messages" :key="msg.id">
                <div v-if="shouldShowMsgTime(msg, index)" class="time-separator">
                  {{ formatMsgTime(msg.createdAt) }}
                </div>
                <div class="message-row" :class="msg.senderId === authStore.user?.id ? 'message-mine' : 'message-theirs'">
                  <div class="bubble">{{ msg.content }}</div>
                </div>
              </template>
            </div>
            <div class="chat-input-area">
              <input v-model="newMessageText" @keyup.enter="sendMessage" placeholder="输入消息..."
                     class="chat-input" autocomplete="off" />
              <button @click="sendMessage" class="send-btn">发送</button>
            </div>
          </div>
        </div>
      </div>
    </main>

    <AlertBox v-model:visible="showAlert" :title="alertConfig.title" :content="alertConfig.content"
              :html-content="alertConfig.htmlContent" :confirm-text="alertConfig.confirmText" />
  </div>
</template>

<style scoped>
.messages-page { min-height: 100vh; display: flex; flex-direction: column; background-color: #f5f5f5; }

.fixed-header {
  position: fixed; top: 0; left: 0; right: 0; height: 75px;
  background-color: #62055f; box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  display: flex; justify-content: space-between; align-items: center;
  padding: 0 40px; z-index: 1000;
}
.header-headline { font-size: 32px; font-weight: 600; color: #fff; margin: 0; }
.header-left { display: flex; align-items: center; }
.header-right { display: flex; align-items: center; gap: 10px; position: relative; }
.user-info { color: #fff; font-size: 16px; font-weight: 500; }
.header-btn { padding: 8px 16px; border: none; background: none; color: white; cursor: pointer; font-size: 16px; }
.header-btn:hover { color: #d4b0d3; }

.avatar-wrapper { cursor: pointer; display: flex; align-items: center; }
.avatar { width: 40px; height: 40px; border-radius: 50%; object-fit: cover; border: 2px solid transparent; transition: all 0.2s; }
.avatar:hover { transform: scale(0.98); }
.avatar-active { transform: scale(0.96); }

.dropdown-menu {
  position: absolute; top: calc(100% + 12px); right: 0; min-width: 160px;
  background: white; border-radius: 12px; box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  overflow: hidden; z-index: 1001;
}
.dropdown-item { display: flex; align-items: center; gap: 12px; padding: 12px 16px; width: 100%; border: none; background: white; text-align: left; cursor: pointer; font-size: 14px; color: #333; transition: background 0.2s; }
.dropdown-item:hover { background: #f5f5f5; }
.dropdown-divider { height: 1px; background: #e0e0e0; margin: 4px 0; }
.logout-item { color: #e31829; }
.logout-item:hover { background: #fff0f0; }
.dropdown-enter-active, .dropdown-leave-active { transition: all 0.25s ease; }
.dropdown-enter-from, .dropdown-leave-to { opacity: 0; transform: translateY(-10px); }

.main-content { margin-top: 75px; flex: 1; display: flex; height: calc(100vh - 75px); }

.messages-container { display: flex; width: 100%; max-width: 1400px; margin: 0 auto; }

.sidebar {
  width: 320px; min-width: 320px; background: white;
  border-right: 1px solid #e5e4e7; display: flex; flex-direction: column;
}

.tab-bar {
  display: flex; border-bottom: 1px solid #e5e4e7;
}
.tab-bar button {
  flex: 1; padding: 16px; border: none; background: none; font-size: 15px;
  font-weight: 500; color: #888; cursor: pointer; position: relative;
  transition: color 0.2s; border-bottom: 2px solid transparent;
}
.tab-bar button.active { color: #62055f; border-bottom-color: #62055f; }
.tab-bar button:hover { color: #62055f; }
.tab-badge { display: inline-block; margin-left: 6px; padding: 2px 7px; font-size: 11px; border-radius: 10px; background: #e31829; color: white; vertical-align: middle; }

.conversation-list { flex: 1; overflow-y: auto; }
.notification-list { flex: 1; overflow-y: auto; }

.empty-state { text-align: center; padding: 60px 20px; color: #bbb; font-size: 14px; line-height: 1.8; }

.conversation-item {
  display: flex; align-items: center; padding: 14px 16px; cursor: pointer;
  gap: 12px; border-bottom: 1px solid #f5f5f5; transition: background 0.15s;
}
.conversation-item:hover { background: #f9f9f9; }
.conversation-item.active { background: #ede0ed; }
.conv-avatar { width: 48px; height: 48px; border-radius: 50%; object-fit: cover; flex-shrink: 0; }
.conv-info { flex: 1; min-width: 0; }
.conv-name { font-size: 15px; font-weight: 500; color: #333; margin-bottom: 4px; }
.conv-preview { font-size: 13px; color: #999; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.conv-meta { display: flex; flex-direction: column; align-items: flex-end; gap: 4px; flex-shrink: 0; }
.conv-time { font-size: 12px; color: #bbb; white-space: nowrap; }
.unread-badge { display: inline-block; padding: 2px 7px; font-size: 11px; border-radius: 10px; background: #e31829; color: white; }

.notification-item { padding: 16px; border-bottom: 1px solid #f5f5f5; cursor: default; }
.notification-item.unread { background: #fdf2fd; }
.notif-content { font-size: 14px; color: #333; line-height: 1.5; }
.notif-time { font-size: 12px; color: #bbb; margin-top: 6px; }

.chat-panel { flex: 1; display: flex; flex-direction: column; background: #f9f9f9; }
.chat-placeholder { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #bbb; gap: 12px; }
.placeholder-icon { font-size: 48px; }

.chat-view { flex: 1; display: flex; flex-direction: column; }
.chat-header-bar {
  display: flex; align-items: center; gap: 12px; padding: 14px 20px;
  background: white; border-bottom: 1px solid #e5e4e7;
}
.chat-header-avatar { width: 36px; height: 36px; border-radius: 50%; object-fit: cover; }
.chat-header-name { font-size: 16px; font-weight: 500; color: #333; }

.chat-messages { flex: 1; overflow-y: auto; padding: 20px; display: flex; flex-direction: column; gap: 2px; }
.empty-chat { text-align: center; color: #bbb; padding: 40px; }

.message-row { max-width: 70%; margin-bottom: 8px; }
.message-mine { align-self: flex-end; margin-left: auto; }
.message-theirs { align-self: flex-start; }

.bubble { padding: 10px 14px; border-radius: 18px; font-size: 14px; line-height: 1.5; word-break: break-word; }
.message-mine .bubble { background: #62055f; color: white; border-bottom-right-radius: 6px; }
.message-theirs .bubble { background: white; color: #333; border-bottom-left-radius: 6px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); }

.time-separator { text-align: center; padding: 12px 0 8px; font-size: 12px; color: #b0b0b0; }

.chat-input-area { display: flex; padding: 16px 20px; background: white; border-top: 1px solid #e5e4e7; gap: 12px; }
.chat-input { flex: 1; padding: 10px 16px; border: 1px solid #e5e4e7; border-radius: 20px; font-size: 14px; outline: none; background: #f9f9f9; }
.chat-input:focus { border-color: #62055f; background: white; }
.send-btn { padding: 10px 28px; background: #62055f; color: white; border: none; border-radius: 20px; font-size: 14px; font-weight: 500; cursor: pointer; transition: background 0.2s; }
.send-btn:hover { background: #7a0e76; }
.send-btn:active { background: #4a0447; }

@media (max-width: 768px) {
  .sidebar { width: 260px; min-width: 260px; }
  .chat-input-area { padding: 12px; gap: 8px; }
  .fixed-header { padding: 0 20px; }
  .header-headline { font-size: 24px; }
}
</style>
