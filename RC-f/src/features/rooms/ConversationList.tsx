import { useConversationStore } from '../../store/conversationStore';
import { ScrollArea } from '../../components/ui/scroll-area';
import { Avatar, AvatarFallback, AvatarImage } from '../../components/ui/avatar';
import { Badge } from '../../components/ui/badge';
import { Users, Plus, LogIn, MessageSquarePlus } from 'lucide-react';
import { cn } from '../../lib/utils';
import { useNavigate, useParams } from 'react-router-dom';
import { useState } from 'react';
import { UserSearchDialog } from '../users/UserSearchDialog';
import { CreateGroupDialog } from './CreateGroupDialog';
import { JoinRoomDialog } from './JoinRoomDialog';
import { Button } from '../../components/ui/button';
import { motion, AnimatePresence } from 'framer-motion';

export function ConversationList() {
  const { conversations } = useConversationStore();
  const { conversationId } = useParams();
  const navigate = useNavigate();
  
  const [isSearchOpen, setIsSearchOpen] = useState(false);
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [isJoinOpen, setIsJoinOpen] = useState(false);

  const dms = conversations.filter(c => c.roomType === 'PRIVATE');
  const rooms = conversations.filter(c => c.roomType === 'GROUP');

  return (
    <div className="w-full flex flex-col glass shrink-0 h-full relative z-10 border-r border-white/5 border-y-0 border-l-0">
      <ScrollArea className="flex-1">
        <div className="p-4 space-y-6">
          
          {/* Direct Messages Section */}
          <div>
            <div className="flex items-center justify-between mb-2">
              <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Direct Messages</h3>
              <Button 
                variant="ghost" 
                size="icon" 
                onClick={() => setIsSearchOpen(true)}
                className="h-6 w-6 text-muted-foreground hover:text-foreground"
                title="Search User"
              >
                <Plus className="w-4 h-4" />
              </Button>
            </div>
            
            <div className="space-y-1">
              {dms.length === 0 ? (
                <div className="text-sm text-muted-foreground text-center py-4 bg-accent/5 rounded-xl border border-dashed border-border">
                  Start a conversation
                </div>
              ) : (
                dms.map(conv => (
                  <button
                    key={conv.id}
                    onClick={() => navigate(`/c/${conv.id}`)}
                    className={cn(
                      "w-full flex items-center gap-3 p-2 rounded-xl text-left transition-colors relative group",
                      conversationId === conv.id 
                        ? "text-foreground dark:text-white" 
                        : "hover:text-foreground dark:hover:text-white text-muted-foreground"
                    )}
                  >
                    {conversationId === conv.id && (
                      <motion.div
                        layoutId="active-room"
                        className="absolute inset-0 bg-primary/20 backdrop-blur-md border border-primary/30 rounded-xl z-0 shadow-[inset_0_1px_1px_rgba(255,255,255,0.1)]"
                        transition={{ type: "spring", stiffness: 300, damping: 30 }}
                      />
                    )}
                    <div className="relative z-10">
                      <Avatar className="w-10 h-10 border border-border/50">
                        <AvatarImage src={conv.avatar} />
                        <AvatarFallback className="bg-primary/5">
                          {(conv.displayName || 'U').charAt(0).toUpperCase()}
                        </AvatarFallback>
                      </Avatar>
                      <div className="absolute bottom-0 right-0 w-3 h-3 rounded-full bg-green-500 border-2 border-background" />
                    </div>
                    <div className="flex-1 overflow-hidden">
                      <span className="font-medium text-sm truncate block">{conv.displayName}</span>
                      {conv.unreadCount > 0 && (
                        <Badge variant="default" className="mt-1 bg-accent hover:bg-accent/90 rounded-full px-1.5 text-[10px]">
                          {conv.unreadCount} New
                        </Badge>
                      )}
                    </div>
                  </button>
                ))
              )}
            </div>
          </div>

          {/* Rooms Section */}
          <div>
            <div className="flex items-center justify-between mb-2">
              <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Rooms</h3>
              <div className="flex gap-1">
                <Button 
                  variant="ghost" 
                  size="icon" 
                  onClick={() => setIsJoinOpen(true)}
                  className="h-6 w-6 text-muted-foreground hover:text-foreground"
                  title="Join Room"
                >
                  <LogIn className="w-4 h-4" />
                </Button>
                <Button 
                  variant="ghost" 
                  size="icon" 
                  onClick={() => setIsCreateOpen(true)}
                  className="h-6 w-6 text-muted-foreground hover:text-foreground"
                  title="Create Room"
                >
                  <MessageSquarePlus className="w-4 h-4" />
                </Button>
              </div>
            </div>

            <div className="space-y-1">
              {rooms.length === 0 ? (
                <div className="text-sm text-muted-foreground text-center py-4 bg-accent/5 rounded-xl border border-dashed border-border">
                  No rooms joined yet
                </div>
              ) : (
                rooms.map(conv => (
                  <button
                    key={conv.id}
                    onClick={() => navigate(`/c/${conv.id}`)}
                    className={cn(
                      "w-full flex items-center gap-3 p-2 rounded-xl text-left transition-colors relative group",
                      conversationId === conv.id 
                        ? "text-foreground dark:text-white" 
                        : "hover:text-foreground dark:hover:text-white text-muted-foreground"
                    )}
                  >
                    {conversationId === conv.id && (
                      <motion.div
                        layoutId="active-room"
                        className="absolute inset-0 bg-primary/20 backdrop-blur-md border border-primary/30 rounded-xl z-0 shadow-[inset_0_1px_1px_rgba(255,255,255,0.1)]"
                        transition={{ type: "spring", stiffness: 300, damping: 30 }}
                      />
                    )}
                    <Avatar className="w-10 h-10 border border-border/50 relative z-10 group-hover:border-primary/50 transition-colors">
                      <AvatarFallback className="bg-primary/5">
                        <Users className="w-4 h-4" />
                      </AvatarFallback>
                    </Avatar>
                    <div className="flex-1 overflow-hidden">
                      <div className="flex justify-between items-baseline">
                        <span className="font-medium text-sm truncate block">{conv.displayName}</span>
                        {conv.creator && (
                          <span className="text-[10px] text-muted-foreground truncate max-w-[80px]">By {conv.creator}</span>
                        )}
                      </div>
                      <span className="text-xs text-muted-foreground group-hover:text-foreground dark:group-hover:text-white/70 truncate block font-mono mt-0.5">{conv.roomId}</span>
                    </div>
                  </button>
                ))
              )}
            </div>
          </div>

        </div>
      </ScrollArea>

      <UserSearchDialog open={isSearchOpen} onOpenChange={setIsSearchOpen} />
      <CreateGroupDialog open={isCreateOpen} onOpenChange={setIsCreateOpen} />
      <JoinRoomDialog open={isJoinOpen} onOpenChange={setIsJoinOpen} />
    </div>
  );
}
