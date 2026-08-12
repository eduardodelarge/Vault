import { create } from 'zustand';
import type { AccountInfo } from '../types/user';

interface AuthState {
  accessToken: string | null;
  user: AccountInfo | null;
  setSession: (accessToken: string, user: AccountInfo | null) => void;
  setAccessToken: (accessToken: string | null) => void;
  setUser: (user: AccountInfo | null) => void;
  clear: () => void;
}

// accessToken vive só em memória (nunca localStorage) para reduzir a superfície de um XSS
// roubar o token; o refresh token fica em cookie HttpOnly, fora do alcance do JS.
export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  user: null,
  setSession: (accessToken, user) => set({ accessToken, user }),
  setAccessToken: (accessToken) => set({ accessToken }),
  setUser: (user) => set({ user }),
  clear: () => set({ accessToken: null, user: null }),
}));
