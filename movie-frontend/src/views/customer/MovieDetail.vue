<!-- eslint-disable no-empty -->
<!-- eslint-disable no-unused-vars -->
<script setup>
import { ref, onMounted, computed, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getMovieDetail } from '@/api/movie'
import { getUserScheduleList } from '@/api/schedule'
import { getAllCinemas, getAllHalls } from '@/api/cinema'
import {
  getReviewList,
  addReview,
  likeReview,
  deleteReview,
  addReply,
  deleteReply,
} from '@/api/review'
import { useUserStore } from '@/stores/user'
import {
  Star,
  StarFilled,
  Location,
  VideoPlay,
  Timer,
  Edit,
  ChatDotRound,
  Delete,
  ArrowDown,
  ArrowUp
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 注意：原代码中的 const movieId = route.params.id 是静态赋值，
// 路由变化时不会更新，已移除。在需要使用 ID 的地方直接使用 route.params.id

// --- 基础数据 ---
const movie = ref({})
const allSchedules = ref([])
const cinemas = ref([])
const hallMap = ref({})
const selectedCinemaId = ref(null)
const loading = ref(true)

// --- 影评相关状态 ---
const reviews = ref([])
const reviewLoading = ref(false)
const reviewPage = ref(1)
const hasMoreReviews = ref(true)

// 写评论弹窗
const showWriteDialog = ref(false)
const writeForm = ref({ score: 10, content: '' })
const submitting = ref(false)

// 回复框控制
const activeReplyBox = ref({})
const replyContent = ref('')
const currentReplyTarget = ref(null) // 记录当前回复的是谁(targetUserId)

// --- 辅助函数：获取Token ---
const getToken = () => {
  return userStore.token || localStorage.getItem('token')
}

// --- 排片过滤 ---
const filteredSchedules = computed(() => {
  if (!selectedCinemaId.value) return []
  return allSchedules.value.filter((s) => s.cinemaId === selectedCinemaId.value)
})

// --- 初始化数据 ---
const loadInitData = async () => {
  const currentMovieId = route.params.id
  if (!currentMovieId) return

  loading.value = true
  try {
    const [detailRes, cinemaRes, hallRes, scheduleRes] = await Promise.all([
      getMovieDetail(currentMovieId),
      getAllCinemas(),
      getAllHalls(),
      getUserScheduleList(currentMovieId),
    ])

    movie.value = detailRes.data || detailRes
    const cinemaData = cinemaRes.data || cinemaRes
    cinemas.value = cinemaData.records || cinemaData || []

    const hallData = hallRes.data || hallRes
    if (hallData) {
      hallData.forEach((h) => (hallMap.value[h.id] = h.name))
    }

    allSchedules.value = scheduleRes.data || scheduleRes || []

    if (cinemas.value.length > 0) {
      const cinemaIdsWithSchedule = new Set(allSchedules.value.map((s) => s.cinemaId))
      const targetCinema = cinemas.value.find((c) => cinemaIdsWithSchedule.has(c.id))
      selectedCinemaId.value = targetCinema ? targetCinema.id : cinemas.value[0].id
    }

    // 加载第一页影评
    await loadMoreReviews(true)
  } catch (e) {
    console.error(e)
    ElMessage.error('数据加载异常')
  } finally {
    loading.value = false
  }
}

// --- 监听路由变化（核心修复：解决搜索跳转不刷新问题） ---
watch(
  () => route.params.id,
  (newId) => {
    if (newId) {
      // 重置所有状态
      movie.value = {}
      allSchedules.value = []
      selectedCinemaId.value = null
      reviews.value = []
      reviewPage.value = 1
      hasMoreReviews.value = true
      activeReplyBox.value = {}
      
      // 重新加载数据
      loadInitData()
    }
  }
)

// --- 影评核心逻辑 ---
const loadMoreReviews = async (isReset = false) => {
  const currentMovieId = route.params.id
  
  if (isReset) {
    reviewPage.value = 1
    reviews.value = []
    hasMoreReviews.value = true
  }

  if (!hasMoreReviews.value || reviewLoading.value) return

  reviewLoading.value = true
  try {
    const res = await getReviewList(currentMovieId, { page: reviewPage.value, size: 5 })
    const data = res.data || res
    const newReviews = data.records || []

    newReviews.forEach((r) => {
      // 初始化状态
      r.showReply = false // 是否展开整个回复区
      r.replyList = r.replyList || []

      // 【关键修改】控制回复显示数量，默认显示3条
      r.isRepliesExpanded = false
      r.visibleReplyCount = 2

      r.likeCount = r.likeCount || 0
      r.isLiked = !!r.isLiked
    })

    if (newReviews.length < 5) {
      hasMoreReviews.value = false
    }

    reviews.value = [...reviews.value, ...newReviews]
    reviewPage.value++
  } catch (e) {
    console.error(e)
  } finally {
    reviewLoading.value = false
  }
}

// --- 交互功能 ---

// 1. 写影评前检查登录
const openWriteDialog = () => {
  if (!getToken()) {
    ElMessage.warning('请先登录')
    return router.push('/login')
  }
  showWriteDialog.value = true
}

const submitReview = async () => {
  if (!writeForm.value.content.trim()) return ElMessage.warning('写点什么吧')
  submitting.value = true
  try {
    await addReview({
      movieId: route.params.id, // 使用动态ID
      score: writeForm.value.score,
      content: writeForm.value.content,
    })
    ElMessage.success('影评发布成功')
    showWriteDialog.value = false
    writeForm.value.content = ''
    loadMoreReviews(true)
  } catch (e) {
  } finally {
    submitting.value = false
  }
}

// 2. 点赞
const handleLike = async (item, type) => {
  if (!getToken()) {
    ElMessage.warning('请先登录')
    return router.push('/login')
  }
  try {
    await likeReview(item.id)
    item.isLiked = !item.isLiked
    item.likeCount = item.isLiked ? item.likeCount + 1 : item.likeCount - 1
  } catch (e) {
    console.error(e)
  }
}

// 3. 切换回复框 (回复楼主)
const toggleReplyBox = (reviewId) => {
  // 关闭其他
  Object.keys(activeReplyBox.value).forEach((k) => {
    if (parseInt(k) !== reviewId) activeReplyBox.value[k] = false
  })

  activeReplyBox.value[reviewId] = !activeReplyBox.value[reviewId]
  replyContent.value = ''
  currentReplyTarget.value = null // 清空回复目标，默认回复楼主
}

// 4. 点击回复某人 (楼中楼)
const handleReplyToUser = (reviewId, targetNickname, targetUserId) => {
  if (!getToken()) return router.push('/login')

  // 打开输入框
  activeReplyBox.value[reviewId] = true
  // 填充 @前缀
  replyContent.value = `回复 @${targetNickname} ：`
  // 记录目标ID
  currentReplyTarget.value = targetUserId

  // 自动聚焦 (如果能获取到DOM ref的话更好，这里简化处理)
}

// 5. 提交回复
const submitReply = async (review) => {
  let content = replyContent.value

  // 如果是回复某人，且内容以 "回复 @xxx ：" 开头，可以把前缀去掉，只传真实内容
  // 或者后端处理。这里演示简单的处理逻辑：
  if (currentReplyTarget.value && content.includes('：')) {
    // 简单的切分，防止用户误删了冒号导致格式不对，这里只取冒号后的内容
    // 实际项目中可以不做处理，直接传给后端，或者前端校验更严格
    const parts = content.split('：')
    if (parts.length > 1) {
      // 取最后一个冒号后面的所有内容（防止用户内容里也有冒号）
      content = parts.slice(1).join('：')
    }
  }

  if (!content.trim()) return ElMessage.warning('请输入回复内容')

  if (!getToken()) {
    ElMessage.warning('请先登录')
    return router.push('/login')
  }

  try {
    await addReply({
      reviewId: review.id,
      targetUserId: currentReplyTarget.value, // 传入目标ID
      content: content,
    })
    ElMessage.success('回复成功')
    activeReplyBox.value[review.id] = false
    replyContent.value = ''
    currentReplyTarget.value = null
    loadMoreReviews(true)
  } catch (e) {
    console.error(e)
  }
}

const handleDelete = (id, type) => {
  ElMessageBox.confirm('确定删除这条内容吗？', '提示', { type: 'warning' })
    .then(async () => {
      if (type === 'review') await deleteReview(id)
      else await deleteReply(id)
      ElMessage.success('删除成功')
      loadMoreReviews(true)
    })
    .catch(() => {})
}

// 展开更多回复
const expandReplies = (review) => {
  if (review.isRepliesExpanded) {
    // 如果已经是展开状态，则收起
    review.visibleReplyCount = 3
    review.isRepliesExpanded = false
  } else {
    // 否则展开全部
    review.visibleReplyCount = review.replyList.length
    review.isRepliesExpanded = true
  }
}

const formatTime = (timeStr) => (timeStr ? timeStr.split('T')[1].substring(0, 5) : '')
const goToSeat = (scheduleId) => router.push(`/customer/seats/${scheduleId}`)

onMounted(() => {
  console.log('当前登录用户ID:', userStore.userId)
  loadInitData()
})
</script>

<template>
  <div class="detail-page" v-loading="loading" element-loading-background="#0d0d0d">
    <!-- 顶部虚化背景 -->
    <div class="hero-banner" :style="{ backgroundImage: `url(${movie?.posterUrl || ''})` }">
      <div class="overlay"></div>
    </div>

    <div class="content-wrapper" v-if="movie && movie.title">
      <!-- 电影信息卡片 -->
      <section class="main-info-card">
        <div class="poster-side">
          <div class="vintage-frame">
            <img :src="movie.posterUrl" class="v-poster" />
          </div>
          <div class="play-icon-overlay" v-if="movie.trailerUrl">
            <el-icon @click="window.open(movie.trailerUrl)"><VideoPlay /></el-icon>
          </div>
        </div>

        <div class="text-side">
          <h1 class="movie-title">{{ movie.title }}</h1>
          <p class="movie-subtitle">{{ movie.originalTitle }}</p>
          <div class="meta-row">
            <span class="v-tag">{{ movie.genre }}</span>
            <span class="v-tag">{{ movie.country }}</span>
            <span class="v-tag"
              ><el-icon><Timer /></el-icon> {{ movie.duration }} min</span
            >
          </div>
          <div class="rating-display">
            <div class="gold-score">{{ movie.rating || 'N/A' }}</div>
            <div class="rating-right">
              <el-rate v-model="movie.rating" disabled :max="10" />
              <span class="review-count">{{ movie.reviewCount || 0 }} 人评论</span>
            </div>
          </div>
          <div class="synopsis-box">
            <h3 class="gold-label">STORYLINE / 剧情简介</h3>
            <p class="description">{{ movie.synopsis }}</p>
          </div>
        </div>
      </section>

      <section class="cast-section">
        <div class="cast-container">
          <!-- 导演部分 -->
          <div class="cast-group" v-if="movie.directorList && movie.directorList.length > 0">
            <h3 class="gold-label">DIRECTORS / 导演</h3>
            <div class="cast-list">
              <div class="cast-item" v-for="d in movie.directorList" :key="d.id">
                <div class="avatar-wrapper">
                  <el-avatar :size="80" :src="d.avatarUrl" class="cast-avatar">
                    {{ d.name.charAt(0) }}
                  </el-avatar>
                </div>
                <span class="cast-name">{{ d.name }}</span>
                <span class="cast-role">导演</span>
              </div>
            </div>
          </div>

          <!-- 主演部分 -->
          <div class="cast-group" v-if="movie.actorList && movie.actorList.length > 0">
            <h3 class="gold-label">CAST / 主演</h3>
            <div class="cast-list">
              <div class="cast-item" v-for="a in movie.actorList" :key="a.id">
                <div class="avatar-wrapper">
                  <el-avatar :size="80" :src="a.avatarUrl" class="cast-avatar">
                    {{ a.name.charAt(0) }}
                  </el-avatar>
                </div>
                <span class="cast-name">{{ a.name }}</span>
                <span class="cast-role">饰 主演</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 选座购票 -->
      <section class="booking-section">
        <div class="booking-header">
          <div class="title-with-line">
            <h2 class="section-h2">SHOWTIMES / 选座购票</h2>
            <div class="accent-line"></div>
          </div>
          <div class="filter-controls">
            <span class="filter-label"
              ><el-icon><Location /></el-icon> 影院：</span
            >
            <el-select
              v-model="selectedCinemaId"
              filterable
              placeholder="选择影院"
              class="vintage-select"
              popper-class="vintage-dropdown-popper"
            >
              <el-option v-for="c in cinemas" :key="c.id" :label="c.name" :value="c.id">
                <div class="opt-content">
                  <span class="opt-name">{{ c.name }}</span>
                  <span class="opt-city">{{ c.city }}</span>
                </div>
              </el-option>
            </el-select>
          </div>
        </div>

        <div class="schedule-list">
          <div class="schedule-card" v-for="s in filteredSchedules" :key="s.id">
            <div class="time-col">
              <div class="start-time">{{ formatTime(s.startTime) }}</div>
              <div class="end-time">预计 {{ formatTime(s.endTime) }} 散场</div>
            </div>
            <div class="hall-col">
              <div class="hall-name">{{ hallMap[s.hallId] || s.hallId + '号厅' }}</div>
              <div class="hall-type">Standard 2D / {{ movie.language }} / {{ movie.country }}</div>
            </div>
            <div class="price-col">
              <span class="currency">￥</span><span class="amount">{{ s.price }}</span>
            </div>
            <div class="action-col">
              <button class="gold-button" @click="goToSeat(s.id)">选座购票</button>
            </div>
          </div>
          <div v-if="filteredSchedules.length === 0" class="empty-schedules">
            <el-empty description="该影院今日暂无场次，请切换影院" />
          </div>
        </div>
      </section>

      <!-- 影评区域 -->
      <section class="review-section">
        <div class="booking-header">
          <div class="title-with-line">
            <h2 class="section-h2">CRITICS / 观众热评</h2>
            <div class="accent-line"></div>
          </div>
          <el-button type="warning" class="write-btn" @click="openWriteDialog">
            <el-icon><Edit /></el-icon> 写影评
          </el-button>
        </div>

        <div
          class="rev-container scrollable-box"
          v-infinite-scroll="loadMoreReviews"
          :infinite-scroll-disabled="reviewLoading || !hasMoreReviews"
          :infinite-scroll-distance="50"
        >
          <transition-group name="staggered-fade">
            <div
              class="rev-card"
              v-for="(r, index) in reviews"
              :key="r.id"
              :style="{ '--i': index % 10 }"
            >
              <!-- 头部 -->
              <div class="rev-header">
                <div class="user-info">
                  <el-avatar :src="r.avatarUrl" :size="40" class="gold-avatar" />
                  <div class="user-meta">
                    <span class="username">{{ r.nickname }}</span>
                    <el-rate v-model="r.score" disabled size="small" :max="10" />
                  </div>
                </div>
                <div class="rev-time">{{ r.createTime?.split('T')[0] }}</div>
              </div>

              <!-- 内容 -->
              <div class="rev-content">{{ r.content }}</div>

              <!-- 操作栏 -->
              <div class="rev-actions">
                <div class="act-left">
                  <span class="act-btn" :class="{ active: r.isLiked }" @click="handleLike(r, 1)">
                    <el-icon v-if="r.isLiked"><StarFilled /></el-icon>
                    <el-icon v-else><Star /></el-icon>
                    {{ r.likeCount || 0 }}
                  </span>
                  <!-- 点击回复按钮：回复楼主 -->
                  <span class="act-btn" @click="toggleReplyBox(r.id)">
                    <el-icon><ChatDotRound /></el-icon>
                    {{ r.replyList?.length ? r.replyList.length : '' }} 回复
                  </span>
                </div>
                <!-- 删除按钮 -->
                <div class="act-right" v-if="userStore.userId && userStore.userId === r.userId">
                  <span class="act-btn delete" @click="handleDelete(r.id, 'review')">
                    <el-icon><Delete /></el-icon> 删除
                  </span>
                </div>
              </div>

              <!-- 回复输入框 -->
              <div class="reply-input-box" v-if="activeReplyBox[r.id]">
                <el-input
                  v-model="replyContent"
                  placeholder="友善发言，温暖人心..."
                  type="textarea"
                  :rows="2"
                  resize="none"
                  class="retro-textarea"
                />
                <div class="reply-footer">
                  <el-button type="primary" size="small" color="#d4af37" @click="submitReply(r)"
                    >发送</el-button
                  >
                </div>
              </div>

              <!-- 
                回复列表：样式重构 
                v-if: 只有有数据才渲染
                这里去掉了 v-show，因为默认是展开的，只是只显示前3条
              -->
              <div class="replies-wrapper" v-if="r.replyList && r.replyList.length > 0">
                <!-- 循环渲染：使用 slice 控制显示数量 -->
                <div
                  class="reply-card"
                  v-for="reply in r.replyList.slice(0, r.visibleReplyCount)"
                  :key="reply.id"
                >
                  <!-- 左侧头像 -->
                  <div class="reply-avatar-col">
                    <el-avatar :src="reply.avatarUrl" :size="24" class="sub-avatar" />
                  </div>

                  <!-- 右侧内容 -->
                  <div class="reply-main-col">
                    <!-- 昵称 + 回复对象 -->
                    <div class="reply-meta">
                      <span class="reply-nick">{{ reply.nickname }}</span>

                      <!-- 如果有 targetUserId，显示 ▶ 某某 -->
                      <span v-if="reply.targetUserId" class="reply-target-tag">
                        回复 <span class="at-name">@{{ reply.targetNickname }}</span>
                      </span>
                    </div>

                    <!-- 内容 -->
                    <div class="reply-text-content">
                      {{ reply.content }}
                    </div>

                    <!-- 底部栏：时间 + 点赞 + 回复按钮 -->
                    <div class="reply-actions-row">
                      <span class="reply-date">{{
                        reply.createTime?.substring(5, 16).replace('T', ' ')
                      }}</span>

                      <span
                        class="sub-act-btn"
                        @click="handleLike(reply, 2)"
                        :class="{ active: reply.isLiked }"
                      >
                        <el-icon><StarFilled v-if="reply.isLiked" /><Star v-else /></el-icon>
                        {{ reply.likeCount || 0 }}
                      </span>

                      <!-- 楼中楼回复 -->
                      <span
                        class="sub-act-btn"
                        @click="handleReplyToUser(r.id, reply.nickname, reply.userId)"
                      >
                        回复
                      </span>

                      <span
                        class="sub-act-btn delete-btn"
                        v-if="userStore.userId === reply.userId"
                        @click="handleDelete(reply.id, 'reply')"
                      >
                        删除
                      </span>
                    </div>
                  </div>
                </div>

                <!-- 查看更多按钮 -->
                <div class="view-more-replies" v-if="r.replyList.length > 3">
                  <span class="more-btn" @click="expandReplies(r)">
                    <!-- 根据状态显示不同文字 -->
                    <span v-if="!r.isRepliesExpanded">
                      共 {{ r.replyList.length }} 条回复，点击查看 <el-icon><ArrowDown /></el-icon>
                    </span>
                    <span v-else>
                      收起回复 <el-icon><ArrowUp /></el-icon>
                    </span>
                  </span>
                </div>
              </div>
            </div>
          </transition-group>

          <div v-if="reviewLoading" class="loading-state">
            <span class="dot-flashing">加载更多精彩...</span>
          </div>
          <div v-if="!hasMoreReviews && reviews.length > 0" class="no-more">- THE END -</div>
          <el-empty
            v-if="reviews.length === 0 && !reviewLoading"
            description="暂无评论，快来抢沙发"
          />
        </div>
      </section>
    </div>

    <!-- 写影评弹窗 -->
    <el-dialog
      v-model="showWriteDialog"
      title="撰写影评"
      width="500px"
      class="retro-dialog"
      append-to-body
    >
      <el-form label-position="top">
        <el-form-item label="您的评分">
          <el-rate v-model="writeForm.score" :max="10" show-score text-color="#ff9900" />
        </el-form-item>
        <el-form-item label="影评内容">
          <el-input
            v-model="writeForm.content"
            type="textarea"
            :rows="5"
            placeholder="分享你的观影感受..."
            class="retro-textarea"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showWriteDialog = false">取消</el-button>
        <el-button type="warning" @click="submitReview" :loading="submitting">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.detail-page {
  background-color: #0d0d0d;
  min-height: 100vh;
  color: #ccc;
  font-family: 'Georgia', serif;
}
.hero-banner {
  height: 500px;
  background-size: cover;
  filter: blur(50px) brightness(0.3);
  position: absolute;
  width: 100%;
  top: 0;
  z-index: 0;
}
.content-wrapper {
  position: relative;
  z-index: 1;
  max-width: 1100px;
  margin: 0 auto;
  padding: 100px 20px;
}

/* 电影卡片 */
.main-info-card {
  display: flex;
  gap: 60px;
  margin-bottom: 80px;
}
.vintage-frame {
  border: 5px solid #d4af37;
  padding: 6px;
  background: #1a1a1a;
}
.v-poster {
  width: 300px;
  display: block;
}
.movie-title {
  font-size: 52px;
  color: #d4af37;
  margin: 0;
  line-height: 1.1;
}
.movie-subtitle {
  color: #888;
  font-style: italic;
  margin: 10px 0 30px;
  font-size: 20px;
}
.v-tag {
  border: 1px solid #d4af37;
  color: #d4af37;
  padding: 4px 15px;
  margin-right: 15px;
  background: rgba(212, 175, 55, 0.05);
}
.gold-score {
  font-size: 64px;
  color: #ffaa00;
  font-weight: bold;
}
.rating-display {
  display: flex;
  align-items: center;
  gap: 25px;
  margin: 35px 0;
}
.rating-right {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.review-count {
  font-size: 12px;
  color: #666;
}
.gold-label {
  color: #d4af37;
  font-size: 14px;
  letter-spacing: 2px;
  border-bottom: 1px solid #333;
  padding-bottom: 10px;
  margin-bottom: 20px;
}
.description {
  line-height: 1.9;
  color: #aaa;
  font-size: 16px;
  text-align: justify;
}

/* 排片卡片布局 */
.schedule-list {
  min-height: 200px;
}
.schedule-card {
  display: flex;
  align-items: center;
  background: #151515;
  border: 1px solid #252525;
  padding: 25px 40px;
  margin-bottom: 15px;
  transition: 0.3s;
  gap: 30px;
}
.schedule-card:hover {
  border-color: #d4af37;
  background: #1a1a1a;
  transform: scale(1.02);
}
.time-col {
  min-width: 120px;
  display: flex;
  flex-direction: column;
}
.time-col .start-time {
  font-size: 38px;
  font-weight: bold;
  color: #fff;
  line-height: 1;
}
.time-col .end-time {
  font-size: 13px;
  color: #555;
  margin-top: 8px;
}
.hall-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 5px;
  overflow: hidden;
}
.hall-name {
  font-size: 20px;
  color: #d4af37;
  font-weight: bold;
  white-space: nowrap;
}
.hall-type {
  color: #888;
  font-size: 13px;
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
}
.price-col {
  min-width: 100px;
  text-align: right;
}
.price-col .amount {
  font-size: 36px;
  font-weight: bold;
  color: #f56c6c;
}
.price-col .currency {
  font-size: 18px;
  color: #f56c6c;
}
.action-col {
  min-width: 120px;
}
.gold-button {
  width: 100%;
  background: #d4af37;
  color: #000;
  border: none;
  padding: 15px 0;
  font-weight: bold;
  font-size: 16px;
  cursor: pointer;
  white-space: nowrap;
}

/* 影评区域 */
.booking-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 40px;
}
.title-with-line {
  flex: 1;
}
.section-h2 {
  font-size: 26px;
  color: #d4af37;
  margin: 0;
  letter-spacing: 2px;
}
.accent-line {
  width: 60px;
  height: 4px;
  background: #d4af37;
  margin-top: 10px;
}
.filter-controls {
  display: flex;
  align-items: center;
}
.filter-label {
  color: #d4af37;
  font-weight: bold;
  margin-right: 15px;
  white-space: nowrap; /* 强制在一行显示 */
  display: flex; /* 使用 flex 让图标和文字对齐更完美 */
  align-items: center;
  gap: 5px;
}

.rev-container {
  height: 800px;
  overflow-y: auto;
  padding-right: 10px;
}
.rev-container::-webkit-scrollbar {
  width: 6px;
}
.rev-container::-webkit-scrollbar-thumb {
  background: #333;
  border-radius: 3px;
}
.rev-container::-webkit-scrollbar-track {
  background: #111;
}

.rev-card {
  background: #151515;
  border: 1px solid #333;
  padding: 20px;
  margin-bottom: 20px;
  border-radius: 4px;
  transition: all 0.3s ease;
  content-visibility: auto;
}
.rev-card:hover {
  border-color: #555;
  background: #1a1a1a;
}
.rev-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 15px;
}
.user-info {
  display: flex;
  gap: 15px;
  align-items: center;
}
.gold-avatar {
  border: 2px solid #d4af37;
}
.user-meta {
  display: flex;
  flex-direction: column;
}
.username {
  color: #d4af37;
  font-weight: bold;
  font-size: 16px;
}
.rev-time {
  color: #666;
  font-size: 12px;
}
.rev-content {
  color: #ccc;
  line-height: 1.6;
  font-size: 15px;
  margin-bottom: 15px;
  padding-left: 55px;
}

.rev-actions {
  display: flex;
  justify-content: space-between;
  padding-left: 55px;
  margin-bottom: 10px;
}
.act-btn {
  color: #666;
  font-size: 13px;
  cursor: pointer;
  margin-right: 20px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition: 0.2s;
}
.act-btn:hover {
  color: #d4af37;
}
.act-btn.active {
  color: #d4af37;
}
.act-btn.delete:hover {
  color: #f56c6c;
}

.reply-input-box {
  margin-left: 55px;
  background: #222;
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 15px;
  animation: slideDown 0.3s ease;
}
.reply-footer {
  text-align: right;
  margin-top: 10px;
}

/* ====================================
   全新回复列表样式 (仿B站/卡片式)
   ==================================== */
.replies-wrapper {
  margin-left: 55px;
  margin-top: 10px;
  background: #101010;
  border-radius: 6px;
  padding: 5px 15px;
  border: 1px solid #2a2a2a;
}
.reply-card {
  display: flex;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #1f1f1f;
}
.reply-card:last-child {
  border-bottom: none;
}
.reply-avatar-col {
  flex-shrink: 0;
  padding-top: 2px;
}
.sub-avatar {
  border: 1px solid #444;
}
.reply-main-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.reply-meta {
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.reply-nick {
  color: #bfa15f; /* 暗金 */
  font-weight: bold;
  font-size: 13px;
}
.reply-target-tag {
  color: #888;
  font-size: 12px;
}
.at-name {
  color: #6a92b3; /* 类似链接的淡蓝色，标示被回复人 */
  margin-left: 2px;
}
.reply-text-content {
  color: #ddd;
  font-size: 14px;
  line-height: 1.5;
}
.reply-actions-row {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-top: 4px;
}
.reply-date {
  color: #555;
  font-size: 12px;
}
.sub-act-btn {
  font-size: 12px;
  color: #666;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 3px;
  transition: color 0.2s;
}
.sub-act-btn:hover {
  color: #ccc;
}
.sub-act-btn.active {
  color: #d4af37;
}
.sub-act-btn.delete-btn:hover {
  color: #f56c6c;
}

/* 查看更多按钮 */
.view-more-replies {
  padding: 10px 0 5px 45px; /* 对齐内容 */
  font-size: 13px;
}
.more-btn {
  color: #6a92b3;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.more-btn:hover {
  color: #8bb6da;
}

/* ==================================== */

.loading-state {
  text-align: center;
  padding: 20px;
  color: #666;
  font-size: 12px;
}
.no-more {
  text-align: center;
  padding: 30px;
  color: #444;
  font-size: 12px;
  letter-spacing: 2px;
}
.write-btn {
  background-color: #d4af37;
  color: #000;
  border: none;
  font-weight: bold;
}

.staggered-fade-enter-active,
.staggered-fade-leave-active {
  transition: all 0.5s ease;
}
.staggered-fade-enter-from,
.staggered-fade-leave-to {
  opacity: 0;
  transform: translateY(30px);
}
.rev-card {
  transition-delay: calc(0.05s * var(--i));
}
@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

:deep(.retro-dialog) {
  background: #1a1a1a;
  border: 1px solid #d4af37;
}
:deep(.retro-dialog .el-dialog__title) {
  color: #d4af37;
}
:deep(.retro-dialog .el-dialog__body) {
  color: #ccc;
}
:deep(.retro-textarea .el-textarea__inner) {
  background: #222;
  border: 1px solid #444;
  color: #eee;
}
:deep(.retro-textarea .el-textarea__inner:focus) {
  border-color: #d4af37;
}

/* 深度覆盖选择框样式 */
:deep(.vintage-select) {
  width: 280px; /* 适当增加宽度，防止长影院名显示不全 */
}

/* 覆盖输入框包装器 */
:deep(.vintage-select .el-input__wrapper) {
  background-color: #1a1a1a !important; /* 深色背景 */
  box-shadow: 0 0 0 1px #d4af37 inset !important; /* 金色边框 */
  border-radius: 4px;
  height: 40px;
}

/* 覆盖输入框内容 */
:deep(.vintage-select .el-input__inner) {
  color: #d4af37 !important; /* 金色文字 */
  font-weight: bold;
  background-color: transparent !important;
}

/* 覆盖右侧箭头图标颜色 */
:deep(.vintage-select .el-input__suffix .el-icon) {
  color: #d4af37 !important;
}

/* 鼠标悬停时的边框颜色 */
:deep(.vintage-select:hover .el-input__wrapper) {
  box-shadow: 0 0 0 1px #ffaa00 inset !important; /* 悬停时稍微发亮的金色 */
}

/* 消除聚焦时的蓝色光圈 */
:deep(.vintage-select .el-input.is-focus .el-input__wrapper) {
  box-shadow: 0 0 0 1px #ffaa00 inset !important;
}
</style>

<style>
.vintage-dropdown-popper {
  background-color: #1a1a1a !important;
  border: 1px solid #d4af37 !important;
  min-width: 320px !important;
}
.vintage-dropdown-popper .el-select-dropdown__item {
  color: #888 !important;
  height: auto !important;
  padding: 10px 20px !important;
  border-bottom: 1px solid #222;
}
.vintage-dropdown-popper .el-select-dropdown__item.hover,
.vintage-dropdown-popper .el-select-dropdown__item:hover {
  background-color: #d4af37 !important;
  color: #000 !important;
}
.vintage-dropdown-popper .el-select-dropdown__item.selected {
  color: #d4af37 !important;
  font-weight: bold;
}
.opt-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.opt-name {
  font-size: 14px;
  font-weight: bold;
  white-space: nowrap;
}
.opt-city {
  font-size: 12px;
  opacity: 0.7;
  margin-left: 20px;
  white-space: nowrap;
}
/* 演职人员板块样式 */
.cast-section {
  margin-bottom: 60px;
  padding: 30px;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid #1a1a1a;
  border-radius: 8px;
}

.cast-container {
  display: flex;
  flex-direction: column;
  gap: 40px;
}

.cast-group {
  width: 100%;
}

.cast-list {
  display: flex;
  flex-wrap: wrap;
  gap: 30px;
  margin-top: 20px;
}

.cast-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100px;
  transition: transform 0.3s ease;
}

.cast-item:hover {
  transform: translateY(-5px);
}

.avatar-wrapper {
  position: relative;
  padding: 4px;
  border: 2px solid #333;
  border-radius: 50%;
  margin-bottom: 12px;
  transition: border-color 0.3s;
}

.cast-item:hover .avatar-wrapper {
  border-color: #d4af37;
}

.cast-avatar {
  background: #1a1a1a;
  color: #d4af37;
  font-weight: bold;
  border: 1px solid #222;
}

.cast-name {
  color: #eee;
  font-size: 14px;
  font-weight: bold;
  text-align: center;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  width: 100%;
}

.cast-role {
  color: #666;
  font-size: 12px;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .cast-list {
    gap: 15px;
    justify-content: space-around;
  }
}
</style>