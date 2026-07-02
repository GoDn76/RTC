import { create } from 'zustand';
import type { User } from '../types';

interface AuthState {
  token: string | null;
  user: User | null;
  isAuthenticated: boolean;
  
  pendingVerificationEmail?: string;
  forgotPasswordEmail?: string;
  pendingRegistration?: Record<string, any>;

  setAuth: (token: string, user: User) => void;
  setUser: (user: User) => void;
  clearAuth: () => void;
  
  setPendingVerificationEmail: (email?: string) => void;
  setForgotPasswordEmail: (email?: string) => void;
  setPendingRegistration: (payload?: Record<string, any>) => void;
  clearTempEmails: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  token: localStorage.getItem('chat_token'),
  user: null, // Should be loaded via GET /auth/me
  isAuthenticated: !!localStorage.getItem('chat_token'),
  
  setAuth: (token, user) => {
    localStorage.setItem('chat_token', token);
    set({ token, user, isAuthenticated: true });
  },
  setUser: (user) => set({ user }),
  clearAuth: () => {
    localStorage.removeItem('chat_token');
    set({ token: null, user: null, isAuthenticated: false });
  },

  setPendingVerificationEmail: (email) => set({ pendingVerificationEmail: email }),
  setForgotPasswordEmail: (email) => set({ forgotPasswordEmail: email }),
  setPendingRegistration: (payload) => set({ pendingRegistration: payload }),
  clearTempEmails: () => set({ pendingVerificationEmail: undefined, forgotPasswordEmail: undefined, pendingRegistration: undefined }),
}));
