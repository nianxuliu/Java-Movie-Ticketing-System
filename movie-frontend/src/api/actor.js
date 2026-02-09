import request from '@/utils/request'

// 获取列表 (搜索)
export function getActorList(params) {
  return request({ url: '/actorInfo/list', method: 'get', params })
}
// 新增
export function addActor(data) {
  return request({ url: '/actorInfo/add', method: 'post', data })
}
// 修改
export function updateActor(data) {
  return request({ url: '/actorInfo/update', method: 'put', data })
}
// 删除
export function deleteActor(id) {
  return request({ url: `/actorInfo/delete/${id}`, method: 'delete' })
}
