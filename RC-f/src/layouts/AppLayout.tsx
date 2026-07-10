import { Outlet } from 'react-router-dom';
import { Sidebar } from '../features/rooms/Sidebar';

export function AppLayout() {
  return (
    <div className="flex h-screen w-full bg-background overflow-hidden relative">
      {/* Premium Layered Background */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden bg-[#0B0F0D] dark:bg-background">
        {/* Soft emerald radial glows */}
        <div className="absolute top-[-20%] right-[-10%] w-[70vw] h-[70vw] bg-[#00C896] rounded-full blur-[150px] opacity-[0.03] mix-blend-screen" />
        <div className="absolute bottom-[-20%] left-[-10%] w-[60vw] h-[60vw] bg-[#00C896] rounded-full blur-[180px] opacity-[0.02] mix-blend-screen" />
        <div className="absolute top-[30%] left-[20%] w-[40vw] h-[40vw] bg-[#0E1C17] rounded-full blur-[120px] opacity-40 mix-blend-screen" />
        
        {/* Vignette effect (darker edges) */}
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,transparent_0%,rgba(0,0,0,0.4)_100%)] pointer-events-none" />
        
        {/* Extremely subtle noise texture overlay for realism */}
        <div className="absolute inset-0 opacity-[0.015]" style={{ backgroundImage: `url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.65' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)'/%3E%3C/svg%3E")` }} />
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
