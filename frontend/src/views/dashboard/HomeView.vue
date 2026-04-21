<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ShopOutlined,
  UserOutlined,
  StarOutlined,
  ExceptionOutlined,
  ClockCircleOutlined,
} from '@ant-design/icons-vue'
import { getDashboardStatistics } from '@/api/dashboard'
import type { DashboardData } from '@/types/dashboard'

const router = useRouter()
const loading = ref(false)
const data = ref<DashboardData | null>(null)

const isTenantAdmin = computed(() => data.value?.roleCode === 'tenant_admin')
const isShopOwner = computed(() => data.value?.roleCode === 'shop_owner')

const fetchData = async () => {
  loading.value = true
  try {
    data.value = await getDashboardStatistics()
  } catch {
    message.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)

const sentimentTotal = computed(() => {
  if (!data.value?.reviewStatistics?.sentimentStats) return 0
  const { positive, neutral, negative } = data.value.reviewStatistics.sentimentStats
  return positive + neutral + negative
})

const sentimentPercent = computed(() => (value: number) => {
  if (sentimentTotal.value === 0) return 0
  return Math.round((value / sentimentTotal.value) * 100)
})

const platformTotal = computed(() => {
  if (!data.value?.reviewStatistics?.platformStats) return 0
  const { meituan, ele } = data.value.reviewStatistics.platformStats
  return meituan + ele
})

const ticketTotal = computed(() => {
  if (!data.value?.ticketStatistics) return 0
  const { pendingCount, processingCount, pendingVerifyCount, archivedCount } = data.value.ticketStatistics
  return pendingCount + processingCount + pendingVerifyCount + archivedCount
})

const priorityTotal = computed(() => {
  if (!data.value?.ticketStatistics?.priorityStats) return 0
  const { high, medium, low } = data.value.ticketStatistics.priorityStats
  return high + medium + low
})

const priorityPercent = computed(() => (value: number) => {
  if (priorityTotal.value === 0) return 0
  return Math.round((value / priorityTotal.value) * 100)
})

const typeTotal = computed(() => {
  if (!data.value?.ticketStatistics?.typeStats) return 0
  const { negativeReview, complaint, metric, custom } = data.value.ticketStatistics.typeStats
  return negativeReview + complaint + metric + custom
})

const goToReviews = () => router.push('/reviews')
const goToTickets = () => router.push('/tickets')
const goToUsers = () => router.push('/users')
const goToShops = () => router.push('/shops')
</script>

<template>
  <div class="dashboard">
    <a-spin :spinning="loading">
      <template v-if="data">
        <a-row :gutter="16" class="stat-cards">
          <a-col :span="6" v-if="isTenantAdmin">
            <a-card class="stat-card" hoverable @click="goToShops">
              <a-statistic
                title="门店数量"
                :value="data.statCards.shopCount"
                :value-style="{ color: '#1890ff' }"
              >
                <template #prefix>
                  <ShopOutlined />
                </template>
              </a-statistic>
            </a-card>
          </a-col>

          <a-col :span="6" v-if="isTenantAdmin">
            <a-card class="stat-card" hoverable @click="goToUsers">
              <a-statistic
                title="用户数量"
                :value="data.statCards.userCount"
                :value-style="{ color: '#722ed1' }"
              >
                <template #prefix>
                  <UserOutlined />
                </template>
              </a-statistic>
            </a-card>
          </a-col>

          <a-col :span="6">
            <a-card class="stat-card" hoverable @click="goToReviews">
              <a-statistic
                :title="isTenantAdmin ? '今日新增评价' : '今日评价'"
                :value="data.statCards.todayReviewCount"
                :value-style="{ color: '#faad14' }"
              >
                <template #prefix>
                  <StarOutlined />
                </template>
              </a-statistic>
            </a-card>
          </a-col>

          <a-col :span="6">
            <a-card class="stat-card" hoverable @click="goToTickets">
              <a-statistic
                :title="isTenantAdmin ? '待核销工单' : '待处理工单'"
                :value="data.statCards.pendingTicketCount"
                :value-style="{ color: '#f5222d' }"
              >
                <template #prefix>
                  <ExceptionOutlined />
                </template>
              </a-statistic>
            </a-card>
          </a-col>

          <template v-if="isShopOwner">
            <a-col :span="6">
              <a-card class="stat-card" hoverable @click="goToReviews">
                <a-statistic
                  title="待回复评价"
                  :value="data.statCards.pendingReviewCount"
                  :value-style="{ color: '#fa8c16' }"
                >
                  <template #prefix>
                    <ClockCircleOutlined />
                  </template>
                </a-statistic>
              </a-card>
            </a-col>
          </template>
        </a-row>

        <a-row :gutter="16" class="chart-section" v-if="data">
          <a-col :span="12">
            <a-card title="评价统计" :bordered="false" class="chart-card">
              <a-descriptions :column="2" size="small">
                <a-descriptions-item label="总评价数">
                  {{ data.reviewStatistics.totalCount }}
                </a-descriptions-item>
                <a-descriptions-item label="平均评分">
                  {{ data.reviewStatistics.avgStarRating }} 星
                </a-descriptions-item>
                <a-descriptions-item label="已回复">
                  {{ data.reviewStatistics.replyCount }}
                </a-descriptions-item>
                <a-descriptions-item label="未回复">
                  {{ data.reviewStatistics.unReplyCount }}
                </a-descriptions-item>
                <a-descriptions-item label="回复率">
                  {{ data.reviewStatistics.replyRate }}%
                </a-descriptions-item>
              </a-descriptions>

              <a-divider orientation="left">情感分布</a-divider>
              <a-row :gutter="16">
                <a-col :span="8">
                  <div class="progress-item">
                    <div class="progress-label">
                      <span class="label-text positive">正面</span>
                      <span>{{ data.reviewStatistics.sentimentStats?.positive || 0 }}</span>
                    </div>
                    <a-progress
                      :percent="sentimentPercent(data.reviewStatistics.sentimentStats?.positive || 0)"
                      :show-info="false"
                      :stroke-color="'#52c41a'"
                    />
                  </div>
                </a-col>
                <a-col :span="8">
                  <div class="progress-item">
                    <div class="progress-label">
                      <span class="label-text neutral">中性</span>
                      <span>{{ data.reviewStatistics.sentimentStats?.neutral || 0 }}</span>
                    </div>
                    <a-progress
                      :percent="sentimentPercent(data.reviewStatistics.sentimentStats?.neutral || 0)"
                      :show-info="false"
                      :stroke-color="'#faad14'"
                    />
                  </div>
                </a-col>
                <a-col :span="8">
                  <div class="progress-item">
                    <div class="progress-label">
                      <span class="label-text negative">负面</span>
                      <span>{{ data.reviewStatistics.sentimentStats?.negative || 0 }}</span>
                    </div>
                    <a-progress
                      :percent="sentimentPercent(data.reviewStatistics.sentimentStats?.negative || 0)"
                      :show-info="false"
                      :stroke-color="'#f5222d'"
                    />
                  </div>
                </a-col>
              </a-row>

              <a-divider orientation="left">平台分布</a-divider>
              <a-row :gutter="16">
                <a-col :span="12">
                  <div class="progress-item">
                    <div class="progress-label">
                      <span>美团</span>
                      <span>{{ data.reviewStatistics.platformStats?.meituan || 0 }}</span>
                    </div>
                    <a-progress
                      :percent="platformTotal > 0 ? Math.round((data.reviewStatistics.platformStats?.meituan || 0) / platformTotal * 100) : 0"
                      :show-info="false"
                      :stroke-color="'#ffc107'"
                    />
                  </div>
                </a-col>
                <a-col :span="12">
                  <div class="progress-item">
                    <div class="progress-label">
                      <span>饿了么</span>
                      <span>{{ data.reviewStatistics.platformStats?.ele || 0 }}</span>
                    </div>
                    <a-progress
                      :percent="platformTotal > 0 ? Math.round((data.reviewStatistics.platformStats?.ele || 0) / platformTotal * 100) : 0"
                      :show-info="false"
                      :stroke-color="'#1890ff'"
                    />
                  </div>
                </a-col>
              </a-row>
            </a-card>
          </a-col>

          <a-col :span="12">
            <a-card title="工单统计" :bordered="false" class="chart-card">
              <a-descriptions :column="2" size="small">
                <a-descriptions-item label="工单总数">
                  {{ data.ticketStatistics.totalCount }}
                </a-descriptions-item>
                <a-descriptions-item label="处理中">
                  <a-badge status="processing" :text="data.ticketStatistics.processingCount" />
                </a-descriptions-item>
                <a-descriptions-item label="待核销">
                  <a-badge status="warning" :text="data.ticketStatistics.pendingVerifyCount" />
                </a-descriptions-item>
                <a-descriptions-item label="已归档">
                  <a-badge status="success" :text="data.ticketStatistics.archivedCount" />
                </a-descriptions-item>
              </a-descriptions>

              <a-divider orientation="left">优先级分布</a-divider>
              <a-row :gutter="16">
                <a-col :span="8">
                  <div class="progress-item">
                    <div class="progress-label">
                      <span class="label-text high">高</span>
                      <span>{{ data.ticketStatistics.priorityStats?.high || 0 }}</span>
                    </div>
                    <a-progress
                      :percent="priorityPercent(data.ticketStatistics.priorityStats?.high || 0)"
                      :show-info="false"
                      :stroke-color="'#f5222d'"
                    />
                  </div>
                </a-col>
                <a-col :span="8">
                  <div class="progress-item">
                    <div class="progress-label">
                      <span class="label-text medium">中</span>
                      <span>{{ data.ticketStatistics.priorityStats?.medium || 0 }}</span>
                    </div>
                    <a-progress
                      :percent="priorityPercent(data.ticketStatistics.priorityStats?.medium || 0)"
                      :show-info="false"
                      :stroke-color="'#faad14'"
                    />
                  </div>
                </a-col>
                <a-col :span="8">
                  <div class="progress-item">
                    <div class="progress-label">
                      <span class="label-text low">低</span>
                      <span>{{ data.ticketStatistics.priorityStats?.low || 0 }}</span>
                    </div>
                    <a-progress
                      :percent="priorityPercent(data.ticketStatistics.priorityStats?.low || 0)"
                      :show-info="false"
                      :stroke-color="'#52c41a'"
                    />
                  </div>
                </a-col>
              </a-row>

              <a-divider orientation="left">类型分布</a-divider>
              <a-row :gutter="16">
                <a-col :span="6">
                  <div class="type-item">
                    <div class="type-value">{{ data.ticketStatistics.typeStats?.negativeReview || 0 }}</div>
                    <div class="type-label">差评工单</div>
                  </div>
                </a-col>
                <a-col :span="6">
                  <div class="type-item">
                    <div class="type-value">{{ data.ticketStatistics.typeStats?.complaint || 0 }}</div>
                    <div class="type-label">投诉工单</div>
                  </div>
                </a-col>
                <a-col :span="6">
                  <div class="type-item">
                    <div class="type-value">{{ data.ticketStatistics.typeStats?.metric || 0 }}</div>
                    <div class="type-label">指标工单</div>
                  </div>
                </a-col>
                <a-col :span="6">
                  <div class="type-item">
                    <div class="type-value">{{ data.ticketStatistics.typeStats?.custom || 0 }}</div>
                    <div class="type-label">自定义</div>
                  </div>
                </a-col>
              </a-row>
            </a-card>
          </a-col>
        </a-row>
      </template>

      <template v-else>
        <a-empty description="暂无数据" />
      </template>
    </a-spin>
  </div>
</template>

<style scoped>
.dashboard {
  max-width: 1600px;
  margin: 0 auto;
}

.stat-cards {
  margin-bottom: 24px;
}

.stat-card {
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  border-radius: 8px;
}

.stat-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.chart-section {
  margin-bottom: 24px;
}

.chart-card {
  min-height: 380px;
  height: auto;
  display: flex;
  flex-direction: column;
  border-radius: 8px;
}

.chart-card :deep(.ant-card-body) {
  flex: 1;
  padding: 20px;
}

.stat-card {
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  border-radius: 8px;
}

.stat-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.chart-section {
  margin-bottom: 24px;
}

.chart-card {
  min-height: 380px;
  height: auto;
  display: flex;
  flex-direction: column;
  border-radius: 8px;
}

.chart-card :deep(.ant-card-body) {
  flex: 1;
  padding: 20px;
}

.action-section {
  margin-bottom: 16px;
}

.progress-item {
  margin-bottom: 8px;
}

.progress-label {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
  font-size: 12px;
}

.label-text {
  font-weight: 500;
}

.label-text.positive {
  color: #52c41a;
}

.label-text.neutral {
  color: #faad14;
}

.label-text.negative {
  color: #f5222d;
}

.label-text.high {
  color: #f5222d;
}

.label-text.medium {
  color: #faad14;
}

.label-text.low {
  color: #52c41a;
}

.type-item {
  text-align: center;
  padding: 12px;
  background: #f5f5f5;
  border-radius: 4px;
}

.type-value {
  font-size: 20px;
  font-weight: 600;
  color: #1890ff;
}

.type-label {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}
</style>
