import Link from 'next/link'
import { Button } from '@/components/ui/button'

export default function HomePage() {
  return (
    <main className="bg-muted flex min-h-screen flex-col items-center justify-center p-8 text-center">
      <h1 className="text-primary mb-3 text-4xl font-bold">Nihongo IT</h1>
      <p className="text-muted-foreground mb-8 max-w-md">
        Học tiếng Nhật chuyên ngành IT — từ vựng, hội thoại, flashcards với spaced repetition và
        phân tích phát âm bằng AI.
      </p>
      <div className="flex gap-3">
        <Button asChild size="lg">
          <Link href="/login">Đăng nhập</Link>
        </Button>
        <Button asChild size="lg" variant="outline">
          <Link href="/register">Đăng ký</Link>
        </Button>
      </div>
    </main>
  )
}
