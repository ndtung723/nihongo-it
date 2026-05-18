import Link from 'next/link'
import { ChevronRight, BookOpen } from 'lucide-react'
import categoryService from '@/services/category.service'
import { Card, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'

export const metadata = {
  title: 'Học theo chủ đề | Nihongo IT',
}

// Server Component: fetch categories at request time. No 'use client' here.
export default async function LearningPage() {
  let categories: Awaited<ReturnType<typeof categoryService.getAllCategories>> = []
  try {
    categories = await categoryService.getAllCategories()
  } catch {
    // Will render empty state below
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Học theo chủ đề</h1>
        <p className="text-muted-foreground text-sm">
          Khám phá từ vựng IT được sắp xếp theo danh mục và chủ đề.
        </p>
      </div>

      {categories.length === 0 ? (
        <p className="text-muted-foreground py-10 text-center">Chưa có danh mục nào.</p>
      ) : (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {categories.map((cat) => (
            <Link key={cat.categoryId} href={`/vocabulary/category/${cat.categoryId}`}>
              <Card className="hover:border-primary/50 transition-colors">
                <CardContent className="p-4">
                  <div className="flex items-center justify-between gap-2">
                    <div className="min-w-0 flex-1">
                      <h3 className="flex items-center gap-2 truncate text-lg font-semibold">
                        <BookOpen className="text-primary size-4 shrink-0" />
                        {cat.name}
                      </h3>
                      {cat.meaning && (
                        <p className="text-muted-foreground mt-1 truncate text-sm">
                          {cat.meaning}
                        </p>
                      )}
                    </div>
                    <ChevronRight className="text-muted-foreground size-4 shrink-0" />
                  </div>
                  {typeof cat.topicCount === 'number' && (
                    <Badge variant="secondary" className="mt-3">
                      {cat.topicCount} chủ đề
                    </Badge>
                  )}
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}
