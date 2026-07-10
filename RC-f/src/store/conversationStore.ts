import { create } from 'zustand';
import type { Conversation } from '../types';
import { saveRoomsToCache, saveRoomToCache, getRoomsFromCache, clearRoomsCache } from '../lib/db';

interface ConversationState {
  conversations: Conversation[];
  activeConversationId: string | null;
  setConversations: (conversations: Conversation[]) => void;
  addConversation: (conversation: Conversation) => void;
  updateConversation: (id: string, updates: Partial<Conversation>) => void;
  setActiveConversation: (id: string | null) => void;
  isSidebarCollapsed: boolean;
  toggleSidebar: () => void;
  clearConversations: () => void;
}

export const useConversationStore = create<ConversationState>((set) => ({
  conversations: [],
  activeConversationId: null,
  isSidebarCollapsed: false,
  toggleSidebar: () => set((state) => ({ isSidebarCollapsed: !state.isSidebarCollapsed })),
  setConversations: (conversations) => {
    saveRoomsToCache(conversations);
    set({ conversations });
  },
  addConversation: (conversation) =>
    set((state) => {
      if (state.conversations.some((c) => c.id === conversation.id)) return state;
      const newRooms = [conversation, ...state.conversations];
      saveRoomToCache(conversation);
      return { conversations: newRooms };
    }),
  updateConversation: (id, updates) =>
    set((state) => {
      const newRooms = state.conversations.map((c) => {
        if (c.id === id) {
          const updated = { ...c, ...updates };
          saveRoomToCache(updated);
          return updated;
        }
        return c;
      });
      return { conversations: newRooms };
    }),
  setActiveConversation: (id) => set({ activeConversationId: id }),
  clearConversations: () => {
    clearRoomsCache();
    set({ conversations: [], activeConversationId: null });
  }
}));

// Hydrate from cache
getRoomsFromCache().then(rooms => {
  if (rooms && rooms.length > 0) {
    // Sort by last message time if needed
    const sorted = rooms.sort((a, b) => (b.lastMessageTime || 0) - (a.lastMessageTime || 0));
    useConversationStore.setState({ conversations: sorted });
  }
});
