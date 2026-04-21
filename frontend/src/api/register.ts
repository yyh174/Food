import { post } from '@/utils/request'

export interface RegisterPayload {
  username: string
  password: string
  inviteCode?: string
}

export const registerApi = async (payload: RegisterPayload) => {
  return post('/auth/register', payload)
}
