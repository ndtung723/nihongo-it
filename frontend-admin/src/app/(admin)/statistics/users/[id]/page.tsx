import { UserStatsDetailClient } from './UserStatsDetailClient'

interface Props {
  params: Promise<{ id: string }>
}

export default async function UserStatsDetailPage({ params }: Props) {
  const { id } = await params
  return <UserStatsDetailClient userId={id} />
}
