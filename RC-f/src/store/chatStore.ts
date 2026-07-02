import { create } from 'zustand';
import type { ChatMessage } from '../types';

interface ChatState {
  messagesByRoom: Record<string, ChatMessage[]>;
  addMessage: (roomId: string, message: ChatMessage) => void;
  updateMessage: (roomId: string, messageId: string, updates: Partial<ChatMessage>) => void;
  setHistory: (roomId: string, messages: ChatMessage[]) => void;
}

export const useChatStore = create<ChatState>((set) => ({
  messagesByRoom: {},
  addMessage: (roomId, message) =>
    set((state) => {
      const roomMessages = state.messagesByRoom[roomId] || [];
      // Prevent duplicates
      if (roomMessages.some((m) => m.id === message.id)) return state;
      return {
        messagesByRoom: {
          ...state.messagesByRoom,
          [roomId]: [...roomMessages, message],
        },
      };
    }),
  updateMessage: (roomId, messageId, updates) =>
    set((state) => {
      const roomMessages = state.messagesByRoom[roomId] || [];
      return {
        messagesByRoom: {
          ...state.messagesByRoom,
          [roomId]: roomMessages.map((m) => (m.id === messageId ? { ...m, ...updates } : m)),
        },
      };
    }),
  setHistory: (roomId, messages) =>
    set((state) => {
      const existingMessages = state.messagesByRoom[roomId] || [];
      const messageMap = new Map();
      
      // Add existing messages first
      existingMessages.forEach(m => messageMap.set(m.id, m));
      // Add incoming history (overwriting duplicates with the newer data payload if any)
      messages.forEach(m => messageMap.set(m.id, m));
      
      // Convert back to array and sort chronologically (oldest to newest)
      const merged = Array.from(messageMap.values()).sort((a, b) => a.timestamp - b.timestamp);
      
      return {
        messagesByRoom: {
          ...state.messagesByRoom,
          [roomId]: merged,
        },
      };
    }),
}));
