import { post } from '@/utils/request'

export default {
  composeThePaper: query => post('/api/admin/exam/paper/compose', query),
  getKnowledge: query => post('/api/admin/exam/paper/getKnowledge', query)
}
