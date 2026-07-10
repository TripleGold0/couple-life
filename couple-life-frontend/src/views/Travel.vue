<template>
  <div class="travel-wrap">
    <section class="map-card love-card">
      <div class="page-head">
        <div>
          <div class="title-line">
            <h2 class="gradient-title">旅行地图日志</h2>
            <span class="trip-count">{{ travels.length }} 个足迹</span>
          </div>
          <p>在地图上标记你们一起去过的地方。</p>
        </div>
        <el-button type="primary" size="large" class="add-travel-btn" @click="openAdd">
          <el-icon><Plus /></el-icon>
          新增旅行
        </el-button>
      </div>
      <el-alert v-if="!isCoupleBound" type="info" :closable="false" class="bind-tip" show-icon>
        当前为未绑定状态，地图仅展示空白框架，<el-link type="primary" :underline="false" @click="goBind">点击去绑定情侣关系</el-link>后即可记录你们的足迹。
      </el-alert>
      <div ref="mapRef" class="map"></div>
    </section>
    <el-drawer v-model="drawer" title="旅行详情" size="440px">
      <template v-if="current">
        <el-carousel height="240px" v-if="current.images?.length">
          <el-carousel-item v-for="url in current.images" :key="url">
            <img :src="url" class="travel-img" />
          </el-carousel-item>
        </el-carousel>
        <div class="drawer-content">
          <h2>{{ current.locationName }}</h2>
          <p class="travel-meta">{{ current.travelDate }} · {{ current.country }} {{ current.city }}</p>
          <p class="travel-desc">{{ current.detail || current.summary }}</p>
          <el-divider />
          <div class="feeling-section">
            <div class="feeling-header">
          <h4>我的感受</h4>
              <el-button v-if="!editingFeeling" type="primary" link @click="startEditFeeling">编辑</el-button>
            </div>
            <template v-if="!editingFeeling">
              <p>{{ current.myFeeling }}</p>
            </template>
            <template v-else>
              <el-input v-model="editedFeeling" type="textarea" :rows="3" placeholder="写下你的感受..." />
              <div class="feeling-actions">
                <el-button type="primary" size="small" @click="saveFeeling">保存</el-button>
                <el-button size="small" @click="cancelEditFeeling">取消</el-button>
              </div>
            </template>
          </div>
          <div class="feeling-section">
        <h4>伴侣感受</h4><p>{{ current.partnerFeeling }}</p>
          </div>
          <el-button type="danger" plain @click="removeCurrent">删除记录</el-button>
        </div>
      </template>
    </el-drawer>
  <el-dialog v-model="formVisible" title="新增旅行记录" width="640px">
      <el-form :model="form" label-position="top">
        <el-row :gutter="12"><el-col :span="12"><el-form-item label="地点"><el-input v-model="form.locationName" placeholder="去了哪里？" /></el-form-item></el-col><el-col :span="12"><el-form-item label="旅行日期"><el-date-picker v-model="form.travelDate" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item></el-col></el-row>
        <el-row :gutter="12"><el-col :span="12"><el-form-item label="经度"><el-input-number v-model="form.longitude" :precision="6" style="width: 100%" /></el-form-item></el-col><el-col :span="12"><el-form-item label="纬度"><el-input-number v-model="form.latitude" :precision="6" style="width: 100%" /></el-form-item></el-col></el-row>
        <el-form-item label="简短评论"><el-input v-model="form.summary" placeholder="一句话概括这次旅行" /></el-form-item>
        <el-form-item label="详细记录"><el-input v-model="form.detail" type="textarea" :rows="3" placeholder="记录旅途中的故事..." /></el-form-item>
        <el-form-item label="我的感受"><el-input v-model="form.myFeeling" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="伴侣感受"><el-input v-model="form.partnerFeeling" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="旅行图片">
          <el-upload :show-file-list="false" :before-upload="handleImageUpload" multiple accept="image/*">
            <el-button>
              <el-icon><Picture /></el-icon>
              上传图片
            </el-button>
          </el-upload>
          <div class="image-preview"><el-image v-for="url in form.imageUrls" :key="url" :src="url" fit="cover" /></div>
        </el-form-item>
        <el-button type="primary" size="large" class="full" @click="save">保存</el-button>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture, Plus } from '@element-plus/icons-vue'
import { addTravel, deleteTravel, getTravelDetail, getTravels, updateTravel } from '../api/travel'
import { uploadImage } from '../api/user'
import { useUserStore } from '../stores/userStore'
import { loadAMap } from '../utils/amapLoader'

const router = useRouter()
const userStore = useUserStore()
const isCoupleBound = computed(() => !!userStore.user?.partner)

const mapRef = ref()
const travels = ref([])
const drawer = ref(false)
const formVisible = ref(false)
const current = ref(null)
const editingFeeling = ref(false)
const editedFeeling = ref('')
const form = reactive({ locationName: '', country: '', city: '', longitude: 116.4074, latitude: 39.9042, travelDate: '', summary: '', detail: '', myFeeling: '', partnerFeeling: '', imageUrls: [] })
let map = null
let markers = []
let AMapApi = null

onMounted(load)

watch(isCoupleBound, (bound) => {
  if (bound) load()
})

watch(drawer, (val) => {
  if (!val) {
    editingFeeling.value = false
    editedFeeling.value = ''
  }
})

async function load() {
  if (isCoupleBound.value) {
    try {
      const list = await getTravels()
      travels.value = Array.isArray(list) ? list : []
    } catch (e) {
      // 保留已有数据，避免网络波动导致全部记录消失
    }
  } else {
    travels.value = []
  }
  await nextTick()
  initMap()
}

async function initMap() {
  if (!mapRef.value) return
  if (map) { map.destroy(); map = null }

  try {
    AMapApi = await loadAMap()
  } catch (_) {
    ElMessage.error('地图加载失败，请稍后重试')
    return
  }

  console.log('开始初始化地图...')

  map = new AMapApi.Map(mapRef.value, {
    zoom: 4,
    center: [104.072998, 35.86166],
    mapStyle: 'amap://styles/normal',
    viewMode: '2D'
  })

  // 加载插件
  AMapApi.plugin(['AMap.ToolBar', 'AMap.Scale', 'AMap.MapType'], function() {
    // 添加控件
    map.addControl(new AMapApi.ToolBar({ position: 'LT' }))
    map.addControl(new AMapApi.Scale({ position: 'LB' }))
    map.addControl(new AMapApi.MapType({ position: 'RT' }))
  })
  
  // 加载标记
  loadMarkers()
  
  // 监听地图点击事件
  map.on('click', handleMapClick)
  console.log('地图初始化完成，点击事件已绑定')
}

function loadMarkers() {
  // 清除现有标记
  markers.forEach(marker => map.remove(marker))
  markers = []
  
  // 添加新标记
  travels.value.forEach(travel => {
    addMarker(travel)
  })
}

function addMarker(travel) {
  if (!map) return
  
  const marker = new AMapApi.Marker({
    position: [travel.longitude, travel.latitude],
    title: travel.locationName,
    content: '<div class="heart-marker"></div>',
    offset: new AMapApi.Pixel(-10, -10)
  })
  
  marker.travelId = travel.id
  
  marker.on('click', async () => {
    if (!ensureBound()) return
    current.value = await getTravelDetail(travel.id)
    drawer.value = true
  })
  
  map.add(marker)
  markers.push(marker)
}

function removeMarker(travelId) {
  const index = markers.findIndex(m => m.travelId === travelId)
  if (index > -1) {
    map.remove(markers[index])
    markers.splice(index, 1)
  }
}

onBeforeUnmount(() => { if (map) { map.destroy(); map = null } })

function ensureBound() {
  if (isCoupleBound.value) return true
  ElMessageBox.confirm('您未绑定情侣关系，请先完成绑定', '提示', {
    confirmButtonText: '去绑定', cancelButtonText: '取消', type: 'warning'
  }).then(() => { router.push('/app/profile') }).catch(() => {})
  return false
}

function goBind() { router.push('/app/profile') }

function openAdd() {
  if (!ensureBound()) return
  Object.assign(form, { locationName: '', country: '', city: '', longitude: 116.4074, latitude: 39.9042, travelDate: '', summary: '', detail: '', myFeeling: '', partnerFeeling: '', imageUrls: [] })
  formVisible.value = true
}

async function handleImageUpload(file) {
  try { const res = await uploadImage(file, 'travel'); form.imageUrls.push(res.url) }
  catch (e) { ElMessage.error('图片上传失败') }
  return false
}

async function save() {
  if (!form.locationName?.trim()) return ElMessage.warning('请填写地点名称')
  if (!form.travelDate) return ElMessage.warning('请选择旅行日期')
  await addTravel({ ...form })
    ElMessage.success('新增成功')
  formVisible.value = false
  await load()
}

async function removeCurrent() {
  try {
    await ElMessageBox.confirm('确定要删除这条旅行记录吗？删除后不可恢复。', '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  const deleteId = current.value.id
  try {
    await deleteTravel(deleteId)
    removeMarker(deleteId)
    travels.value = travels.value.filter(t => t.id !== deleteId)
    ElMessage.success('删除成功')
    drawer.value = false
    current.value = null
    await load()
  } catch {
    ElMessage.error('删除失败，请重试')
  }
}

function startEditFeeling() {
  editedFeeling.value = current.value.myFeeling || ''
  editingFeeling.value = true
}

function cancelEditFeeling() {
  editingFeeling.value = false
  editedFeeling.value = ''
}

async function saveFeeling() {
  try {
    await updateTravel(current.value.id, { myFeeling: editedFeeling.value })
    current.value.myFeeling = editedFeeling.value
    editingFeeling.value = false
    ElMessage.success('感受已更新')
  } catch {
    ElMessage.error('更新失败，请重试')
  }
}

function handleMapClick(e) {
  console.log('地图点击事件触发', e)
  if (!isCoupleBound.value) {
    ElMessage.warning('请先绑定情侣关系后再打卡')
    return
  }
  const { lng, lat } = e.lnglat
  console.log('点击位置:', lng, lat)
  Object.assign(form, { 
    locationName: '', 
    country: '', 
    city: '', 
    longitude: lng, 
    latitude: lat, 
    travelDate: '', 
    summary: '', 
    detail: '', 
    myFeeling: '', 
    partnerFeeling: '', 
    imageUrls: [] 
  })
  formVisible.value = true
  console.log('formVisible 设置为:', formVisible.value)
}
</script>

<style scoped>
.travel-wrap {
  display: grid;
  grid-template-columns: 1fr;
  gap: 20px;
  min-height: calc(100vh - 126px);
}

.map-card {
  padding: 28px;
  min-height: calc(100vh - 126px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18px;
  margin-bottom: 14px;
}

.title-line {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.page-head h2 {
  margin: 0;
  font-size: 28px;
}

.page-head p {
  color: var(--love-text-light);
  margin: 6px 0 0;
}

.trip-count {
  padding: 5px 10px;
  border: 1px solid rgba(255, 124, 168, 0.22);
  border-radius: 999px;
  background: rgba(255, 247, 251, 0.74);
  color: #8e5e73;
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
}

.add-travel-btn {
  gap: 6px;
  flex: 0 0 auto;
}

.bind-tip {
  margin: 14px 0;
  border-radius: var(--love-radius-sm);
  flex: 0 0 auto;
}

.map {
  flex: 1;
  min-height: 520px;
  border-radius: var(--love-radius);
  border: 1px solid rgba(255, 255, 255, 0.78);
  background:
    linear-gradient(135deg, rgba(255, 247, 232, 0.72), rgba(231, 220, 255, 0.72)),
    radial-gradient(circle at 20% 20%, rgba(255, 124, 168, 0.18), transparent 30%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.74);
  overflow: hidden;
}

.travel-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: var(--love-radius-sm);
}

.drawer-content {
  padding-top: 8px;
}

.drawer-content h2 {
  margin: 0 0 8px;
  font-size: 22px;
  color: var(--love-text);
}

.travel-meta {
  color: var(--love-text-light);
  font-size: 14px;
  margin: 0 0 12px;
}

.travel-desc {
  color: #8e6d7c;
  line-height: 1.7;
  margin: 0 0 16px;
}

.feeling-section {
  margin-bottom: 16px;
}

.feeling-section h4 {
  margin: 0 0 4px;
  font-size: 15px;
  color: var(--love-text);
}

.feeling-section p {
  color: #8e6d7c;
  line-height: 1.7;
  margin: 0;
}

.feeling-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.feeling-header h4 {
  margin: 0;
}

.feeling-actions {
  margin-top: 8px;
  display: flex;
  gap: 8px;
}

.image-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.image-preview .el-image {
  width: 80px;
  height: 80px;
  border-radius: var(--love-radius-xs);
  transition: transform 0.2s;
}

.image-preview .el-image:hover {
  transform: scale(1.05);
}

.full {
  width: 100%;
}

@media (max-width: 900px) {
  .travel-wrap,
  .map-card {
    min-height: auto;
  }

  .map-card {
    padding: 20px;
  }

  .page-head {
    flex-direction: column;
  }

  .add-travel-btn {
    width: 100%;
    justify-content: center;
  }

  .map {
    min-height: 460px;
  }
}

@media (max-width: 640px) {
  .map-card {
    padding: 16px;
  }

  .page-head h2 {
    font-size: 24px;
  }

  .map {
    min-height: 380px;
    border-radius: var(--love-radius-sm);
  }
}
</style>

<!-- 非 scoped 样式：心形标记必须在 AMap DOM 中生效 -->
<style>
.heart-marker {
  width: 20px;
  height: 20px;
  background: #ff6f9f;
  position: relative;
  transform: rotate(-45deg);
  box-shadow: 0 2px 6px rgba(255, 111, 159, 0.4);
  transition: transform 0.2s;
  cursor: pointer;
}

.heart-marker:hover {
  transform: rotate(-45deg) scale(1.2);
}

.heart-marker::before,
.heart-marker::after {
  content: '';
  width: 20px;
  height: 20px;
  background: #ff6f9f;
  border-radius: 50%;
  position: absolute;
}

.heart-marker::before {
  top: -10px;
  left: 0;
}

.heart-marker::after {
  top: 0;
  left: 10px;
}
</style>
