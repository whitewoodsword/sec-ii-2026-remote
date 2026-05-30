<template>
    <div class="my-demand-page">
        <div class="demand-container">
            <!-- 头部 -->
            <div class="page-header">
                <button class="back-btn" @click="router.back()"><span class="back-icon">←</span> 返回</button>
                <h1 class="page-title">我的需求</h1>
                <button class="create-btn" @click="createDemand"><span>+</span> 发布新需求</button>
            </div>

            <!-- 搜索和筛选栏 -->
            <div class="search-filter-bar">
                <div class="search-box">
                    <input type="text" v-model="searchKeyword" placeholder="搜索需求标题..." @keyup.enter="handleSearch" />
                    <button @click="handleSearch">🔍</button>
                </div>
                <div class="filter-group">
                    <select v-model="filters.status" @change="handleFilterChange">
                        <option value="">全部状态</option>
                        <option value="PENDING">待接取</option>
                        <option value="ACCEPTED">已接取</option>
                        <option value="REJECTED">已拒绝</option>
                        <option value="COMPLETED">已完成</option>
                        <option value="CANCELLED">已取消</option>
                        <option value="EXPIRED">已过期</option>
                    </select>
                    <select v-model="filters.category" @change="handleFilterChange">
                        <option value="">全部分类</option>
                        <option value="快递代取">快递代取</option>
                        <option value="学习辅导">学习辅导</option>
                        <option value="二手交易">二手交易</option>
                        <option value="活动组队">活动组队</option>
                        <option value="其他">其他</option>
                    </select>
                </div>
                <button class="refresh-btn" @click="resetFilters">重置</button>
            </div>

            <!-- 统计 -->
            <div class="stats-bar">共 {{ totalElements }} 条需求</div>

            <!-- 加载中 -->
            <div v-if="loading" class="loading-state">
                <div class="loading-spinner"></div>
                <p>加载中...</p>
            </div>

            <!-- 空状态 -->
            <div v-else-if="demands.length === 0" class="empty-state">
                <div class="empty-icon">📋</div>
                <p>{{ searchKeyword || filters.status || filters.category ? '暂无符合条件的需求' : '暂无发布的需求' }}</p>
                <button v-if="!searchKeyword && !filters.status && !filters.category" class="create-empty-btn"
                    @click="createDemand">立即发布</button>
            </div>

            <!-- 需求列表 -->
            <div v-else class="demand-list">
                <div v-for="demand in demands" :key="demand.id" class="demand-card" @click="viewDetail(demand.id)">
                    <div v-if="firstImageUrl(demand.pictureUrls)" class="card-image" @click="viewDetail(demand.id)">
                        <img :src="getFullImageUrl(firstImageUrl(demand.pictureUrls))" :alt="demand.title" />
                    </div>
                    

                    <div class="card-content" @click="viewDetail(demand.id)">
                        <div class="card-header">
                            <h3 class="demand-title">{{ demand.title }}</h3>
                            <span class="status-badge" :style="getStatusStyle(demand.status)">{{
                                getStatusText(demand.status) }}</span>
                        </div>
                        <p class="demand-desc">{{ truncate(demand.description) }}</p>
                        <div class="demand-meta">
                            <span>地点: {{ demand.location || '地点不限' }}</span>
                            <span>酬劳: {{ formatReward(demand.reward) }}</span>
                            <span>创建时间: {{ formatDate(demand.createdAt) }}</span>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 分页 -->
            <div v-if="totalPages > 1" class="pagination-btn">
                <button class="page-btn" :disabled="currentPage === 0" @click="changePage(currentPage - 1)">上一页</button>
                <span class="page-info">第 {{ currentPage + 1 }} / {{ totalPages }} 页</span>
                <button class="page-btn" :disabled="currentPage >= totalPages - 1"
                    @click="changePage(currentPage + 1)">下一页</button>
            </div>
        </div>

        <AlertBox v-model:visible="showAlert" :title="alertConfig.title" :content="alertConfig.content"
            :html-content="alertConfig.htmlContent" :confirm-text="alertConfig.confirmText"
            @confirm="handleAlertConfirm" />
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import AlertBox from './SmallComponents/AlertBox.vue'

const authStore = useAuthStore()
const router = useRouter()

let demands = ref([])
const loading = ref(false)
const totalElements = ref(0)
const totalPages = ref(0)
const currentPage = ref(0)
const pageSize = 10

const searchKeyword = ref('')
const filters = ref({ status: '', category: '' })

const showAlert = ref(false)
const alertConfig = ref({ title: '系统通知', content: '', htmlContent: '', confirmText: '确定' })

const showNotification = (title, content) => {
    alertConfig.value = { title, content, htmlContent: '', confirmText: '确定' }
    showAlert.value = true
}

const getFullImageUrl = (url) => `http://localhost:8080${url}`
const firstImageUrl = (urls) => {
    if (!urls) return null
    const list = urls.split(';').filter(u => u.trim())
    return list.length > 0 ? list[0] : null
}
const truncate = (text, len = 80) => !text ? '暂无描述' : (text.length > len ? text.slice(0, len) + '...' : text)
const formatReward = (reward) => (!reward && reward !== 0) ? '面议' : `¥${reward.toFixed(2)}`
const formatDate = (dateStr) => {
    if (!dateStr) return ''
    const d = new Date(dateStr)
    return `${d.getFullYear()}/${d.getMonth() + 1}/${d.getDate()} ${d.getHours()}:${String(d.getMinutes()).padStart(2, '0')}`
}
const getStatusText = (status) => ({
    'PENDING': '待接取', 'ACCEPTED': '已接取', 'REJECTED': '已拒绝',
    'COMPLETED': '已完成', 'CANCELLED': '已取消', 'EXPIRED': '已过期'
}[status] || status)
const getStatusStyle = (status) => {
    const styles = {
        'PENDING': { backgroundColor: '#fff3e0', color: '#ff9800' },
        'ACCEPTED': { backgroundColor: '#e3f2fd', color: '#2196f3' },
        'REJECTED': { backgroundColor: '#ffebee', color: '#f44336' },
        'COMPLETED': { backgroundColor: '#e8f5e9', color: '#4caf50' },
        'CANCELLED': { backgroundColor: '#f5f5f5', color: '#9e9e9e' },
        'EXPIRED': { backgroundColor: '#f5f5f5', color: '#9e9e9e' }
    }
    return styles[status] || { backgroundColor: '#f0f0f0', color: '#666' }
}

const fetchDemands = async () => {
    if (!authStore.isLoggedIn) { router.push('/login'); return }
    loading.value = true
    try {
        let url = `http://localhost:8080/demands/search?publisherId=${authStore.user?.id}`
        if (searchKeyword.value) url += `&keyword=${searchKeyword.value}`
        if (filters.value.status) url += `&status=${filters.value.status}`
        if (filters.value.category) url += `&category=${filters.value.category}`
        url += `&page=${currentPage.value}&size=${pageSize}`
        const res = await fetch(url, { headers: { 'Authorization': `Bearer ${authStore.token}` } })
        const result = await res.json()
        if (result.code === 200 && result.data) {
            console.log(result)
            let list = result.data.content || []
            console.log(list)

            demands.value = list
            console.log(demands.value)
            totalElements.value = result.data.totalElements
            totalPages.value = result.data.totalPages
        } else throw new Error(result.message || '获取失败')
    } catch (error) {
        console.error('获取失败:', error)
        showNotification('加载失败', error.message)
    } finally {
        loading.value = false
    }
}

const changePage = (page) => { if (page >= 0 && page < totalPages.value) { currentPage.value = page; fetchDemands() } }
const handleSearch = () => { currentPage.value = 0; fetchDemands() }
const handleFilterChange = () => { currentPage.value = 0; fetchDemands() }
const resetFilters = () => { searchKeyword.value = ''; filters.value = { status: '', category: '' }; currentPage.value = 0; fetchDemands() }
const viewDetail = (id) => router.push(`/demand/${id}`)
const editDemand = (id) => router.push(`/edit/demand/${id}`)
const createDemand = () => router.push('/create/demand')

const cancelDemand = async (id) => {
    if (!confirm('确定取消这个需求吗？')) return
    try {
        const res = await fetch(`http://localhost:8080/demands/${id}/status?status=CANCELLED`, {
            method: 'PATCH', headers: { 'Authorization': `Bearer ${authStore.token}` }
        })
        const result = await res.json()
        if (result.code === 200) { showNotification('操作成功', '需求已取消'); fetchDemands() }
        else throw new Error(result.message)
    } catch (error) { showNotification('操作失败', error.message) }
}

const handleAlertConfirm = () => { if (router.currentRoute.value.path === '/my/demands') fetchDemands() }

onMounted(fetchDemands)
</script>

<style scoped>
.my-demand-page {
    min-height: 100vh;
    background: linear-gradient(135deg, #62055f 0%, #8b1a86 100%);
    padding: 40px 20px;
}

.demand-container {
    max-width: 1100px;
    margin: 0 auto;
    background: white;
    border-radius: 24px;
    box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
    overflow: hidden;
}

.page-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 20px 28px;
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
    cursor: pointer;
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

.create-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 8px 18px;
    background: #62055f;
    color: white;
    border: none;
    border-radius: 24px;
    cursor: pointer;
}

.create-btn:hover {
    background: #7a0e76;
    transform: translateY(-1px);
}

.search-filter-bar {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 16px;
    padding: 16px 28px;
    background: white;
    border-bottom: 1px solid #f0f0f0;
}

.search-box {
    display: flex;
    flex: 1;
    min-width: 200px;
}

.search-box input {
    flex: 1;
    padding: 8px 12px;
    background-color: #ffffff;
    color: #000;
    border: 1px solid #e5e4e7;
    border-radius: 8px 0 0 8px;
    outline: none;
}

.search-box input:focus {
    border-color: #62055f;
}

.search-box button {
    padding: 8px 12px;
    background: #62055f;
    color: white;
    border: none;
    border-radius: 0 8px 8px 0;
    cursor: pointer;
}

.filter-group {
    display: flex;
    gap: 12px;
}

.filter-group select {
    padding: 8px 12px;
    border: 1px solid #e5e4e7;
    background-color: #ffffff;
    color: #000;
    border-radius: 8px;
    outline: none;
    cursor: pointer;
}

.refresh-btn {
    padding: 8px 16px;
    background: #0a9017;
    border: 1px solid #e5e4e7;
    border-radius: 8px;
    cursor: pointer;
}

.stats-bar {
    padding: 12px 28px;
    font-size: 13px;
    color: #666;
    background: #faf9fb;
    border-bottom: 1px solid #f0f0f0;
}

.loading-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 60px;
    color: #666;
}

.loading-spinner {
    width: 40px;
    height: 40px;
    border: 3px solid #e5e4e7;
    border-top-color: #62055f;
    border-radius: 50%;
    animation: spin 1s linear infinite;
    margin-bottom: 16px;
}

@keyframes spin {
    to {
        transform: rotate(360deg);
    }
}

.empty-state {
    text-align: center;
    padding: 60px;
    color: #999;
}

.empty-icon {
    font-size: 48px;
    margin-bottom: 16px;
}

.create-empty-btn {
    margin-top: 20px;
    padding: 10px 24px;
    background: #62055f;
    color: white;
    border: none;
    border-radius: 24px;
    cursor: pointer;
}

.demand-list {
    padding: 20px 28px;
}

.demand-card {
    display: flex;
    gap: 16px;
    padding: 16px;
    border-bottom: 1px solid #f0f0f0;
    transition: background 0.2s;
}

.demand-card:hover {
    background: #faf9fb;
}

.card-image {
    width: 80px;
    height: 80px;
    border-radius: 12px;
    overflow: hidden;
    cursor: pointer;
    flex-shrink: 0;
    background: #f5f5f5;
}

.card-image img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.card-image-placeholder {
    width: 80px;
    height: 80px;
    border-radius: 12px;
    background: #f5f5f5;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    flex-shrink: 0;
    color: #ccc;
    font-size: 24px;
}

.card-content {
    flex: 1;
    cursor: pointer;
}

.card-header {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
    margin-bottom: 8px;
}

.demand-title {
    font-size: 16px;
    font-weight: 600;
    color: #333;
    margin: 0;
}

.status-badge {
    padding: 2px 10px;
    border-radius: 20px;
    font-size: 12px;
    font-weight: 500;
}

.cue_text {
    margin-top: 30px;
    font-size: 10px;
    color: #999;
}

.demand-desc {
    font-size: 13px;
    color: #666;
    margin: 8px 0;
    line-height: 1.5;
    overflow: hidden;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
}

.demand-meta {
    display: flex;
    gap: 16px;
    font-size: 12px;
    color: #999;
}

.card-actions {
    display: flex;
    flex-direction: column;
    gap: 8px;
    justify-content: center;
    flex-shrink: 0;
}

.action-btn {
    padding: 6px 16px;
    border: none;
    border-radius: 20px;
    font-size: 12px;
    cursor: pointer;
    transition: all 0.2s;
}

.view-btn {
    background: #62055f;
    color: white;
}

.view-btn:hover {
    background: #7a0e76;
}

.edit-btn {
    background: #f5f5f5;
    color: #62055f;
    border: 1px solid #e5e4e7;
}

.edit-btn:hover {
    background: #f0eaf0;
}

.cancel-action-btn {
    background: #fff0f0;
    color: #f44336;
    border: 1px solid #ffcdd2;
}

.cancel-action-btn:hover {
    background: #ffebee;
}

.pagination-btn {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 16px;
    padding: 20px 28px;
    border-top: 1px solid #f0f0f0;
}

.page-btn {
    padding: 8px 20px;
    background: #62055f;
    border: 1px solid #e5e4e7;
    border-radius: 20px;
    cursor: pointer;
}

.page-btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}

.page-info {
    font-size: 14px;
    color: #666;
}

@media (max-width: 768px) {
    .demand-card {
        flex-wrap: wrap;
    }

    .card-actions {
        flex-direction: row;
    }

    .search-filter-bar {
        flex-direction: column;
        align-items: stretch;
    }

    .filter-group {
        justify-content: space-between;
    }
}
</style>