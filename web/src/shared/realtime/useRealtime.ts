import { useCallback, useEffect, useRef, useState } from 'react'
import { getToken } from '../api/client'

export type WsEnvelope = {
  type: 'hello' | 'state' | 'cmd' | 'error' | 'mqtt'
  topic?: string
  payload?: string
}

export function useRealtime() {
  const [wsConnected, setWsConnected] = useState(false)
  const [mqttConnected, setMqttConnected] = useState(false)
  const [values, setValues] = useState<Record<string, string>>({})
  const [lastError, setLastError] = useState<string | null>(null)
  const wsRef = useRef<WebSocket | null>(null)
  const mqttRef = useRef(false)
  const prevRef = useRef<Record<string, string>>({})

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

      ws.onopen = () => setWsConnected(true)
      ws.onclose = () => {
        setWsConnected(false)
        mqttRef.current = false
        setMqttConnected(false)
        wsRef.current = null
        if (!closed) {
          retryTimer = window.setTimeout(connect, 2000)
        }
      }
      ws.onerror = () => setWsConnected(false)
      ws.onmessage = (ev) => {
        try {
          const msg = JSON.parse(String(ev.data)) as WsEnvelope
          if (msg.type === 'mqtt') {
            const ok = msg.payload === '1'
            mqttRef.current = ok
            setMqttConnected(ok)
            return
          }
          if (msg.type === 'state' && msg.topic) {
            delete prevRef.current[msg.topic]
            setValues((prev) => ({ ...prev, [msg.topic!]: msg.payload ?? '' }))
            return
          }
          if (msg.type === 'error') {
            const topic = msg.topic
            if (topic && topic in prevRef.current) {
              const restore = prevRef.current[topic]
              setValues((prev) => ({ ...prev, [topic]: restore }))
              delete prevRef.current[topic]
            }
            setLastError(msg.payload || 'Ошибка команды')
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
    if (!ws || ws.readyState !== WebSocket.OPEN) {
      setLastError('Нет связи с сервером (WebSocket)')
      return
    }
    if (!mqttRef.current) {
      setLastError('MQTT не подключён — команда не уйдёт на Wiren Board')
      return
    }
    setLastError(null)
    setValues((prev) => {
      prevRef.current[topic] = prev[topic] ?? '—'
      return { ...prev, [topic]: payload }
    })
    ws.send(JSON.stringify({ type: 'cmd', topic, payload } satisfies WsEnvelope))
  }, [])

  const clearError = useCallback(() => setLastError(null), [])

  return {
    connected: wsConnected,
    mqttConnected,
    values,
    sendCmd,
    lastError,
    clearError,
  }
}
