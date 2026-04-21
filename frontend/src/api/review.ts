import type { PageResult } from '../utils/request'
import type { ReviewItem, ReviewDetail, ReviewStatistics, ReviewListParams, ReplyTemplateItem } from '../types/review'
import { get, post } from '../utils/request'

export async function getReviewList(params: ReviewListParams = {}): Promise<PageResult<ReviewItem>> {
  return get<PageResult<ReviewItem>>('/reviews', { params })
}

export async function getReviewDetail(id: number): Promise<ReviewDetail> {
  return get<ReviewDetail>(`/reviews/${id}`)
}

export async function replyReview(id: number, content: string): Promise<void> {
  await post(`/reviews/${id}/reply`, { content, useTemplate: false })
}

export async function replyReviewWithTemplate(id: number, templateId: number): Promise<void> {
  await post(`/reviews/${id}/reply-with-template`, { templateId })
}

export async function getReviewStatistics(shopId?: number): Promise<ReviewStatistics> {
  return get<ReviewStatistics>('/reviews/statistics', {
    params: shopId ? { shopId } : undefined,
  })
}

export async function getReplyTemplateList(): Promise<ReplyTemplateItem[]> {
  return get<ReplyTemplateItem[]>('/reply-templates')
}
