import { create } from 'zustand';
import type { ChatMessage } from '../types';
import { saveMessageToCache, saveMessagesToCache, getMessagesFromCache } from '../lib/db';

interface ChatState {
  messagesByRoom: Record<string, ChatMessage[]>;
  addMessage: (roomId: string, message: ChatMessage) => void;
  updateMessage: (roomId: string, messageId: string, updates: Partial<ChatMessage>) => void;
  setHistory: (roomId: string, messages: ChatMessage[]) => void;
  loadHistoryFromCache: (roomId: string) => Promise<void>;
}

export const useChatStore = create<ChatState>((set) => ({
  messagesByRoom: {},
  addMessage: (roomId, message) =>
    set((state) => {
      const roomMessages = state.messagesByRoom[roomId] || [];
      if (roomMessages.some((m) => m.id === message.id)) return state;
      
      saveMessageToCache(roomId, message);
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
      const updatedMessages = roomMessages.map((m) => {
        if (m.id === messageId) {
          const updated = { ...m, ...updates };
          saveMessageToCache(roomId, updated);
          return updated;
        }
        return m;
      });
      return {
        messagesByRoom: {
          ...state.messagesByRoom,
          [roomId]: updatedMessages,
        },
      };
    }),
  setHistory: (roomId, messages) =>
    set((state) => {
      const existingMessages = state.messagesByRoom[roomId] || [];
      const messageMap = new Map();
      
      existingMessages.forEach(m => messageMap.set(m.id, m));
      messages.forEach(m => messageMap.set(m.id, m));
      
      const merged = Array.from(messageMap.values()).sort((a, b) => a.timestamp - b.timestamp);
      
      saveMessagesToCache(roomId, merged);
      
      return {
        messagesByRoom: {
          ...state.messagesByRoom,
          [roomId]: merged,
        },
      };
    }),
  loadHistoryFromCache: async (roomId: string) => {
    const cached = await getMessagesFromCache(roomId);
    if (cached && cached.length > 0) {
      set((state) => {
        const existingMessages = state.messagesByRoom[roomId] || [];
        const messageMap = new Map();
        cached.forEach(m => messageMap.set(m.id, m));
        existingMessages.forEach(m => messageMap.set(m.id, m)); // in-memory wins if duplicate
        const merged = Array.from(messageMap.values()).sort((a, b) => a.timestamp - b.timestamp);
        return {
          messagesByRoom: {
            ...state.messagesByRoom,
            [roomId]: merged,
          },
        };
      });
    }
  }
}));
