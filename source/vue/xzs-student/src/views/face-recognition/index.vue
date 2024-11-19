<template>
  <div class="face-recognition">
    <h1>Face Recognition</h1>
    <video ref="video" width="640" height="480" autoplay></video>
    <button @click="startRecognition">Start Recognition</button>
    <!-- <p v-if="recognitionResult">Recognition Result: {{ recognitionResult }}</p> !-->
  </div>
</template>

<script>
import axios from 'axios'
import userApi from '@/api/user'

export default {
  data () {
    return {
      name: 'FaceRecognition',
      userName: ''// 当前用户
    }
  },
  methods: {
    async startRecognition () {
      const videoElement = this.$refs.video
      const canvas = document.createElement('canvas')
      const context = canvas.getContext('2d')

      canvas.width = videoElement.videoWidth
      canvas.height = videoElement.videoHeight
      context.drawImage(videoElement, 0, 0, canvas.width, canvas.height)

      const imageData = canvas.toDataURL('image/jpeg')

      try {
        const response = await axios.post('/api/face-recognition', { image: imageData })
        console.log(response.data.result)
        if (response.data.result.includes('success') && response.data.result.includes(this.userName)) {
          // 假设你希望在识别成功后使用一个特定的 examId 进行跳转
          alert('识别成功！' + this.userName)
          this.$router.push({ path: '/do', query: { id: this.examId } })
        } else if (response.data.result.includes('success') && !response.data.result.includes(this.userName)) {
          alert('人脸不匹配')
        } else {
          alert('未识别到人脸，请重试！')
        }
      } catch (error) {
        console.error('Error during face recognition:', error)
        // this.recognitionResult = 'Recognition failed'
      }
    }
  },
  mounted () {
    // 获取当前用户信息
    userApi.getCurrentUser().then(re => {
      this.userName = re.response.realName
    }).catch(error => {
      console.error('Failed to load user info:', error)
    })
    // 设置页面标题
    document.title = this.$route.meta.title || 'Face Recognition'
    this.examId = this.$route.query.id || null // 获取路由参数中的 id 值,
    // 设置页面背景颜色
    document.body.style.backgroundColor = this.$route.meta.bodyBackground || '#fff'

    // 初始化摄像头
    navigator.mediaDevices.getUserMedia({ video: true })
      .then(stream => {
        this.$refs.video.srcObject = stream
      })
      .catch(error => {
        console.error('Error accessing camera:', error)
      })
  },
  beforeDestroy () {
    // 清理页面背景颜色
    document.body.style.backgroundColor = ''
  }
}
</script>

<style scoped>
.face-recognition {
  text-align: center;
}
</style>
