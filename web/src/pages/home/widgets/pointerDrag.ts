import { useEffect, useRef, useState, type PointerEvent as ReactPointerEvent } from 'react'
import { stopBubble } from './widgetChrome'

export type DragAxis = 'x' | 'xy'

/** Pointer-drag: onMove на каждом кадре, onEnd при отпускании. */
export function usePointerDrag(
  onMove: (clientX: number, clientY: number, rect: DOMRect) => void,
  opts: { axis?: DragAxis; onEnd?: () => void } = {},
) {
  const { axis = 'x', onEnd } = opts
  const elRef = useRef<HTMLDivElement | null>(null)
  const unbind = useRef<(() => void) | null>(null)
  const onEndRef = useRef(onEnd)
  onEndRef.current = onEnd
  const [dragging, setDragging] = useState(false)

  useEffect(() => () => unbind.current?.(), [])

  function onPointerDown(e: ReactPointerEvent) {
    e.preventDefault()
    stopBubble(e)
    const el = elRef.current
    if (!el) return
    unbind.current?.()
    el.setPointerCapture?.(e.pointerId)

    const apply = (clientX: number, clientY: number) => {
      setDragging(true)
      onMove(clientX, clientY, el.getBoundingClientRect())
    }
    apply(e.clientX, e.clientY)

    const move = (ev: PointerEvent) => {
      if (axis === 'x') apply(ev.clientX, e.clientY)
      else apply(ev.clientX, ev.clientY)
    }
    const up = () => {
      cleanup()
      setDragging(false)
      onEndRef.current?.()
    }
    const cleanup = () => {
      window.removeEventListener('pointermove', move)
      window.removeEventListener('pointerup', up)
      window.removeEventListener('pointercancel', up)
      if (unbind.current === cleanup) unbind.current = null
    }
    unbind.current = cleanup
    window.addEventListener('pointermove', move)
    window.addEventListener('pointerup', up)
    window.addEventListener('pointercancel', up)
  }

  return { elRef, dragging, onPointerDown }
}
