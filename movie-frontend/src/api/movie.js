import request from '@/utils/request'

/**
 * 获取首页热门电影 Top 10 (或 Top 20)
 * 对应后端：@GetMapping("/top10")
 */
export function getTop20() {
  return request({
    url: '/movie/top20',
    method: 'get',
  })
}

/**
 * 获取电影详情（包含演职人员列表）
 * 对应后端：@GetMapping("/detail/{id}")
 */
export function getMovieDetail(id) {
  return request({
    url: `/movie/detail/${id}`,
    method: 'get',
  })
}

/**
 * 搜索电影（ES 或 数据库模糊查询）
 * 对应后端：@PostMapping("/search")
 * @param {Object} data { keyword: string, page: number, size: number }
 */
export function searchMovies(data) {
  return request({
    url: '/movie/search',
    method: 'post',
    data,
  })
}

/**
 * 管理端：分页获取电影列表
 * 对应后端：@GetMapping("/list")
 */
export function getMovieList(params) {
  return request({
    url: '/movie/list',
    method: 'get',
    params,
  })
}

/**
 * 管理端：发布电影
 */
export function addMovie(data) {
  return request({
    url: '/movie/add',
    method: 'post',
    data,
  })
}

/**
 * 管理端：修改电影
 */
export function updateMovie(data) {
  return request({
    url: '/movie/update',
    method: 'put',
    data,
  })
}

/**
 * 管理端：删除电影
 */
export function deleteMovie(id) {
  return request({
    url: `/movie/delete/${id}`,
    method: 'delete',
  })
}
