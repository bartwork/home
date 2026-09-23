import { useCallback, useEffect, useRef, useState } from 'react'
import { getToken } from '../api/client'

export type WsEnvelope = {
  type: 'hello' | 'state' | 'cmd' | 'error'
  topic?: string
  payload?: string
  deviceId?: number
}

export function useRealtime() {
  const [connected, setConnected] = useState(false)
  const [values, setValues] = useState<Record<string, string>>({})
  const wsRef = useRef<WebSocket | null>(null)

  useEffect(() => {
    const token = getToken()
    if (!token) return

    let closed = false
    let retryTimer: number | undefined
    let ws: WebSocket | null = null

    const connect = () => {
      if (closed) return
      const proto = window.location.protocol === 'https:' ? 'wss' : 'ws'
      const url = `${proto}://${window.location.host}/api/v1/ws?token=${encodeURIComponent(token)}`
      ws = new WebSocket(url)
      wsRef.current = ws

      ws.onopen = () => setConnected(true)
      ws.onclose = () => {
        setConnected(false)
        wsRef.current = null
        if (!closed) {
          retryTimer = window.setTimeout(connect, 2000)
        }
      }
      ws.onerror = () => setConnected(false)
      ws.onmessage = (ev) => {
        try {
          const msg = JSON.parse(String(ev.data)) as WsEnvelope
          if (msg.type === 'state' && msg.topic) {
            setValues((prev) => ({ ...prev, [msg.topic!]: msg.payload ?? '' }))
          }
        } catch {
          /* ignore */
        }
      }
    }

    connect()

    return () => {
      closed = true
      if (retryTimer !== undefined) window.clearTimeout(retryTimer)
      ws?.close()
      wsRef.current = null
    }
  }, [])

  const sendCmd = useCallback((topic: string, payload: string) => {
    const ws = wsRef.current
    if (!ws || ws.readyState !== WebSocket.OPEN) return
    ws.send(JSON.stringify({ type: 'cmd', topic, payload } satisfies WsEnvelope))
  }, [])

  return { connected, values, sendCmd }
}
