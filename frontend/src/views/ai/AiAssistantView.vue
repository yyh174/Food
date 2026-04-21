<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { request, type ApiResponse } from '@/utils/request'
import { aiApi, type AiMessage, type AiSession } from '@/api/ai'
import { message, Modal } from 'ant-design-vue'
import { RobotOutlined, SendOutlined, DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'

const STORAGE_KEY = 'ai_assistant_current_session'

const messages = ref<AiMessage[]>([])
const inputText = ref('')
const loading = ref(false)
const currentSession = ref<AiSession | null>(null)
const sessions = ref<AiSession[]>([])
const messageListRef = ref<HTMLElement | null>(null)
const selectedKeys = ref<string[]>([])

const canSend = computed(() => inputText.value.trim() && !loading.value)

const scrollToBottom = () => {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

const saveCurrentSession = () => {
  if (currentSession.value) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({
      id: currentSession.value.id,
      tenantId: currentSession.value.tenantId,
      userId: currentSession.value.userId,
      title: currentSession.value.title,
      createdAt: currentSession.value.createdAt,
    }))
  } else {
    localStorage.removeItem(STORAGE_KEY)
  }
}

const restoreCurrentSession = async () => {
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved) {
    try {
      const savedSession = JSON.parse(saved) as AiSession
      const session = sessions.value.find(s => s.id === savedSession.id)
      if (session) {
        await selectSession(session)
      }
    } catch (e) {
      console.error('恢复会话失败:', e)
    }
  }
}

const loadSessions = async () => {
  try {
    const res = await request.get<ApiResponse<AiSession[]>>('/ai/sessions')
    if (res.data.code === 200) {
      sessions.value = res.data.data || []
      if (!currentSession.value) {
        await restoreCurrentSession()
      }
    }
  } catch (error) {
    console.error('加载会话列表失败:', error)
  }
}

const createNewSession = async () => {
  try {
    const res = await request.post<ApiResponse<AiSession>>('/ai/new-session')
    if (res.data.code === 200) {
      currentSession.value = res.data.data
      messages.value = []
      selectedKeys.value = [String(res.data.data.id)]
      saveCurrentSession()
      await loadSessions()
      scrollToBottom()
    }
  } catch (error) {
    message.error('创建会话失败')
  }
}

const selectSession = async (session: AiSession) => {
  currentSession.value = session
  selectedKeys.value = [String(session.id)]
  saveCurrentSession()
  try {
    const res = await request.get<ApiResponse<AiMessage[]>>(`/ai/history/${session.id}`)
    if (res.data.code === 200) {
      messages.value = (res.data.data || []).filter((m: AiMessage) => m.role !== 'system')
      scrollToBottom()
    }
  } catch (error) {
    message.error('加载历史消息失败')
  }
}

const handleDeleteSession = (session: AiSession, e: Event) => {
  e.stopPropagation()
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除这个会话吗？删除后无法恢复。',
    okText: '确认',
    cancelText: '取消',
    async onOk() {
      try {
        const res = await request.delete(`/ai/sessions/${session.id}`)
        if (res.data.code === 200) {
          message.success('会话已删除')
          if (currentSession.value?.id === session.id) {
            currentSession.value = null
            messages.value = []
            selectedKeys.value = []
            localStorage.removeItem(STORAGE_KEY)
          }
          await loadSessions()
        }
      } catch (error) {
        message.error('删除失败')
      }
    },
  })
}

const sendMessage = async () => {
  if (!inputText.value.trim() || loading.value) return

  if (!currentSession.value) {
    await createNewSession()
    if (!currentSession.value) return
  }

  const content = inputText.value.trim()
  inputText.value = ''
  loading.value = true
  const thisSessionId = currentSession.value?.id

  const userMsg = { role: 'user' as const, content }
  messages.value.push(userMsg)
  scrollToBottom()

  messages.value.push({
    role: 'assistant',
    content: '正在分析中...',
  })
  const aiMsgIndex = messages.value.length - 1

  try {
    const res = await request.post<ApiResponse<AiSession>>('/ai/chat', {
      content,
      sessionId: currentSession.value?.id,
    })

    if (res.data.code === 200) {
      await loadSessions()
      messages.value.splice(aiMsgIndex, 1)
      if (currentSession.value?.id === thisSessionId) {
        const historyRes = await request.get<ApiResponse<AiMessage[]>>(`/ai/history/${thisSessionId}`)
        if (historyRes.data.code === 200) {
          messages.value = (historyRes.data.data || []).filter((m: AiMessage) => m.role !== 'system')
        }
      }
    } else {
      messages.value.splice(aiMsgIndex, 1)
      message.error(res.data.message || '发送失败')
    }
  } catch (error: any) {
    messages.value.splice(aiMsgIndex, 1)
    message.error(error?.message || '网络出现问题，请稍后重试。')
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

const handleKeyDown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

const quickQuestions = [
  '本月销量为什么下降？',
  '哪个菜差评最多？',
  '今天客流为什么变少？',
  '周末和工作日销量差异原因？',
  '总结顾客吐槽最多的3个问题',
]

const askQuestion = (question: string) => {
  inputText.value = question
  sendMessage()
}

onMounted(async () => {
  await loadSessions()
})
</script>

<template>
  <div class="ai-assistant-wrapper">
    <div class="ai-assistant-container">
      <!-- 左侧会话列表 -->
      <div class="ai-sidebar">
        <div class="sidebar-header">
          <a-button type="primary" block @click="createNewSession">
            <PlusOutlined /> 新建会话
          </a-button>
        </div>
        <div class="session-list">
          <div
            v-for="session in sessions"
            :key="session.id"
            class="session-item"
            :class="{ active: currentSession?.id === session.id }"
            @click="selectSession(session)"
          >
            <RobotOutlined />
            <span class="session-title">{{ session.title || '新会话' }}</span>
            <DeleteOutlined class="delete-btn" @click="(e) => handleDeleteSession(session, e)" />
          </div>
          <a-empty v-if="sessions.length === 0" description="暂无会话" />
        </div>
      </div>

      <!-- 右侧对话区域 -->
      <div class="ai-main">
        <template v-if="currentSession">
          <div class="chat-header">
            <RobotOutlined class="header-icon" />
            <span>AI 智能分析助手</span>
            <span class="header-tips">基于经营数据分析，智能诊断门店问题</span>
          </div>

          <div ref="messageListRef" class="message-list">
            <div v-if="messages.length === 0" class="welcome-section">
              <div class="welcome-icon">
                <RobotOutlined />
              </div>
              <h2>您好，我是AI分析助手</h2>
              <p>我可以帮您分析门店经营数据、诊断异常原因、总结顾客反馈</p>
              <div class="quick-questions">
                <div class="quick-title">试试问我这些问题：</div>
                <a-button
                  v-for="q in quickQuestions"
                  :key="q"
                  class="quick-btn"
                  @click="askQuestion(q)"
                >
                  {{ q }}
                </a-button>
              </div>
            </div>

            <div
              v-for="(msg, index) in messages"
              :key="index"
              class="message-item"
              :class="msg.role"
            >
              <div class="message-avatar">
                <RobotOutlined v-if="msg.role === 'assistant'" />
                <span v-else>我</span>
              </div>
              <div class="message-content">
                <div
                  class="message-text"
                  v-html="msg.content ? msg.content.replace(/\n/g, '<br>') : ''"
                ></div>
              </div>
            </div>
          </div>

          <div class="input-area">
            <a-textarea
              v-model:value="inputText"
              placeholder="输入您的问题，按 Enter 发送..."
              :rows="2"
              :disabled="loading"
              @pressEnter="handleKeyDown"
            />
            <a-button
              type="primary"
              :disabled="!canSend"
              @click="sendMessage"
            >
              <SendOutlined /> 发送
            </a-button>
          </div>
        </template>

        <div v-else class="placeholder-section">
          <div class="placeholder-icon">
            <RobotOutlined />
          </div>
          <div class="placeholder-text">请选择会话</div>
          <div class="placeholder-hint">或在左侧创建新会话开始对话</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ai-assistant-wrapper {
  height: 100%;
  overflow: hidden;
}

.ai-assistant-container {
  display: flex;
  height: 100%;
  background: #f6f8fa;
}

.ai-sidebar {
  width: 260px;
  min-width: 260px;
  background: #fff;
  border-right: 1px solid #d0d7de;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 12px;
  border-bottom: 1px solid #d0d7de;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.session-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 4px;
}

.session-item:hover {
  background: #f0f0f0;
}

.session-item.active {
  background: #e6f7ff;
  color: #1890ff;
}

.session-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}

.delete-btn {
  opacity: 0;
  color: #999;
  transition: opacity 0.2s, color 0.2s;
}

.session-item:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  color: #ff4d4f;
}

.ai-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  overflow: hidden;
}

.chat-header {
  padding: 12px 20px;
  background: #fff;
  border-bottom: 1px solid #d0d7de;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.header-icon {
  font-size: 20px;
  color: #1890ff;
}

.chat-header span:first-of-type {
  font-size: 15px;
  font-weight: 600;
}

.header-tips {
  color: #999;
  font-size: 12px;
  margin-left: auto;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
  min-height: 0;
}

.welcome-section {
  text-align: center;
  padding: 32px 16px;
}

.welcome-icon {
  font-size: 48px;
  color: #1890ff;
  margin-bottom: 12px;
}

.welcome-section h2 {
  margin-bottom: 6px;
  color: #333;
  font-size: 18px;
}

.welcome-section p {
  color: #666;
  margin-bottom: 20px;
  font-size: 14px;
}

.quick-questions {
  text-align: left;
  max-width: 500px;
  margin: 0 auto;
}

.quick-title {
  color: #666;
  margin-bottom: 10px;
  font-size: 13px;
}

.quick-btn {
  margin: 0 6px 6px 0;
  font-size: 13px;
}

.message-item {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.message-item.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.message-item.assistant .message-avatar {
  background: #1890ff;
  color: #fff;
}

.message-item.user .message-avatar {
  background: #52c41a;
  color: #fff;
}

.message-content {
  max-width: 75%;
}

.message-text {
  padding: 10px 14px;
  border-radius: 8px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
}

.message-item.assistant .message-text {
  background: #f5f5f5;
  border: 1px solid #e8e8e8;
}

.message-item.user .message-text {
  background: #1890ff;
  color: #fff;
}

.input-area {
  padding: 12px 20px;
  background: #fff;
  border-top: 1px solid #d0d7de;
  display: flex;
  gap: 10px;
  align-items: flex-end;
  flex-shrink: 0;
}

.input-area .ant-input {
  flex: 1;
}

/* Placeholder styles */
.placeholder-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #999;
  background: linear-gradient(135deg, #f6f8fa 0%, #e8ecf0 100%);
}

.placeholder-icon {
  font-size: 64px;
  color: #d0d7de;
  margin-bottom: 16px;
}

.placeholder-text {
  font-size: 18px;
  font-weight: 500;
  color: #8c8c8c;
  margin-bottom: 6px;
}

.placeholder-hint {
  font-size: 13px;
  color: #bfbfbf;
}
</style>
