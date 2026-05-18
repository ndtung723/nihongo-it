'use client'

import { ChevronLeft, ChevronRight } from 'lucide-react'
import { Button } from '@/components/ui/button'

interface PaginationProps {
  page: number // 0-based
  totalPages: number
  onChange: (page: number) => void
}

export function Pagination({ page, totalPages, onChange }: PaginationProps) {
  if (totalPages <= 1) return null
  const display = page + 1
  return (
    <div className="flex items-center justify-center gap-2">
      <Button
        size="sm"
        variant="outline"
        onClick={() => onChange(page - 1)}
        disabled={page <= 0}
        aria-label="Trang trước"
      >
        <ChevronLeft className="size-4" />
      </Button>
      <span className="text-muted-foreground text-sm tabular-nums">
        {display} / {totalPages}
      </span>
      <Button
        size="sm"
        variant="outline"
        onClick={() => onChange(page + 1)}
        disabled={page >= totalPages - 1}
        aria-label="Trang sau"
      >
        <ChevronRight className="size-4" />
      </Button>
    </div>
  )
}
