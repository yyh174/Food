import { get, post, del } from '../utils/request'
import type { PageResult } from '../utils/request'
import type {
  TicketItem,
  TicketDetail,
  TicketStatistics,
  TicketTypeItem,
  TicketListParams,
  TicketCreateForm,
  TicketAssignForm,
  TicketActionForm,
  TicketRejectForm,
  ReviewForTicket,
} from '../types/ticket'

export async function getTicketList(params: TicketListParams): Promise<PageResult<TicketItem>> {
  return get<PageResult<TicketItem>>('/tickets', { params })
}

export async function getTicketDetail(id: number): Promise<TicketDetail> {
  return get<TicketDetail>(`/tickets/${id}`)
}

export async function createTicket(data: TicketCreateForm): Promise<TicketItem> {
  return post<TicketItem>('/tickets', data)
}

export async function assignTicket(id: number, data: TicketAssignForm): Promise<void> {
  await post(`/tickets/${id}/assign`, data)
}

export async function submitTicket(id: number, data: TicketActionForm): Promise<void> {
  await post(`/tickets/${id}/submit`, data)
}

export async function verifyTicket(id: number, data: TicketActionForm): Promise<void> {
  await post(`/tickets/${id}/verify`, data)
}

export async function archiveTicket(id: number, data: TicketActionForm): Promise<void> {
  await post(`/tickets/${id}/archive`, data)
}

export async function rejectTicket(id: number, data: TicketRejectForm): Promise<void> {
  await post(`/tickets/${id}/reject`, data)
}

export async function getTicketStatistics(params: { shopId?: number; viewAs?: string }): Promise<TicketStatistics> {
  return get<TicketStatistics>('/tickets/statistics', { params })
}

export async function getTicketTypes(type?: string): Promise<TicketTypeItem[]> {
  return get<TicketTypeItem[]>('/tickets/types', type ? { params: { type } } : undefined)
}

export async function createTicketType(name: string): Promise<TicketTypeItem> {
  return post<TicketTypeItem>('/tickets/types', { name })
}

export async function deleteTicketType(id: number): Promise<void> {
  await del(`/tickets/types/${id}`)
}

export async function getReviewsForTicket(params: {
  shopId?: number
  starRating?: number
  page?: number
  pageSize?: number
}): Promise<PageResult<ReviewForTicket>> {
  return get<PageResult<ReviewForTicket>>('/reviews/for-ticket', { params })
}
