import request from '@/utils/request' // 导入我们封装的 axios

// 登录接口
export function login(data) {
  return request({
    url: '/user/login',
    method: 'post',
    data,
  })
}

// 获取用户信息
export function getInfo() {
  return request({
    url: '/user/me',
    method: 'get',
  })
}

// 管理员获取用户列表
export function getUserList(params) {
  return request({
    url: '/user/list',
    method: 'get',
    params,
  })
}

// 修改用户状态 (封号/解封)
// status: 0禁用 1正常
export function updateUserStatus(userId, status) {
  return request({
    url: `/user/status/${userId}/${status}`,
    method: 'put',
  })
}

export function register(data) {
  return request({
    url: '/user/register',
    method: 'post',
    data,
  })
}
export function updateUserInfo(data) {
  return request({
    url: '/user/update',
    method: 'put',
    data,
  })
}

export function updateLoginPassword(data) {
  return request({
    url: '/user/password',
    method: 'put',
    data, // { oldPassword, newPassword }
  })
}
