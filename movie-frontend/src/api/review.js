import request from '@/utils/request'

// 管理员获取所有影评 (支持关键词搜索)
export function getAdminReviewList(params) {
  return request({
    url: '/review/admin/list',
    method: 'get',
    params,
  })
}

// 删除影评
export function deleteReview(id) {
  return request({
    url: `/review/delete/${id}`,
    method: 'delete',
  })
}

// 获取某条影评下的回复 (复用原有接口)
export function getReplyList(reviewId, params) {
  return request({
    url: `/reply/list/${reviewId}`,
    method: 'get',
    params,
  })
}

// 删除回复
export function deleteReply(id) {
  return request({
    url: `/reply/delete/${id}`,
    method: 'delete',
  })
}
export function getReviewList(movieId, params) {
  return request({
    url: `/review/list/${movieId}`,
    method: 'get',
    params,
  })
}
export function addReview(data) {
  return request({
    url: '/review/add',
    method: 'post',
    data, // { movieId, score, content }
  })
}
export function likeReview(reviewId) {
  return request({
    url: `/review/like/${reviewId}`,
    method: 'post',
  })
}
export function addReply(data) {
  return request({
    url: '/reply/add',
    method: 'post',
    data, // { reviewId, targetUserId, content }
  })
}
export function likeReply(replyId) {
  return request({
    url: `/reply/like/${replyId}`,
    method: 'post',
  })
}
