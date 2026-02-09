import request from '@/utils/request'

// 获取首页统计数据
export function getReportData() {
  return request({
    url: '/report/index',
    method: 'get',
  })
}
