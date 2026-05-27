<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Star, ChatLineSquare, Medal, Search, Edit, User } from '@element-plus/icons-vue'
import api from '../api'

const activeTab = ref('submit')

// ---- Submit Form ----
const submitForm = reactive({
  orderId: '',
  reviewerId: '',
  revieweeId: '',
  rating: 0,
  comment: '',
  reviewerRole: 'DEMANDER',
})
const submitting = ref(false)
const submitFormRef = ref(null)

const submitRules = {
  orderId: [{ required: true, message: '请输入订单ID', trigger: 'blur' }],
  reviewerId: [{ required: true, message: '请输入评价人ID', trigger: 'blur' }],
  revieweeId: [{ required: true, message: '请输入被评价人ID', trigger: 'blur' }],
  rating: [
    { required: true, message: '请选择评分', trigger: 'change' },
    { type: 'number', min: 1, max: 5, message: '评分需在1-5之间', trigger: 'change' },
  ],
}

async function handleSubmit() {
  if (!submitFormRef.value) return
  try {
    await submitFormRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    const res = await api.post('/reviews', {
      orderId: Number(submitForm.orderId),
      reviewerId: Number(submitForm.reviewerId),
      revieweeId: Number(submitForm.revieweeId),
      rating: submitForm.rating,
      comment: submitForm.comment,
      reviewerRole: submitForm.reviewerRole,
    })
    if (res.data.code === 200) {
      ElMessage.success('评价提交成功！')
      submitForm.orderId = ''
      submitForm.reviewerId = ''
      submitForm.revieweeId = ''
      submitForm.rating = 0
      submitForm.comment = ''
    } else {
      ElMessage.error(res.data.message || '提交失败')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '请求失败')
  } finally {
    submitting.value = false
  }
}

// ---- Review List ----
const searchType = ref('order')
const searchId = ref('')
const reviews = ref([])
const loadingReviews = ref(false)

async function loadReviews() {
  if (!searchId.value) {
    ElMessage.warning('请输入查询ID')
    return
  }
  loadingReviews.value = true
  try {
    const path = searchType.value === 'order'
      ? `/reviews/order/${searchId.value}`
      : `/reviews/user/${searchId.value}`
    const res = await api.get(path)
    if (res.data.code === 200) {
      reviews.value = res.data.data || []
      if (reviews.value.length === 0) {
        ElMessage.info('暂无评价记录')
      }
    }
  } catch {
    ElMessage.error('查询失败')
  } finally {
    loadingReviews.value = false
  }
}

// ---- Credit ----
const creditUserId = ref('')
const creditInfo = ref(null)
const loadingCredit = ref(false)

async function loadCredit() {
  if (!creditUserId.value) {
    ElMessage.warning('请输入用户ID')
    return
  }
  loadingCredit.value = true
  try {
    const res = await api.get(`/reviews/user/${creditUserId.value}/credit`)
    if (res.data.code === 200) {
      creditInfo.value = res.data.data
    }
  } catch {
    ElMessage.error('查询失败')
  } finally {
    loadingCredit.value = false
  }
}

const creditLevelColors = {
  '优秀': '#67c23a',
  '良好': '#409eff',
  '一般': '#e6a23c',
  '较差': '#f56c6c',
  '暂无': '#909399',
}

function formatDate(d) {
  if (!d) return ''
  return new Date(d).toLocaleString('zh-CN')
}
</script>

<template>
  <div class="review-app">
    <!-- Header -->
    <header class="app-header">
      <div class="header-left">
        <span class="logo">CampusHub</span>
        <span class="subtitle">评价与信用中心</span>
      </div>
      <div class="header-right">
        <el-tag type="info" effect="plain" round>校园互助服务平台</el-tag>
      </div>
    </header>

    <!-- Main Content -->
    <main class="app-main">
      <el-tabs v-model="activeTab" class="main-tabs" type="border-card">
        <!-- Tab 1: Submit Review -->
        <el-tab-pane name="submit">
          <template #label>
            <span class="tab-label">
              <el-icon><Edit /></el-icon> 提交评价
            </span>
          </template>
          <div class="tab-content">
            <div class="card-intro">
              <h3>对完成的订单进行双向评价</h3>
              <p>诚信评价，共建可信校园互助社区</p>
            </div>
            <el-form
              ref="submitFormRef"
              :model="submitForm"
              :rules="submitRules"
              label-width="120px"
              label-position="left"
              class="submit-form"
              size="large"
            >
              <el-row :gutter="24">
                <el-col :span="12">
                  <el-form-item label="订单 ID" prop="orderId">
                    <el-input v-model="submitForm.orderId" placeholder="输入已完成订单的ID" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="评分" prop="rating">
                    <el-rate v-model="submitForm.rating" :max="5" show-score />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="24">
                <el-col :span="12">
                  <el-form-item label="评价人 ID" prop="reviewerId">
                    <el-input v-model="submitForm.reviewerId" placeholder="你的用户ID">
                      <template #prefix>
                        <el-icon><User /></el-icon>
                      </template>
                    </el-input>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="被评价人 ID" prop="revieweeId">
                    <el-input v-model="submitForm.revieweeId" placeholder="对方的用户ID">
                      <template #prefix>
                        <el-icon><User /></el-icon>
                      </template>
                    </el-input>
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="24">
                <el-col :span="12">
                  <el-form-item label="我的角色">
                    <el-radio-group v-model="submitForm.reviewerRole">
                      <el-radio-button value="DEMANDER">
                        <el-icon><Star /></el-icon> 需求方
                      </el-radio-button>
                      <el-radio-button value="SERVER">
                        <el-icon><Star /></el-icon> 服务方
                      </el-radio-button>
                    </el-radio-group>
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="评语">
                <el-input
                  v-model="submitForm.comment"
                  type="textarea"
                  :rows="4"
                  placeholder="写一段评语，分享你的感受吧..."
                  maxlength="500"
                  show-word-limit
                />
              </el-form-item>
              <el-form-item>
                <el-button
                  type="primary"
                  :loading="submitting"
                  @click="handleSubmit"
                  size="large"
                  round
                  class="submit-btn"
                >
                  <el-icon><ChatLineSquare /></el-icon>
                  提交评价
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <!-- Tab 2: View Reviews -->
        <el-tab-pane name="list">
          <template #label>
            <span class="tab-label">
              <el-icon><Search /></el-icon> 查看评价
            </span>
          </template>
          <div class="tab-content">
            <div class="card-intro">
              <h3>查询评价记录</h3>
              <p>按订单或用户查看历史评价</p>
            </div>
            <div class="search-bar">
              <el-radio-group v-model="searchType" size="default">
                <el-radio-button value="order">按订单</el-radio-button>
                <el-radio-button value="user">按用户</el-radio-button>
              </el-radio-group>
              <el-input
                v-model="searchId"
                :placeholder="searchType === 'order' ? '输入订单ID' : '输入用户ID'"
                clearable
                class="search-input"
                @keyup.enter="loadReviews"
              />
              <el-button type="primary" :loading="loadingReviews" @click="loadReviews" round>
                <el-icon><Search /></el-icon> 查询
              </el-button>
            </div>

            <div v-if="reviews.length > 0" class="review-cards">
              <el-card
                v-for="r in reviews"
                :key="r.id"
                class="review-card"
                shadow="hover"
              >
                <template #header>
                  <div class="review-card-header">
                    <div class="review-card-title">
                      <el-tag
                        :type="r.reviewerRole === 'DEMANDER' ? 'primary' : 'success'"
                        effect="light"
                        round
                        size="small"
                      >
                        {{ r.reviewerRole === 'DEMANDER' ? '需求方' : '服务方' }}
                      </el-tag>
                      <span class="review-order">订单 #{{ r.orderId }}</span>
                    </div>
                    <div class="review-rate">
                      <el-rate v-model="r.rating" disabled show-score size="small" />
                    </div>
                  </div>
                </template>
                <div class="review-body">
                  <div class="review-users">
                    <span class="user-tag">评价人 ID: {{ r.reviewerId }}</span>
                    <el-icon><Star /></el-icon>
                    <span class="user-tag">被评价人 ID: {{ r.revieweeId }}</span>
                  </div>
                  <p v-if="r.comment" class="review-comment">"{{ r.comment }}"</p>
                  <p v-else class="review-comment empty">（无文字评价）</p>
                  <div class="review-time">{{ formatDate(r.createdAt) }}</div>
                </div>
              </el-card>
            </div>
            <el-empty v-else-if="!loadingReviews && searchId" description="暂无评价记录" />
          </div>
        </el-tab-pane>

        <!-- Tab 3: Credit -->
        <el-tab-pane name="credit">
          <template #label>
            <span class="tab-label">
              <el-icon><Medal /></el-icon> 信用查询
            </span>
          </template>
          <div class="tab-content">
            <div class="card-intro">
              <h3>用户信用分查询</h3>
              <p>信用分基于收到的评价计算，满分100</p>
            </div>
            <div class="search-bar">
              <el-input
                v-model="creditUserId"
                placeholder="输入用户ID"
                clearable
                class="search-input"
                @keyup.enter="loadCredit"
              />
              <el-button type="primary" :loading="loadingCredit" @click="loadCredit" round>
                <el-icon><Search /></el-icon> 查询
              </el-button>
            </div>

            <div v-if="creditInfo" class="credit-result">
              <el-row :gutter="24">
                <el-col :span="8">
                  <el-card shadow="hover" class="stat-card">
                    <div class="stat-value" :style="{ color: creditLevelColors[creditInfo.creditLevel] }">
                      {{ creditInfo.creditScore?.toFixed(0) }}
                    </div>
                    <div class="stat-label">信用分</div>
                  </el-card>
                </el-col>
                <el-col :span="8">
                  <el-card shadow="hover" class="stat-card">
                    <div class="stat-value">
                      {{ creditInfo.avgRating?.toFixed(1) }}
                      <span class="stat-suffix">/ 5</span>
                    </div>
                    <div class="stat-label">平均评分</div>
                  </el-card>
                </el-col>
                <el-col :span="8">
                  <el-card shadow="hover" class="stat-card">
                    <div class="stat-value">
                      {{ creditInfo.reviewCount }}
                    </div>
                    <div class="stat-label">评价数量</div>
                  </el-card>
                </el-col>
              </el-row>

              <el-card shadow="hover" class="level-card">
                <div class="level-content">
                  <div class="level-icon">
                    <el-icon :size="48"><Medal /></el-icon>
                  </div>
                  <div class="level-info">
                    <div class="level-badge" :style="{ background: creditLevelColors[creditInfo.creditLevel] }">
                      {{ creditInfo.creditLevel }}
                    </div>
                    <p class="level-desc">
                      <template v-if="creditInfo.creditLevel === '优秀'">
                        用户信誉极佳，值得信赖，好评率极高。
                      </template>
                      <template v-else-if="creditInfo.creditLevel === '良好'">
                        用户信誉较好，多数交易顺利完成。
                      </template>
                      <template v-else-if="creditInfo.creditLevel === '一般'">
                        用户信誉一般，建议沟通确认后再进行交易。
                      </template>
                      <template v-else-if="creditInfo.creditLevel === '较差'">
                        用户信誉较低，请谨慎交易。
                      </template>
                      <template v-else>
                        该用户暂未收到评价，信用分待定。
                      </template>
                    </p>
                  </div>
                </div>
              </el-card>

              <div class="credit-explanation">
                <h4>信用规则说明</h4>
                <ul>
                  <li>信用分 = 平均评分 &times; 20（满分 100）</li>
                  <li>90 分及以上为 <b style="color:#67c23a">"优秀"</b></li>
                  <li>70-89 分为 <b style="color:#409eff">"良好"</b></li>
                  <li>50-69 分为 <b style="color:#e6a23c">"一般"</b></li>
                  <li>50 分以下为 <b style="color:#f56c6c">"较差"</b></li>
                  <li>每位用户在订单完成后可对对方进行一次评价</li>
                </ul>
              </div>
            </div>
            <el-empty v-else-if="!loadingCredit && creditUserId" description="点击查询按钮查看信用分" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </main>

    <footer class="app-footer">
      <span>CampusHub &copy; 2026 · 校园互助服务平台 · 评价与信用模块</span>
    </footer>
  </div>
</template>

<style scoped>
.review-app {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8edf5 100%);
  font-family: 'Segoe UI', system-ui, -apple-system, sans-serif;
}

.app-header {
  background: #fff;
  padding: 16px 40px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: baseline;
  gap: 16px;
}

.logo {
  font-size: 24px;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.subtitle {
  font-size: 14px;
  color: #909399;
  font-weight: 500;
}

.app-main {
  max-width: 960px;
  margin: 32px auto;
  padding: 0 20px;
}

.main-tabs {
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 24px rgba(0,0,0,0.06);
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 500;
}

.tab-content {
  padding: 24px 16px;
}

.card-intro {
  text-align: center;
  margin-bottom: 32px;
}

.card-intro h3 {
  font-size: 22px;
  color: #303133;
  margin: 0 0 8px 0;
}

.card-intro p {
  color: #909399;
  font-size: 14px;
  margin: 0;
}

.submit-form {
  max-width: 800px;
  margin: 0 auto;
}

.submit-btn {
  padding: 12px 48px;
  font-size: 16px;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.search-input {
  width: 280px;
}

.review-cards {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-card {
  border-radius: 10px;
  transition: transform 0.2s;
}

.review-card:hover {
  transform: translateY(-2px);
}

.review-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.review-card-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.review-order {
  color: #909399;
  font-size: 13px;
}

.review-body {
  padding: 4px 0;
}

.review-users {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.user-tag {
  background: #f0f2f5;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 13px;
  color: #606266;
}

.review-comment {
  font-size: 15px;
  color: #303133;
  line-height: 1.6;
  margin: 0 0 8px 0;
}

.review-comment.empty {
  color: #c0c4cc;
  font-style: italic;
}

.review-time {
  font-size: 12px;
  color: #c0c4cc;
}

.stat-card {
  text-align: center;
  border-radius: 12px;
}

.stat-value {
  font-size: 42px;
  font-weight: 700;
  color: #303133;
}

.stat-suffix {
  font-size: 16px;
  font-weight: 400;
  color: #909399;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 8px;
}

.credit-result {
  margin-top: 4px;
}

.level-card {
  margin-top: 24px;
  border-radius: 12px;
  background: linear-gradient(135deg, #fafbff 0%, #f0f3ff 100%);
}

.level-content {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 8px 0;
}

.level-icon {
  color: #667eea;
}

.level-info {
  flex: 1;
}

.level-badge {
  display: inline-block;
  padding: 6px 24px;
  border-radius: 20px;
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 10px;
}

.level-desc {
  color: #606266;
  font-size: 14px;
  margin: 0;
}

.credit-explanation {
  margin-top: 24px;
  padding: 20px 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

.credit-explanation h4 {
  margin: 0 0 12px 0;
  color: #303133;
  font-size: 16px;
}

.credit-explanation ul {
  margin: 0;
  padding-left: 20px;
}

.credit-explanation li {
  color: #606266;
  font-size: 14px;
  line-height: 2;
}

.app-footer {
  text-align: center;
  padding: 24px;
  color: #c0c4cc;
  font-size: 13px;
}
</style>
