import { useEffect } from 'react'

/** Обновляет document.title для вкладки браузера. */
export function useDocumentTitle(title: string) {
  useEffect(() => {
    const prev = document.title
    document.title = title
    return () => {
      document.title = prev
    }
  }, [title])
}
