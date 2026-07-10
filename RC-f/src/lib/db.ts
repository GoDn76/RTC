import { openDB } from 'idb';
import type { DBSchema, IDBPDatabase } from 'idb';
import type { User, Conversation, ChatMessage } from '../types';

interface ReChatOnDB extends DBSchema {
  user: {
    key: string;
    value: User;
  };
  rooms: {
    key: string;
    value: Conversation;
    indexes: { 'by-updated': number };
  };
  messages: {
    key: string;
    value: ChatMessage & { conversationId: string };
    indexes: { 'by-conversation': string, 'by-timestamp': number };
  };
  settings: {
    key: string;
    value: any;
  };
}

let dbPromise: Promise<IDBPDatabase<ReChatOnDB>> | null = null;

export const getDB = () => {
  if (!dbPromise) {
    dbPromise = openDB<ReChatOnDB>('realtime-chat', 1, {
      upgrade(db) {
        if (!db.objectStoreNames.contains('user')) {
          db.createObjectStore('user');
        }
        if (!db.objectStoreNames.contains('rooms')) {
          const roomStore = db.createObjectStore('rooms', { keyPath: 'id' });
          roomStore.createIndex('by-updated', 'lastMessageTime');
        }
        if (!db.objectStoreNames.contains('messages')) {
          const messageStore = db.createObjectStore('messages', { keyPath: 'id' });
          messageStore.createIndex('by-conversation', 'conversationId');
          messageStore.createIndex('by-timestamp', 'timestamp');
        }
        if (!db.objectStoreNames.contains('settings')) {
          db.createObjectStore('settings');
        }
      },
    });
  }
  return dbPromise;
};

// -- User --
export const saveUserToCache = async (user: User) => {
  const db = await getDB();
  await db.put('user', user, 'current');
};

export const getUserFromCache = async (): Promise<User | undefined> => {
  const db = await getDB();
  return db.get('user', 'current');
};

export const clearUserCache = async () => {
  const db = await getDB();
  await db.delete('user', 'current');
};

// -- Rooms --
export const saveRoomsToCache = async (rooms: Conversation[]) => {
  const db = await getDB();
  const tx = db.transaction('rooms', 'readwrite');
  for (const room of rooms) {
    tx.store.put(room);
  }
  await tx.done;
};

export const saveRoomToCache = async (room: Conversation) => {
  const db = await getDB();
  await db.put('rooms', room);
};

export const getRoomsFromCache = async (): Promise<Conversation[]> => {
  const db = await getDB();
  return db.getAll('rooms');
};

export const clearRoomsCache = async () => {
  const db = await getDB();
  await db.clear('rooms');
};

// -- Messages --
export const saveMessagesToCache = async (conversationId: string, messages: ChatMessage[]) => {
  const db = await getDB();
  const tx = db.transaction('messages', 'readwrite');
  for (const msg of messages) {
    tx.store.put({ ...msg, conversationId });
  }
  await tx.done;
};

export const saveMessageToCache = async (conversationId: string, message: ChatMessage) => {
  const db = await getDB();
  await db.put('messages', { ...message, conversationId });
};

export const getMessagesFromCache = async (conversationId: string): Promise<ChatMessage[]> => {
  const db = await getDB();
  const messages = await db.getAllFromIndex('messages', 'by-conversation', conversationId);
  return messages.sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime());
};

export const clearMessagesCache = async () => {
  const db = await getDB();
  await db.clear('messages');
};

// -- Settings (e.g. last active room) --
export const saveSetting = async (key: string, value: any) => {
  const db = await getDB();
  await db.put('settings', value, key);
};

export const getSetting = async (key: string) => {
  const db = await getDB();
  return db.get('settings', key);
};
