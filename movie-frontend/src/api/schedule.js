import request from '@/utils/request'

// 根据电影ID查询排片
export function getScheduleList(movieId) {
  return request({
    url: `/schedule/admin/list/${movieId}`,
    method: 'get',
  })
}

// 新增排片
export function addSchedule(data) {
  return request({
    url: '/schedule/add',
    method: 'post',
    data, // { cinemaId, hallId, movieId, startTime, price }
  })
}

// 删除排片
export function deleteSchedule(id) {
  return request({
    url: `/schedule/delete/${id}`,
    method: 'delete',
  })
}

// 获取某部电影的所有排片场次（只查未开场的）
export function getUserScheduleList(movieId, cinemaId = null) {
  return request({
    url: `/schedule/list/${movieId}`,
    method: 'get',
    params: { cinemaId }, // 传给后端过滤
  })
}
