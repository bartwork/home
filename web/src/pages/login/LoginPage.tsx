import { useState, type FormEvent } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { ApiError } from '../../shared/api/client'
import { useAuth } from '../../shared/auth/AuthContext'
import styles from './login.module.css'

export function LoginPage() {
  const { token, login } = useAuth()
  const navigate = useNavigate()
  const [loginValue, setLoginValue] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  if (token) {
    return <Navigate to="/" replace />
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setPending(true)
    try {
      await login(loginValue.trim(), password)
      navigate('/', { replace: true })
    } catch (err) {
      if (err instanceof ApiError && (err.status === 401 || err.status === 403)) {
        setError('Неверный логин или пароль')
      } else {
        setError('Не удалось войти. Попробуйте ещё раз.')
      }
    } finally {
      setPending(false)
    }
  }

  return (
    <main className={styles.page}>
      <div className={styles.glow} aria-hidden />
      <div className={styles.grid} aria-hidden />

      <section className={styles.panel}>
        <p className={styles.brand}>home</p>
        <h1 className={styles.title}>Вход в контур</h1>
        <p className={styles.lead}>
          Локальный доступ к Wirenboard: устройства, сценарии и живые виджеты.
        </p>

        <form className={styles.form} onSubmit={onSubmit}>
          <label className={styles.field}>
            <span>Email или телефон</span>
            <input
              autoComplete="username"
              value={loginValue}
              onChange={(e) => setLoginValue(e.target.value)}
              placeholder="admin@home.local"
              required
            />
          </label>
          <label className={styles.field}>
            <span>Пароль</span>
            <input
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </label>

          {error ? <p className={styles.error}>{error}</p> : null}

          <button className={styles.submit} type="submit" disabled={pending}>
            {pending ? 'Входим…' : 'Войти'}
          </button>
        </form>
      </section>
    </main>
  )
}
