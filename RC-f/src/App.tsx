import { useEffect, useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from './store/authStore';
import { authApi } from './api/auth';
import { chatSocket } from './websocket/chatSocket';
import { Toaster } from 'sonner';

// Components
import { StartupScreen } from './components/StartupScreen';

// Layouts
import { AuthLayout } from './layouts/AuthLayout';
import { AppLayout } from './layouts/AppLayout';

// Pages
import { Login } from './pages/Login';
import { Register } from './pages/Register';
import { VerifyOtpPage } from './pages/VerifyOtpPage';
import { ForgotPassword } from './pages/ForgotPassword';
import { ResetPassword } from './pages/ResetPassword';
import { ChatApp } from './pages/ChatApp';

function ProtectedRoute() {
  const { isAuthenticated, user, setUser } = useAuthStore();
  
  useEffect(() => {
    if (isAuthenticated) {
      if (!user) {
        authApi.getMe().then(profile => {
          setUser({
            id: profile.uuid,
            name: profile.name,
            email: profile.email,
            emailVerified: profile.emailVerified
          });
        }).catch(() => {
          useAuthStore.getState().clearAuth();
        });
      }
      chatSocket.connect();
    } else {
      chatSocket.disconnect();
    }
  }, [isAuthenticated, user, setUser]);

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
}

function PublicRoute() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  
  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}

export default function App() {
  const [isReady, setIsReady] = useState(false);

  return (
    <>
      {!isReady && <StartupScreen onReady={() => setIsReady(true)} />}
      
      {isReady && (
        <BrowserRouter>
          <Routes>
            <Route element={<PublicRoute />}>
              <Route element={<AuthLayout />}>
                <Route path="/login" element={<Login />} />
                <Route path="/register" element={<Register />} />
                <Route path="/verify-otp" element={<VerifyOtpPage />} />
                <Route path="/forgot-password" element={<ForgotPassword />} />
                <Route path="/reset-password" element={<ResetPassword />} />
              </Route>
            </Route>

            <Route element={<ProtectedRoute />}>
              <Route element={<AppLayout />}>
                <Route path="/" element={<ChatApp />} />
                <Route path="/c/:conversationId" element={<ChatApp />} />
              </Route>
            </Route>
            
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
          <Toaster theme="dark" position="bottom-right" richColors />
        </BrowserRouter>
      )}
    </>
  );
}
