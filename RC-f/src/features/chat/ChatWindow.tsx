import { useEffect, useRef } from 'react';
import { useChatStore } from '../../store/chatStore';
import { useAuthStore } from '../../store/authStore';
import { useConversationStore } from '../../store/conversationStore';
import { chatSocket } from '../../websocket/chatSocket';
import { motion } from 'framer-motion';

import { MessageInput } from './MessageInput';
import { Avatar, AvatarFallback, AvatarImage } from '../../components/ui/avatar';
import { Phone, Video, MoreVertical, Hash, Copy } from 'lucide-react';
import { Button } from '../../components/ui/button';
import { toast } from 'sonner';
import { cn } from '../../lib/utils';

interface Props {
  conversationId: string;
}

export function ChatWindow({ conversationId }: Props) {
  const { user } = useAuthStore();
  const { messagesByRoom } = useChatStore();
  const { conversations } = useConversationStore();
  const scrollRef = useRef<HTMLDivElement>(null);

  const conversation = conversations.find(c => c.id === conversationId);
  const rawMessages = messagesByRoom[conversation?.id || conversationId];
  const messages = rawMessages || [];
  const isDM = conversation?.roomType === 'PRIVATE';

  useEffect(() => {
    // Request history when conversation changes
    if (conversationId && conversation) {
      useChatStore.getState().loadHistoryFromCache(conversationId);
      
      if (isDM) {
        const targetId = conversation?.targetUserId || (conversation as any)?.userId || (conversation as any)?.participantId;
        if (targetId) {
          chatSocket.send({
            action: 'GET_CHATS',
            targetUserId: targetId,
            limit: 50,
            offset: 0,
          });
        } else {
          console.error("Missing targetUserId for GET_CHATS in DM:", conversation);
        }
      } else {
        chatSocket.send({
          action: 'GET_CHATS',
          roomId: conversation?.roomId || conversationId,
          limit: 50,
          offset: 0,
        });
      }
    }
  }, [conversationId, isDM, conversation]);

  // Auto-scroll to bottom when messages change
  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [rawMessages]);

  if (!conversation) {
    return (
      <div className="flex-1 flex items-center justify-center bg-background/95">
        <div className="text-center text-muted-foreground">
          <Hash className="w-12 h-12 mx-auto mb-4 opacity-20" />
          <h3 className="text-lg font-medium text-foreground">Conversation not found</h3>
          <p>Please select a valid conversation.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex-1 flex flex-col bg-transparent h-full relative overflow-hidden">
      {/* Subtle ambient lighting for chat workspace */}
      <div className="absolute inset-0 pointer-events-none z-0">
        <div className="absolute top-[40%] left-[50%] -translate-x-1/2 -translate-y-1/2 w-[60%] h-[60%] bg-emerald-500/5 rounded-full blur-[140px] mix-blend-screen" />
      </div>

      {/* Header */}
      <div className="h-[72px] shrink-0 border-b border-white/5 bg-background/40 backdrop-blur-xl flex items-center justify-between px-6 z-20 relative">
        <div className="flex items-center gap-3">
          <Avatar className="w-10 h-10 border border-border/50">
            <AvatarImage src={conversation.avatar} />
            <AvatarFallback className="bg-primary/5">
              {(conversation.displayName || 'U').charAt(0).toUpperCase()}
            </AvatarFallback>
          </Avatar>
          <div>
            <h3 className="font-semibold">{conversation.displayName}</h3>
            {isDM ? (
              <p className="text-xs text-green-500">Online</p>
            ) : (
              <div className="flex items-center gap-1 group">
                <p className="text-xs text-muted-foreground font-mono">{conversation.roomId || conversationId}</p>
                <Button 
                  variant="ghost" 
                  size="icon" 
                  className="w-4 h-4 opacity-0 group-hover:opacity-100 transition-opacity" 
                  onClick={() => {
                    const idToCopy = conversation.roomId || conversationId;
                    navigator.clipboard.writeText(idToCopy);
                    toast.success('Room ID copied to clipboard');
                  }}
                >
                  <Copy className="w-3 h-3 text-muted-foreground" />
                </Button>
                {conversation.creator && (
                  <>
                    <span className="text-muted-foreground mx-1">•</span>
                    <span className="text-xs text-muted-foreground">By {conversation.creator}</span>
                  </>
                )}
              </div>
            )}
          </div>
        </div>

        <div className="flex items-center gap-2">
          {isDM && (
            <>
              <Button variant="ghost" size="icon" className="text-muted-foreground hover:text-white hover:bg-white/5 transition-colors">
                <Phone className="w-5 h-5" />
              </Button>
              <Button variant="ghost" size="icon" className="text-muted-foreground hover:text-white hover:bg-white/5 transition-colors">
                <Video className="w-5 h-5" />
              </Button>
            </>
          )}
          <Button variant="ghost" size="icon" className="text-muted-foreground hover:text-white hover:bg-white/5 transition-colors">
            <MoreVertical className="w-5 h-5" />
          </Button>
        </div>
      </div>

      {/* Message List */}
      <div ref={scrollRef} className="flex-1 px-6 overflow-y-auto relative z-10">
        <div className="flex flex-col justify-end min-h-full py-6">
          {messages.length === 0 ? (
            <div className="text-center text-muted-foreground py-10 mt-auto">
              No messages yet. Say hi!
            </div>
          ) : (
            messages.map((msg, index) => {
              const isOwn = Boolean(user?.id && msg.userId && user.id === msg.userId);
              const isSystemJoin = msg.message === `${msg.userId}joined the room${msg.userId}`;
              
              // Find the previous and next real messages to determine grouping
              let prevRealMsg = null;
              for (let i = index - 1; i >= 0; i--) {
                const m = messages[i];
                if (m.message !== `${m.userId}joined the room${m.userId}`) {
                  prevRealMsg = m;
                  break;
                }
              }
              
              let nextRealMsg = null;
              for (let i = index + 1; i < messages.length; i++) {
                const m = messages[i];
                if (m.message !== `${m.userId}joined the room${m.userId}`) {
                  nextRealMsg = m;
                  break;
                }
              }
              
              const isGrouped = prevRealMsg && prevRealMsg.userId === msg.userId && (msg.timestamp - prevRealMsg.timestamp < 300000); // 5 mins
              const isNextGrouped = nextRealMsg && nextRealMsg.userId === msg.userId && (nextRealMsg.timestamp - msg.timestamp < 300000);
              const isFirst = index === 0;

              if (isSystemJoin) {
                return (
                  <motion.div 
                    initial={{ opacity: 0, y: 10, scale: 0.95 }}
                    animate={{ opacity: 1, y: 0, scale: 1 }}
                    layout
                    key={msg.id} 
                    className="flex justify-center w-full my-4"
                  >
                    <div className="glass-panel text-muted-foreground border-white/10 px-4 py-1.5 rounded-full text-xs font-medium inline-flex items-center gap-2 shadow-sm">
                      <div className="w-1.5 h-1.5 rounded-full bg-emerald-500 shadow-[0_0_8px_rgba(16,185,129,0.5)]" />
                      {isOwn ? 'You' : msg.name} joined the room
                    </div>
                  </motion.div>
                );
              }

              // Telegram-style corner rounding based on position in group
              let roundedClasses = "rounded-2xl";
              if (isOwn) {
                if (isGrouped && isNextGrouped) roundedClasses = "rounded-l-2xl rounded-r-sm";
                else if (isGrouped && !isNextGrouped) roundedClasses = "rounded-tl-2xl rounded-bl-2xl rounded-tr-sm rounded-br-2xl";
                else if (!isGrouped && isNextGrouped) roundedClasses = "rounded-tl-2xl rounded-bl-2xl rounded-tr-2xl rounded-br-sm";
                else roundedClasses = "rounded-2xl rounded-tr-sm";
              } else {
                if (isGrouped && isNextGrouped) roundedClasses = "rounded-r-2xl rounded-l-sm";
                else if (isGrouped && !isNextGrouped) roundedClasses = "rounded-tr-2xl rounded-br-2xl rounded-tl-sm rounded-bl-2xl";
                else if (!isGrouped && isNextGrouped) roundedClasses = "rounded-tr-2xl rounded-br-2xl rounded-tl-2xl rounded-bl-sm";
                else roundedClasses = "rounded-2xl rounded-tl-sm";
              }

              return (
                <motion.div 
                  initial={{ opacity: 0, y: 5, scale: 0.98 }}
                  animate={{ opacity: 1, y: 0, scale: 1 }}
                  layout
                  key={msg.id} 
                  className={cn(
                    "flex gap-3 max-w-[85%] md:max-w-[75%]",
                    isOwn ? "ml-auto flex-row-reverse" : "mr-auto",
                    isFirst ? "mt-auto" : (isGrouped ? "mt-0.5" : "mt-6")
                  )}
                >
                  {!isOwn && (
                    <div className="shrink-0 w-8">
                      {!isNextGrouped && (
                        <Avatar className="w-8 h-8 border border-border/50 self-end mt-auto mb-0">
                          <AvatarFallback className="bg-primary/10 text-xs">
                            {msg.name.charAt(0).toUpperCase()}
                          </AvatarFallback>
                        </Avatar>
                      )}
                    </div>
                  )}

                  <div className={cn("flex flex-col gap-0.5", isOwn ? "items-end" : "items-start", "max-w-full")}>
                    {!isGrouped && !isOwn && (
                      <span className="text-xs font-semibold text-muted-foreground pl-1 mb-0.5">
                        {msg.name}
                      </span>
                    )}
                    
                    <div className={cn(
                      "px-3 py-2 relative shadow-sm text-[15px] flex flex-col min-w-[80px]",
                      roundedClasses,
                      isOwn 
                        ? "bg-gradient-to-br from-primary to-emerald-500 text-white shadow-[inset_0_1px_1px_rgba(255,255,255,0.2),0_4px_12px_rgba(22,199,132,0.2)]" 
                        : "glass-3d text-foreground"
                    )}>
                      <p className="whitespace-pre-wrap break-words leading-snug">{msg.message}</p>
                      <div className="flex justify-end items-center gap-1 mt-1 -mb-1 opacity-70">
                        <span className={cn(
                          "text-[10px]",
                          isOwn ? "text-white" : "text-muted-foreground"
                        )}>
                          {new Date(msg.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </span>
                      </div>
                    </div>
                  </div>
                </motion.div>
              );
            })
          )}
        </div>
      </div>

      {/* Input Area */}
      <MessageInput conversationId={conversationId} isDM={isDM} />
    </div>
  );
}
