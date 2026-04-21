import { get } from '../utils/request'
import type { DashboardData } from '../types/dashboard'

export async function getDashboardStatistics(): Promise<DashboardData> {
  return get<DashboardData>('/dashboard/statistics')
}
