import request from '@/utils/request'

// 获取钱包信息（余额等）
export function getWalletInfo() {
  return request({
    url: '/userWallet/info',
    method: 'get',
  })
}

// 设置支付密码 (第一次设置)
export function setPayPassword(data) {
  return request({
    url: '/userWallet/set-password',
    method: 'post',
    data, // { payPassword: "..." }
  })
}

// 修改支付密码
export function changePayPassword(data) {
  return request({
    url: '/userWallet/change-password',
    method: 'post',
    data, // { oldPassword, newPassword }
  })
}

// 充值
export function rechargeWallet(data) {
  return request({
    url: '/userWallet/recharge',
    method: 'post',
    data, // { amount: 100 }
  })
}
