'use client'

import { Component, type ErrorInfo, type ReactNode } from 'react'

interface Props {
  children: ReactNode
  fallback?: ReactNode
}

interface State {
  hasError: boolean
  error: Error | null
}

export class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false, error: null }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error }
  }

  componentDidCatch(error: Error, info: ErrorInfo): void {
    console.error('ErrorBoundary caught:', error, info)
  }

  reset = () => this.setState({ hasError: false, error: null })

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) return this.props.fallback
      return (
        <div className="flex min-h-[50vh] flex-col items-center justify-center gap-4 p-6 text-center">
          <h2 className="text-xl font-semibold">Đã xảy ra lỗi</h2>
          <p className="text-muted-foreground text-sm">
            {this.state.error?.message ?? 'Vui lòng thử lại sau.'}
          </p>
          <button
            type="button"
            onClick={this.reset}
            className="bg-primary text-primary-foreground hover:bg-primary/90 rounded-md px-4 py-2 text-sm"
          >
            Thử lại
          </button>
        </div>
      )
    }
    return this.props.children
  }
}
