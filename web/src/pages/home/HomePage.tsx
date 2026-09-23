import { useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { listDevices, type Device } from '../../shared/api/client'
import { useAuth } from '../../shared/auth/AuthContext'
import { useRealtime } from '../../shared/realtime/useRealtime'
import styles from './home.module.css'

function switchOn(value: string) {
  return value === '1' || value === 'true' || value === 'on'
}

export function HomePage() {
  const { token, user, logout } = useAuth()
  const { connected, values, sendCmd } = useRealtime()
  const [devices, setDevices] = useState<Device[]>([])
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!token) return
    let cancelled = false
    listDevices()
      .then((res) => {
        if (!cancelled) setDevices(res.devices ?? [])
      })
      .catch((err: Error) => {
        if (!cancelled) setError(err.message)
      })
    return () => {
      cancelled = true
    }
  }, [token])

  if (!token) {
    return <Navigate to="/login" replace />
  }

  return (
    <main className={styles.page}>
      <header className={styles.top}>
        <p className={styles.brand}>home</p>
        <div className={styles.meta}>
          <span className={connected ? styles.online : styles.offline}>
            {connected ? 'live' : 'offline'}
          </span>
          <button type="button" className={styles.logout} onClick={logout}>
            Выйти
          </button>
        </div>
      </header>

      <section className={styles.body}>
        <h1>Устройства</h1>
        <p className={styles.lead}>
          {user
            ? `${user.firstName || user.email} · MQTT → WebSocket`
            : 'MQTT → WebSocket'}
        </p>
        {error ? <p className={styles.error}>{error}</p> : null}

        <ul className={styles.list}>
          {devices.map((d) => {
            const value = values[d.mqttTopic] ?? '—'
            const on = switchOn(value)
            return (
              <li key={d.id} className={styles.card}>
                <div>
                  <p className={styles.name}>{d.name}</p>
                  <p className={styles.topic}>{d.mqttTopic}</p>
                </div>
                <div className={styles.actions}>
                  {d.deviceType === 'switch' ? (
                    <button
                      type="button"
                      className={on ? styles.switchOn : styles.switchOff}
                      onClick={() => sendCmd(d.mqttTopic, on ? '0' : '1')}
                    >
                      {on ? 'ON' : 'OFF'}
                    </button>
                  ) : (
                    <p className={styles.value}>
                      {value}
                      {d.unit ? ` ${d.unit}` : ''}
                    </p>
                  )}
                </div>
              </li>
            )
          })}
        </ul>
      </section>
    </main>
  )
}
