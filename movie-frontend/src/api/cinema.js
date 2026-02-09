import request from '@/utils/request'

// 影院相关
export function getCinemaList(params) {
  return request({ url: '/cinema/list', method: 'get', params })
}
export function addCinema(data) {
  return request({ url: '/cinema/add', method: 'post', data })
}
export function updateCinema(data) {
  return request({ url: '/cinema/update', method: 'put', data })
}
export function deleteCinema(id) {
  return request({ url: `/cinema/delete/${id}`, method: 'delete' })
}
export function getAllCinemas() {
  return request({
    url: '/cinema/list',
    method: 'get',
    params: { page: 1, size: 1000 }, // 一次性取回做筛选
  })
}

// 影厅相关 (写在一起方便)
export function getHallList(cinemaId) {
  return request({ url: `/hall/list/${cinemaId}`, method: 'get' })
}
export function addHall(data) {
  return request({ url: '/hall/add', method: 'post', data })
}
export function updateHall(data) {
  return request({ url: '/hall/update', method: 'put', data })
}
export function deleteHall(id) {
  return request({ url: `/hall/delete/${id}`, method: 'delete' })
}
export function getAllHalls() {
  return request({ url: '/hall/list/all', method: 'get' })
}
