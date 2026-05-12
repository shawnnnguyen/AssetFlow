import { createContext, useContext, useState } from 'react';
import { api } from '../api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [userId, setUserId]     = useState(() => localStorage.getItem('userId'));
  const [token, setToken]       = useState(() => localStorage.getItem('token'));
  const [username, setUsername] = useState(() => localStorage.getItem('username'));

  function persist(res) {
    localStorage.setItem('token',    res.token);
    localStorage.setItem('userId',   String(res.userId));
    localStorage.setItem('username', res.username);
    setToken(res.token);
    setUserId(String(res.userId));
    setUsername(res.username);
  }

  async function login(creds) {
    const res = await api.auth.login(creds);
    if (res.token) persist(res);
    return res;
  }

  async function register(creds) {
    const res = await api.auth.register(creds);
    if (res.token) persist(res);
    return res;
  }

  function logout() {
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

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
