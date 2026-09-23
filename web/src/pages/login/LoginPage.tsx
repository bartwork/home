import { useState, type FormEvent } from 'react'
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { Navigate, useNavigate } from 'react-router-dom'
import { ApiError } from '../../shared/api/client'
import { useAuth } from '../../shared/auth/AuthContext'
import { useDocumentTitle } from '../../shared/useDocumentTitle'
import { LoginBackdrop } from './LoginBackdrop'

export function LoginPage() {
  useDocumentTitle('Мой дом')
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
    <Box
      component="main"
      sx={{
        position: 'relative',
        isolation: 'isolate',
        minHeight: '100dvh',
        display: 'grid',
        placeItems: 'center',
        px: 2,
        py: 4,
        overflow: 'hidden',
      }}
    >
      <LoginBackdrop />

      <Paper
        elevation={0}
        sx={{
          position: 'relative',
          zIndex: 1,
          width: 'min(400px, 100%)',
          p: { xs: 3.5, sm: 4.5 },
          border: '1px solid rgba(160, 200, 230, 0.18)',
          borderRadius: 3,
          bgcolor: 'rgba(10, 16, 26, 0.68)',
          backdropFilter: 'blur(24px) saturate(1.25)',
          boxShadow: [
            '0 30px 90px rgba(0, 0, 0, 0.5)',
            '0 0 0 1px rgba(255, 255, 255, 0.03)',
            'inset 0 1px 0 rgba(255, 255, 255, 0.08)',
          ].join(', '),
        }}
      >
        <Typography
          variant="h1"
          sx={{
            fontSize: { xs: '1.85rem', sm: '2.1rem' },
            mb: 3.5,
            textAlign: 'center',
            fontWeight: 700,
            letterSpacing: '-0.02em',
            color: 'text.primary',
          }}
        >
          Мой дом
        </Typography>

        <Box component="form" onSubmit={onSubmit} noValidate>
          <Stack spacing={2.25}>
            <TextField
              label="Логин"
              name="login"
              value={loginValue}
              onChange={(e) => setLoginValue(e.target.value)}
              placeholder="admin"
              autoComplete="username"
              required
              autoFocus
            />
            <TextField
              label="Пароль"
              name="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
              required
            />

            {error ? (
              <Alert severity="error" variant="outlined">
                {error}
              </Alert>
            ) : null}

            <Button
              type="submit"
              variant="contained"
              size="large"
              disabled={pending}
              sx={{
                mt: 0.5,
                py: 1.4,
                borderRadius: 2,
                fontSize: '1rem',
                background: 'linear-gradient(135deg, #7ad4e2 0%, #4fb8ca 55%, #3aa8ba 100%)',
                color: '#061018',
                '&:hover': {
                  background: 'linear-gradient(135deg, #8adceb 0%, #5ec8d8 55%, #3aa8ba 100%)',
                },
              }}
            >
              {pending ? <CircularProgress size={22} color="inherit" /> : 'Войти'}
            </Button>
          </Stack>
        </Box>
      </Paper>
    </Box>
  )
}
