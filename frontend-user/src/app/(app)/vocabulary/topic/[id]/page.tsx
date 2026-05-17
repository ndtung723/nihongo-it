import Link from 'next/link'
import { ArrowLeft } from 'lucide-react'
import topicService from '@/services/topic.service'
import { Button } from '@/components/ui/button'
import { TopicVocabularyList } from './TopicVocabularyList'

interface Props {
  params: Promise<{ id: string }>
}

export default async function TopicDetailPage({ params }: Props) {
  const { id } = await params
  const topic = await topicService.getTopicById(id).catch(() => null)

  if (!topic) {
    return (
      <div className="space-y-4 py-10 text-center">
        <p className="text-muted-foreground">Không tìm thấy chủ đề.</p>
        <Button asChild variant="outline">
          <Link href="/vocabulary/learning">← Quay về danh mục</Link>
        </Button>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <Button asChild variant="ghost" size="sm" className="-ml-2">
        <Link href={`/vocabulary/category/${topic.categoryId}`}>
          <ArrowLeft className="mr-1 size-4" />
          {topic.categoryName ?? 'Danh mục'}
        </Link>
      </Button>

      <div>
        <h1 className="text-2xl font-bold">{topic.name}</h1>
        {topic.meaning && <p className="text-muted-foreground">{topic.meaning}</p>}
      </div>

      <TopicVocabularyList topicName={topic.name} />
    </div>
  )
}
