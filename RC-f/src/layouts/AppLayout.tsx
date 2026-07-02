import { Outlet } from 'react-router-dom';
import { Sidebar } from '../features/rooms/Sidebar';

export function AppLayout() {
  return (
    <div className="flex h-screen w-full bg-background overflow-hidden relative">
      {/* Dynamic Background Elements */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden">
        <div className="absolute top-[-10%] right-[-5%] w-[60vw] h-[60vw] bg-primary/10 rounded-full blur-[100px] opacity-40 mix-blend-screen" />
        <div className="absolute bottom-[-10%] left-[-10%] w-[50vw] h-[50vw] bg-accent/10 rounded-full blur-[120px] opacity-30 mix-blend-screen" />
        
        {/* Subtle noise texture overlay if appropriate, but keeping it clean for now */}
        <div className="absolute inset-0 bg-background/50 backdrop-blur-[2px]" />
      </div>

      {/* Main UI Container, floating above background */}
      <div className="flex w-full h-full relative z-10">
        <Sidebar />
        
        <main className="flex-1 flex overflow-hidden">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
