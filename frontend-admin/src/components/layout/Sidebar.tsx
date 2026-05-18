'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import {
  BarChart3,
  BookOpen,
  FolderTree,
  LayoutDashboard,
  MessageSquare,
  Tag,
  Users,
} from 'lucide-react'
import { cn } from '@/lib/utils'

const NAV_ITEMS = [
  { href: '/', label: 'Dashboard', Icon: LayoutDashboard, exact: true },
  { href: '/users', label: 'Người dùng', Icon: Users },
  { href: '/categories', label: 'Danh mục', Icon: FolderTree },
  { href: '/topics', label: 'Chủ đề', Icon: Tag },
  { href: '/vocabulary', label: 'Từ vựng', Icon: BookOpen },
  { href: '/conversations', label: 'Hội thoại', Icon: MessageSquare },
  { href: '/statistics', label: 'Thống kê', Icon: BarChart3 },
]

function isActive(pathname: string, href: string, exact?: boolean): boolean {
  if (exact) return pathname === href
  return pathname === href || pathname.startsWith(`${href}/`)
}

export function Sidebar() {
  const pathname = usePathname()
  return (
    <aside className="bg-card hidden w-60 shrink-0 border-r md:flex md:flex-col">
      <div className="border-b p-4">
        <Link href="/" className="text-primary text-lg font-semibold">
          Nihongo IT
        </Link>
        <p className="text-muted-foreground text-xs uppercase tracking-wide">Quản trị</p>
      </div>
      <nav className="flex-1 space-y-1 p-3">
        {NAV_ITEMS.map(({ href, label, Icon, exact }) => {
          const active = isActive(pathname, href, exact)
          return (
            <Link
              key={href}
              href={href}
              className={cn(
                'flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium transition-colors',
                active
                  ? 'bg-primary text-primary-foreground'
                  : 'text-muted-foreground hover:bg-muted hover:text-foreground',
              )}
            >
              <Icon className="size-4" />
              {label}
            </Link>
          )
        })}
      </nav>
    </aside>
  )
}

export function MobileNavLinks({ onNavigate }: { onNavigate?: () => void }) {
  const pathname = usePathname()
  return (
    <nav className="space-y-1 p-3">
      {NAV_ITEMS.map(({ href, label, Icon, exact }) => {
        const active = isActive(pathname, href, exact)
        return (
          <Link
            key={href}
            href={href}
            onClick={onNavigate}
            className={cn(
              'flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium transition-colors',
              active
                ? 'bg-primary text-primary-foreground'
                : 'text-muted-foreground hover:bg-muted hover:text-foreground',
            )}
          >
            <Icon className="size-4" />
            {label}
          </Link>
        )
      })}
    </nav>
  )
}
