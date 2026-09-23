import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import {
  getStoredUser,
  getToken,
  login as apiLogin,
  setSession,
  type User,
} from '../api/client'

type AuthState = {
  token: string | null
  user: User | null
  login: (login: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setTokenState] = useState<string | null>(() => getToken())
  const [user, setUser] = useState<User | null>(() => getStoredUser())

  const login = useCallback(async (loginValue: string, password: string) => {
    const res = await apiLogin(loginValue, password)
    setSession(res.token, res.user)
    setTokenState(res.token)
    setUser(res.user)
  }, [])

  const logout = useCallback(() => {
    setSession(null)
    setTokenState(null)
    setUser(null)
  }, [])

  const value = useMemo(
    () => ({ token, user, login, logout }),
    [token, user, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth outside AuthProvider')
  }
  return ctx
}
