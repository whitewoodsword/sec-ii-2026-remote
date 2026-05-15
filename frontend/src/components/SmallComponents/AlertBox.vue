<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'

// Props
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: '系统通知'
  },
  content: {
    type: String,
    default: ''
  },
  // 支持 HTML 内容
  htmlContent: {
    type: String,
    default: ''
  },
  // 确定按钮文字
  confirmText: {
    type: String,
    default: '确定'
  },
  // 是否显示关闭图标
  showCloseIcon: {
    type: Boolean,
    default: true
  }
})

// Emits
const emit = defineEmits(['update:visible', 'confirm', 'close'])

// 控制内部滚动区域的 ref
const contentRef = ref(null)

// 监听 visible 变化，控制 body 滚动和光标样式
watch(() => props.visible, (newVal) => {
  if (newVal) {
    document.body.style.overflow = 'hidden'
    // 禁止光标交互
    document.body.style.cursor = 'default'
    // 禁止页面所有点击事件（通过添加类名）
    document.body.classList.add('modal-open')
  } else {
    document.body.style.overflow = ''
    document.body.style.cursor = ''
    document.body.classList.remove('modal-open')
  }
})

// 确定按钮处理
const handleConfirm = () => {
  emit('confirm')  
  emit('update:visible', false)
  emit('close')
}

// 关闭处理
const handleClose = () => {
  emit('update:visible', false)
  emit('close')
}

// 点击遮罩层关闭(不使用)
const handleOverlayClick = (e) => {
  if (e.target === e.currentTarget) {
    handleClose()
  }
}

// 键盘 ESC 关闭
const handleEsc = (e) => {
  if (e.key === 'Escape' && props.visible) {
    handleClose()
  }
}

// 阻止遮罩层上的光标交互冒泡
const handleContainerClick = (e) => {
  e.stopPropagation()
}

onMounted(() => {
  document.addEventListener('keydown', handleEsc)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleEsc)
  document.body.style.overflow = ''
  document.body.style.cursor = ''
  document.body.classList.remove('modal-open')
})
</script>

<template>
  <transition name="modal-fade">
    <div v-if="visible" class="notification-overlay">
      <div class="notification-container" @click="handleContainerClick">

        <!-- 标题栏 -->
        <div class="notification-header">
          <h3 class="notification-title">{{ title }}</h3>
        </div>
        <!-- 可滚动的内容区域 -->
        <div class="notification-content" ref="contentRef">
          <div v-if="htmlContent" v-html="htmlContent" class="content-text"></div>
          <div v-else class="content-text">{{ content }}</div>
        </div>

        <!-- 确定按钮区域 -->
        <div class="notification-footer">
          <button class="confirm-btn" @click="handleConfirm">
            {{ confirmText }}
          </button>
        </div>
      </div>
    </div>
  </transition>
</template>

<style scoped>

/*TODO: 在通知框弹出时禁止其他区域点击*/

/* 遮罩层 */
.notification-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  backdrop-filter: blur(2px);
  cursor: pointer;
}

/* 通知框容器 - 添加边框效果 */
.notification-container {
  min-width: 320px;
  min-height: 280px;
  max-width: 480px;
  width: 90%;
  background: white;
  border-radius: 16px;
  /* 添加边框 */
  border: 2px solid rgba(0, 0, 0, 0.3);
  box-shadow: 0 20px 35px -8px rgba(0, 0, 0, 0.25);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: modalSlideIn 0.3s ease-out;
  cursor: default;
  position: relative;
}


/* 标题栏 */
.notification-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 12px 24px;
  border-bottom: 1px solid #e5e4e7;
  background: white;
}

.notification-title {
  font-size: 18px;
  font-weight: 600;
  color: #62055f;
  margin: 0;
  letter-spacing: -0.2px;
}

.close-btn {
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  border-radius: 50%;
  cursor: pointer;
  font-size: 18px;
  color: #999;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.close-btn:hover {
  background-color: #f0f0f0;
  color: #62055f;
}

/* 可滚动内容区域 */
.notification-content {
  flex: 1;
  padding: 20px 24px;
  overflow-y: auto;
  max-height: 400px;
  min-height: 140px;
}

/* 滚动条样式 */
.notification-content::-webkit-scrollbar {
  width: 6px;
}

.notification-content::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.notification-content::-webkit-scrollbar-thumb {
  background: #c0c0c0;
  border-radius: 3px;
}

.notification-content::-webkit-scrollbar-thumb:hover {
  background: #a0a0a0;
}

.content-text {
  font-size: 14px;
  line-height: 1.6;
  color: #333;
  white-space: pre-wrap;
  word-break: break-word;
}

/* 内容中支持的 HTML 样式 */
.content-text :deep(p) {
  margin: 0 0 12px 0;
}

.content-text :deep(p:last-child) {
  margin-bottom: 0;
}

.content-text :deep(ul), 
.content-text :deep(ol) {
  margin: 8px 0;
  padding-left: 20px;
}

.content-text :deep(li) {
  margin: 4px 0;
}

.content-text :deep(strong) {
  color: #62055f;
}

.content-text :deep(a) {
  color: #62055f;
  text-decoration: none;
}

.content-text :deep(a:hover) {
  text-decoration: underline;
}

/* 底部按钮区域 */
.notification-footer {
  padding: 16px 24px 24px 24px;
  border-top: 1px solid #e5e4e7;
  background: white;
  display: flex;
  justify-content: flex-end;
}

.confirm-btn {
  padding: 10px 28px;
  background: #62055f;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.confirm-btn:hover {
  background: #7a0e76;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(98, 5, 95, 0.3);
}

.confirm-btn:active {
  transform: translateY(0);
}

/* 动画 */
@keyframes modalSlideIn {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(-10px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.25s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

/* 响应式调整 */
@media (max-width: 480px) {
  .notification-container {
    min-width: 280px;
    width: 85%;
  }
  
  .notification-header {
    padding: 16px 20px 10px 20px;
  }
  
  .notification-content {
    padding: 16px 20px;
  }
  
  .notification-footer {
    padding: 14px 20px 20px 20px;
  }
  
  .confirm-btn {
    padding: 8px 24px;
  }
}
</style>