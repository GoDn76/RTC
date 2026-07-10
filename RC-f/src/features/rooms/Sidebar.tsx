import { useState } from 'react';
import { useAuthStore } from '../../store/authStore';
import { useConversationStore } from '../../store/conversationStore';
import { Avatar, AvatarFallback, AvatarImage } from '../../components/ui/avatar';
import { Badge } from '../../components/ui/badge';
import { ScrollArea } from '../../components/ui/scroll-area';
import { MessageSquarePlus, LogOut, Settings, Sun, Moon, PanelLeftClose, PanelLeftOpen, Users, Plus, LogIn } from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTheme } from 'next-themes';
import { motion, AnimatePresence } from 'framer-motion';
import { CreateGroupDialog } from './CreateGroupDialog';
import { UserSearchDialog } from '../users/UserSearchDialog';
import { JoinRoomDialog } from './JoinRoomDialog';
import { cn } from '../../lib/utils';

export function Sidebar() {
  const { user, clearAuth } = useAuthStore();
  const { conversations, isSidebarCollapsed, toggleSidebar } = useConversationStore();
  const { conversationId } = useParams();
  const navigate = useNavigate();
  const { theme, setTheme } = useTheme();
  
  const [isSearchOpen, setIsSearchOpen] = useState(false);
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [isJoinOpen, setIsJoinOpen] = useState(false);

  const dms = conversations.filter(c => c.roomType === 'PRIVATE');
  const rooms = conversations.filter(c => c.roomType === 'GROUP');

  const handleLogout = () => {
    clearAuth();
    navigate('/login');
  };

  return (
    <motion.div 
      initial={false}
      animate={{ width: isSidebarCollapsed ? 72 : 320 }}
      transition={{ type: "spring", stiffness: 300, damping: 30 }}
      className="flex flex-col glass h-full shrink-0 relative z-20 border-r border-white/5 border-y-0 border-l-0 shadow-2xl overflow-hidden"
    >
      {/* Header section */}
      <div className="p-4 flex items-center justify-between shrink-0 h-[72px]">
         <AnimatePresence>
            {!isSidebarCollapsed && (
              <motion.div 
                initial={{ opacity: 0, width: 0 }}
                animate={{ opacity: 1, width: 'auto' }}
                exit={{ opacity: 0, width: 0 }}
                className="font-bold text-xl whitespace-nowrap overflow-hidden text-emerald-500"
              >
                ReChatOn
              </motion.div>
            )}
         </AnimatePresence>
         
         <div className={cn("flex items-center justify-center w-full", !isSidebarCollapsed && "w-auto")}>
            <button 
              className="w-10 h-10 flex items-center justify-center rounded-xl text-muted-foreground hover:text-foreground hover:bg-white/5 transition-colors"
              onClick={toggleSidebar}
              title={isSidebarCollapsed ? "Expand Sidebar" : "Collapse Sidebar"}
            >
              {isSidebarCollapsed ? <PanelLeftOpen className="w-5 h-5" /> : <PanelLeftClose className="w-5 h-5" />}
            </button>
         </div>
      </div>

      <ScrollArea className="flex-1 w-full px-2">
         {/* DMs */}
         <div className="mb-6">
            <div className={cn("flex items-center mb-2 px-2", isSidebarCollapsed ? "justify-center" : "justify-between")}>
               {!isSidebarCollapsed && <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Direct Messages</h3>}
               <button 
                 onClick={() => setIsSearchOpen(true)}
                 className="w-8 h-8 flex items-center justify-center rounded-lg hover:bg-white/5 text-muted-foreground transition-colors"
                 title="New Direct Message"
               >
                 <Plus className="w-4 h-4" />
               </button>
            </div>
            
            <div className="space-y-1">
               {dms.map(conv => (
                  <button
                    key={conv.id}
                    onClick={() => navigate(`/c/${conv.id}`)}
                    className={cn(
                      "w-full flex items-center p-2 rounded-xl text-left transition-colors relative group",
                      isSidebarCollapsed ? "justify-center" : "gap-3",
                      conversationId === conv.id 
                        ? "text-foreground dark:text-white" 
                        : "hover:text-foreground dark:hover:text-white text-muted-foreground"
                    )}
                    title={isSidebarCollapsed ? conv.displayName : undefined}
                  >
                    {conversationId === conv.id && (
                      <motion.div
                        layoutId="active-room"
                        className="absolute inset-0 bg-emerald-500/10 backdrop-blur-md border border-emerald-500/20 rounded-xl z-0 shadow-[inset_0_1px_1px_rgba(255,255,255,0.05)]"
                        transition={{ type: "spring", stiffness: 300, damping: 30 }}
                      />
                    )}
                    <div className="relative z-10 shrink-0">
                      <Avatar className="w-10 h-10 border border-border/50">
                        <AvatarImage src={conv.avatar} />
                        <AvatarFallback className="bg-primary/5">
                          {(conv.displayName || 'U').charAt(0).toUpperCase()}
                        </AvatarFallback>
                      </Avatar>
                      <div className="absolute bottom-0 right-0 w-3 h-3 rounded-full bg-green-500 border-2 border-background" />
                      {isSidebarCollapsed && conv.unreadCount > 0 && (
                        <div className="absolute -top-1 -right-1 w-4 h-4 rounded-full bg-accent text-white text-[9px] font-bold flex items-center justify-center border-2 border-background shadow-sm">
                          {conv.unreadCount}
                        </div>
                      )}
                    </div>
                    
                    {!isSidebarCollapsed && (
                      <motion.div 
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        className="flex-1 overflow-hidden z-10"
                      >
                        <span className="font-medium text-sm truncate block">{conv.displayName}</span>
                        {conv.unreadCount > 0 && (
                          <Badge variant="default" className="mt-1 bg-accent hover:bg-accent/90 rounded-full px-1.5 text-[10px]">
                            {conv.unreadCount} New
                          </Badge>
                        )}
                      </motion.div>
                    )}
                  </button>
               ))}
            </div>
         </div>

         {/* Rooms */}
         <div className="mb-6">
            <div className={cn("flex items-center mb-2 px-2", isSidebarCollapsed ? "justify-center flex-col gap-2" : "justify-between")}>
               {!isSidebarCollapsed && <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Rooms</h3>}
               <div className={cn("flex", isSidebarCollapsed ? "flex-col gap-2" : "gap-1")}>
                 <button 
                   onClick={() => setIsJoinOpen(true)}
                   className="w-8 h-8 flex items-center justify-center rounded-lg hover:bg-white/5 text-muted-foreground transition-colors"
                   title="Join Room"
                 >
                   <LogIn className="w-4 h-4" />
                 </button>
                 <button 
                   onClick={() => setIsCreateOpen(true)}
                   className="w-8 h-8 flex items-center justify-center rounded-lg hover:bg-white/5 text-muted-foreground transition-colors"
                   title="Create Room"
                 >
                   <MessageSquarePlus className="w-4 h-4" />
                 </button>
               </div>
            </div>
            
            <div className="space-y-1">
               {rooms.map(conv => (
                  <button
                    key={conv.id}
                    onClick={() => navigate(`/c/${conv.id}`)}
                    className={cn(
                      "w-full flex items-center p-2 rounded-xl text-left transition-colors relative group",
                      isSidebarCollapsed ? "justify-center" : "gap-3",
                      conversationId === conv.id 
                        ? "text-foreground dark:text-white" 
                        : "hover:text-foreground dark:hover:text-white text-muted-foreground"
                    )}
                    title={isSidebarCollapsed ? conv.displayName : undefined}
                  >
                    {conversationId === conv.id && (
                      <motion.div
                        layoutId="active-room"
                        className="absolute inset-0 bg-emerald-500/10 backdrop-blur-md border border-emerald-500/20 rounded-xl z-0 shadow-[inset_0_1px_1px_rgba(255,255,255,0.05)]"
                        transition={{ type: "spring", stiffness: 300, damping: 30 }}
                      />
                    )}
                    <Avatar className="w-10 h-10 border border-border/50 relative z-10 shrink-0">
                      <AvatarFallback className="bg-primary/5">
                        <Users className="w-4 h-4" />
                      </AvatarFallback>
                    </Avatar>
                    
                    {!isSidebarCollapsed && (
                      <motion.div 
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        className="flex-1 overflow-hidden z-10"
                      >
                        <div className="flex justify-between items-baseline">
                          <span className="font-medium text-sm truncate block">{conv.displayName}</span>
                        </div>
                        <span className="text-xs text-muted-foreground group-hover:text-foreground dark:group-hover:text-white/70 truncate block font-mono mt-0.5">{conv.roomId}</span>
                      </motion.div>
                    )}
                  </button>
               ))}
            </div>
         </div>
      </ScrollArea>

      {/* Footer Controls */}
      <div className={cn("p-4 border-t border-white/5 flex gap-2 shrink-0", isSidebarCollapsed ? "flex-col items-center justify-center" : "items-center justify-between")}>
         <div className={cn("flex", isSidebarCollapsed ? "flex-col gap-2" : "gap-1")}>
           <button 
             onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')}
             className="w-10 h-10 flex items-center justify-center rounded-xl text-muted-foreground hover:text-foreground hover:bg-white/5 transition-colors overflow-hidden relative"
             title="Toggle Theme"
           >
             <AnimatePresence mode="wait">
               {theme === 'dark' ? (
                 <motion.div key="moon" initial={{ y: -20 }} animate={{ y: 0 }} exit={{ y: 20 }} transition={{ duration: 0.2 }}>
                   <Moon className="w-5 h-5" />
                 </motion.div>
               ) : (
                 <motion.div key="sun" initial={{ y: -20 }} animate={{ y: 0 }} exit={{ y: 20 }} transition={{ duration: 0.2 }}>
                   <Sun className="w-5 h-5" />
                 </motion.div>
               )}
             </AnimatePresence>
           </button>
           <button className="w-10 h-10 flex items-center justify-center rounded-xl text-muted-foreground hover:text-foreground hover:bg-white/5 transition-colors">
             <Settings className="w-5 h-5" />
           </button>
           <button onClick={handleLogout} className="w-10 h-10 flex items-center justify-center rounded-xl text-muted-foreground hover:text-destructive hover:bg-white/5 transition-colors">
             <LogOut className="w-5 h-5" />
           </button>
         </div>

         {user && (
           <Avatar className={cn("border-2 border-transparent hover:border-emerald-500 cursor-pointer transition-colors shrink-0", isSidebarCollapsed ? "w-10 h-10 mt-2" : "w-10 h-10")}>
             <AvatarImage src={user.avatar} alt={user.name} />
             <AvatarFallback className="bg-primary/10">{user.name.charAt(0).toUpperCase()}</AvatarFallback>
           </Avatar>
         )}
      </div>

      <UserSearchDialog open={isSearchOpen} onOpenChange={setIsSearchOpen} />
      <CreateGroupDialog open={isCreateOpen} onOpenChange={setIsCreateOpen} />
      <JoinRoomDialog open={isJoinOpen} onOpenChange={setIsJoinOpen} />
    </motion.div>
  );
}
