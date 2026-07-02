import { create } from 'zustand';
import type { Conversation } from '../types';

interface ConversationState {
  conversations: Conversation[];
  activeConversationId: string | null;
  setConversations: (conversations: Conversation[]) => void;
  addConversation: (conversation: Conversation) => void;
  updateConversation: (id: string, updates: Partial<Conversation>) => void;
  setActiveConversation: (id: string | null) => void;
  isSidebarCollapsed: boolean;
  toggleSidebar: () => void;
}

export const useConversationStore = create<ConversationState>((set) => ({
  conversations: [],
  activeConversationId: null,
  isSidebarCollapsed: false,
  toggleSidebar: () => set((state) => ({ isSidebarCollapsed: !state.isSidebarCollapsed })),
  setConversations: (conversations) => set({ conversations }),
  addConversation: (conversation) =>
    set((state) => {
      // Avoid duplicates
      if (state.conversations.some((c) => c.id === conversation.id)) return state;
      return { conversations: [conversation, ...state.conversations] };
    }),
  updateConversation: (id, updates) =>
    set((state) => ({
      conversations: state.conversations.map((c) =>
        c.id === id ? { ...c, ...updates } : c
      ),
    })),
  setActiveConversation: (id) => set({ activeConversationId: id }),
}));
