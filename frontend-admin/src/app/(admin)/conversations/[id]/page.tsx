import Link from 'next/link'
import { notFound } from 'next/navigation'
import { ArrowLeft, MessageSquare, SquarePen } from 'lucide-react'
import conversationService from '@/services/conversation.service'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

interface Props {
  params: Promise<{ id: string }>
}

export default async function ConversationDetailPage({ params }: Props) {
  const { id } = await params
  const conversation = await conversationService.getById(id).catch(() => null)
  if (!conversation) notFound()

  const lines = (conversation.lines ?? []).slice().sort((a, b) => a.orderIndex - b.orderIndex)

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div className="flex items-start justify-between gap-4">
        <Button asChild variant="ghost" size="sm" className="-ml-2">
          <Link href="/conversations">
            <ArrowLeft className="mr-1 size-4" />
            Danh sách
          </Link>
        </Button>
        <Button asChild>
          <Link href={`/conversations/${id}/edit`}>
            <SquarePen className="mr-1 size-4" />
            Sửa câu thoại
          </Link>
        </Button>
      </div>

      <div>
        <h1 className="text-2xl font-bold">{conversation.title}</h1>
        {conversation.description && (
          <p className="text-muted-foreground mt-1">{conversation.description}</p>
        )}
        <div className="mt-3 flex flex-wrap gap-2">
          {conversation.jlptLevel && <Badge variant="secondary">{conversation.jlptLevel}</Badge>}
          {typeof conversation.unit === 'number' && (
            <Badge variant="outline">Bài {conversation.unit}</Badge>
          )}
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <MessageSquare className="size-4" />
            Câu thoại ({lines.length})
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          {lines.length === 0 ? (
            <p className="text-muted-foreground py-6 text-center text-sm">
              Hội thoại này chưa có câu nào.
            </p>
          ) : (
            lines.map((line) => (
              <div key={line.lineId} className="border-b py-3 last:border-0">
                <div className="mb-1 flex items-center gap-2">
                  <Badge variant="outline" className="capitalize">
                    {line.speaker}
                  </Badge>
                  <span className="text-muted-foreground text-xs tabular-nums">
                    #{line.orderIndex + 1}
                  </span>
                </div>
                <p className="text-lg leading-relaxed">{line.japaneseText}</p>
                {line.vietnameseTranslation && (
                  <p className="text-muted-foreground mt-1 text-sm italic">
                    {line.vietnameseTranslation}
                  </p>
                )}
                {line.notes && (
                  <p className="bg-muted/40 mt-2 rounded p-2 text-xs">
                    <span className="font-medium">Ghi chú:</span> {line.notes}
                  </p>
                )}
                {line.importantVocab && (
                  <p className="text-muted-foreground mt-1 text-xs">
                    <span className="font-medium">Từ quan trọng:</span> {line.importantVocab}
                  </p>
                )}
              </div>
            ))
          )}
        </CardContent>
      </Card>
    </div>
  )
}
