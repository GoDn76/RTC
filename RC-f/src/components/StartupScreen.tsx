import { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { authApi } from '../api/auth';
import { toast } from 'sonner';

interface Props {
  onReady: () => void;
}

const MESSAGES = [
  'Starting ReChatOn...',
  'Preparing your workspace...',
  'Connecting to server...',
  'Loading conversations...',
  'Synchronizing cache...',
];

export function StartupScreen({ onReady }: Props) {
  const [messageIndex, setMessageIndex] = useState(0);
  const [isFadingOut, setIsFadingOut] = useState(false);

  useEffect(() => {
    // Cycle messages every 2.5 seconds
    const interval = setInterval(() => {
      setMessageIndex((prev) => (prev + 1) % MESSAGES.length);
    }, 2500);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    let mounted = true;

    const wakeUpLoop = async () => {
      let awake = false;
      let attempts = 0;
      
      while (!awake && mounted && attempts < 30) { // Max 60 seconds (30 * 2s)
        try {
          // This will throw because credentials are bad
          await authApi.login({ email: 'startup_ping@test.com', password: 'invalid_password' });
          awake = true; // Shouldn't reach here, but if it does, it's awake
        } catch (error: any) {
          if (error.message !== 'Failed to fetch' && !error.message.includes('NetworkError') && !error.message.includes('fetch')) {
            // It's a valid HTTP error response!
            awake = true;
          }
        }
        
        if (!awake) {
          attempts++;
          await new Promise(res => setTimeout(res, 2000));
        }
      }
      
      if (mounted) {
        if (!awake) {
          toast.error("Backend is unreachable. Entering offline mode.");
        }
        setIsFadingOut(true);
        setTimeout(() => {
          onReady();
        }, 800); // Wait for fade out animation
      }
    };

    wakeUpLoop();

    return () => {
      mounted = false;
    };
  }, [onReady]);

  return (
    <AnimatePresence>
      {!isFadingOut && (
        <motion.div
          initial={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.8, ease: "easeInOut" }}
          className="fixed inset-0 z-50 flex flex-col items-center justify-center bg-background overflow-hidden"
        >
          {/* Background effects */}
          <div className="absolute inset-0 pointer-events-none">
            <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-emerald-500/20 rounded-full blur-[120px] mix-blend-screen animate-pulse" />
          </div>

          <motion.div 
            initial={{ scale: 0.9, opacity: 0, y: 20 }}
            animate={{ scale: 1, opacity: 1, y: 0 }}
            transition={{ duration: 0.7, ease: "easeOut" }}
            className="glass-panel p-12 rounded-[2.5rem] flex flex-col items-center relative z-10 w-[90%] max-w-md shadow-2xl border border-white/10"
          >
            {/* Logo */}
            <motion.div 
              animate={{ 
                boxShadow: ['0 0 0 0 rgba(16, 185, 129, 0)', '0 0 0 20px rgba(16, 185, 129, 0)'],
              }}
              transition={{ repeat: Infinity, duration: 2 }}
              className="w-24 h-24 bg-gradient-to-br from-emerald-400 to-emerald-600 rounded-3xl flex items-center justify-center mb-6 shadow-[0_0_30px_rgba(16,185,129,0.3)]"
            >
              <svg className="w-12 h-12 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
              </svg>
            </motion.div>

            <h1 className="text-4xl font-bold tracking-tight mb-2 text-foreground">ReChatOn</h1>
            <p className="text-emerald-500 font-medium tracking-wide text-sm mb-10">Conversations. Instantly.</p>

            <div className="h-8 flex items-center justify-center relative w-full">
              <AnimatePresence mode="wait">
                <motion.p
                  key={messageIndex}
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -10 }}
                  transition={{ duration: 0.3 }}
                  className="text-muted-foreground text-sm font-medium absolute"
                >
                  {MESSAGES[messageIndex]}
                </motion.p>
              </AnimatePresence>
            </div>
            
            <div className="mt-8 flex gap-2">
              <motion.div animate={{ scale: [1, 1.2, 1] }} transition={{ repeat: Infinity, duration: 1, delay: 0 }} className="w-2 h-2 rounded-full bg-emerald-500/50" />
              <motion.div animate={{ scale: [1, 1.2, 1] }} transition={{ repeat: Infinity, duration: 1, delay: 0.2 }} className="w-2 h-2 rounded-full bg-emerald-500/80" />
              <motion.div animate={{ scale: [1, 1.2, 1] }} transition={{ repeat: Infinity, duration: 1, delay: 0.4 }} className="w-2 h-2 rounded-full bg-emerald-500/50" />
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
