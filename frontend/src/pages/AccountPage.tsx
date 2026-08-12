import { type FormEvent, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import * as usersApi from '../api/usersApi';

export function AccountPage() {
  const { data: account, isLoading } = useQuery({ queryKey: ['account'], queryFn: usersApi.getMe });

  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [status, setStatus] = useState<{ type: 'error' | 'success'; message: string } | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleChangePassword(e: FormEvent) {
    e.preventDefault();
    setStatus(null);
    setSubmitting(true);
    try {
      await usersApi.changePassword(currentPassword, newPassword);
      setStatus({ type: 'success', message: 'Senha alterada com sucesso.' });
      setCurrentPassword('');
      setNewPassword('');
    } catch (err) {
      if (err instanceof AxiosError && err.response?.status === 401) {
        setStatus({ type: 'error', message: 'Senha atual incorreta.' });
      } else {
        setStatus({ type: 'error', message: 'Nao foi possivel trocar a senha.' });
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="page">
      <h1>Minha conta</h1>

      {isLoading && <p>Carregando...</p>}

      {account && (
        <div className="account-info">
          <div>
            <span className="label">Nome</span>
            <span>{account.displayName || '—'}</span>
          </div>
          <div>
            <span className="label">Email</span>
            <span>{account.email}</span>
          </div>
          <div>
            <span className="label">Conta criada em</span>
            <span>{new Date(account.createdAt).toLocaleString('pt-BR')}</span>
          </div>
          <div>
            <span className="label">Ultimo login</span>
            <span>{account.lastLoginAt ? new Date(account.lastLoginAt).toLocaleString('pt-BR') : '—'}</span>
          </div>
        </div>
      )}

      <h2>Trocar senha</h2>
      {status && <div className={status.type === 'error' ? 'form-error' : 'form-success'}>{status.message}</div>}

      <form className="note-form" onSubmit={handleChangePassword}>
        <label>
          Senha atual
          <input
            type="password"
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
            required
          />
        </label>

        <label>
          Nova senha
          <input
            type="password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            minLength={8}
            required
          />
        </label>

        <div className="note-form-actions">
          <button type="submit" disabled={submitting}>
            {submitting ? 'Salvando...' : 'Trocar senha'}
          </button>
        </div>
      </form>
    </div>
  );
}
