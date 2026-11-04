import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { api, getToken } from './api'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(!!getToken())

  useEffect(() => {
    if (!getToken()) return
    api('/auth/me')
      .then(setUser)
      .catch(() => localStorage.removeItem('token'))
      .finally(() => setLoading(false))
  }, [])

  const handleAuth = useCallback(({ token, user }) => {
    localStorage.setItem('token', token)
    setUser(user)
    return user
  }, [])

  const login = (email, password) =>
    api('/auth/login', { method: 'POST', body: { email, password }, auth: false }).then(handleAuth)

  const register = (name, email, password) =>
    api('/auth/register', { method: 'POST', body: { name, email, password }, auth: false }).then(handleAuth)

  const logout = () => {
    localStorage.removeItem('token')
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout }}>{children}</AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
