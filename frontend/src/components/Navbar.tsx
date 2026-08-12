import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import * as authApi from '../api/authApi';

export function Navbar() {
  const navigate = useNavigate();
  const { accessToken, user, clear } = useAuthStore();

  if (!accessToken) {
    return null;
  }

  async function handleLogout() {
    try {
      await authApi.logout();
    } finally {
      clear();
      navigate('/login', { replace: true });
    }
  }

  return (
    <nav className="navbar">
      <Link to="/" className="navbar-brand">
        VaultDesk
      </Link>
      <div className="navbar-links">
        <Link to="/">Notas</Link>
        <Link to="/account">{user?.displayName || user?.email || 'Conta'}</Link>
        <button type="button" onClick={handleLogout} className="link-button">
          Sair
        </button>
      </div>
    </nav>
  );
}
