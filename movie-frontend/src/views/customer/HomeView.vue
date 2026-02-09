<!-- eslint-disable no-unused-vars -->
<!-- eslint-disable vue/no-unused-vars -->
<script setup>
import { ref, onMounted } from 'vue'
import { getTop20 } from '@/api/movie' // 请确保此处接口已支持返回20条
import { useRouter } from 'vue-router'
import { Star, Film, Timer, Calendar, InfoFilled } from '@element-plus/icons-vue'

const router = useRouter()
const movieList = ref([])
const loading = ref(true)

// 详情弹窗控制
const detailVisible = ref(false)
const activeMovie = ref({})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getTop20()
    movieList.value = res.data || res
  } catch (error) {
    console.error('加载失败', error)
  } finally {
    loading.value = false
  }
}

const openDetail = (movie) => {
  activeMovie.value = movie
  detailVisible.value = true
}

const goToMovie = (id) => {
  router.push({
    name: 'MovieDetail',
    params: { id: id },
  })
}

onMounted(() => loadData())
</script>

<template>
  <div class="vintage-home">
    <!-- 顶部装饰栏 -->
    <header class="retro-header">
      <div class="logo">MOVIE<span>COLLECTION</span></div>
      <div class="tagline">WALL OF FAME - PRIVATE SCREENING</div>
    </header>

    <!-- 电影展示区域 -->
    <main class="wall-container" v-loading="loading" element-loading-background="rgba(0,0,0,0.5)">
      <div class="wall-grid">
        <!-- 每一个 item 代表一个独立的展示单元（电影+独立书架） -->
        <div
          v-for="item in movieList"
          :key="item.id"
          class="display-unit"
          @click="openDetail(item)"
        >
          <!-- 这里的 wrapper 负责 DVD 盒子的动画 -->
          <div class="dvd-box-wrapper">
            <!-- DVD 盒子主体 -->
            <div class="dvd-case">
              <img :src="item.posterUrl" class="poster-front" />
              <div class="case-spine"></div>
            </div>

            <!-- 隐藏在盒子里的光盘 -->
            <div class="dvd-disc">
              <div class="disc-inner" :style="{ backgroundImage: `url(${item.posterUrl})` }"></div>
            </div>

            <!-- 盒子底部阴影 -->
            <div class="box-shadow"></div>
          </div>

          <!-- 独立的小书架/隔板 -->
          <div class="single-shelf-board">
            <div class="wood-texture"></div>
            <div class="shelf-label">{{ item.title }}</div>
          </div>
        </div>
      </div>
    </main>

    <!-- 电影详情对话框 (保持原样) -->
    <el-dialog
      v-model="detailVisible"
      width="900px"
      class="retro-dialog"
      :show-close="false"
      destroy-on-close
      append-to-body
    >
      <div class="detail-display" v-if="activeMovie.id">
        <div class="poster-large">
          <img :src="activeMovie.posterUrl" alt="poster" />
          <div class="poster-texture"></div>
        </div>
        <div class="info-content">
          <h1 class="movie-title">{{ activeMovie.title }}</h1>
          <p class="movie-sub">{{ activeMovie.originalTitle }}</p>

          <div class="meta-tags">
            <el-tag effect="dark" type="warning" size="small">{{ activeMovie.genre }}</el-tag>
            <el-tag effect="dark" type="info" size="small">{{ activeMovie.country }}</el-tag>
          </div>

          <div class="stats-grid">
            <div class="stat-item">
              <el-icon><Star /></el-icon>
              <span>{{ activeMovie.rating }} / 10</span>
            </div>
            <div class="stat-item">
              <el-icon><Timer /></el-icon>
              <span>{{ activeMovie.duration }} min</span>
            </div>
            <div class="stat-item">
              <el-icon><Calendar /></el-icon>
              <span>{{ activeMovie.releaseDate }}</span>
            </div>
          </div>

          <div class="synopsis-box">
            <h3>
              <el-icon><InfoFilled /></el-icon> 剧情简介
            </h3>
            <p>{{ activeMovie.synopsis }}</p>
          </div>

          <div class="action-bar">
            <el-button type="warning" class="buy-btn" @click="goToMovie(activeMovie.id)">
              立即购票 / 查看详情
            </el-button>
            <el-button link class="close-btn" @click="detailVisible = false">返回墙面</el-button>
          </div>
        </div>
      </div>
    </el-dialog>

    <footer class="vintage-footer">
      <p>© 2025 VINTAGE MOVIE COLLECTOR</p>
    </footer>
  </div>
</template>

<style scoped>
/* --- 核心修改：背景部分 --- */
.vintage-home {
  min-height: 100vh;
  position: relative; /* 为灯光定位 */

  /* 古典红砖墙背景：底层砖红颜色 + 砖纹图案叠加 */
  background-color: #230e09;
  background-image:
    linear-gradient(rgba(0, 0, 0, 0.4), rgba(0, 0, 0, 0.6)),
    /* 整体环境调暗 */ url('https://www.transparenttextures.com/patterns/brick-wall.png'); /* 砖块纹理 */

  color: #d4af37;
  font-family: 'Georgia', serif;
  padding: 40px 20px;
  overflow-x: hidden;
}

/* 顶部散射灯光效果 */

/* 确保内容在灯光层之上 */
.retro-header,
.wall-container,
.vintage-footer {
  position: relative;
  z-index: 2;
}

/* --- 以下保持原样，未做修改 --- */
.retro-header {
  text-align: center;
  margin-bottom: 60px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  padding-bottom: 20px;
}

.logo {
  font-size: 42px;
  font-weight: bold;
  letter-spacing: 4px;
  color: #eee;
  text-shadow: 2px 4px 6px rgba(0, 0, 0, 0.5);
}
.logo span {
  color: #111;
  background: #d4af37;
  padding: 0 10px;
  margin-left: 10px;
}

.wall-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px 0;
}

.wall-grid {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  column-gap: 80px;
  row-gap: 100px;
  padding-bottom: 100px;
}

.display-unit {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 180px;
  z-index: 1;
}

.display-unit:hover {
  z-index: 100;
}

.display-unit:nth-child(even) {
  margin-top: 100px;
}

.dvd-box-wrapper {
  position: relative;
  width: 160px;
  height: 250px;
  cursor: pointer;
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  transform: rotateY(-10deg);
  transform-style: preserve-3d;
}

.display-unit:hover .dvd-box-wrapper {
  transform: translateY(-15px) rotateY(0deg) scale(1.05);
}

.dvd-case {
  position: relative;
  width: 100%;
  height: 100%;
  background: #222;
  border-radius: 2px 4px 4px 2px;
  z-index: 5;
  box-shadow: 10px 10px 20px rgba(0, 0, 0, 0.7); /* 加深阴影适配砖墙 */
  border: 1px solid #444;
  overflow: hidden;
}

.poster-front {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.case-spine {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 12px;
  background: linear-gradient(to right, rgba(255, 255, 255, 0.2), transparent);
  z-index: 6;
}

.dvd-disc {
  position: absolute;
  top: 15px;
  right: 5px;
  width: 140px;
  height: 140px;
  background: #333;
  border-radius: 50%;
  z-index: 4;
  transition: all 0.6s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  transform: translateX(0);
  opacity: 0;
  border: 2px solid #555;
  display: flex;
  justify-content: center;
  align-items: center;
}

.display-unit:hover .dvd-disc {
  transform: translateX(85px) rotate(180deg);
  opacity: 1;
}

.disc-inner {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background-size: cover;
  clip-path: circle(50% at 50% 50%);
  position: relative;
}
.disc-inner::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 25px;
  height: 25px;
  background: #111;
  border-radius: 50%;
  box-shadow: inset 0 0 5px #000;
}

.single-shelf-board {
  margin-top: -10px;
  width: 200px;
  height: 20px;
  position: relative;
  z-index: 0;
}

.wood-texture {
  width: 100%;
  height: 100%;
  background: linear-gradient(to bottom, #4e342e, #2b1d1a);
  border-radius: 4px;
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.8);
}

.wood-texture::before,
.wood-texture::after {
  content: '';
  position: absolute;
  bottom: -10px;
  width: 15px;
  height: 15px;
  background: #1a0f0d;
  clip-path: polygon(0 0, 100% 0, 0 100%);
}
.wood-texture::before {
  left: 20px;
}
.wood-texture::after {
  right: 20px;
  transform: scaleX(-1);
}

.shelf-label {
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  margin-top: 15px;
  font-size: 13px;
  color: #c4a484;
  text-align: center;
  width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.8);
}

.display-unit:hover .shelf-label {
  color: #d4af37;
}

:deep(.retro-dialog) {
  background: #1c1412 url('https://www.transparenttextures.com/patterns/dark-leather.png') !important;
  border: 1px solid #d4af37;
}
.detail-display {
  display: flex;
  gap: 40px;
  color: #eee;
  padding: 20px;
}
.poster-large {
  flex: 0 0 260px;
}
.poster-large img {
  width: 100%;
  border-radius: 4px;
}
.info-content {
  flex: 1;
}
.movie-title {
  color: #d4af37;
  font-size: 32px;
}
.stats-grid {
  display: flex;
  gap: 30px;
  margin: 20px 0;
  border-top: 1px dashed #333;
  border-bottom: 1px dashed #333;
  padding: 15px 0;
}
.stat-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #ccc;
}
.buy-btn {
  background: #d4af37;
  color: #000;
  border: none;
  font-weight: bold;
}
.vintage-footer {
  text-align: center;
  color: rgba(255, 255, 255, 0.3);
  margin-top: 80px;
  font-size: 12px;
}

@media (max-width: 768px) {
  .display-unit:nth-child(even) {
    margin-top: 0;
  }
  .wall-grid {
    gap: 40px;
  }
}
</style>
