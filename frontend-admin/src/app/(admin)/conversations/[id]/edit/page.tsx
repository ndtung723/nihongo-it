import { LinesEditor } from './LinesEditor'

interface Props {
  params: Promise<{ id: string }>
}

export default async function ConversationEditPage({ params }: Props) {
  const { id } = await params
  return <LinesEditor conversationId={id} />
}
