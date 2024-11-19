<template>
    <div class="compose-paper">
      <el-form ref="form" :model="form" label-width="120px">
        <!-- 年级选择 -->
        <el-form-item label="选择年级" required>
          <el-select v-model="form.grade" placeholder="请选择年级">
            <el-option label="大一" value="大一"></el-option>
            <el-option label="大二" value="大二"></el-option>
            <el-option label="大三" value="大三"></el-option>
            <el-option label="大四" value="大四"></el-option>
          </el-select>
        </el-form-item>
        <!-- 学科选择 -->
        <el-form-item label="选择学科" required>
          <el-select v-model="form.subject" placeholder="请选择学科">
            <el-option label="数据结构" value="数据结构"></el-option>
            <!-- 如果有其他学科可以继续添加 -->
          </el-select>
        </el-form-item>
        <!-- 难易程度选择 -->
        <el-form-item label="选择难易程度" required>
          <el-select v-model="form.difficulty" placeholder="请选择难易程度">
            <el-option label="易" value="易"></el-option>
            <el-option label="中" value="中"></el-option>
            <el-option label="难" value="难"></el-option>
          </el-select>
        </el-form-item>
        <!-- 知识点选择和输入分数 -->
        <el-form-item label="知识点分数分配">
          <div class="knowledge-points-container">
            <div v-for="(point, index) in knowledgePoints" :key="index" class="knowledge-point">
              <el-checkbox v-model="point.selected">{{ point.label }}</el-checkbox>
              <el-input v-if="point.selected" v-model.number="point.score" placeholder="请输入分数"></el-input>
            </div>
          </div>
        </el-form-item>
        <!-- 试卷名称 -->
        <el-form-item label="试卷名称" required>
          <el-input v-model="form.paperName" placeholder="请输入试卷名称"></el-input>
        </el-form-item>
        <!-- 建议时长 -->
        <el-form-item label="限制时长" required>
          <el-input-number v-model="form.suggestedDuration" :min="30" :max="180" placeholder="请输入建议时长"></el-input-number>
        </el-form-item>
        <!-- 提交按钮 -->
        <el-form-item>
          <el-button type="primary" @click="submitForm">一键组卷</el-button>
        </el-form-item>
      </el-form>
    </div>
  </template>
<script>
import composePaperAPI from '@/api/composePaper'
export default {
  data () {
    return {
      form: {
        grade: '',
        subject: '',
        difficulty: '',
        paperName: '',
        suggestedDuration: 60
      },
      // knowledgePoints: [
      //   { label: 'Dijkstra', selected: false, score: 0 },
      //   { label: 'Bellman-Ford', selected: false, score: 0 },
      //   { label: 'Prim', selected: false, score: 0 },
      //   { label: 'Kruskal', selected: false, score: 0 },
      //  ]
      knowledgePoints: [
        // 等待后端加载
      ]
    }
  },
  mounted () {
    console.log('开始获取知识点')
    this.fetchKnowledge()
  },
  methods: {
    fetchKnowledge () {
      let _this = this
      composePaperAPI.getKnowledge().then(re => {
        console.log('后端知识点的返回值：' + JSON.stringify(re))
        console.log(JSON.stringify(re).response)
        console.log(re.response)
        if (re.code === 1) {
          this.knowledgePoints = re.response.map(point => ({
            label: point, // 将知识点赋值为 label
            selected: false, // 初始化选中状态
            score: 0 // 初始化分数
          }))
          _this.$message.success(re.message)
        } else {
          _this.$message.error(re.message)
        }
      }).catch(e => {
        this.formLoading = false
      })
    },
    submitForm () {
      // 收集知识点及对应的分数
      const selectedKnowledgePoints = this.knowledgePoints
        .filter(point => point.selected)
        .map(point => ({ label: point.label, score: point.score }))

      // 将表单数据和知识点传递给后端
      const paperData = {
        grade: this.form.grade,
        subject: this.form.subject,
        difficulty: this.form.difficulty,
        paperName: this.form.paperName,
        suggestedDuration: this.form.suggestedDuration,
        knowledgePoints: selectedKnowledgePoints
      }

      console.log('组卷数据:', paperData)

      let _this = this
      // 发送数据到后端
      composePaperAPI.composeThePaper(paperData).then(re => {
        console.log('进入composePaperAPI')
        console.log(re)
        console.log(re.message)
        if (re.code === 1) {
          _this.$message.success(re.response)
          _this.$router.push('/exam/paper/list')
        } else {
          _this.$message.error(re.message)
          this.formLoading = false
        }
      }).catch(e => {
        this.formLoading = false
        console.log('compose来到catch')
        console.error('Error in composePaperAPI:', e) // 打印错误信息
        this.$message.error('请求失败，请稍后重试。') // 提示用户
      })
    }
  }
}
</script>

<style scoped>
.knowledge-points-container {
  display: flex;
  flex-wrap: wrap;
}

.knowledge-point {
  display: inline-block;
  margin-right: 10px;
  margin-bottom: 10px;
}
</style>
