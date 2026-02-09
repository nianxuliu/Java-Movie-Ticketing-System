import request from '@/utils/request'

// 管理员分页查询所有订单
export function getAdminOrderList(params) {
  return request({
    url: '/order/admin/list',
    method: 'get',
    params, // { page, size, orderNo }
  })
}

export function getSeatMap(scheduleId) {
  return request({
    url: `/order/seats/${scheduleId}`,
    method: 'get',
  })
}
export function createOrder(data) {
  return request({
    url: '/order/create',
    method: 'post',
    data,
  })
}
/**
 * 根据订单号获取订单详情
 * 对应后端：OrderController -> (建议补充一个根据单号查详情的接口，如果还没写，
 * 可以暂时通过 /order/my 列表过滤或后端补充)
 */
export function getOrderDetail(orderNo) {
  return request({
    url: `/order/detail/${orderNo}`, // 假设后端有这个路径，如果没有请联系我修改
    method: 'get',
  })
}

/**
 * 支付订单
 * 对应后端：OrderController -> @PostMapping("/pay")
 * data: { orderNo: string, payPassword: password }
 */
export function payOrder(data) {
  return request({
    url: '/order/pay',
    method: 'post',
    data,
  })
}

// ... 之前的接口 ...

// 获取我的订单列表
export function getMyOrders(params) {
  return request({
    url: '/order/my',
    method: 'get',
    params, // { page, size, status }
  })
}

// 手动取消订单
export function cancelOrder(orderNo) {
  return request({
    url: `/order/cancel/${orderNo}`,
    method: 'post',
  })
}

// 申请退款 (仅演示，部分逻辑可能需要后端支持)
export function refundOrder(orderNo) {
  return request({
    url: `/order/refund/${orderNo}`,
    method: 'post',
  })
}
