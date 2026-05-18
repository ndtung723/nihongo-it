import { useCallback, useEffect, useState } from 'react'
import { extractApiError } from '@/types/common.types'

/**
 * Fetch data on mount and on `fetcher` reference change.
 *
 * The caller is responsible for memoizing `fetcher` (typically with `useCallback`)
 * so it only changes when the relevant inputs change. Inline arrow functions will
 * re-fetch on every render.
 *
 * Example:
 *   const fetcher = useCallback(() => api.get(`/items/${id}`), [id])
 *   const { data, loading, error, refresh } = useAsyncData(fetcher)
 */
export function useAsyncData<T>(fetcher: () => Promise<T>) {
  const [data, setData] = useState<T | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const run = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setData(await fetcher())
    } catch (e) {
      setError(extractApiError(e, 'Đã xảy ra lỗi'))
    } finally {
      setLoading(false)
    }
  }, [fetcher])

  useEffect(() => {
    // Intentional fetch-on-mount: the React Compiler's set-state-in-effect rule
    // flags this pattern, but invoking the fetcher here is the whole point of the hook.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void run()
  }, [run])

  return { data, loading, error, refresh: run }
}
