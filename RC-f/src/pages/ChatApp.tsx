import { useParams } from 'react-router-dom';
import { ChatWindow } from '../features/chat/ChatWindow';
import { MessageSquare } from 'lucide-react';

export function ChatApp() {
  const { conversationId } = useParams();

  return (
    <div className="flex-1 h-full flex">
      {conversationId ? (
        <ChatWindow conversationId={conversationId} />
      ) : (
        <div className="flex-1 flex flex-col items-center justify-center bg-background/95">
          <div className="glass-panel p-8 rounded-3xl flex flex-col items-center text-center animate-in fade-in slide-in-from-bottom-4 duration-500">
            <div className="w-16 h-16 bg-accent/20 rounded-2xl flex items-center justify-center mb-6 text-accent">
              <MessageSquare className="w-8 h-8" />
            </div>
            <h2 className="text-2xl font-bold mb-2">Real-Time Chat</h2>
            <p className="text-muted-foreground max-w-[250px]">
              Select a conversation from the sidebar or create a new one to start messaging.
            </p>
          </div>
        </div>
      )}
    </div>
  );
}
