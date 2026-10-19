import { useEffect, useState } from 'react'
import {
  createColorFavorite,
  deleteColorFavorite,
  listColorFavorites,
  resetColorFavorites,
  type ColorFavorite,
} from '../../../shared/api/client'

/** Синхронно с colors.MaxFavorites / defaultPalette. */
export const MAX_COLOR_FAVORITES = 8

/** Стандартная палитра (как на бэке), пока в БД пусто. */
export const DEFAULT_COLOR_PALETTE: Array<{
  hue: number
  saturation: number
  brightness: number
}> = [
  { hue: 28, saturation: 85, brightness: 100 },
  { hue: 48, saturation: 70, brightness: 100 },
  { hue: 0, saturation: 95, brightness: 100 },
  { hue: 330, saturation: 75, brightness: 100 },
  { hue: 280, saturation: 80, brightness: 100 },
  { hue: 210, saturation: 85, brightness: 100 },
  { hue: 175, saturation: 70, brightness: 100 },
  { hue: 130, saturation: 70, brightness: 100 },
]

export function useColorFavorites() {
  const [items, setItems] = useState<ColorFavorite[]>([])
  const [busy, setBusy] = useState(false)
  const [ready, setReady] = useState(false)

  async function reload() {
    try {
      setItems(await listColorFavorites())
    } catch {
      setItems([])
    } finally {
      setReady(true)
    }
  }

  useEffect(() => {
    void reload()
  }, [])

  /** Показ: сохранённые или стандартные (ещё не в БД). */
  const display =
    items.length > 0
      ? items
      : ready
        ? DEFAULT_COLOR_PALETTE.map((c, i) => ({
            id: -(i + 1),
            hue: c.hue,
            saturation: c.saturation,
            brightness: c.brightness,
            sortOrder: i,
            createdAt: '',
          }))
        : []

  const persisted = items.length > 0

  async function add(color: { hue: number; saturation: number; brightness: number }) {
    if (busy) return
    setBusy(true)
    try {
      await createColorFavorite(color)
      await reload()
    } catch {
      /* ignore */
    } finally {
      setBusy(false)
    }
  }

  async function remove(id: number) {
    if (busy || id <= 0) return
    setBusy(true)
    try {
      await deleteColorFavorite(id)
      await reload()
    } catch {
      /* ignore */
    } finally {
      setBusy(false)
    }
  }

  async function reset() {
    if (busy) return
    setBusy(true)
    try {
      setItems(await resetColorFavorites())
    } catch {
      /* ignore */
    } finally {
      setBusy(false)
    }
  }

  return { items: display, persisted, busy, reload, add, remove, reset }
}
