import { UserDetailClient } from './UserDetailClient'

interface Props {
  params: Promise<{ id: string }>
}

export default async function UserDetailPage({ params }: Props) {
  const { id } = await params
  return <UserDetailClient userId={id} />
}
