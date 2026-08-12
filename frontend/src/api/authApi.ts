import { httpClient } from './httpClient';
import type { AccountInfo } from '../types/user';

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}

export function register(email: string, password: string, displayName: string) {
  return httpClient
    .post<AccountInfo>('/auth/register', { email, password, displayName })
    .then((res) => res.data);
}

export function login(email: string, password: string) {
  return httpClient.post<AuthResponse>('/auth/login', { email, password }).then((res) => res.data);
}

export function refresh() {
  return httpClient.post<AuthResponse>('/auth/refresh').then((res) => res.data);
}

export function logout() {
  return httpClient.post<void>('/auth/logout');
}
