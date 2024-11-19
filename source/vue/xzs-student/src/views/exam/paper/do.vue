<template>
  <div>
    <el-row  class="do-exam-title">
      <el-col :span="24">
        <span :key="item.itemOrder"  v-for="item in answer.answerItems">
             <el-tag :type="questionCompleted(item.completed)" class="do-exam-title-tag" @click="goAnchor('#question-'+item.itemOrder)">{{item.itemOrder}}</el-tag>
        </span>
        <span class="do-exam-time">
          <label>剩余时间：</label>
          <label>{{formatSeconds(remainTime)}}</label>
        </span>
      </el-col>
    </el-row>
    <el-row  class="do-exam-title-hidden">
      <el-col :span="24">
        <span :key="item.itemOrder"  v-for="item in answer.answerItems">
             <el-tag  class="do-exam-title-tag" >{{item.itemOrder}}</el-tag>
        </span>
        <span class="do-exam-time">
          <label>剩余时间：</label>
        </span>
      </el-col>
    </el-row>
    <el-container  class="app-item-contain">
      <el-header class="align-center">
        <h1>{{form.name}}</h1>
        <div>
          <span class="question-title-padding">试卷总分：{{form.score}}</span>
          <span class="question-title-padding">考试时间：{{form.suggestTime}}分钟</span>
        </div>
      </el-header>
      <el-main>
        <el-form :model="form" ref="form" v-loading="formLoading" label-width="100px">
          <el-row :key="index"  v-for="(titleItem,index) in form.titleItems">
            <h3>{{titleItem.name}}</h3>
            <el-card class="exampaper-item-box" v-if="titleItem.questionItems.length!==0">
              <el-form-item :key="questionItem.itemOrder" :label="questionItem.itemOrder+'.'"
                            v-for="questionItem in titleItem.questionItems"
                            class="exam-question-item" label-width="50px" :id="'question-'+ questionItem.itemOrder">
                <QuestionEdit :qType="questionItem.questionType" :question="questionItem"
                              :answer="answer.answerItems[questionItem.itemOrder-1]"/>
              </el-form-item>
            </el-card>
          </el-row>
          <el-row class="do-align-center">
            <el-button type="primary" @click="submitForm">提交</el-button>
            <el-button>取消</el-button>
          </el-row>
        </el-form>
      </el-main>
    </el-container>
  </div>
</template>

<script>
import { mapState, mapGetters } from 'vuex'
import { formatSeconds } from '@/utils'
import QuestionEdit from '../components/QuestionEdit'
import examPaperApi from '@/api/examPaper'
import examPaperAnswerApi from '@/api/examPaperAnswer'

export default {
  components: { QuestionEdit },
  data () {
    return {
      form: {},
      formLoading: false,
      answer: {
        questionId: null,
        doTime: 0,
        answerItems: []
      },
      timer: null,
      remainTime: 0,
      hasFocus: true
    }
  },
  created () {
    let id = this.$route.query.id
    let _this = this
    if (id && parseInt(id) !== 0) {
      _this.formLoading = true
      examPaperApi.select(id).then(re => {
        _this.form = re.response
        _this.remainTime = re.response.suggestTime * 60
        _this.initAnswer()
        _this.timeReduce()
        _this.formLoading = false
      })
    }
  },
  mounted () {
    this.addCheatPrevention()
    // 监听页面失去焦点事件
    window.addEventListener('blur', this.handleBlur)
    window.addEventListener('focus', this.handleFocus)
  },
  beforeDestroy () {
    window.clearInterval(this.timer)
    this.removeCheatPrevention()
    window.removeEventListener('blur', this.handleBlur)
    window.removeEventListener('focus', this.handleFocus)
  },
  methods: {
    formatSeconds (theTime) {
      return formatSeconds(theTime)
    },
    timeReduce () {
      let _this = this
      this.timer = setInterval(function () {
        if (_this.remainTime <= 0) {
          _this.submitForm()
        } else {
          ++_this.answer.doTime
          --_this.remainTime
        }
      }, 1000)
    },
    questionCompleted (completed) {
      return this.enumFormat(this.doCompletedTag, completed)
    },
    goAnchor (selector) {
      this.$el.querySelector(selector).scrollIntoView({ behavior: 'instant', block: 'center', inline: 'nearest' })
    },
    initAnswer () {
      this.answer.id = this.form.id
      let titleItemArray = this.form.titleItems
      for (let tIndex in titleItemArray) {
        let questionArray = titleItemArray[tIndex].questionItems
        for (let qIndex in questionArray) {
          let question = questionArray[qIndex]
          this.answer.answerItems.push({ questionId: question.id, content: null, contentArray: [], completed: false, itemOrder: question.itemOrder })
        }
      }
    },
    submitForm () {
      let _this = this
      window.clearInterval(_this.timer)
      _this.formLoading = true
      examPaperAnswerApi.answerSubmit(this.answer).then(re => {
        if (re.code === 1) {
          _this.$alert('试卷得分：' + re.response + '分', '考试结果', {
            confirmButtonText: '返回考试记录',
            callback: action => {
              _this.$router.push('/record/index')
            }
          })
        } else {
          _this.$message.error(re.message)
        }
        _this.formLoading = false
      }).catch(e => {
        _this.formLoading = false
      })
    },
    // 防作弊：监听按键事件
    addCheatPrevention () {
      window.addEventListener('keydown', this.preventCheating)
    },
    removeCheatPrevention () {
      window.removeEventListener('keydown', this.preventCheating)
    },
    preventCheating (event) {
      if ((event.altKey && event.key === 'Tab')) {
        event.preventDefault() // 阻止默认行为，防止切屏
        this.$message.warning('检测到切屏行为，请保持专注考试！')
      }
    },
    // 处理失去焦点
    handleBlur () {
      this.hasFocus = false
      this.$message.warning('检测到页面切出，请保持专注考试！')
    },
    // 处理重新获得焦点
    handleFocus () {
      this.hasFocus = true
    }
  },
  computed: {
    ...mapGetters('enumItem', ['enumFormat']),
    ...mapState('enumItem', {
      doCompletedTag: state => state.exam.question.answer.doCompletedTag
    })
  }
}
</script>

<style lang="scss" scoped>
.align-center {
  text-align: center
}

.exam-question-item {
  padding: 10px;

  .el-form-item__label {
    font-size: 15px !important;
  }
}

.question-title-padding {
  padding-left: 25px;
  padding-right: 25px;
}
</style>
