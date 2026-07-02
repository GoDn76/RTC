import { useState } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '../../components/ui/dialog';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { chatSocket } from '../../websocket/chatSocket';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';


interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function JoinRoomDialog({ open, onOpenChange }: Props) {
  const [roomId, setRoomId] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const { user } = useAuthStore();

  const handleJoin = () => {
    if (!roomId.trim()) return;
    setLoading(true);
    // Send JOIN action
    chatSocket.send({
      action: 'JOIN',
      roomId: roomId.trim()
    });

    // Also broadcast the join message to the room like before
    chatSocket.send({
      action: 'CHAT',
      roomId: roomId.trim(),
      message: `${user?.id}joined the room${user?.id}`
    });

    setLoading(false);
    onOpenChange(false);
    
    // Navigate to the joined room
    navigate(`/c/${roomId.trim()}`);
    
    setRoomId('');
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md glass-panel p-6">
        <DialogHeader>
          <DialogTitle>Join Room</DialogTitle>
          <DialogDescription>
            Enter a Room ID to join an existing group conversation.
          </DialogDescription>
        </DialogHeader>
        
        <div className="mt-4">
          <Input 
            placeholder="Room ID (e.g. ABC-123)"
            value={roomId}
            onChange={(e) => setRoomId(e.target.value)}
            className="bg-background/50 border-white/5"
          />
        </div>

        <DialogFooter className="mt-4">
          <Button variant="ghost" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button 
            className="bg-accent text-white hover:bg-accent/90"
            onClick={handleJoin}
            disabled={loading || !roomId.trim()}
          >
            Join Room
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
