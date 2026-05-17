import { useMemo, useState } from 'react'

export function usePagination(defaultPageSize = 10) {
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(defaultPageSize)
  const [total, setTotal] = useState(0)

  const totalPages = useMemo(() => Math.ceil(total / pageSize), [total, pageSize])
  const hasNextPage = page < totalPages
  const hasPrevPage = page > 1

  function reset() {
    setPage(1)
  }

  return {
    page,
    pageSize,
    total,
    totalPages,
    hasNextPage,
    hasPrevPage,
    setPage,
    setPageSize,
    setTotal,
    reset,
  }
}
