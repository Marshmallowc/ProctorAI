<template>
  <div class="app-container">
    <el-form :model="queryParam" ref="queryForm" :inline="true">
      <!-- 选择专业 -->
      <el-form-item label="专业：" >
        <el-select v-model="queryParam.majorId" @change="onMajorChange" clearable>
          <el-option v-for="item in majors" :key="item" :value="item" :label="item"></el-option>
        </el-select>
      </el-form-item>

      <!-- 选择班级 -->
      <el-form-item label="班级：">
        <el-select v-model="queryParam.classofParam" clearable @change="handleClassChange">
          <el-option v-for="item in classes" :key="item" :value="item" :label="item"></el-option>
        </el-select>
      </el-form-item>

       <!-- 选择班级 -->
       <el-form-item label="试卷名称：">
        <el-select v-model="queryParam.papersname" clearable @change="handlePaperChange">
          <el-option v-for="item in paperNames" :key="item" :value="item" :label="item"></el-option>
        </el-select>
      </el-form-item>

      <el-form-item>
        <el-button type="success" @click="exportData">导出成绩</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="listLoading" :data="tableData" border fit highlight-current-row style="width: 100%">
      <el-table-column prop="id" label="id"/>
      <el-table-column prop="paperName" label="试卷名称"/>
      <el-table-column prop="school" label="学校"/>
      <el-table-column prop="realName" label="姓名"/>
      <el-table-column prop="stuNo" label="学号"/>
      <el-table-column  label="得分" width="100px" >
        <template slot-scope="{row}">
          {{row.userScore}} / {{row.paperScore}}
        </template>
      </el-table-column>
      <el-table-column  label="题目对错" width="80px" >
        <template slot-scope="{row}">
          {{row.questionCorrect}} / {{row.questionCount}}
        </template>
      </el-table-column>
      <el-table-column prop="doTime" label="耗时" width="100px"/>
      <el-table-column prop="createTime" label="提交时间" width="160px"/>
    </el-table>
    <!--
    <pagination v-show="total>0" :total="total" :page.sync="queryParam.pageIndex" :limit.sync="queryParam.pageSize"
                @pagination="search"/>
    -->
  </div>
</template>

<script>

import { mapGetters, mapState, mapActions } from 'vuex'
// import Pagination from '@/components/Pagination'
import examPaperAnswerApi from '@/api/examPaperAnwser'

export default {
  // components: { Pagination },
  data () {
    return {
      queryParam: {
        subjectId: null,
        majorId: null,
        pageIndex: 1,
        pageSize: 100,
        classofParam: null,
        papersname: null,
        barChartUrl: null,
        pieChartUrl: null,
        reportUrl: null
      },
      listLoading: false,
      tableData: [],
      total: 0,
      majors: [],
      classes: [],
      paperNames: []
    }
  },
  created () {
    this.initMajor()
    this.initSubject()
    this.search()
  },
  methods: {
    search () {
      this.listLoading = true
      examPaperAnswerApi.page(this.queryParam).then(data => {
        const re = data.response
        console.log('完整的响应数据:', data)
        console.log('处理后的数据:', re)
        console.log('这里是之前的queryParam中的数据', this.queryParam)
        console.log('subjecs', this.subjects)
        this.tableData = re.list
        this.total = re.total
        this.queryParam.pageIndex = re.pageNum
        this.listLoading = false
      })
    },
    submitForm () {
      this.queryParam.pageIndex = 1
      this.search()
    },
    ...mapActions('exam', { initSubject: 'initSubject' }),
    // 初始化专业号、班级号、试卷名称列表（新增）
    initMajor () {
      // 调用API获取专业号
      examPaperAnswerApi.getMajors().then(response => {
        console.log('获取专业号')
        console.log(response)
        this.majors = response.response.majorCodes
        this.classes = response.response.classNos
        console.log(this.majors)
        console.log('这里是majors', this.classes)
        console.log('这里是paperNames', this.paperNames)
      })
        .catch(error => {
          console.error('API 调用出错:', error)
        })
    },
    // 当选择专业号时，获取该专业下的班级（新增）
    onMajorChange (majorId) {
      this.queryParam.majorId = majorId
      this.getClassesByMajor(majorId)
    },
    handleClassChange (value) {
      this.getPaperByClass(value)
      console.log('选择的班级:', value)
      this.queryParam.classofParam = value
      console.log('这里是queryParam.class', this.queryParam.classofParam)
    },
    handlePaperChange (value) {
      console.log('选择的试卷名称', value)
      this.queryParam.papersname = value
      console.log('这里是papersname', this.queryParam.papersname)
    },
    // 根据专业号获取班级列表（新增）
    getClassesByMajor (majorId) {
      examPaperAnswerApi.getClassesByMajor(majorId).then(response => {
        console.log('这里是根据专业号获取班级列表中的response', response)
        this.classes = response.response
      })
    },
    getPaperByClass (value) {
      examPaperAnswerApi.getPaperByClass(value).then(response => {
        console.log('这里是根据班级获取试卷列表中的response', response)
        this.paperNames = response.response
      })
    },
    // 导出成绩，包含图表和Excel报表（新增）
    exportData () {
      if (!this.queryParam.majorId) {
        this.$message.error('请先选择专业号')
        return
      }

      // 调用后端接口，生成图表和Excel报表
      examPaperAnswerApi.exportGrades(this.queryParam).then(response => {
        console.log('这里是生成报表之后的前端代码', response)
        this.barChartUrl = decodeURIComponent(response.barChartUrl)
        this.pieChartUrl = decodeURIComponent(response.pieChartUrl)
        this.reportUrl = decodeURIComponent(response.reportUrl)
        console.log('这里是barChatUrl', this.barChartUrl)
        console.log('这里是pieChatUrl', this.pieChartUrl)
        console.log('这里是reportUrl', this.reportUrl)
        // 在页面上显示文件路径
        this.$message.success({
          message: `成绩报表存储在: ${this.reportUrl}<br>成绩饼状图存储在: ${this.pieChartUrl}<br>成绩直方图存储在: ${this.barChartUrl}`,
          dangerouslyUseHTMLString: true
        })
        // 下载Excel报表
        if (this.reportUrl) {
          this.downloadFile(encodeURI(this.reportUrl))
        } else {
          console.warn('报表URL为空，无法下载报表')
        }
        // 打开图标链接展示或下载
        if (this.pieChartUrl) {
          window.open(this.pieChartUrl, '_blank')
        } else {
          console.warn('饼状图URL为空')
        }
        if (this.barChartUrl) {
          window.open(encodeURI(this.barChartUrl), '_blank')
        } else {
          console.warn('直方图URL为空')
        }
      }).catch(error => {
        console.error(error)
        this.$message.error('导出失败')
      })
    },
    // 下载文件
    downloadFile (url) {
      const link = document.createElement('a')
      link.href = url
      link.download = '成绩报表.xlsx'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
    }
  },
  computed: {
    ...mapGetters('enumItem', ['enumFormat']),
    ...mapGetters('exam', ['subjectEnumFormat']),
    ...mapState('exam', { subjects: state => state.subjects })
  }
}
</script>
