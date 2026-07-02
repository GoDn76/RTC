import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { GoogleOAuthProvider } from '@react-oauth/google';
import { env } from './config/env';
import { ThemeProvider } from 'next-themes';
import App from './App.tsx';
import './index.css';
import { Toaster } from './components/ui/sonner';

const queryClient = new QueryClient();

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <ThemeProvider attribute="class" defaultTheme="dark" enableSystem>
        <GoogleOAuthProvider clientId={env.googleClientId}>
          <App />
          <Toaster />
        </GoogleOAuthProvider>
      </ThemeProvider>
    </QueryClientProvider>
  </StrictMode>,
);
