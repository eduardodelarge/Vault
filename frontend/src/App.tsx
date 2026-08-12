import { useEffect, useState } from 'react';
import { Navigate, Route, BrowserRouter, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Navbar } from './components/Navbar';
import { ProtectedRoute } from './components/ProtectedRoute';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { DashboardPage } from './pages/DashboardPage';
import { NoteEditorPage } from './pages/NoteEditorPage';
import { AccountPage } from './pages/AccountPage';
import { useAuthStore } from './store/authStore';
import * as authApi from './api/authApi';
import * as usersApi from './api/usersApi';
import './App.css';

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: false, refetchOnWindowFocus: false } },
});

function App() {
  // O access token so existe em memoria, entao ao recarregar a pagina tentamos
  // reidratar a sessao usando o cookie HttpOnly do refresh token.
  const [bootstrapped, setBootstrapped] = useState(false);
  const setSession = useAuthStore((state) => state.setSession);

  useEffect(() => {
    authApi
      .refresh()
      .then(async ({ accessToken }) => {
        setSession(accessToken, null);
        const account = await usersApi.getMe();
        setSession(accessToken, account);
      })
      .catch(() => {
        // sem sessao valida, o usuario cai nas rotas publicas (login/register)
      })
      .finally(() => setBootstrapped(true));
  }, [setSession]);

  if (!bootstrapped) {
    return (
      <div className="bootstrap-loading">
        <p>Carregando VaultDesk...</p>
      </div>
    );
  }

  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Navbar />
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <DashboardPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/notes/new"
            element={
              <ProtectedRoute>
                <NoteEditorPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/notes/:id"
            element={
              <ProtectedRoute>
                <NoteEditorPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/account"
            element={
              <ProtectedRoute>
                <AccountPage />
              </ProtectedRoute>
            }
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

export default App;
