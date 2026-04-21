import { request } from '@/utils/request'

export interface AiMessage {
  id?: number
  sessionId?: number
  role: 'user' | 'assistant' | 'system'
  content: string
  createdAt?: string
}

export interface AiSession {
  id: number
  tenantId: number
  shopId?: number
  userId: number
  title?: string
  createdAt: string
}

export const aiApi = {
  chat: (data: { content: string; sessionId?: number }) =>
    request.post('/ai/chat', data),

  chatStream: (data: { content: string; sessionId?: number }) =>
    request.post('/ai/chat/stream', data, { timeout: 300000 }),

  getHistory: (sessionId: number) =>
    request.get(`/ai/history/${sessionId}`),

  newSession: () =>
    request.post('/ai/new-session'),

  getSessions: () =>
    request.get('/ai/sessions'),

  deleteSession: (sessionId: number) =>
    request.delete(`/ai/sessions/${sessionId}`),
}
