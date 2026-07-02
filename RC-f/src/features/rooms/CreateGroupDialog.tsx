import { useState } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '../../components/ui/dialog';
import { Input } from '../../components/ui/input';
import { Button } from '../../components/ui/button';
import { chatSocket } from '../../websocket/chatSocket';

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function CreateGroupDialog({ open, onOpenChange }: Props) {
  const [loading, setLoading] = useState(false);
  const [roomName, setRoomName] = useState('');

  const handleCreate = () => {
    if (!roomName.trim()) return;
    setLoading(true);
    // Send CREATE_ROOM for Group
    chatSocket.send({
      action: 'CREATE_ROOM',
      roomName: roomName.trim(),
    });
    // For now we just close, relying on global state when ROOM_CREATED comes
    setLoading(false);
    onOpenChange(false);
    setRoomName('');
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md glass-panel p-6">
        <DialogHeader>
          <DialogTitle>Create Group</DialogTitle>
          <DialogDescription>
            Create a new group conversation to chat with multiple people.
          </DialogDescription>
        </DialogHeader>
        
        <div className="mt-4">
          <Input 
            placeholder="Room Name (e.g. Anime Fans)"
            value={roomName}
            onChange={(e) => setRoomName(e.target.value)}
            className="bg-background/50 border-white/5"
            maxLength={50}
          />
        </div>
        
        <DialogFooter className="mt-4">
          <Button variant="ghost" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button 
            className="bg-accent text-white hover:bg-accent/90"
            onClick={handleCreate}
            disabled={loading || !roomName.trim()}
          >
            Create Group
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
