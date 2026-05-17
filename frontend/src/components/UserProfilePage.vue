<template>
  <div class="user-profile-page" @click="closeDropdowns">
    <header class="fixed-header">
      <div class="header-left">
        <h1 class="header-headline">校园互助服务平台</h1>
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
            <button class="dropdown-item" @click="goToProfile">
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
      </div>
    </header>

    <main class="main-content">
      <div class="profile-container">
        <!-- 左侧：头像卡片 -->
        <div class="profile-sidebar">
          <div class="avatar-card">
            <div class="avatar-large-wrapper" @click="triggerAvatarUpload">
              <img 
                :src="displayAvatar" 
                alt="用户头像"
                class="avatar-large"
              />
              <div class="avatar-edit-overlay">
                <span>更换头像</span>
              </div>
            </div>
            <h3 class="user-name-display">{{ editableName || authStore.user?.name || '用户' }}</h3>
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

            <input 
              type="file" 
              ref="avatarInput" 
              style="display: none" 
              accept="image/*"
              @change="handleAvatarChange"
            />
          </div>
        </div>

        <!-- 右侧：信息编辑与操作区 -->
        <div class="profile-content">
          <!-- 基本信息卡片 -->
          <div class="info-card">
            <h3 class="card-title">基本信息</h3>
            <div class="info-form">
              <div class="form-row">
                <label class="form-label">昵称</label>
                <div class="form-field">
                  <input 
                    v-model="editableName" 
                    type="text" 
                    class="name-input"
                    placeholder="请输入昵称"
                    @blur="saveName"
                  />
                  <button class="edit-btn" @click="saveName">保存</button>
                </div>
              </div>
              <div class="form-row">
                <label class="form-label">手机号</label>
                <div class="form-field">
                  <span class="readonly-text">{{ authStore.user?.phone || '未绑定' }}</span>
                </div>
              </div>
              <div class="form-row">
                <label class="form-label">用户权限</label>
                <div class="form-field">
                  <span class="readonly-text">{{ authStore.user?.isAdmin ? '系统管理员':'普通用户' }}</span>
                </div>
              </div>
              <div class="form-row">
                <label class="form-label">注册时间</label>
                <div class="form-field">
                  <span class="readonly-text">{{ formatDate(authStore.user?.isAdmin) || '未知' }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 功能按钮区域 -->
          <div class="actions-card">
            <h3 class="card-title">我的服务</h3>
            <div class="button-grid">
              <button class="action-btn" @click="handleMyOrders">
                <span class="btn-icon">📋</span>
                我的订单
              </button>
              <button class="action-btn" @click="handleMessagesCenter">
                <span class="btn-icon">💬</span>
                我的消息
              </button>
              <button class="action-btn" @click="handleMyNeeds">
                <span class="btn-icon">📢</span>
                我的需求
              </button>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 通知框组件（与主页面一致） -->
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
import { useAuthStore } from '../stores/auth'
import { ref, computed, onMounted } from 'vue'
import AlertBox from './SmallComponents/AlertBox.vue'
import router from '../router'
import axios from 'axios'

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

const userScoreInfo = ref({
  averageScore: null,
  scoreNum: null
})

// 可编辑的昵称
const editableName = ref('')

// 头像上传相关ref
const avatarInput = ref(null)


const showNotification = (title, content, isHtml = false) => {
  alertConfig.value = {
    title: title,
    content: isHtml ? '' : content,
    htmlContent: isHtml ? content : '',
    confirmText: '确定'
  }
  showAlert.value = true
}

const displayAvatar = computed(() => {
  if (authStore.user?.avatarPath) {
    return 'http://localhost:8080'+authStore.user.avatarPath
  }
  return 'https://picsum.photos/120/120?random=1'
})


const formatDate = (dateStr) => {
  if (!dateStr) return null
  try {
    const date = new Date(dateStr)
    return date.toLocaleDateString('zh-CN')
  } catch {
    return dateStr
  }
}

// 保存昵称
const saveName = async () => {
  if (!editableName.value || editableName.value === authStore.user?.name) {
    alert('请输入昵称')
    return
  }
  try {
    // TODO: 调用后端API更新用户昵称
    const response = await axios.put('http://localhost:8080/users/name', {
      id: authStore.user?.id,
      name: editableName.value,
      avatarPath: authStore.user?.avatarPath
    },{headers:{ 'Content-Type': 'application/json'}})
    // 模拟更新store中的用户信息
    if(response.data.success && authStore.user){
      authStore.updateUserName(editableName.value)
      showNotification('更新成功', '您的用户昵称已更新！')
    }else{
      console.log(response)
    }
    
  } catch (error) {
    showNotification('更新失败', error.message || '请稍后重试')
    // 恢复原昵称
    editableName.value = authStore.user?.name || ''
  }
}

// 触发头像上传
const triggerAvatarUpload = () => {
  avatarInput.value?.click()
}

// 处理头像文件选择
const handleAvatarChange = async(event) => {
  const file = event.target.files[0]
  if (!file) return
  
  if (!file.type.startsWith('image/')) {
    showNotification('格式错误', '请选择图片文件')
    return
  }
  
  if (file.size > 2 * 1024 * 1024) {
    showNotification('文件过大', '请选择小于2MB的图片')
    return
  }
  
  try {
    const formData = new FormData()
    formData.append('file', file)

    const response = await axios.post('http://localhost:8080/api/files/upload-single', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })

    if (response.data.code === 200) {
      const avatarUrl = response.data.data
      const ret = await axios.put('http://localhost:8080/users/avatarPath', {
        id: authStore.user?.id,
        name: authStore.user?.name,
        avatarPath: avatarUrl
      },{headers:{ 'Content-Type': 'application/json'}})

      if (ret.data.success) {
        authStore.updateUserAvatarPath(avatarUrl)
        showNotification('更新成功', '您的用户头像已更新！')
      }
      
      if (authStore.user) {
        authStore.user.avatarPath = avatarUrl
      }
      
      

      //showNotification('上传成功', '头像已更新')
    } else {
      throw new Error(response.data.message || '上传失败')
    }

  } catch (error) {
    console.error('头像上传失败:', error)
    showNotification('上传失败', error.response?.data?.message || error.message || '请稍后重试')
  } finally {
    event.target.value = ''
  }
}

// 处理退出登录（复用主页面逻辑）
const handleLogout = () => {
  const username = authStore.user?.name || '用户'
  showNotification('退出登录', `再见，${username}！期待你的再次使用。`)
  authStore.logout()
  showDropdown.value = false
  router.push('/')
}

// 关闭下拉菜单
const closeDropdowns = () => {
  showDropdown.value = false
}

// 个人主页（当前页面）
const goToProfile = () => {
  // 已在当前页面，无需操作
  showDropdown.value = false
}

// 消息（下拉菜单中的消息）
const handleMessages = () => {
  showNotification('功能开发中', '消息中心功能正在开发中，敬请期待！')
  showDropdown.value = false
}

// 管理后台
const handleAdmin = () => {
  showNotification('功能开发中', '管理后台功能正在开发中，敬请期待！')
  showDropdown.value = false
}

// 功能按钮：我的订单
const handleMyOrders = () => {
  showNotification('功能开发中', '我的订单功能正在开发中，敬请期待！')
}

// 功能按钮：我的消息（内容区的消息）
const handleMessagesCenter = () => {
  showNotification('功能开发中', '消息中心功能正在开发中，敬请期待！')
}

// 功能按钮：我的需求
const handleMyNeeds = () => {
  showNotification('功能开发中', '我的需求功能正在开发中，敬请期待！')
}

// 通知框确认后的回调
const handleAlertConfirm = () => {
  console.log('通知框已关闭')
}

// 初始化用户信息
const initUserInfo = async () => {
  // 确保已登录
  if (!authStore.isLoggedIn) {
    router.push('/login')
    return
  }
  
  // 设置可编辑昵称的初始值
  editableName.value = authStore.user?.name || ''
  
  // TODO: 从后端获取用户评分信息
  // const scoreInfo = await api.getUserScoreInfo(authStore.user.id)
  // userScoreInfo.value = scoreInfo
  
  // 模拟示例数据（实际应替换为真实API调用）
  userScoreInfo.value = {
    averageScore: authStore.user?.averageScore || null,
    scoreNum: authStore.user?.scoreNum || null
  }
}

onMounted(() => {
  initUserInfo()
})
</script>

<style scoped>
/* 保持与HomePage一致的风格，并增加个人页面特有样式 */
.user-profile-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f9f9f9;
}

/* 复用主页面的头部样式 */
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
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  position: relative;
}

.user-info {
  color: #ffff;
  font-size: 16px;
  font-weight: 500;
  line-height: 1;
  display: flex;
  align-items: center;
  height: 100%;
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
  border: 2px solid transparent;
  transition: all 0.2s ease;
  cursor: pointer;
  display: block;
}

.avatar:hover {
  transform: scale(0.98);
  box-shadow: inset 0 2px 8px rgba(0, 0, 0, 0.2), 0 2px 4px rgba(255, 255, 255, 0.1);
}

.avatar-active {
  transform: scale(0.96);
  box-shadow: inset 0 3px 10px rgba(0, 0, 0, 0.25);
}

/* 下拉菜单 */
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

/* 主内容区域 */
.main-content {
  margin-top: 75px;
  flex: 1;
  padding: 40px;
  background-color: #f5f5f5;
}

.profile-container {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  gap: 30px;
  flex-wrap: wrap;
}

/* 左侧边栏 */
.profile-sidebar {
  flex: 0 0 300px;
}

.avatar-card {
  background: white;
  border-radius: 16px;
  padding: 30px 20px;
  text-align: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transition: transform 0.2s;
}

.avatar-large-wrapper {
  position: relative;
  width: 120px;
  height: 120px;
  margin: 0 auto 20px;
  cursor: pointer;
  border-radius: 50%;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.avatar-large {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: filter 0.3s;
}

.avatar-edit-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
  color: white;
  font-size: 14px;
  font-weight: 500;
  text-align: center;
  border-radius: 50%;
}

.avatar-large-wrapper:hover .avatar-edit-overlay {
  opacity: 1;
}

.user-name-display {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin: 0 0 16px 0;
}

.user-stats {
  display: flex;
  justify-content: space-around;
  border-top: 1px solid #eee;
  padding-top: 16px;
}

.stat-item {
  text-align: center;
}

.stat-label {
  display: block;
  font-size: 12px;
  color: #888;
  margin-bottom: 6px;
}

.stat-value {
  display: block;
  font-size: 18px;
  font-weight: 600;
  color: #62055f;
}

/* 右侧内容区 */
.profile-content {
  flex: 1;
  min-width: 280px;
}

.info-card,
.actions-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0 0 20px 0;
  padding-bottom: 12px;
  border-bottom: 2px solid #f0f0f0;
}

.info-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.form-label {
  width: 80px;
  font-weight: 500;
  color: #666;
  font-size: 14px;
}

.form-field {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.name-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #000;
  border-radius: 10px;
  background-color: white;
  font-size: 14px;
  color: #000;
  transition: border-color 0.2s;
  min-width: 180px;
}

.name-input:focus {
  outline: none;
  border-color: #62055f;
}

.edit-btn {
  padding: 6px 16px;
  background-color: #62055f;
  color: white;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-size: 13px;
  transition: background-color 0.2s;
}

.edit-btn:hover {
  background-color: #4a0447;
}

.readonly-text {
  color: #333;
  font-size: 14px;
  padding: 6px 0;
}

/* 功能按钮网格 */
.button-grid {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.action-btn {
  flex: 1;
  min-width: 140px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 14px 20px;
  background-color: #f8f8f8;
  border: 1px solid #e0e0e0;
  border-radius: 40px;
  font-size: 15px;
  font-weight: 500;
  color: #333;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-btn:hover {
  background-color: #62055f;
  border-color: #62055f;
  color: white;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(98, 5, 95, 0.2);
}

.btn-icon {
  font-size: 18px;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .profile-container {
    flex-direction: column;
  }
  
  .profile-sidebar {
    flex: auto;
  }
  
  .main-content {
    padding: 20px;
  }
  
  .fixed-header {
    padding: 0 20px;
  }
  
  .header-headline {
    font-size: 28px;
  }
}
</style>