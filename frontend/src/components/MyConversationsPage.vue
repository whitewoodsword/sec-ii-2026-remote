<template>
  <div class="conversations-page">
    <!-- 顶部导航栏 -->
    <header class="page-header">
      <button class="back-btn" @click="goBack">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M15 18L9 12L15 6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        返回
      </button>
      <h1 class="page-title">消息</h1>
      <div class="header-placeholder"></div>
    </header>

    <div class="conversations-container">
      <!-- 左侧：对话列表 -->
      <aside class="conversations-sidebar">
        <div class="sidebar-header">
          <h2>对话</h2>
        </div>

        <div class="conversations-list" v-if="!loadingConversations">
          <div
            v-for="conv in conversations"
            :key="conv.conversationId || conv.id"
            class="conversation-item"
            :class="{ active: activeConversationId === (conv.conversationId || conv.id) }"
            @click="selectConversation(conv)"
          >
            <div class="conv-avatar">
              <img :src="getAvatarUrl(conv.otherUser?.avatarPath)" alt="" />
            </div>
            <div class="conv-info">
              <div class="conv-name">{{ conv.otherUser?.name || '用户' }}</div>
              <div class="conv-last-msg">{{ conv.lastMessage?.content || '暂无消息' }}</div>
            </div>
            <div class="conv-meta">
              <div class="conv-time">{{ formatRelativeTime(conv.lastMessage?.createdAt) }}</div>
              <div v-if="conv.unreadCount > 0" class="unread-badge">{{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}</div>
            </div>
          </div>
          <div v-if="conversations.length === 0" class="empty-conversations">
            <span>暂无对话</span>
          </div>
        </div>

        <div v-else class="loading-sidebar">
          <div class="loading-spinner"></div>
          <span>加载对话中...</span>
        </div>
      </aside>

      <!-- 右侧：聊天区域 -->
      <main class="chat-area">
        <template v-if="activeConversation">
          <!-- 聊天头部 -->
          <div class="chat-header">
            <div class="chat-user-info">
              <img :src="getAvatarUrl(activeConversation.otherUser?.avatarPath)" alt="" class="chat-avatar" />
              <div class="chat-user-name">{{ activeConversation.otherUser?.name || '用户' }}</div>
            </div>
          </div>

          <!-- 消息列表 -->
          <div class="messages-container" ref="messagesContainer">
            <div v-if="loadingMessages && messages.length === 0" class="loading-messages">
              <div class="loading-spinner"></div>
              <span>加载消息中...</span>
            </div>
            <div v-else class="messages-list">
              <div
                v-for="msg in messages"
                :key="msg.id"
                class="message-item"
                :class="{ 'own-message': msg.senderId === currentUserId }"
              >
                <div class="message-bubble">
                  <div class="message-content">{{ msg.content }}</div>
                  <div class="message-time">{{ formatTime(msg.createdAt) }}</div>
                </div>
              </div>

              <div v-if="messages.length === 0 && !loadingMessages" class="empty-messages">
                <span>暂无消息</span>
              </div>
            </div>
          </div>

          <!-- 输入区域 -->
          <div class="input-area">
            <textarea
              v-model="newMessage"
              placeholder="输入消息... (Shift+Enter 换行，Enter 发送)"
              class="message-input"
              @keydown="handleKeydown"
              rows="1"
              ref="messageInput"
            ></textarea>
            <button class="send-btn" @click="sendMessage" :disabled="sending">
              发送
            </button>
          </div>
        </template>

        <div v-else class="empty-chat">
          <div class="empty-chat-icon">💬</div>
          <div class="empty-chat-text">选择对话开始聊天</div>
        </div>
      </main>
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
import { ref, onMounted, onUnmounted, nextTick, watch, computed } from 'vue'
import { useAuthStore } from '../stores/auth'
import { useRouter } from 'vue-router'
import AlertBox from './SmallComponents/AlertBox.vue'

const authStore = useAuthStore()
const router = useRouter()

// 数据状态
const conversations = ref([])
const activeConversation = ref(null)
const activeConversationId = ref(null)
const messages = ref([])
const newMessage = ref('')
const loadingConversations = ref(false)
const loadingMessages = ref(false)
const sending = ref(false)
let pollingInterval = null
let isInitialLoad = ref(true)

// 输入框引用
const messageInput = ref(null)

// 通知相关
const showAlert = ref(false)
const alertConfig = ref({ title: '', content: '', confirmText: '确定' })
const showNotification = (title, content) => {
  alertConfig.value = { title, content, confirmText: '确定' }
  showAlert.value = true
}
const handleAlertConfirm = () => {}

// 返回上一页
const goBack = () => {
  router.back()
}

// 工具
const defaultAvatar = 'http://localhost:8080/api/files/default-avatar.png'
const currentUserId = computed(() => authStore.user?.id)

const getAvatarUrl = (avatarPath) => {
  if (!avatarPath) return defaultAvatar
  if (avatarPath.startsWith('http')) return avatarPath
  return `http://localhost:8080${avatarPath}`
}

const formatTime = (isoString) => {
  if (!isoString) return ''
  const date = new Date(isoString)
  return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
}

const formatRelativeTime = (isoString) => {
  if (!isoString) return ''
  const date = new Date(isoString)
  const now = new Date()
  const diff = now - date
  if (diff < 24 * 3600 * 1000) return formatTime(isoString)
  if (diff < 7 * 24 * 3600 * 1000) return `${date.getMonth()+1}/${date.getDate()}`
  return `${date.getFullYear()}/${date.getMonth()+1}/${date.getDate()}`
}

// 自动调整textarea高度
const autoResizeTextarea = () => {
  nextTick(() => {
    if (messageInput.value) {
      messageInput.value.style.height = 'auto'
      const newHeight = Math.min(messageInput.value.scrollHeight, 120)
      messageInput.value.style.height = newHeight + 'px'
    }
  })
}

// 处理键盘事件
const handleKeydown = (event) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendMessage()
  }
  autoResizeTextarea()
}

// 滚动到底部
const messagesContainer = ref(null)
const shouldAutoScroll = ref(true)
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value && shouldAutoScroll.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

const handleScroll = () => {
  if (!messagesContainer.value) return
  const { scrollTop, scrollHeight, clientHeight } = messagesContainer.value
  const isAtBottom = scrollHeight - scrollTop - clientHeight < 50
  shouldAutoScroll.value = isAtBottom
}

let savedScrollPosition = 0

const fetchConversations = async (showLoading = false) => {
  if (!currentUserId.value) return
  
  if (showLoading && isInitialLoad.value) {
    loadingConversations.value = true
  }
  
  try {
    const response = await fetch(`http://localhost:8080/conversations/user/${currentUserId.value}?page=0&size=20`, {
      headers: { 'Authorization': `Bearer ${authStore.token}` }
    })
    const result = await response.json()
    
    if (result.code === 200 && result.data?.content) {
      const newConversations = result.data.content
      conversations.value = newConversations
      
      if (activeConversationId.value) {
        const updatedConv = newConversations.find(
          c => (c.conversationId || c.id) === activeConversationId.value
        )
        if (updatedConv) {
          activeConversation.value = updatedConv
        }
      }
      
      if (isInitialLoad.value) {
        isInitialLoad.value = false
      }
    }
  } catch (err) {
    console.error('获取对话列表失败:', err)
    if (showLoading) {
      showNotification('加载失败', err.message)
    }
  } finally {
    if (showLoading && loadingConversations.value) {
      loadingConversations.value = false
    }
  }
}

const selectConversation = async (conv) => {
  const conversationId = conv.conversationId || conv.id
  
  activeConversation.value = conv
  activeConversationId.value = conversationId
  shouldAutoScroll.value = true
  await loadMessages(conversationId, true)
  await markMessagesAsRead(conversationId)
  scrollToBottom()
}

const updateMessagesIncrementally = (newMessages) => {
  if (!newMessages || newMessages.length === 0) return false
  
  const existingMessageMap = new Map()
  messages.value.forEach(msg => {
    existingMessageMap.set(msg.id, msg)
  })
  
  let hasChanges = false
  const updatedMessages = []
  
  for (const newMsg of newMessages) {
    const existingMsg = existingMessageMap.get(newMsg.id)
    if (!existingMsg) {
      updatedMessages.push(newMsg)
      hasChanges = true
    } else if (JSON.stringify(existingMsg) !== JSON.stringify(newMsg)) {
      updatedMessages.push(newMsg)
      hasChanges = true
    } else {
      updatedMessages.push(existingMsg)
    }
  }
  
  if (hasChanges) {
    updatedMessages.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt))
    messages.value = updatedMessages
  }
  
  return hasChanges
}

const loadMessages = async (conversationId, showLoading = false) => {
  if (!conversationId) return
  
  if (showLoading && messages.value.length === 0) {
    loadingMessages.value = true
  }
  
  if (messagesContainer.value && !shouldAutoScroll.value) {
    savedScrollPosition = messagesContainer.value.scrollTop
  }
  
  try {
    const response = await fetch(`http://localhost:8080/conversations/${conversationId}?userId=${currentUserId.value}`, {
      headers: { 'Authorization': `Bearer ${authStore.token}` }
    })
    const result = await response.json()
    
    if (result.code === 200 && result.data) {
      let newMessages = []
      if (result.data.messages && Array.isArray(result.data.messages)) {
        newMessages = result.data.messages
      } else if (result.data.messageList && Array.isArray(result.data.messageList)) {
        newMessages = result.data.messageList
      } else {
        newMessages = await loadMessagesFromAPI(conversationId)
      }
      
      const hasChanges = updateMessagesIncrementally(newMessages)
      
      if (hasChanges && !shouldAutoScroll.value && savedScrollPosition > 0) {
        nextTick(() => {
          if (messagesContainer.value) {
            messagesContainer.value.scrollTop = savedScrollPosition
          }
        })
      } else if (hasChanges && shouldAutoScroll.value) {
        scrollToBottom()
      }
    } else {
      const newMessages = await loadMessagesFromAPI(conversationId)
      const hasChanges = updateMessagesIncrementally(newMessages)
      
      if (hasChanges && !shouldAutoScroll.value && savedScrollPosition > 0) {
        nextTick(() => {
          if (messagesContainer.value) {
            messagesContainer.value.scrollTop = savedScrollPosition
          }
        })
      } else if (hasChanges && shouldAutoScroll.value) {
        scrollToBottom()
      }
    }
  } catch (err) {
    console.error('加载消息失败:', err)
    const newMessages = await loadMessagesFromAPI(conversationId)
    const hasChanges = updateMessagesIncrementally(newMessages)
    
    if (hasChanges && !shouldAutoScroll.value && savedScrollPosition > 0) {
      nextTick(() => {
        if (messagesContainer.value) {
          messagesContainer.value.scrollTop = savedScrollPosition
        }
      })
    } else if (hasChanges && shouldAutoScroll.value) {
      scrollToBottom()
    }
  } finally {
    if (showLoading) {
      loadingMessages.value = false
    }
  }
}

const loadMessagesFromAPI = async (conversationId) => {
  try {
    const response = await fetch(`http://localhost:8080/messages/conversation/${conversationId}/all?userId=${currentUserId.value}`, {
      headers: { 'Authorization': `Bearer ${authStore.token}` }
    })
    const result = await response.json()
    
    if (result.code === 200 && result.data) {
      return Array.isArray(result.data) ? result.data : (result.data.content || [])
    }
    return []
  } catch (err) {
    console.error('加载消息失败:', err)
    return []
  }
}

const markMessagesAsRead = async (conversationId) => {
  try {
    await fetch(`http://localhost:8080/messages/read/conversation/${conversationId}?userId=${currentUserId.value}`, {
      method: 'PATCH',
      headers: { 'Authorization': `Bearer ${authStore.token}` }
    })
    await fetchConversations(false)
  } catch (err) {
    console.error('标记已读失败', err)
  }
}

const sendMessage = async () => {
  if (!newMessage.value.trim()) return
  if (!activeConversation.value) {
    showNotification('提示', '请先选择一个对话')
    return
  }
  
  sending.value = true
  const content = newMessage.value.trim()
  const receiverId = activeConversation.value.otherUser?.id
  
  if (!receiverId) {
    showNotification('错误', '无法获取接收者信息')
    sending.value = false
    return
  }
  
  const tempMessage = {
    id: Date.now(),
    senderId: currentUserId.value,
    receiverId: receiverId,
    content: content,
    createdAt: new Date().toISOString(),
    isRead: false
  }
  messages.value.push(tempMessage)
  scrollToBottom()
  newMessage.value = ''
  
  if (messageInput.value) {
    messageInput.value.style.height = 'auto'
  }
  
  try {
    const response = await fetch(`http://localhost:8080/messages/send?senderId=${currentUserId.value}&receiverId=${receiverId}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${authStore.token}`
      },
      body: JSON.stringify({ content })
    })
    const result = await response.json()
    
    if (result.code === 200 || result.code === 201) {
      messages.value = messages.value.filter(m => m.id !== tempMessage.id)
      const conversationId = activeConversation.value.conversationId || activeConversation.value.id
      await loadMessages(conversationId, false)
      await fetchConversations(false)
      scrollToBottom()
    } else {
      messages.value = messages.value.filter(m => m.id !== tempMessage.id)
      throw new Error(result.message || '发送失败')
    }
  } catch (err) {
    console.error('发送失败:', err)
    showNotification('发送失败', err.message)
    scrollToBottom()
  } finally {
    sending.value = false
  }
}

const startPolling = () => {
  pollingInterval = setInterval(async () => {
    if (!currentUserId.value || !document.hasFocus()) return
    
    await fetchConversations(false)
    
    if (activeConversationId.value) {
      const oldMessageCount = messages.value.length
      await loadMessages(activeConversationId.value, false)
      
      if (messages.value.length > oldMessageCount && shouldAutoScroll.value) {
        scrollToBottom()
      }
      
      await markMessagesAsRead(activeConversationId.value)
    }
  }, 5000)
}

const handleVisibilityChange = () => {
  if (document.hidden) {
    if (pollingInterval) {
      clearInterval(pollingInterval)
      pollingInterval = setInterval(async () => {
        if (!currentUserId.value) return
        await fetchConversations(false)
        if (activeConversationId.value) {
          await loadMessages(activeConversationId.value, false)
          await markMessagesAsRead(activeConversationId.value)
        }
      }, 15000)
    }
  } else {
    if (pollingInterval) {
      clearInterval(pollingInterval)
      pollingInterval = setInterval(async () => {
        if (!currentUserId.value) return
        await fetchConversations(false)
        if (activeConversationId.value) {
          await loadMessages(activeConversationId.value, false)
          await markMessagesAsRead(activeConversationId.value)
          if (shouldAutoScroll.value) {
            scrollToBottom()
          }
        }
      }, 5000)
    }
    fetchConversations(false)
    if (activeConversationId.value) {
      loadMessages(activeConversationId.value, false)
    }
  }
}

onMounted(async () => {
  if (!authStore.isLoggedIn) {
    router.push('/login')
    return
  }
  
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.addEventListener('scroll', handleScroll)
    }
  })
  
  document.addEventListener('visibilitychange', handleVisibilityChange)
  
  await fetchConversations(true)
  startPolling()
})

onUnmounted(() => {
  if (pollingInterval) clearInterval(pollingInterval)
  if (messagesContainer.value) {
    messagesContainer.value.removeEventListener('scroll', handleScroll)
  }
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})

watch(messages, () => {
  if (shouldAutoScroll.value) {
    scrollToBottom()
  }
}, { deep: false })

watch(newMessage, () => {
  autoResizeTextarea()
})
</script>

<style scoped>
* {
  scrollbar-width: none;
  -ms-overflow-style: none;
}

*::-webkit-scrollbar {
  display: none;
}

.conversations-page {
  min-height: 84.8vh;
  max-height: 84.8vh;
  background: #ffffff;
  display: flex;
  flex-direction: column;
}

/* 顶部导航栏 */
.page-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: #ffffff;
  border-bottom: 1px solid #e8ecf0;
  padding: 16px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  backdrop-filter: blur(10px);
  background-color: rgba(255, 255, 255, 0.98);
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: transparent;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 500;
  color: #62055f;
  cursor: pointer;
  transition: all 0.2s ease;
}

.back-btn:hover {
  background: #f5f2f7;
}

.back-btn svg {
  width: 20px;
  height: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0;
  letter-spacing: -0.3px;
}

.header-placeholder {
  width: 80px;
}

.conversations-container {
  flex: 1;
  width: 100%;
  background: #ffffff;
  display: flex;
  overflow: hidden;
}

/* 左侧边栏 */
.conversations-sidebar {
  width: 360px;
  background: #fafbfc;
  border-right: 1px solid #e8ecf0;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-header {
  padding: 20px 20px 12px;
}

.sidebar-header h2 {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0;
}

.conversations-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 0;
}

.conversation-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 20px;
  cursor: pointer;
  transition: all 0.2s ease;
  border-radius: 12px;
  margin: 0 8px;
}

.conversation-item:hover {
  background: #f0f2f8;
}

.conversation-item.active {
  background: #ede8f5;
}

.conv-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  background: #e8ecf0;
}

.conv-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.conv-info {
  flex: 1;
  min-width: 0;
}

.conv-name {
  font-weight: 600;
  color: #1a1a2e;
  font-size: 15px;
  margin-bottom: 4px;
}

.conv-last-msg {
  font-size: 13px;
  color: #6c7a8e;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conv-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.conv-time {
  font-size: 11px;
  color: #9aa6b5;
}

.unread-badge {
  background: #62055f;
  color: white;
  font-size: 11px;
  font-weight: 500;
  padding: 2px 7px;
  border-radius: 12px;
  min-width: 20px;
  text-align: center;
}

/* 右侧聊天区 */
.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #ffffff;
}

.chat-header {
  padding: 16px 24px;
  border-bottom: 1px solid #e8ecf0;
  background: #ffffff;
}

.chat-user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chat-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.chat-user-name {
  font-weight: 600;
  font-size: 17px;
  color: #1a1a2e;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 24px 28px;
  background: #fafbfc;
}

.messages-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-item {
  display: flex;
  max-width: 65%;
}

.message-item.own-message {
  align-self: flex-end;
}

.message-bubble {
  background: #ffffff;
  padding: 10px 16px;
  border-radius: 18px;
  border-top-left-radius: 4px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  border: 1px solid #e8ecf0;
  max-width: 100%;
  word-wrap: break-word;
  word-break: break-word;
  white-space: normal;
}

.own-message .message-bubble {
  background: #62055f;
  border-color: #62055f;
  border-top-right-radius: 4px;
  border-top-left-radius: 18px;
}

.message-content {
  font-size: 14px;
  line-height: 1.5;
  color: #1a1a2e;
  white-space: pre-wrap;
  word-wrap: break-word;
  word-break: break-word;
}

.own-message .message-content {
  color: #ffffff;
}

.message-time {
  font-size: 10px;
  margin-top: 6px;
  color: #9aa6b5;
  text-align: right;
}

.own-message .message-time {
  color: rgba(255, 255, 255, 0.7);
}

/* 输入区域 */
.input-area {
  padding: 16px 24px;
  border-top: 1px solid #e8ecf0;
  background: #ffffff;
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.message-input {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid #dce1e8;
  border-radius: 12px;
  font-size: 14px;
  background: #ffffff;
  color: #1a1a2e;
  outline: none;
  transition: all 0.2s ease;
  font-family: inherit;
  resize: none;
  overflow-y: auto;
  min-height: 44px;
  max-height: 120px;
  line-height: 1.5;
}

.message-input:focus {
  border-color: #62055f;
  box-shadow: 0 0 0 3px rgba(98, 5, 95, 0.08);
}

.message-input::placeholder {
  color: #9aa6b5;
}

.send-btn {
  background: #62055f;
  color: white;
  border: none;
  border-radius: 12px;
  padding: 10px 24px;
  font-weight: 500;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  height: 44px;
  white-space: nowrap;
}

.send-btn:hover:not(:disabled) {
  background: #7a0e76;
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.empty-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #9aa6b5;
}

.empty-chat-icon {
  font-size: 48px;
  margin-bottom: 12px;
  opacity: 0.5;
}

.empty-chat-text {
  font-size: 14px;
}

.loading-sidebar,
.loading-messages {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  gap: 12px;
  color: #9aa6b5;
}

.loading-spinner {
  width: 28px;
  height: 28px;
  border: 2px solid #e8ecf0;
  border-top-color: #62055f;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.empty-conversations,
.empty-messages {
  text-align: center;
  padding: 48px 20px;
  color: #9aa6b5;
  font-size: 14px;
}

@media (max-width: 768px) {
  .page-header {
    padding: 12px 16px;
  }
  
  .page-title {
    font-size: 18px;
  }
  
  .back-btn span {
    display: none;
  }
  
  .back-btn svg {
    width: 24px;
    height: 24px;
  }
  
  .conversations-sidebar {
    width: 280px;
  }
  
  .message-item {
    max-width: 85%;
  }
  
  .messages-container {
    padding: 16px;
  }
  
  .input-area,
  .chat-header {
    padding: 12px 16px;
  }
  
  .send-btn {
    padding: 8px 18px;
  }
}
</style>