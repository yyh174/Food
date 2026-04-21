export interface TicketItem {
  id: number
  title: string
  typeId: number
  typeName: string
  shopId: number
  shopName: string
  status: number
  statusName: string
  priority: string
  priorityText: string
  priorityColor: string
  deadline: string | null
  creatorId: number
  creatorName: string
  assigneeId: number | null
  assigneeName: string
  reviewId: number | null
  reviewStar: number | null
  createdAt: string
  updatedAt: string
}

export interface TicketDetail {
  id: number
  ticketNo: string
  title: string
  description: string
  typeId: number
  typeName: string
  shopId: number
  shopName: string
  status: number
  statusName: string
  priority: string
  priorityText: string
  priorityColor: string
  deadline: string | null
  creatorId: number
  creatorName: string
  assigneeId: number | null
  assigneeName: string
  reviewId: number | null
  review: ReviewBasic | null
  images: string[]
  logs: TicketLog[]
  createdAt: string
  updatedAt: string
}

export interface ReviewBasic {
  id: number
  platform: number
  platformOrderId: string
  starRating: number
  content: string
  createdAt: string
}

export interface TicketLog {
  id: number
  action: string
  operatorId: number
  operatorName: string
  operatorRole: string
  content: string
  createdAt: string
}

export interface TicketTypeItem {
  id: number
  name: string
  type: string
  isDefault: boolean
  supportReview: boolean
}

export interface TicketStatistics {
  totalCount: number
  pendingCount: number
  processingCount: number
  pendingVerifyCount: number
  archivedCount: number
  overdueCount: number
  priorityStats: {
    high: number
    medium: number
    low: number
  }
  typeStats: {
    negativeReview: number
    complaint: number
    metric: number
    custom: number
  }
}

export interface TicketListParams {
  page?: number
  pageSize?: number
  shopId?: number
  typeId?: number
  status?: number
  viewAs?: string
  keyword?: string
  startDate?: string
  endDate?: string
}

export interface TicketCreateForm {
  title: string
  description: string
  typeId: number
  shopId: number
  deadline?: string
  images?: string[]
  reviewId?: number
}

export interface TicketAssignForm {
  assigneeId: number
  note?: string
}

export interface TicketActionForm {
  content: string
}

export interface TicketRejectForm {
  content: string
  deadline?: string
}

export interface ReviewForTicket {
  id: number
  platform: number
  platformOrderId: string
  shopId: number
  shopName: string
  starRating: number
  content: string
  createdAt: string
  alreadyLinked: boolean
}
