import { type FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AxiosError } from 'axios';
import * as authApi from '../api/authApi';

export function RegisterPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await authApi.register(email, password, displayName);
      navigate('/login', { replace: true, state: { registered: true } });
    } catch (err) {
      if (err instanceof AxiosError && err.response?.status === 409) {
        setError('Ja existe uma conta com esse email.');
      } else if (err instanceof AxiosError && err.response?.data?.details?.length) {
        setError(err.response.data.details.join(', '));
      } else {
        setError('Nao foi possivel criar a conta.');
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-page">
      <form className="auth-form" onSubmit={handleSubmit}>
        <h1>VaultDesk</h1>
        <p className="auth-subtitle">Crie sua conta</p>

        {error && <div className="form-error">{error}</div>}

        <label>
          Nome
          <input value={displayName} onChange={(e) => setDisplayName(e.target.value)} autoFocus />
        </label>

        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </label>

        <label>
          Senha
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            minLength={8}
            required
          />
        </label>

        <button type="submit" disabled={loading}>
          {loading ? 'Criando...' : 'Criar conta'}
        </button>

        <p className="auth-switch">
          Ja tem conta? <Link to="/login">Entrar</Link>
        </p>
      </form>
    </div>
  );
}
