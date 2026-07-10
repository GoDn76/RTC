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
        <div className="flex-1 flex flex-col items-center justify-center bg-transparent relative overflow-hidden">
          {/* Subtle background glow for empty state */}
          <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
            <div className="w-[40vw] h-[40vw] bg-emerald-500/5 rounded-full blur-[100px]" />
          </div>
          
          <div className="relative z-10 flex flex-col items-center text-center animate-in fade-in slide-in-from-bottom-8 duration-700">
            <div className="w-24 h-24 mb-8 relative">
              {/* Floating animation for the icon */}
              <div className="absolute inset-0 bg-emerald-500/20 blur-xl rounded-full animate-pulse" />
              <div className="w-full h-full glass-panel rounded-3xl flex items-center justify-center text-emerald-500 shadow-[0_0_40px_rgba(16,185,129,0.15)] animate-[float_4s_ease-in-out_infinite]">
                <MessageSquare className="w-10 h-10" />
              </div>
            </div>
            
            <h2 className="text-3xl font-bold mb-4 tracking-tight text-transparent bg-clip-text bg-gradient-to-br from-white to-white/60">
              Welcome to ReChatOn
            </h2>
            
            <p className="text-muted-foreground max-w-[300px] text-sm leading-relaxed mb-6">
              Select an existing conversation<br/>
              or create a new room to begin chatting.
            </p>
            
            <p className="text-xs font-medium text-emerald-500/70 uppercase tracking-widest mb-12">
              Your conversations will appear here instantly
            </p>
            
            <div className="flex items-center gap-2 px-4 py-2 rounded-full bg-orange-500/10 border border-orange-500/20 text-orange-500/80 text-xs">
              <span className="relative flex h-2 w-2">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-orange-400 opacity-75"></span>
                <span className="relative inline-flex rounded-full h-2 w-2 bg-orange-500"></span>
              </span>
              Notice: The current backend is temporary. Chat history may be reset periodically.
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
