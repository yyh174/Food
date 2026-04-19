import { request } from '@/utils/request'
import type { PageResult } from '@/utils/request'

export interface InviteCodeResponse {
  inviteCode: string
  expireTime: string
  status: string
}

export interface InviteCodeDetailResponse {
  id: number
  inviteCode: string
  expireTime: string
  status: string
  createdAt: string
  usedTime: string | null
  usedUserId: number | null
  usedUsername: string | null
}

export interface InviteCodeListParams {
  status?: string
  page?: number
  pageSize?: number
}

export async function generateInviteCode(): Promise<InviteCodeResponse> {
  const res = await request.post<InviteCodeResponse>('/invite-code/generate', {})
  return res.data.data
}

export async function getInviteCodeList(params: InviteCodeListParams): Promise<PageResult<InviteCodeDetailResponse>> {
  const res = await request.get<PageResult<InviteCodeDetailResponse>>('/invite-code/list', { params })
  return res.data.data
}

export async function invalidateInviteCode(id: number): Promise<void> {
  await request.put(`/invite-code/invalidate/${id}`, {})
}

export async function getLatestInviteCode(): Promise<InviteCodeDetailResponse | null> {
  const res = await request.get<InviteCodeDetailResponse | null>('/invite-code/latest')
  return res.data.data
}

export async function regenerateInviteCode(): Promise<InviteCodeResponse> {
  const res = await request.post<InviteCodeResponse>('/invite-code/regenerate', {})
  return res.data.data
}
