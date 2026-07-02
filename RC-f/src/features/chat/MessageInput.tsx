import { useState, useRef, useEffect } from 'react';
import type { KeyboardEvent } from 'react';
import { Send, Smile, Paperclip } from 'lucide-react';
import { Button } from '../../components/ui/button';
import { chatSocket } from '../../websocket/chatSocket';
import { useConversationStore } from '../../store/conversationStore';
import { motion } from 'framer-motion';

interface Props {
  conversationId: string;
  isDM: boolean;
}

export function MessageInput({ conversationId, isDM }: Props) {
  const [message, setMessage] = useState('');
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const handleSend = () => {
    if (!message.trim()) return;

    const store = useConversationStore.getState();
    const conversation = store.conversations.find(c => c.id === conversationId);

    if (isDM) {
      const targetId = conversation?.targetUserId || (conversation as any)?.userId || (conversation as any)?.participantId;
      if (targetId) {
        chatSocket.send({
          action: 'CHAT',
          targetUserId: targetId,
          message: message.trim(),
        });
      } else {
        console.error("Missing targetUserId in DM conversation:", conversation);
      }
    } else {
      chatSocket.send({
        action: 'CHAT',
        roomId: conversation?.roomId || conversationId,
        message: message.trim(),
      });
    }

    setMessage('');
    
    // Reset textarea height
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
    }
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  // Auto-resize textarea
  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
      textareaRef.current.style.height = `${Math.min(textareaRef.current.scrollHeight, 120)}px`;
    }
  }, [message]);

  return (
    <div className="p-4 bg-transparent pb-6">
      <div className="max-w-4xl mx-auto flex items-end gap-2 glass-panel p-2 focus-within:border-primary/50 focus-within:ring-2 focus-within:ring-primary/20 transition-all duration-300">
        <Button variant="ghost" size="icon" className="shrink-0 text-muted-foreground hover:text-foreground hover:bg-foreground/5 rounded-xl h-10 w-10 transition-colors">
          <Paperclip className="w-5 h-5" />
        </Button>
        
        <textarea
          ref={textareaRef}
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="Message..."
          className="flex-1 max-h-[120px] min-h-[40px] py-2 px-3 bg-transparent border-none outline-none resize-none overflow-y-auto font-sans placeholder:text-muted-foreground leading-relaxed"
          rows={1}
        />
        
        <div className="flex items-center gap-1 shrink-0 pb-1 pr-1">
          <Button variant="ghost" size="icon" className="text-muted-foreground hover:text-foreground hover:bg-foreground/5 rounded-xl h-10 w-10 hidden sm:flex transition-colors">
            <Smile className="w-5 h-5" />
          </Button>
          <motion.div whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.95 }}>
            <Button 
              onClick={handleSend}
              disabled={!message.trim()}
              className="h-10 w-10 rounded-xl bg-gradient-to-br from-primary to-emerald-500 text-white hover:opacity-90 disabled:opacity-50 disabled:grayscale transition-all shadow-[0_4px_12px_rgba(22,199,132,0.2)]"
            >
              <Send className="w-5 h-5 ml-0.5" />
            </Button>
          </motion.div>
        </div>
      </div>
    </div>
  );
}
