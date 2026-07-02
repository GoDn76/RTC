import { useAuthStore } from '../store/authStore';
import { useConversationStore } from '../store/conversationStore';
import { useChatStore } from '../store/chatStore';
import type { WsOutgoingMessage, WsIncomingMessage } from '../types';
import { toast } from 'sonner';

class ChatSocketService {
  private socket: WebSocket | null = null;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 10;
  private baseDelay = 1000; // 1 second
  private isConnecting = false;
  private url: string;
  private messageQueue: WsOutgoingMessage[] = [];
  private hasLoadedRooms = false;

  constructor() {
    // In production, this would be an environment variable
    const host = import.meta.env.VITE_WS_HOST || window.location.host;
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    this.url = `${protocol}//${host}/ws/chat`;
  }

  public connect() {
    if (this.socket?.readyState === WebSocket.OPEN || this.isConnecting) {
      return;
    }

    const token = useAuthStore.getState().token;
    if (!token) {
      console.error('[WebSocket] Cannot connect: No token found');
      return;
    }

    this.isConnecting = true;
    const wsUrl = `${this.url}?token=${token}`;
    
    try {
      this.socket = new WebSocket(wsUrl);

      this.socket.onopen = this.handleOpen.bind(this);
      this.socket.onmessage = this.handleMessage.bind(this);
      this.socket.onclose = this.handleClose.bind(this);
      this.socket.onerror = this.handleError.bind(this);
    } catch (error) {
      console.error('[WebSocket] Error creating socket:', error);
      this.isConnecting = false;
      this.scheduleReconnect();
    }
  }

  public disconnect() {
    if (this.socket) {
      this.socket.close();
      this.socket = null;
    }
    this.reconnectAttempts = 0;
    this.hasLoadedRooms = false;
    this.messageQueue = [];
  }

  public send(payload: WsOutgoingMessage) {
    console.log('[WebSocket] Attempting to send:', payload, 'readyState:', this.socket?.readyState, 'hasLoadedRooms:', this.hasLoadedRooms);
    if (this.socket?.readyState === WebSocket.OPEN) {
      if (payload.action === 'GET_ROOMS' || this.hasLoadedRooms) {
        console.log('[WebSocket] Actually sending to socket:', payload);
        this.socket.send(JSON.stringify(payload));
      } else {
        console.log('[WebSocket] Queueing message (waiting for ROOMS):', payload);
        this.messageQueue.push(payload);
      }
    } else {
      console.log('[WebSocket] Queueing message (socket not open):', payload);
      this.messageQueue.push(payload);
    }
  }

  private handleOpen() {
    console.log('[WebSocket] Connected. Sending GET_ROOMS...');
    this.isConnecting = false;
    this.reconnectAttempts = 0;
    this.hasLoadedRooms = false;
    this.send({ action: 'GET_ROOMS' });
  }

  private handleMessage(event: MessageEvent) {
    console.log('[WebSocket] Received message raw data:', event.data);
    try {
      const data = JSON.parse(event.data) as WsIncomingMessage;
      console.log('[WebSocket] Parsed incoming message:', data);
      this.processIncomingMessage(data);
    } catch (error) {
      console.error('[WebSocket] Failed to parse message:', event.data, error);
    }
  }

  private handleClose(event: CloseEvent) {
    console.log(`[WebSocket] Disconnected: ${event.code} ${event.reason}`);
    this.socket = null;
    this.isConnecting = false;
    this.scheduleReconnect();
  }

  private handleError(event: Event) {
    console.error('[WebSocket] Error:', event);
  }

  private scheduleReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('[WebSocket] Max reconnect attempts reached. Please refresh.');
      // Optional: dispatch to a UI store to show a global "Disconnected" banner
      return;
    }

    const delay = this.baseDelay * Math.pow(2, this.reconnectAttempts);
    this.reconnectAttempts++;
    console.log(`[WebSocket] Reconnecting in ${delay}ms (Attempt ${this.reconnectAttempts})`);

    setTimeout(() => {
      this.connect();
    }, delay);
  }

  private processIncomingMessage(message: WsIncomingMessage) {
    switch (message.type) {
      case 'ROOMS': {
        const payload = message.payload;
        const store = useConversationStore.getState();
        
        const conversations = payload.map((r: any) => {
          const targetId = r.targetUserId || r.userId || r.participantId || r.targetId || r.otherParticipantId;
          return {
            id: r.roomType === 'PRIVATE' && targetId ? targetId : r.roomId,
            roomId: r.roomId,
            targetUserId: targetId,
            roomType: r.roomType,
            displayName: r.displayName,
            unreadCount: 0,
          };
        });
        
        store.setConversations(conversations);
        this.hasLoadedRooms = true;
        
        // Flush queue
        while (this.messageQueue.length > 0) {
          const msg = this.messageQueue.shift();
          if (msg) this.send(msg);
        }
        break;
      }
      case 'ROOM_CREATED': {
        const payload = message.payload;
        const id = payload.roomId;
        
        if (!id) break;

        const store = useConversationStore.getState();
        if (!store.conversations.some(c => c.id === id)) {
          store.addConversation({
            id,
            roomId: id,
            targetUserId: payload.targetUserId,
            roomType: payload.roomType === 'PRIVATE' ? 'PRIVATE' : 'GROUP',
            displayName: payload.roomName || 'New Room', 
            creator: payload.creator,
            creatorId: payload.creatorId,
            createdAt: payload.createdAt,
            unreadCount: 0,
          });
        }
        
        toast.success('Room created successfully!');
        window.location.href = `/c/${id}`;
        break;
      }
      case 'ADD_CHAT': {
        const chatMsg = message.payload as any;
        const store = useConversationStore.getState();
        
        let conv = store.conversations.find(c => c.roomId === chatMsg.roomId || c.id === chatMsg.roomId);
        if (!conv && chatMsg.userId) {
           conv = store.conversations.find(c => c.targetUserId === chatMsg.userId || c.id === chatMsg.userId);
        }
        if (!conv && chatMsg.targetUserId) {
           conv = store.conversations.find(c => c.targetUserId === chatMsg.targetUserId || c.id === chatMsg.targetUserId);
        }
        
        if (!conv) {
           console.warn('[WebSocket] ADD_CHAT could not find conversation for message:', chatMsg);
           // Add the room to the sidebar if we don't have it (e.g., we just joined via sending a CHAT)
           const newRoomId = chatMsg.roomId || chatMsg.targetUserId || chatMsg.userId;
           if (newRoomId) {
             store.addConversation({
               id: newRoomId,
               roomId: chatMsg.roomId,
               targetUserId: chatMsg.targetUserId || chatMsg.userId,
               roomType: chatMsg.roomId ? 'GROUP' : 'PRIVATE',
               displayName: 'Joined Room',
               unreadCount: 0,
             });
             conv = store.conversations.find(c => c.id === newRoomId);
           }
        }

        const convId = conv ? conv.id : chatMsg.roomId;
        
        useChatStore.getState().addMessage(convId, chatMsg);
        
        if (conv) {
          store.updateConversation(convId, {
            lastMessage: chatMsg.message,
            lastMessageTime: chatMsg.timestamp,
            unreadCount: store.activeConversationId !== convId ? conv.unreadCount + 1 : 0
          });
        }
        break;
      }
      case 'UPDATE_CHAT': {
        const chatMsg = message.payload;
        const store = useConversationStore.getState();
        const conv = store.conversations.find(c => c.roomId === chatMsg.roomId || c.id === chatMsg.roomId);
        const convId = conv ? conv.id : chatMsg.roomId;
        useChatStore.getState().updateMessage(convId, chatMsg.id, chatMsg);
        break;
      }
      case 'HISTORY': {
        const payload = message.payload as any;
        if (!Array.isArray(payload)) break;
        if (payload.length === 0) {
          console.warn('[WebSocket] Received empty HISTORY payload');
          break;
        }

        const firstMsg = payload[0];
        const store = useConversationStore.getState();
        
        let conv = store.conversations.find(c => c.roomId === firstMsg.roomId || c.id === firstMsg.roomId);
        if (!conv && firstMsg.userId) {
           conv = store.conversations.find(c => c.targetUserId === firstMsg.userId || c.id === firstMsg.userId);
        }

        if (!conv) {
          // If we received history for a room we don't know about, we probably just joined it via ID
          store.addConversation({
              id: firstMsg.roomId,
              roomId: firstMsg.roomId,
              roomType: 'GROUP', // default fallback
              displayName: 'Joined Room',
              unreadCount: 0,
          });
          conv = store.conversations.find(c => c.id === firstMsg.roomId);
        }

        if (conv) {
          useChatStore.getState().setHistory(conv.id, payload);
        }
        break;
      }
      case 'JOIN_SUCCESS': {
        toast.success(message.payload as string);
        break;
      }
      case 'ERROR': {
        console.error('[WebSocket] Server Error:', message.payload);
        toast.error('An error occurred', {
          description: message.payload
        });
        break;
      }
      default:
        console.warn('[WebSocket] Unknown message type:', message);
    }
  }
}

export const chatSocket = new ChatSocketService();
