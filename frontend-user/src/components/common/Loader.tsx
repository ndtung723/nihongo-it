import { Loader2 } from 'lucide-react'
import { cn } from '@/lib/utils'

interface LoaderProps {
  size?: number
  className?: string
  label?: string
}

export function Loader({ size = 24, className, label }: LoaderProps) {
  return (
    <div
      className={cn('flex items-center justify-center gap-2 p-4', className)}
      role="status"
      aria-live="polite"
    >
      <Loader2 className="text-primary animate-spin" size={size} />
      {label && <span className="text-muted-foreground text-sm">{label}</span>}
    </div>
  )
}
