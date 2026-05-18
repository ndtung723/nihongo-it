import { TranslationTool } from './TranslationTool'

export const metadata = {
  title: 'Dịch thuật | Nihongo IT',
}

export default function TranslationPage() {
  return (
    <main className="container mx-auto px-4 py-6">
      <TranslationTool />
    </main>
  )
}
