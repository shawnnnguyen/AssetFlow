import { createContext, useContext, useState } from 'react';
import type { ReactNode } from 'react';
import { api } from '../api';
import type { AuthContextValue, AuthResponse, LoginCredentials, RegisterCredentials } from '../types';

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [userId, setUserId]     = useState<string | null>(() => localStorage.getItem('userId'));
  const [token, setToken]       = useState<string | null>(() => localStorage.getItem('token'));
  const [username, setUsername] = useState<string | null>(() => localStorage.getItem('username'));

  function persist(res: AuthResponse): void {
    localStorage.setItem('token',    res.token);
    localStorage.setItem('userId',   String(res.userId));
    localStorage.setItem('username', res.username);
    setToken(res.token);
    setUserId(String(res.userId));
    setUsername(res.username);
  }

  async function login(creds: LoginCredentials): Promise<AuthResponse> {
    const res = await api.auth.login(creds);
    if (res.token) persist(res);
    return res;
  }

  async function register(creds: RegisterCredentials): Promise<AuthResponse> {
    const res = await api.auth.register(creds);
    if (res.token) persist(res);
    return res;
  }

  function logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    localStorage.removeItem('username');
    setToken(null);
    setUserId(null);
    setUsername(null);
  }

  return (
    <AuthContext.Provider value={{ userId, token, username, isAuthenticated: !!token, login, logout, register }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
