import { createTheme } from '@mui/material/styles'

export const theme = createTheme({
  cssVariables: true,
  palette: {
    mode: 'dark',
    primary: {
      main: '#5ec8d8',
      dark: '#3aa8ba',
      light: '#8adceb',
      contrastText: '#061018',
    },
    secondary: {
      main: '#f0b35a',
    },
    error: {
      main: '#f07178',
    },
    background: {
      default: '#0a1018',
      paper: '#121a24',
    },
    text: {
      primary: '#e8eef6',
      secondary: '#8b9bb0',
    },
    divider: 'rgba(232, 238, 246, 0.12)',
  },
  typography: {
    fontFamily: '"DM Sans", system-ui, sans-serif',
    h1: {
      fontFamily: '"Syne", sans-serif',
      fontWeight: 700,
      letterSpacing: '-0.03em',
    },
    h2: {
      fontFamily: '"Syne", sans-serif',
      fontWeight: 700,
      letterSpacing: '-0.03em',
    },
    h3: {
      fontFamily: '"Syne", sans-serif',
      fontWeight: 700,
      letterSpacing: '-0.02em',
    },
    button: {
      textTransform: 'none',
      fontWeight: 600,
    },
  },
  shape: {
    borderRadius: 16,
  },
  components: {
    MuiButton: {
      defaultProps: {
        disableElevation: true,
      },
    },
    MuiTextField: {
      defaultProps: {
        variant: 'outlined',
        fullWidth: true,
      },
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          backgroundColor: 'rgba(8, 14, 22, 0.55)',
        },
      },
    },
  },
})
