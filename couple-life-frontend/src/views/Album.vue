<template>
  <div class="album love-card">
    <div class="page-head">
      <div>
        <h2 class="gradient-title">情侣相册</h2>
        <p>按日期收藏照片，每张照片都可以留下评论。</p>
      </div>
      <div class="head-actions">
        <template v-if="!selectMode">
          <el-button @click="enterSelectMode">
            <el-icon><Select /></el-icon>
            批量选择
          </el-button>
          <el-button type="primary" @click="uploadVisible = true">
            <el-icon><Upload /></el-icon>
            上传照片
          </el-button>
        </template>
        <template v-else>
          <el-button @click="toggleSelectAll">{{ allSelected ? '全不选' : '全选' }}</el-button>
          <el-button @click="exitSelectMode">取消</el-button>
        </template>
      </div>
    </div>
    <el-empty v-if="!groups.length" description="还没有照片，上传第一张吧 📸" />
    <div v-for="group in groups" :key="group.date" class="day-row">
      <div class="date">📅 {{ group.date }}</div>
      <div class="photos">
        <div v-for="photo in group.photos" :key="photo.id" class="photo-wrap" :class="{ selected: selectMode && selectedIds.has(photo.id) }" @click="onPhotoClick(photo)">
          <el-image :src="photo.imageUrl" fit="cover" class="photo" />
          <div v-if="selectMode" class="check-mask">
            <el-icon v-if="selectedIds.has(photo.id)" class="check-icon"><CircleCheckFilled /></el-icon>
            <el-icon v-else class="check-icon empty"><Remove /></el-icon>
          </div>
        </div>
      </div>
    </div>

    <div v-if="selectMode" class="select-bar">
      <span>已选 {{ selectedIds.size }} 张</span>
      <el-button type="primary" :disabled="selectedIds.size === 0" :loading="exporting" @click="doExport">📦 导出为 ZIP</el-button>
    </div>

    <el-dialog v-model="viewerVisible" title="照片详情" width="560px">
      <img v-if="currentPhoto" :src="currentPhoto.imageUrl" class="big-photo" />
      <p>{{ currentPhoto?.description }}</p>
      <el-divider />
      <div v-if="!comments.length" class="no-comments">还没有评论，留下第一句回忆吧 💬</div>
      <div v-for="comment in comments" :key="comment.id" class="comment"><b>{{ comment.nickname }}</b>：{{ comment.content }}</div>
      <div class="comment-form"><el-input v-model="commentText" placeholder="写一句回忆评论..." /><el-button type="primary" @click="sendComment">评论</el-button></div>
    </el-dialog>
    <el-dialog v-model="uploadVisible" title="📤 上传照片" width="520px">
      <el-form label-position="top">
        <el-form-item label="归档日期"><el-date-picker v-model="uploadForm.shootDate" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="标题"><el-input v-model="uploadForm.title" placeholder="给这组照片起个标题" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="uploadForm.description" placeholder="记录背后的故事..." /></el-form-item>
        <el-upload v-model:file-list="fileList" multiple drag :auto-upload="false">
          <div class="upload-hint">📸 拖拽或点击选择多张照片</div>
        </el-upload>
        <el-button type="primary" size="large" class="upload-btn" @click="submitUpload">开始上传</el-button>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheckFilled, Remove, Select, Upload } from '@element-plus/icons-vue'
import { addPhotoComment, exportPhotos, getPhotoComments, getPhotos, uploadPhotos } from '../api/album'

const groups = ref([])
const uploadVisible = ref(false)
const viewerVisible = ref(false)
const currentPhoto = ref(null)
const comments = ref([])
const commentText = ref('')
const fileList = ref([])
const uploadForm = reactive({ shootDate: new Date().toISOString().slice(0, 10), title: '', description: '' })

const selectMode = ref(false)
const selectedIds = ref(new Set())
const exporting = ref(false)

const allPhotoIds = computed(() => groups.value.flatMap(g => g.photos.map(p => p.id)))
const allSelected = computed(() => allPhotoIds.value.length > 0 && allPhotoIds.value.every(id => selectedIds.value.has(id)))

onMounted(load)

async function load() { groups.value = await getPhotos() }

function enterSelectMode() { selectMode.value = true; selectedIds.value = new Set() }
function exitSelectMode() { selectMode.value = false; selectedIds.value = new Set() }
function toggleSelectAll() { selectedIds.value = allSelected.value ? new Set() : new Set(allPhotoIds.value) }

function onPhotoClick(photo) {
  if (selectMode.value) {
    const next = new Set(selectedIds.value)
    next.has(photo.id) ? next.delete(photo.id) : next.add(photo.id)
    selectedIds.value = next
  } else {
    openPhoto(photo)
  }
}

async function openPhoto(photo) { currentPhoto.value = photo; comments.value = await getPhotoComments(photo.id); viewerVisible.value = true }

async function sendComment() {
  if (!commentText.value) return
  await addPhotoComment(currentPhoto.value.id, { content: commentText.value })
  commentText.value = ''
  comments.value = await getPhotoComments(currentPhoto.value.id)
}

async function submitUpload() {
  const data = new FormData()
  fileList.value.forEach(item => data.append('files', item.raw))
  data.append('shootDate', uploadForm.shootDate)
  data.append('title', uploadForm.title)
  data.append('description', uploadForm.description)
  await uploadPhotos(data)
  ElMessage.success('上传成功 📸')
  uploadVisible.value = false
  fileList.value = []
  await load()
}

async function doExport() {
  if (selectedIds.value.size === 0) return
  exporting.value = true
  try {
    const ids = Array.from(selectedIds.value)
    const response = await exportPhotos(ids)
    const contentType = response.headers['content-type'] || ''
    if (!contentType.includes('application/zip')) {
      ElMessage.error(await readBlobMessage(response.data))
      return
    }
    const disposition = response.headers['content-disposition'] || ''
    const filename = parseFilename(disposition) || `相册_${new Date().toISOString().slice(0, 10)}.zip`
    triggerDownload(response.data, filename)
    ElMessage.success(`已开始下载 ${ids.length} 张照片`)
    exitSelectMode()
  } catch (e) {
    let message = '导出失败'
    if (e.response?.data instanceof Blob) { message = await readBlobMessage(e.response.data) }
    else if (e.message) { message = e.message }
    ElMessage.error(message)
  } finally { exporting.value = false }
}

async function readBlobMessage(blob) {
  try {
    const text = await blob.text()
    try { return JSON.parse(text).message || text || '导出失败' } catch (_) { return text || '导出失败' }
  } catch (_) { return '导出失败' }
}

function parseFilename(disposition) {
  const star = /filename\*=UTF-8''([^;]+)/i.exec(disposition)
  if (star) return decodeURIComponent(star[1])
  const plain = /filename="?([^";]+)"?/i.exec(disposition)
  return plain ? plain[1] : null
}

function triggerDownload(blob, filename) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}
</script>

<style scoped>
.album {
  padding: 28px;
  padding-bottom: 90px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 8px;
}

.page-head h2 {
  margin: 0;
  font-size: 28px;
}

.page-head p {
  color: var(--love-text-light);
  margin: 6px 0 0;
}

.head-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.head-actions .el-button {
  gap: 6px;
}

.day-row {
  margin-top: 28px;
}

.date {
  font-size: 18px;
  font-weight: 800;
  margin-bottom: 14px;
  color: var(--love-primary);
}

.photos {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
}

.photo-wrap {
  position: relative;
  width: 170px;
  height: 126px;
  border-radius: var(--love-radius-sm);
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 8px 24px rgba(255, 124, 168, 0.15);
  transition: transform 0.2s, box-shadow 0.2s;
}

.photo-wrap:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(255, 124, 168, 0.25);
}

.photo-wrap.selected {
  outline: 3px solid var(--love-primary);
  transform: scale(0.97);
}

.photo {
  width: 100%;
  height: 100%;
}

.check-mask {
  position: absolute;
  top: 6px;
  right: 6px;
  background: rgba(255, 255, 255, 0.85);
  border-radius: 50%;
  padding: 2px;
  line-height: 0;
}

.check-icon {
  font-size: 24px;
  color: var(--love-primary);
}

.check-icon.empty {
  color: #c7c7c7;
}

.big-photo {
  width: 100%;
  max-height: 360px;
  object-fit: cover;
  border-radius: var(--love-radius-sm);
}

.no-comments {
  text-align: center;
  color: var(--love-text-muted);
  padding: 12px 0;
}

.comment {
  padding: 12px;
  background: rgba(255, 247, 251, 0.8);
  border-radius: var(--love-radius-xs);
  margin-bottom: 8px;
  transition: background 0.2s;
}

.comment:hover {
  background: rgba(255, 214, 231, 0.25);
}

.comment-form {
  display: flex;
  gap: 10px;
  margin-top: 12px;
}

.upload-hint {
  padding: 40px 20px;
  color: var(--love-text-light);
  text-align: center;
}

:deep(.el-upload-dragger) {
  min-height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-btn {
  margin-top: 14px;
  width: 100%;
}

.select-bar {
  position: fixed;
  left: 50%;
  bottom: 24px;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 24px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 999px;
  box-shadow: 0 12px 32px rgba(255, 124, 168, 0.25);
  backdrop-filter: blur(12px);
  z-index: 100;
}

@media (max-width: 640px) {
  .album {
    padding: 20px;
    padding-bottom: 104px;
  }

  .page-head {
    flex-direction: column;
    align-items: stretch;
  }

  .page-head h2 {
    font-size: 26px;
  }

  .head-actions {
    display: grid;
    grid-template-columns: 1fr;
  }

  .head-actions .el-button {
    width: 100%;
    justify-content: center;
  }

  .photos {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
  }

  .photo-wrap {
    width: 100%;
    height: auto;
    aspect-ratio: 4 / 3;
  }

  .comment-form {
    flex-direction: column;
  }

  .select-bar {
    left: 16px;
    right: 16px;
    bottom: 16px;
    transform: none;
    justify-content: space-between;
    border-radius: var(--love-radius-sm);
  }
}
</style>
