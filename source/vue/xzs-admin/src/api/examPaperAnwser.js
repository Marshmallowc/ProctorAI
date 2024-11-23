import { post, get } from '@/utils/request'

export default {
  // 分页查询考试成绩
  page: query => post('/api/admin/examPaperAnswer/page', query),

  // 导出成绩
  exportGrades: query => post('/api/admin/examPaperAnswer/export', query, { responseType: 'blob' }),

  // 获取试卷名称列表
  getPaperByClass: value => get(`/api/admin/examPaperAnswer/getPaperByClass/${value}`),

  // 获取专业号列表
  getMajors: () => get('/api/admin/examPaperAnswer/getMajors'),

  // 根据专业号获取班级列表
  getClassesByMajor: majorId => get(`/api/admin/examPaperAnswer/getClassesByMajor/${majorId}`),
  pageList: query => post('/api/admin/examPaperAnswer/pageList', query),
  answerSubmit: form => post('/api/admin/examPaperAnswer/answerSubmit', form),
  read: id => post('/api/admin/examPaperAnswer/read/' + id),
  edit: form => post('/api/admin/examPaperAnswer/edit', form)

}
