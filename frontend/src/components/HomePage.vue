<script setup>
import { useAuthStore } from '../stores/auth'
import { ref } from 'vue'
import AlertBox from './SmallComponents/AlertBox.vue'
import router from '../router'

// 使用 auth store
const authStore = useAuthStore()

// 控制下拉菜单显示
const showDropdown = ref(false)

// 控制通知框显示
const showAlert = ref(false)
const alertConfig = ref({
  title: '系统通知',
  content: '',
  htmlContent: '',
  confirmText: '确定'
})

// 显示通知的方法
const showNotification = (title, content, isHtml = false) => {
  alertConfig.value = {
    title: title,
    content: isHtml ? '' : content,
    htmlContent: isHtml ? content : '',
    confirmText: '确定'
  }
  showAlert.value = true
}

// 处理退出登录
const handleLogout = () => {
  const username = authStore.user?.name || '用户'
  showNotification('退出登录', `再见，${username}！期待你的再次使用。`)
  authStore.logout()
  showDropdown.value = false
}

// 处理登录
const handleLogin = () => {
  // 模拟登录成功
  router.push('/login')
}

// 切换下拉菜单
const toggleDropdown = () => {
  if (authStore.isLoggedIn) {
    showDropdown.value = !showDropdown.value
  }
}

// 关闭下拉菜单
const closeDropdown = () => {
  showDropdown.value = false
}

// 处理个人主页点击
const handleProfile = () => {
  showDropdown.value = false
  router.push('/my/profile')
}

// 处理消息点击
const handleMessages = () => {
  showDropdown.value = false
  router.push('/messages')
}


const handleAdmin = () => {
  showNotification('功能开发中', '管理后台功能正在开发中，敬请期待！')
  showDropdown.value = false
}

// 通知框确认后的回调
const handleAlertConfirm = () => {
  console.log('通知框已关闭')
}
</script>

<template>
  <div class="home-page" @click="closeDropdown">
    <header class="fixed-header">
      <div class="header-left">
        <h1 class="header-headline">校园互助服务平台</h1>
      </div>
      <div class="header-right">
        <!-- 未登录时显示按钮 -->
        <template v-if="!authStore.isLoggedIn">
          <button class="header-btn" @click="handleLogin">登录 / 注册</button>
        </template>
        
        <!-- 已登录时显示头像和下拉菜单 -->
        <template v-else>
          <div class="user-info">{{ authStore.user?.name || '用户' }}</div>
          <div class="avatar-wrapper" @click.stop="toggleDropdown">
            <img 
              :src="authStore.user?.avatarPath? 'http://localhost:8080'+authStore.user.avatarPath: 'https://picsum.photos/40/40?random=1'" 
              alt="用户头像"
              class="avatar"
              :class="{ 'avatar-active': showDropdown }"
            />
          </div>
          
          <!-- 下拉选项框 -->
          <transition name="dropdown">
            <div v-if="showDropdown" class="dropdown-menu">
              <button class="dropdown-item" @click="handleProfile">
                个人主页
              </button>
              <div class="dropdown-divider"></div>
              <button class="dropdown-item" @click="handleMessages">
                消息
              </button>
              <div class="dropdown-divider"></div>
              <template v-if="authStore.user?.isAdmin">
                <button class="dropdown-item" @click="handleAdmin">管理后台</button>
              </template>
              <button class="dropdown-item logout-item" @click="handleLogout">
                退出登录
              </button>
            </div>
          </transition>
        </template>
      </div>
    </header>
    <main class="main-content">
      <div v-if="authStore.isLoggedIn" class="welcome-message">
        <p>欢迎回来，{{ authStore.user?.name || '用户' }}！</p>
      </div>
    </main>

    <!-- 通知框组件 -->
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
.home-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header-headline {
  font-size: 50px;
  font-weight:600;
  color: #ffff;
  margin: 0;
}

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

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;  /* 垂直居中对齐 */
  gap: 10px;  /* 增加间距 */
  position: relative;
}

/* 用户名样式 - 垂直居中 */
.user-info { 
  color: #ffff;
  font-size: 16px;
  font-weight: 500;
  line-height: 1;  /* 确保垂直居中 */
  display: flex;
  align-items: center;
  height: 100%;  /* 占满父容器高度 */
}

/* 头像容器 - 垂直居中 */
.avatar-wrapper {
  cursor: pointer;
  position: relative;
  display: flex;
  align-items: center;
  height: 100%;  /* 占满父容器高度 */
}

/* 头像样式 */
.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid transparent;
  transition: all 0.2s ease;
  cursor: pointer;
  display: block;  /* 移除行内元素默认间距 */
}

.avatar:hover {
  transform: scale(0.98);
  box-shadow: inset 0 2px 8px rgba(0, 0, 0, 0.2), 0 2px 4px rgba(255, 255, 255, 0.1);
}

.avatar-active {
  transform: scale(0.96);
  box-shadow: inset 0 3px 10px rgba(0, 0, 0, 0.25);
}

.header-btn {
  padding: 8px 16px;
  border: none;
  background: none;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s ease;
  color: white;
  display: flex;
  align-items: center;
}

.header-btn:hover {
  color: #d4b0d3;
}

/* 下拉菜单样式 */
.dropdown-menu {
  position: absolute;
  top: calc(100% + 12px);
  right: 0;
  min-width: 160px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  z-index: 1001;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  width: 100%;
  border: none;
  background: white;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.2s ease;
  font-size: 14px;
  color: #333;
}

.dropdown-item:hover {
  background-color: #f5f5f5;
}

.dropdown-icon {
  font-size: 16px;
  width: 20px;
  text-align: center;
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

/* 下拉菜单动画 */
.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.25s ease;
}

.dropdown-enter-from {
  opacity: 0;
  transform: translateY(-10px);
}

.dropdown-enter-to {
  opacity: 1;
  transform: translateY(0);
}

.dropdown-leave-from {
  opacity: 1;
  transform: translateY(0);
}

.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.main-content {
  margin-top: 60px;
  flex: 1;
  padding: 20px;
}

.welcome-message {
  text-align: center;
  padding: 40px;
  font-size: 18px;
  color: #62055f;
}
</style>