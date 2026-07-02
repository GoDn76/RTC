import { useState } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '../../components/ui/dialog';
import { Input } from '../../components/ui/input';
import { useUserSearch } from '../../hooks/useUserSearch';
import { Search, Loader2, UserPlus } from 'lucide-react';
import { Avatar, AvatarFallback, AvatarImage } from '../../components/ui/avatar';
import { ScrollArea } from '../../components/ui/scroll-area';
import { chatSocket } from '../../websocket/chatSocket';

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function UserSearchDialog({ open, onOpenChange }: Props) {
  const [query, setQuery] = useState('');
  const { data: users, isLoading } = useUserSearch(query);

  const handleStartDM = (user: any) => {
    // Send CREATE_ROOM for DM
    chatSocket.send({
      action: 'CREATE_ROOM',
      targetUserId: user.uuid,
    });
    
    // We do NOT create any frontend room. We wait for ROOM_CREATED event from backend.
    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md glass-panel p-6">
        <DialogHeader>
          <DialogTitle>Find Users</DialogTitle>
        </DialogHeader>
        
        <div className="relative mt-2">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
          <Input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search by name or email..."
            className="pl-10 bg-background/50 border-white/5 h-12 text-base"
          />
        </div>

        <div className="mt-4 h-[300px] relative">
          {query.length < 2 && (
            <div className="absolute inset-0 flex items-center justify-center text-sm text-muted-foreground text-center px-4">
              Type at least 2 characters to search for users.
            </div>
          )}

          {query.length >= 2 && isLoading && (
            <div className="absolute inset-0 flex items-center justify-center">
              <Loader2 className="w-6 h-6 animate-spin text-accent" />
            </div>
          )}

          {query.length >= 2 && !isLoading && users?.length === 0 && (
            <div className="absolute inset-0 flex items-center justify-center text-sm text-muted-foreground">
              No users found matching "{query}"
            </div>
          )}

          {query.length >= 2 && !isLoading && users && users.length > 0 && (
            <ScrollArea className="h-full">
              <div className="space-y-2 pr-4">
                {users.map((user) => (
                  <button
                    key={user.uuid}
                    onClick={() => handleStartDM(user)}
                    className="w-full flex items-center gap-3 p-3 rounded-xl hover:bg-accent/10 transition-colors group text-left"
                  >
                    <Avatar className="w-10 h-10 border border-border/50">
                      <AvatarImage src={user.avatar} />
                      <AvatarFallback className="bg-primary/5">
                        {user.name.charAt(0).toUpperCase()}
                      </AvatarFallback>
                    </Avatar>
                    <div className="flex-1 overflow-hidden">
                      <p className="font-medium truncate">{user.name}</p>
                      <p className="text-xs text-muted-foreground truncate">{user.email}</p>
                    </div>
                    <UserPlus className="w-5 h-5 text-muted-foreground opacity-0 group-hover:opacity-100 transition-opacity" />
                  </button>
                ))}
              </div>
            </ScrollArea>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
