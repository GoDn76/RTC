import { useParams } from 'react-router-dom';
import { ConversationList } from '../features/rooms/ConversationList';
import { ChatWindow } from '../features/chat/ChatWindow';
import { MessageSquare } from 'lucide-react';
import { useConversationStore } from '../store/conversationStore';
import { cn } from '../lib/utils';

export function ChatApp() {
  const { conversationId } = useParams();
  const { isSidebarCollapsed } = useConversationStore();

  return (
    <>
      {/* List - hidden on mobile if a conversation is selected */}
      <div 
        className={cn(
          "h-full shrink-0 transition-[width,opacity] duration-300 ease-in-out overflow-hidden",
          conversationId ? 'hidden md:block' : 'block w-full md:w-auto',
          isSidebarCollapsed ? "w-0 opacity-0" : "w-full md:w-[320px] opacity-100"
        )}
      >
        <ConversationList />
      </div>

      {/* Main Window */}
      <div className={`flex-1 h-full ${!conversationId ? 'hidden md:flex' : 'flex'}`}>
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
    </>
  );
}
