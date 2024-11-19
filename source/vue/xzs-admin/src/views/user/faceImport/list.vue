<template>
  <div class="app-container">
    <!-- 查询及上传表单 -->
    <el-form :model="queryParam" ref="queryForm" :inline="true">
      <!-- 查询用户 -->
      <!-- <el-form-item label="用户名：">
        <el-input v-model="queryParam.userName"></el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="submitForm">查询</el-button> -->
        <!-- 添加用户 -->
        <!-- <router-link :to="{path:'/user/faceImport/edit'}" class="link-left">
          <el-button type="primary">添加</el-button>
        </router-link>
      </el-form-item> -->
      <!-- <el-upload
      class="upload-demo"
      drag
      multiple
      action="/api/admin/user/downloadFace"
      :on-success="handleSuccess"
      :on-error="handleError"
      :file-list="fileList"
      :before-upload="beforeUpload"
      :show-file-list="true"
    >
      <i class="el-icon-upload"></i>
      <div class="el-upload__text">将文件拖到此处，或<span class="text-primary">点击上传</span></div>
      <div class="el-upload__tip" slot="tip">只支持图片上传，且不超过2MB</div>
    </el-upload>
    <el-button type="primary" @click="submitFiles" :disabled="fileList.length === 0">提交</el-button> -->
    <h2>面部编码</h2>
    </el-form>

    <!-- 数据表格展示 -->
    <el-table v-loading="listLoading" :data="tableData" border fit highlight-current-row style="width: 100%">
      <el-table-column prop="id" label="Id" />
      <el-table-column prop="stuNo" label="学号"/>
      <el-table-column prop="faceName" label="真实姓名" />
      <el-table-column prop="encoding" label="面部编码" />
    </el-table>
  </div>
</template>

<script>
import faceApi from '@/api/face'

export default {
  data () {
    return {
      queryParam: {
        userName: '', // 查询参数：用户名
        pageIndex: 1, // 当前页
        pageSize: 10 // 每页显示数量
      },
      fileList: [], // 文件列表
      tableData: [], // 表格数据
      total: 0, // 总记录数
      listLoading: false // 加载状态
    }
  },
  methods: {
    // 查询接口，获取表格数据
    search () {
      this.listLoading = true
      faceApi.getFaceList().then(data => {
        console.log('开始获取人脸列表')
        console.log(data)
        this.tableData = data
        console.log([this.tableData])
      })
        .catch(error => {
          console.error('获取人脸列表失败: ', error)
        })
        .finally(() => {
          this.listLoading = false // 无论成功与否，最后都要关闭加载状态
        })
    },
    submitFiles () {
      faceApi.uploadFaceList().then(data => {
        console.log('开始上传图片文件')
      })
    },
    handleSuccess (response, file) {
      console.log('上传成功:', file.name, response)
      // 可以在这里处理成功后的逻辑
    },
    handleError (err, file) {
      console.error('上传失败:', file.name, err)
      // 可以在这里处理失败后的逻辑
    },
    beforeUpload (file) {
      const isImage = file.type.startsWith('image/')
      if (!isImage) {
        this.$message.error('只能上传图片文件！')
      }
      return isImage // 返回 true 继续上传，false 取消上传
    }
  },
  mounted () {
    this.search() // 初始加载表格数据
    console.log('完成获取人脸列表')
  }
}
</script>
