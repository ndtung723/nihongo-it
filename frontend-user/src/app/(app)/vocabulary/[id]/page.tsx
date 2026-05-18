import { VocabularyDetailClient } from './VocabularyDetailClient'

// Next.js 16: params is async — must await before reading
interface Props {
  params: Promise<{ id: string }>
}

export default async function VocabularyDetailPage({ params }: Props) {
  const { id } = await params
  return <VocabularyDetailClient vocabId={id} />
}
