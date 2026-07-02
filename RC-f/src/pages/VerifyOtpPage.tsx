import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { authApi } from '../api/auth';
import { toast } from 'sonner';
import { Loader2 } from 'lucide-react';
import { Button } from '../components/ui/button';
import {
  InputOTP,
  InputOTPGroup,
  InputOTPSlot,
} from '../components/ui/input-otp';
import { motion } from 'framer-motion';

export function VerifyOtpPage() {
  const { pendingVerificationEmail, pendingRegistration, clearTempEmails } = useAuthStore();
  const navigate = useNavigate();
  const [otp, setOtp] = useState('');
  const [loading, setLoading] = useState(false);
  const [resendLoading, setResendLoading] = useState(false);
  const [timeLeft, setTimeLeft] = useState(60);

  useEffect(() => {
    if (!pendingVerificationEmail || !pendingRegistration) {
      toast.error('Registration session expired');
      navigate('/register');
    }
  }, [pendingVerificationEmail, pendingRegistration, navigate]);

  useEffect(() => {
    if (timeLeft > 0) {
      const timerId = setTimeout(() => setTimeLeft(timeLeft - 1), 1000);
      return () => clearTimeout(timerId);
    }
  }, [timeLeft]);

  const handleVerify = async (e: React.FormEvent) => {
    e.preventDefault();
    if (otp.length !== 6) return;

    setLoading(true);
    try {
      await authApi.verifyEmail({ email: pendingVerificationEmail!, otp });
      toast.success('Email verified successfully! You can now log in.');
      clearTempEmails();
      navigate('/login');
    } catch (error: any) {
      toast.error(error.message || 'Verification failed');
    } finally {
      setLoading(false);
    }
  };

  const handleResend = async () => {
    if (timeLeft > 0) return;
    
    if (!pendingRegistration) {
      toast.error('Registration session expired');
      navigate('/register');
      return;
    }

    setResendLoading(true);
    try {
      await authApi.register(pendingRegistration as any);
      toast.success('Verification code sent again');
      setTimeLeft(60);
    } catch (error: any) {
      toast.error(error.message || 'Failed to resend OTP');
    } finally {
      setResendLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-background text-foreground p-4">
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute top-1/4 -right-1/4 w-[800px] h-[800px] bg-primary/10 rounded-full blur-[120px] opacity-40 mix-blend-screen" />
        <div className="absolute -bottom-1/4 -left-1/4 w-[800px] h-[800px] bg-accent/10 rounded-full blur-[120px] opacity-40 mix-blend-screen" />
      </div>

      <motion.div 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, ease: "easeOut" }}
        className="w-full max-w-md relative z-10"
      >
        <div className="glass-panel p-8 sm:p-10 rounded-[2.5rem]">
          <div className="text-center mb-10">
            <h1 className="text-3xl font-bold tracking-tight mb-3">Verify Email</h1>
            <p className="text-muted-foreground text-sm">
              We've sent a 6-digit code to <br/>
              <span className="text-foreground font-medium">{pendingVerificationEmail}</span>
            </p>
          </div>

          <form onSubmit={handleVerify} className="space-y-8">
            <div className="flex justify-center">
              <InputOTP 
                maxLength={6} 
                value={otp} 
                onChange={setOtp}
                disabled={loading}
                autoFocus
              >
                <InputOTPGroup className="gap-2">
                  <InputOTPSlot index={0} className="w-12 h-14 text-lg rounded-xl border-border/50 bg-background/50" />
                  <InputOTPSlot index={1} className="w-12 h-14 text-lg rounded-xl border-border/50 bg-background/50" />
                  <InputOTPSlot index={2} className="w-12 h-14 text-lg rounded-xl border-border/50 bg-background/50" />
                  <InputOTPSlot index={3} className="w-12 h-14 text-lg rounded-xl border-border/50 bg-background/50" />
                  <InputOTPSlot index={4} className="w-12 h-14 text-lg rounded-xl border-border/50 bg-background/50" />
                  <InputOTPSlot index={5} className="w-12 h-14 text-lg rounded-xl border-border/50 bg-background/50" />
                </InputOTPGroup>
              </InputOTP>
            </div>

            <Button 
              type="submit" 
              disabled={otp.length !== 6 || loading}
              className="w-full bg-primary hover:bg-primary/90 text-primary-foreground font-medium h-12 rounded-xl text-base transition-all shadow-md"
            >
              {loading ? <Loader2 className="w-5 h-5 animate-spin" /> : 'Verify Code'}
            </Button>
          </form>

          <div className="mt-8 text-center text-sm text-muted-foreground">
            Didn't receive the code?{' '}
            {timeLeft > 0 ? (
              <span className="text-foreground">Resend in {timeLeft}s</span>
            ) : (
              <button 
                onClick={handleResend}
                disabled={resendLoading}
                className="text-primary hover:text-primary/80 font-medium transition-colors disabled:opacity-50"
              >
                {resendLoading ? 'Sending...' : 'Resend OTP'}
              </button>
            )}
          </div>
        </div>
      </motion.div>
    </div>
  );
}
