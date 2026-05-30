<template>
  <div class="pagination-component">
    <button 
      class="page-btn prev-btn" 
      :disabled="currentPage === 0"
      @click="goToPage(currentPage - 1)"
    >
      上一页
    </button>
    
    <div class="page-numbers">
      <button
        v-for="page in visiblePages"
        :key="page"
        class="page-num"
        :class="{ active: page === currentPage + 1 }"
        @click="goToPage(page - 1)"
      >
        {{ page }}
      </button>
    </div>
    
    <button 
      class="page-btn next-btn" 
      :disabled="currentPage >= totalPages - 1"
      @click="goToPage(currentPage + 1)"
    >
      下一页
    </button>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  currentPage: {
    type: Number,
    required: true,
    default: 0
  },
  totalPages: {
    type: Number,
    required: true,
    default: 0
  },
  maxVisible: {
    type: Number,
    default: 5
  }
})

const emit = defineEmits(['page-change'])

// 计算可见的页码
const visiblePages = computed(() => {
  if (props.totalPages <= 0) return []
  
  const maxVisible = props.maxVisible
  const current = props.currentPage + 1
  const total = props.totalPages
  
  let start = Math.max(1, current - Math.floor(maxVisible / 2))
  let end = Math.min(total, start + maxVisible - 1)
  
  if (end - start + 1 < maxVisible) {
    start = Math.max(1, end - maxVisible + 1)
  }
  
  const pages = []
  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  return pages
})

const goToPage = (page) => {
  if (page < 0 || page >= props.totalPages) return
  if (page === props.currentPage) return
  emit('page-change', page)
}
</script>

<style scoped>
.pagination-component {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #eaeef2;
}

.page-btn {
  padding: 8px 16px;
  border: 1px solid #ddd;
  background: #ffffff;
  color: #333;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) {
  border-color: #62055f;
  color: #62055f;
  background: #faf5fa;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-numbers {
  display: flex;
  gap: 6px;
}

.page-num {
  min-width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #ddd;
  background: #ffffff;
  color: #333;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.page-num:hover {
  border-color: #62055f;
  color: #62055f;
  background: #faf5fa;
}

.page-num.active {
  background: #62055f;
  border-color: #62055f;
  color: #ffffff;
}
</style>