import request from '@/utils/request'

// 上传文件接口
export function uploadFile(file) {
  const data = new FormData()
  data.append('file', file)
  return request({
    url: '/file/upload',
    method: 'post',
    data,
    // 告诉 axios 这是一个文件上传
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}
