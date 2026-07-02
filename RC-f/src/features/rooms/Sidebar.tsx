import { useState } from 'react';
import { useAuthStore } from '../../store/authStore';
import { useConversationStore } from '../../store/conversationStore';
import { Avatar, AvatarFallback, AvatarImage } from '../../components/ui/avatar';
import { MessageSquarePlus, LogOut, Settings, Sun, Moon, PanelLeftClose, PanelLeftOpen } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useTheme } from 'next-themes';
import { motion, AnimatePresence } from 'framer-motion';
import { CreateGroupDialog } from './CreateGroupDialog';

export function Sidebar() {
  const { user, clearAuth } = useAuthStore();
  const navigate = useNavigate();
  const { theme, setTheme } = useTheme();
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const { isSidebarCollapsed, toggleSidebar } = useConversationStore();

  const handleLogout = () => {
    clearAuth();
    navigate('/login');
  };

  return (
    <div className="w-[80px] flex flex-col items-center py-6 glass shrink-0 relative z-20 border-r border-white/5 border-t-0 border-b-0 border-l-0 shadow-2xl">
      <div className="flex-1 w-full flex flex-col items-center gap-6">
        {/* New Group Action */}
        <div className="group relative">
          <button 
            className="w-12 h-12 flex items-center justify-center rounded-2xl glass-button text-primary hover:text-primary-foreground group-hover:rounded-xl relative overflow-hidden"
            title="Create New Group"
            onClick={() => setIsCreateOpen(true)}
          >
            <MessageSquarePlus className="w-5 h-5 relative z-10" />
          </button>
        </div>

        <div className="w-8 h-[2px] bg-border/50 rounded-full" />
        
        {/* Placeholder for favorite groups or active items */}
        {/* Currently empty but designed for future scalability */}
      </div>

      <div className="flex flex-col items-center gap-5 mt-auto">
        <button 
          onClick={toggleSidebar}
          className="w-12 h-12 flex items-center justify-center rounded-full text-muted-foreground hover:text-foreground glass-button transition-colors"
          title="Toggle Sidebar"
        >
          {isSidebarCollapsed ? <PanelLeftOpen className="w-5 h-5" /> : <PanelLeftClose className="w-5 h-5" />}
        </button>

        <button 
          onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')}
          className="w-12 h-12 flex items-center justify-center rounded-full text-muted-foreground hover:text-foreground glass-button overflow-hidden relative"
          title="Toggle Theme"
        >
          <AnimatePresence mode="wait">
            {theme === 'dark' ? (
              <motion.div
                key="moon"
                initial={{ y: -20, opacity: 0, rotate: -90 }}
                animate={{ y: 0, opacity: 1, rotate: 0 }}
                exit={{ y: 20, opacity: 0, rotate: 90 }}
                transition={{ duration: 0.2 }}
              >
                <Moon className="w-5 h-5" />
              </motion.div>
            ) : (
              <motion.div
                key="sun"
                initial={{ y: -20, opacity: 0, rotate: -90 }}
                animate={{ y: 0, opacity: 1, rotate: 0 }}
                exit={{ y: 20, opacity: 0, rotate: 90 }}
                transition={{ duration: 0.2 }}
              >
                <Sun className="w-5 h-5" />
              </motion.div>
            )}
          </AnimatePresence>
        </button>

        <button className="w-12 h-12 flex items-center justify-center rounded-full text-muted-foreground hover:text-foreground glass-button transition-colors">
          <Settings className="w-5 h-5" />
        </button>
        
        <button onClick={handleLogout} className="w-12 h-12 flex items-center justify-center rounded-full text-muted-foreground hover:text-destructive glass-button transition-colors">
          <LogOut className="w-5 h-5" />
        </button>

        {user && (
          <Avatar className="w-12 h-12 border-2 border-transparent hover:border-accent cursor-pointer transition-colors">
            <AvatarImage src={user.avatar} alt={user.name} />
            <AvatarFallback className="bg-primary/10">{user.name.charAt(0).toUpperCase()}</AvatarFallback>
          </Avatar>
        )}
      </div>

      <CreateGroupDialog open={isCreateOpen} onOpenChange={setIsCreateOpen} />
    </div>
  );
}
