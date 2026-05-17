import { redirect } from 'next/navigation'

// User-facing statistics is consolidated into /flashcards/stats (same data, same
// shape). This route exists for compatibility with the Vue app's link structure
// and immediately redirects.
export default function StatisticsPage() {
  redirect('/flashcards/stats')
}
