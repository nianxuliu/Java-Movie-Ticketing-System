import request from '@/utils/request'

export function getDirectorList(params) {
  return request({ url: '/directorInfo/list', method: 'get', params })
}
export function addDirector(data) {
  return request({ url: '/directorInfo/add', method: 'post', data })
}
export function updateDirector(data) {
  return request({ url: '/directorInfo/update', method: 'put', data })
}
export function deleteDirector(id) {
  return request({ url: `/directorInfo/delete/${id}`, method: 'delete' })
}
