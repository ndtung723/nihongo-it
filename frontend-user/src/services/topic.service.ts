import api from '@/lib/api'
import type { Topic } from '@/types/learning.types'

const topicService = {
  getAllTopics: (): Promise<Topic[]> => api.get('/api/v1/learning/topics').then((r) => r.data),

  getTopicById: (id: string): Promise<Topic> =>
    api.get(`/api/v1/learning/topics/${id}`).then((r) => r.data),

  getTopicsByCategoryId: (categoryId: string): Promise<Topic[]> =>
    api.get(`/api/v1/learning/topics/category/${categoryId}`).then((r) => r.data),
}

export default topicService
