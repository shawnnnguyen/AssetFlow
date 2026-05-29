export interface AuthResponse {
  token: string;
  userId: number;
  username: string;
}

export interface LoginCredentials {
  email: string;
  password: string;
}

export interface RegisterCredentials {
  username: string;
  email: string;
  password: string;
}

export interface AuthContextValue {
  userId: string | null;
  token: string | null;
  username: string | null;
  isAuthenticated: boolean;
  login: (creds: LoginCredentials) => Promise<AuthResponse>;
  register: (creds: RegisterCredentials) => Promise<AuthResponse>;
  logout: () => void;
}
