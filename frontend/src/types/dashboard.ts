export interface DashboardData {
  roleCode: string
  statCards: StatCards
  reviewStatistics: ReviewStatistics
  ticketStatistics: TicketStatistics
}

export interface StatCards {
  shopCount: number
  userCount: number
  todayReviewCount: number
  pendingTicketCount: number
  todayTicketCount: number
  pendingReviewCount: number
  repliedReviewCount: number
  totalReviewCount: number
}

export interface ReviewStatistics {
  totalCount: number
  replyCount: number
  unReplyCount: number
  replyRate: number
  avgStarRating: number
  sentimentStats: {
    positive: number
    neutral: number
    negative: number
  }
  platformStats: {
    meituan: number
    ele: number
  }
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
