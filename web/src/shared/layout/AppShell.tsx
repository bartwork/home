import type { ReactNode } from 'react'
import { AppBar, Box, Button, Container, Stack, Toolbar, Typography } from '@mui/material'
import { Link as RouterLink } from 'react-router-dom'
import { APP_NAME } from '../brand'

type Props = {
  title: string
  subtitle?: string
  actions?: ReactNode
  children: ReactNode
  maxWidth?: 'sm' | 'md' | 'lg' | 'xl' | false
}

export function AppShell({ title, subtitle, actions, children, maxWidth = 'sm' }: Props) {
  return (
    <Box
      component="main"
      sx={{
        minHeight: '100dvh',
        background: [
          'radial-gradient(900px 420px at 85% -10%, rgba(94, 200, 216, 0.12), transparent 55%)',
          'radial-gradient(700px 360px at 10% 100%, rgba(240, 179, 90, 0.08), transparent 50%)',
          'var(--mui-palette-background-default)',
        ].join(', '),
      }}
    >
      <AppBar
        position="sticky"
        elevation={0}
        color="transparent"
        sx={{ borderBottom: 1, borderColor: 'divider', backdropFilter: 'blur(10px)' }}
      >
        <Toolbar sx={{ gap: 2, justifyContent: 'space-between' }}>
          <Typography
            component={RouterLink}
            to="/"
            variant="h1"
            sx={{ fontSize: '1.8rem', color: 'inherit', textDecoration: 'none' }}
          >
            {APP_NAME}
          </Typography>
          <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
            {actions}
          </Stack>
        </Toolbar>
      </AppBar>

      <Container
        maxWidth={maxWidth}
        sx={{
          py: { xs: 2, sm: 3 },
          ...(maxWidth === false ? { px: { xs: 1.5, sm: 2.5 }, maxWidth: '100% !important' } : {}),
        }}
      >
        <Typography
          variant="h2"
          sx={{
            fontSize: maxWidth === false ? { xs: '1.35rem', sm: '1.6rem' } : { xs: '1.8rem', sm: '2.4rem' },
            mb: 0.5,
          }}
        >
          {title}
        </Typography>
        {subtitle ? (
          <Typography color="text.secondary" sx={{ mb: maxWidth === false ? 1.5 : 3 }}>
            {subtitle}
          </Typography>
        ) : (
          <Box sx={{ mb: maxWidth === false ? 1.5 : 3 }} />
        )}
        {children}
      </Container>
    </Box>
  )
}

export function ShellLogoutButton({ onClick }: { onClick: () => void }) {
  return (
    <Button variant="outlined" color="inherit" onClick={onClick} size="small">
      Выйти
    </Button>
  )
}
