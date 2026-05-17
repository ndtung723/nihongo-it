import Link from 'next/link'
import { ArrowLeft, ChevronRight } from 'lucide-react'
import categoryService from '@/services/category.service'
import topicService from '@/services/topic.service'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'

interface Props {
  params: Promise<{ id: string }>
}

export default async function CategoryDetailPage({ params }: Props) {
  const { id } = await params

  const [category, topics] = await Promise.all([
    categoryService.getCategoryById(id).catch(() => null),
    topicService.getTopicsByCategoryId(id).catch(() => []),
  ])

  if (!category) {
    return (
      <div className="space-y-4 py-10 text-center">
        <p className="text-muted-foreground">Không tìm thấy danh mục.</p>
        <Button asChild variant="outline">
          <Link href="/vocabulary/learning">← Quay về danh mục</Link>
        </Button>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <Button asChild variant="ghost" size="sm" className="-ml-2">
        <Link href="/vocabulary/learning">
          <ArrowLeft className="mr-1 size-4" />
          Danh mục
        </Link>
      </Button>

      <div>
        <h1 className="text-2xl font-bold">{category.name}</h1>
        {category.meaning && <p className="text-muted-foreground">{category.meaning}</p>}
      </div>

      <section>
        <h2 className="mb-3 text-lg font-semibold">Các chủ đề ({topics.length})</h2>
        {topics.length === 0 ? (
          <p className="text-muted-foreground py-6 text-center">Chưa có chủ đề nào.</p>
        ) : (
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {topics.map((topic) => (
              <Link key={topic.topicId} href={`/vocabulary/topic/${topic.topicId}`}>
                <Card className="hover:border-primary/50 transition-colors">
                  <CardContent className="p-4">
                    <div className="flex items-center justify-between gap-2">
                      <div className="min-w-0 flex-1">
                        <h3 className="truncate font-semibold">{topic.name}</h3>
                        {topic.meaning && (
                          <p className="text-muted-foreground mt-1 truncate text-sm">
                            {topic.meaning}
                          </p>
                        )}
                      </div>
                      <ChevronRight className="text-muted-foreground size-4 shrink-0" />
                    </div>
                    {typeof topic.vocabularyCount === 'number' && (
                      <Badge variant="secondary" className="mt-3">
                        {topic.vocabularyCount} từ
                      </Badge>
                    )}
                  </CardContent>
                </Card>
              </Link>
            ))}
          </div>
        )}
      </section>
    </div>
  )
}
