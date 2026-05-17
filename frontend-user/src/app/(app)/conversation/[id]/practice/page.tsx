import { PracticeSession } from './PracticeSession'

interface Props {
  params: Promise<{ id: string }>
}

export default async function ConversationPracticePage({ params }: Props) {
  const { id } = await params
  return <PracticeSession conversationId={id} />
}
