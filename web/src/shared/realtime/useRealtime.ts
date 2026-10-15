import { useCallback, useEffect, useRef, useState } from 'react'
import { getToken } from '../api/client'
import { samePayload } from './payload'

export type WsEnvelope = {
  type: 'hello' | 'state' | 'cmd' | 'error' | 'mqtt'
  topic?: string
  payload?: string
}

const CMD_TIMEOUT_MS = 5000

type Pending = {
  expected: string
  /** Исходное значение до первой optimistic-команды в цепочке. */
  restore: string
  timer: number
}

export function useRealtime() {
  const [wsConnected, setWsConnected] = useState(false)
  const [mqttConnected, setMqttConnected] = useState(false)
  const [values, setValues] = useState<Record<string, string>>({})
  const [lastError, setLastError] = useState<string | null>(null)
  const wsRef = useRef<WebSocket | null>(null)
  const mqttRef = useRef(false)
  const valuesRef = useRef(values)
  const pendingRef = useRef<Record<string, Pending>>({})

  valuesRef.current = values

  const clearPending = useCallback((topic: string) => {
    const p = pendingRef.current[topic]
    if (!p) return
    window.clearTimeout(p.timer)
    delete pendingRef.current[topic]
  }, [])

  const clearAllPending = useCallback(() => {
    for (const topic of Object.keys(pendingRef.current)) {
      clearPending(topic)
    }
  }, [clearPending])

  const rollback = useCallback(
    (topic: string, message: string) => {
      const p = pendingRef.current[topic]
      const restore = p?.restore
      clearPending(topic)
      if (restore !== undefined) {
        setValues((prev) => ({ ...prev, [topic]: restore }))
      }
      setLastError(message)
    },
    [clearPending],
  )

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
            mqttRef.current = msg.payload === '1'
            setMqttConnected(mqttRef.current)
            return
          }
          if (msg.type === 'state' && msg.topic) {
            const topic = msg.topic
            const payload = msg.payload ?? ''
            const pending = pendingRef.current[topic]
            if (pending && !samePayload(payload, pending.expected)) {
              // Stale retained — ждём expected или таймаут.
              return
            }
            if (pending) clearPending(topic)
            setValues((prev) => ({ ...prev, [topic]: payload }))
            return
          }
          if (msg.type === 'error') {
            const topic = msg.topic
            if (topic && pendingRef.current[topic]) {
              rollback(topic, msg.payload || 'Ошибка команды')
            } else {
              setLastError(msg.payload || 'Ошибка команды')
            }
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
      clearAllPending()
      ws?.close()
      wsRef.current = null
    }
  }, [clearAllPending, clearPending, rollback])

  const sendCmd = useCallback(
    (topic: string, payload: string) => {
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

      const existing = pendingRef.current[topic]
      const restore = existing?.restore ?? valuesRef.current[topic] ?? '—'
      if (existing) window.clearTimeout(existing.timer)
      const timer = window.setTimeout(() => {
        rollback(topic, 'Нет ответа от устройства (MQTT state)')
      }, CMD_TIMEOUT_MS)
      pendingRef.current[topic] = { expected: payload, restore, timer }
      setValues((prev) => ({ ...prev, [topic]: payload }))
      ws.send(JSON.stringify({ type: 'cmd', topic, payload } satisfies WsEnvelope))
    },
    [rollback],
  )

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
