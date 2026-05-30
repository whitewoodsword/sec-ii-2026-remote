<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import AlertBox from './SmallComponents/AlertBox.vue'

const authStore = useAuthStore()
const router = useRouter()

// 表单数据
const formData = ref({
  title: '',
  description: '',
  category: '',
  location: '',
  deadline: '',
  reward: null,
  pictureFiles: []
})


// 分类选项
const categories = ['快递代取', '学习辅导', '二手交易', '活动组队', '其他']

// 加载状态
const loading = ref(false)
const uploading = ref(false)

// 表单验证错误
const errors = ref({})

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
    router.back()
}

// 验证表单
const validateForm = () => {
  errors.value = {}
  
  if (!formData.value.title || formData.value.title.trim().length === 0) {
    errors.value.title = '请输入需求标题'
  } else if (formData.value.title.length > 100) {
    errors.value.title = '标题不能超过100个字符'
  }
  
  if (!formData.value.category) {
    errors.value.category = '请选择需求分类'
  }
  
  if (formData.value.reward !== null && formData.value.reward !== '') {
    const rewardNum = parseFloat(formData.value.reward)
    if (isNaN(rewardNum) || rewardNum < 0) {
      errors.value.reward = '酬金必须为正数'
    }
  }
  
  if (formData.value.deadline) {
    const deadlineDate = new Date(formData.value.deadline)
    if (deadlineDate <= new Date()) {
      errors.value.deadline = '截止时间必须晚于当前时间'
    }
  }
  
  return Object.keys(errors.value).length === 0
}

// 上传图片
const uploadImages = async () => {
  if (formData.value.pictureFiles.length === 0) {
    return ''
  }
  
  uploading.value = true
  const formDataObj = new FormData()
  
  for (const file of formData.value.pictureFiles) {
    formDataObj.append('files', file)
  }
  
  try {
    const response = await fetch('http://localhost:8080/api/files/upload', {
      method: 'POST',
      body: formDataObj
    })
    
    const result = await response.json()
    
    if (result.code === 200 && result.data && result.data.urls) {
      return result.data.urls.join(';')
    } else {
      throw new Error(result.message || '图片上传失败')
    }
  } catch (error) {
    console.error('图片上传失败:', error)
    showNotification('上传失败', '图片上传失败，请重试')
    return null
  } finally {
    uploading.value = false
  }
}

// 处理图片选择
const handleImageSelect = (event) => {
  const files = Array.from(event.target.files)
  const maxSize = 5 * 1024 * 1024 // 5MB
  
  for (const file of files) {
    if (file.size > maxSize) {
      showNotification('文件过大', `${file.name} 超过5MB限制`)
      return
    }
    if (!file.type.startsWith('image/')) {
      showNotification('文件类型错误', `${file.name} 不是图片文件`)
      return
    }
  }
  
  formData.value.pictureFiles.push(...files)
  event.target.value = ''
}

// 移除待上传图片
const removePendingImage = (index) => {
  formData.value.pictureFiles.splice(index, 1)
}

// 获取图片预览URL
const getPreviewUrl = (file) => {
  return URL.createObjectURL(file)
}

// 格式化日期时间
const formatDateTime = (dateTimeStr) => {
  if (!dateTimeStr) return ''
  const date = new Date(dateTimeStr)
  return date.toISOString().slice(0, 16)
}

// 提交表单
const handleSubmit = async () => {
  if (!validateForm()) {
    showNotification('表单验证失败', '请检查并填写正确的信息')
    return
  }
  
  if (!authStore.isLoggedIn) {
    showNotification('请先登录', '您需要登录后才能发布需求')
    router.push('/login')
    return
  }
  
  loading.value = true
  
  // 上传图片
  let pictureUrls = ''
  if (formData.value.pictureFiles.length > 0) {
    const urls = await uploadImages()
    if (urls === null) {
      loading.value = false
      return
    }
    pictureUrls = urls
  }
  
  // 构建请求数据
  const demandData = {
    title: formData.value.title.trim(),
    description: formData.value.description?.trim() || '',
    category: formData.value.category,
    publisherId: authStore.user.id,
    location: formData.value.location?.trim() || '',
    deadline: formData.value.deadline ? new Date(formData.value.deadline).toISOString() : null,
    reward: formData.value.reward ? parseFloat(formData.value.reward) : null,
    pictureUrls: pictureUrls
  }
  
  try {
    const response = await fetch('http://localhost:8080/demands/create', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${authStore.token}`
      },
      body: JSON.stringify(demandData)
    })
    
    const result = await response.json()
    
    if (result.code === 200 || response.status === 201) {
      showNotification('发布成功', '您的需求已成功发布！即将返回上一页。')
    } else {
      throw new Error(result.message || '发布失败')
    }
  } catch (error) {
    console.error('发布失败:', error)
    showNotification('发布失败', error.message || '网络错误，请重试')
  } finally {
    loading.value = false
  }
}

// 返回上一页
const goBack = () => {
  router.back()
}
</script>

<template>
  <div class="create-demand-page">
    <div class="create-demand-container">
      <!-- 头部 -->
      <div class="page-header">
        <button class="back-btn" @click="goBack">
          <span class="back-icon">←</span> 返回
        </button>
        <h1 class="page-title">发布新需求</h1>
        <div class="placeholder"></div>
      </div>

      <!-- 表单 -->
      <form @submit.prevent="handleSubmit" class="demand-form">
        <!-- 标题 -->
        <div class="form-section">
          <label class="form-label required">需求标题</label>
          <input
            type="text"
            v-model="formData.title"
            placeholder="请输入需求标题（不超过100字）"
            class="form-input"
            :class="{ 'error': errors.title }"
            maxlength="100"
          />
          <p v-if="errors.title" class="error-message">{{ errors.title }}</p>
          <p class="hint-text">{{ formData.title.length }}/100</p>
        </div>

        <!-- 分类 -->
        <div class="form-section">
          <label class="form-label required">需求分类</label>
          <div class="category-group">
            <button
              v-for="cat in categories"
              :key="cat"
              type="button"
              class="category-btn"
              :class="{ 'active': formData.category === cat }"
              @click="formData.category = cat"
            >
              {{ cat }}
            </button>
          </div>
          <p v-if="errors.category" class="error-message">{{ errors.category }}</p>
        </div>

        <!-- 描述 -->
        <div class="form-section">
          <label class="form-label">需求描述</label>
          <textarea
            v-model="formData.description"
            placeholder="请详细描述您的需求，以便获得更好的帮助..."
            class="form-textarea"
            rows="6"
            maxlength="2000"
          ></textarea>
          <p class="hint-text">{{ formData.description.length }}/2000</p>
        </div>

        <!-- 地点 -->
        <div class="form-section">
          <label class="form-label">服务地点</label>
          <input
            type="text"
            v-model="formData.location"
            placeholder="例如：XX校区XX教学楼"
            class="form-input"
          />
        </div>

        <!-- 截止时间 -->
        <div class="form-section">
          <label class="form-label">截止时间</label>
          <input
            type="datetime-local"
            v-model="formData.deadline"
            class="form-input"
            :class="{ 'error': errors.deadline }"
            :min="formatDateTime(new Date().toISOString())"
          />
          <p v-if="errors.deadline" class="error-message">{{ errors.deadline }}</p>
          <p class="hint-text">不填写表示无截止时间</p>
        </div>

        <!-- 酬金 -->
        <div class="form-section">
          <label class="form-label">酬金（元）</label>
          <div class="reward-input-wrapper">
            <span class="reward-prefix">¥</span>
            <input
              type="number"
              v-model="formData.reward"
              placeholder="0.00"
              step="0.01"
              min="0"
              class="form-input reward-input"
              :class="{ 'error': errors.reward }"
            />
          </div>
          <p v-if="errors.reward" class="error-message">{{ errors.reward }}</p>
          <p class="hint-text">可不填写，由双方协商</p>
        </div>

        <!-- 图片上传 -->
        <div class="form-section">
          <label class="form-label">上传图片</label>
          
          <!-- 待上传图片预览 -->
          <div v-if="formData.pictureFiles.length > 0" class="image-preview-grid">
            <div 
              v-for="(file, index) in formData.pictureFiles" 
              :key="index" 
              class="image-preview-item"
            >
              <img :src="getPreviewUrl(file)" :alt="file.name" class="preview-image" />
              <button 
                type="button" 
                class="remove-image-btn" 
                @click="removePendingImage(index)"
              >
                ×
              </button>
            </div>
          </div>
          
          <!-- 上传按钮 -->
          <div class="upload-area">
            <label class="upload-btn">
              <input
                type="file"
                accept="image/*"
                multiple
                @change="handleImageSelect"
                :disabled="uploading"
              />
              <span class="upload-icon">📷</span>
              <span>选择图片</span>
            </label>
            <p class="hint-text">支持jpg、png格式，单张不超过5MB，可多选</p>
          </div>
        </div>

        <!-- 提交按钮 -->
        <div class="form-actions">
          <button 
            type="submit" 
            class="submit-btn" 
            :disabled="loading || uploading"
          >
            {{ loading ? '发布中...' : (uploading ? '上传图片中...' : '发布需求') }}
          </button>
          <button type="button" class="cancel-btn" @click="goBack">
            取消
          </button>
        </div>
      </form>
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
.create-demand-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #62055f 0%, #8b1a86 100%);
  padding: 40px 20px;
}

.create-demand-container {
  max-width: 800px;
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

/* 表单 */
.demand-form {
  padding: 32px;
}

.form-section {
  margin-bottom: 28px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 8px;
}

.form-label.required::after {
  content: '*';
  color: #e31829;
  margin-left: 4px;
}

.form-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e5e4e7;
  border-radius: 12px;
  font-size: 14px;
  transition: all 0.2s ease;
  outline: none;
  background: white;
  color: #000;
}

.form-input:focus {
  border-color: #62055f;
  box-shadow: 0 0 0 3px rgba(98, 5, 95, 0.1);
}

.form-input.error {
  border-color: #e31829;
}

.form-textarea {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e5e4e7;
  background-color: #ffffff;
  color: #000;
  border-radius: 12px;
  font-size: 14px;
  resize: vertical;
  font-family: inherit;
  transition: all 0.2s ease;
  outline: none;
}

.form-textarea:focus {
  border-color: #62055f;
  box-shadow: 0 0 0 3px rgba(98, 5, 95, 0.1);
}

.error-message {
  color: #e31829;
  font-size: 12px;
  margin-top: 6px;
}

.hint-text {
  color: #999;
  font-size: 12px;
  margin-top: 6px;
}

/* 分类按钮 */
.category-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.category-btn {
  padding: 8px 20px;
  border: 1px solid #e5e4e7;
  border-radius: 24px;
  background: white;
  color: #666;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.category-btn:hover {
  border-color: #62055f;
  color: #62055f;
}

.category-btn.active {
  background: #62055f;
  border-color: #62055f;
  color: white;
}

/* 酬金输入框 */
.reward-input-wrapper {
  position: relative;
}

.reward-prefix {
  position: absolute;
  left: 16px;
  top: 50%;
  transform: translateY(-50%);
  color: #999;
  font-size: 16px;
}

.reward-input {
  padding-left: 32px;
}

/* 图片上传区域 */
.image-preview-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 16px;
}

.image-preview-item {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #e5e4e7;
}

.preview-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-image-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  border: none;
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.remove-image-btn:hover {
  background: rgba(0, 0, 0, 0.8);
}

.upload-area {
  border: 2px dashed #e5e4e7;
  border-radius: 12px;
  padding: 24px;
  text-align: center;
  background: #faf9fb;
}

.upload-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: #62055f;
  color: white;
  border-radius: 24px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s ease;
}

.upload-btn:hover {
  background: #7a0e76;
  transform: translateY(-1px);
}

.upload-btn input {
  display: none;
}

.upload-icon {
  font-size: 18px;
}

/* 表单按钮 */
.form-actions {
  display: flex;
  gap: 16px;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #e5e4e7;
}

.submit-btn {
  flex: 1;
  padding: 14px;
  background: #62055f;
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.submit-btn:hover:not(:disabled) {
  background: #7a0e76;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(98, 5, 95, 0.3);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.cancel-btn {
  flex: 1;
  padding: 14px;
  background: #f5f5f5;
  color: #666;
  border: 1px solid #e5e4e7;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.cancel-btn:hover {
  background: #e5e4e7;
}

/* 响应式 */
@media (max-width: 640px) {
  .create-demand-container {
    border-radius: 16px;
  }
  
  .page-header {
    padding: 16px 20px;
  }
  
  .demand-form {
    padding: 20px;
  }
  
  .page-title {
    font-size: 20px;
  }
  
  .category-group {
    gap: 8px;
  }
  
  .category-btn {
    padding: 6px 16px;
    font-size: 13px;
  }
}
</style>