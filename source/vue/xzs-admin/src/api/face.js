import { post } from '@/utils/request'

export default {
  getFaceList: query => post('/api/admin/user/getFaceList', query)
}
