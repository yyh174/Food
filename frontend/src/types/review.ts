export interface ReviewItem {
  id: number
  platform: number
  platformOrderId: string
  shopId: number
  shopName: string
  starRating: number
  content: string
  orderTime: string
  sentimentType: number
  tags: string[]
  replyContent: string
  replyTime: string | null
  replyStatus: number
  createdAt: string
}

export interface ReviewDetail extends ReviewItem {
  platformReviewId: string
  updatedAt: string
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

export interface ReviewListParams {
  page?: number
  pageSize?: number
  platform?: number
  shopId?: number
  replyStatus?: number
  keyword?: string
  startDate?: string
  endDate?: string
}

export interface ReviewReplyForm {
  content: string
  useTemplate?: boolean
}

export interface ReplyTemplateItem {
  id: number
  name: string
  content: string
  sentimentType: number
}
