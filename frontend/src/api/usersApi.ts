import { httpClient } from './httpClient';
import type { AccountInfo } from '../types/user';

export function getMe() {
  return httpClient.get<AccountInfo>('/users/me').then((res) => res.data);
}

export function changePassword(currentPassword: string, newPassword: string) {
  return httpClient.put<void>('/users/me/password', { currentPassword, newPassword });
}
