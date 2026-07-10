import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { authApi } from '../api/auth';
import { toast } from 'sonner';
import { Loader2, ArrowLeft, Lock } from 'lucide-react';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import {
  InputOTP,
  InputOTPGroup,
  InputOTPSlot,
} from '../components/ui/input-otp';
import { motion } from 'framer-motion';

const schema = z.object({
  otp: z.string().length(6, 'OTP must be exactly 6 digits'),
  newPassword: z.string().min(8, 'Password must be at least 8 characters'),
  confirmPassword: z.string()
}).refine((data) => data.newPassword === data.confirmPassword, {
  message: "Passwords don't match",
  path: ["confirmPassword"],
});

type FormData = z.infer<typeof schema>;

export function ResetPassword() {
  const navigate = useNavigate();
  const { forgotPasswordEmail, clearTempEmails } = useAuthStore();
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!forgotPasswordEmail) {
      navigate('/forgot-password');
    }
  }, [forgotPasswordEmail, navigate]);

  const { register, handleSubmit, setValue, watch, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      otp: '',
    }
  });

  const otpValue = watch('otp');

  const onSubmit = async (data: FormData) => {
    setLoading(true);
    try {
      await authApi.resetPassword({
        email: forgotPasswordEmail!,
        otp: data.otp,
        newPassword: data.newPassword
      });
      toast.success('Password reset successfully! You can now log in.');
      clearTempEmails();
      navigate('/login');
    } catch (error: any) {
      toast.error(error.message || 'Failed to reset password');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-background text-foreground p-4">
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute top-1/4 -right-1/4 w-[800px] h-[800px] bg-primary/10 rounded-full blur-[120px] opacity-40 mix-blend-screen" />
        <div className="absolute -bottom-1/4 -left-1/4 w-[600px] h-[600px] bg-accent/10 rounded-full blur-[100px] opacity-40 mix-blend-screen" />
      </div>

      <motion.div 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, ease: "easeOut" }}
        className="w-full max-w-md relative z-10"
      >
        <div className="glass-panel p-8 sm:p-10 rounded-[2.5rem]">
          <div className="text-center mb-8">
            <h1 className="text-3xl font-bold tracking-tight mb-2 bg-gradient-to-br from-foreground to-foreground/60 bg-clip-text text-transparent">
              Reset your password
            </h1>
            <p className="text-emerald-500 font-medium tracking-wide text-sm mb-2">ReChatOn Security</p>
            <p className="text-muted-foreground text-sm">
              Enter the OTP sent to <span className="text-foreground font-medium">{forgotPasswordEmail}</span> and your new password
            </p>
          </div>

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
            <div className="space-y-2 flex flex-col items-center">
              <InputOTP 
                maxLength={6} 
                value={otpValue} 
                onChange={(v) => setValue('otp', v, { shouldValidate: true })}
                disabled={loading}
              >
                <InputOTPGroup className="gap-2">
                  <InputOTPSlot index={0} className="w-10 h-12 text-lg rounded-xl border-border/50 bg-background/50" />
                  <InputOTPSlot index={1} className="w-10 h-12 text-lg rounded-xl border-border/50 bg-background/50" />
                  <InputOTPSlot index={2} className="w-10 h-12 text-lg rounded-xl border-border/50 bg-background/50" />
                  <InputOTPSlot index={3} className="w-10 h-12 text-lg rounded-xl border-border/50 bg-background/50" />
                  <InputOTPSlot index={4} className="w-10 h-12 text-lg rounded-xl border-border/50 bg-background/50" />
                  <InputOTPSlot index={5} className="w-10 h-12 text-lg rounded-xl border-border/50 bg-background/50" />
                </InputOTPGroup>
              </InputOTP>
              {errors.otp && (
                <p className="text-destructive text-sm font-medium animate-in slide-in-from-top-1">
                  {errors.otp.message}
                </p>
              )}
            </div>

            <div className="space-y-4">
              <div className="space-y-2">
                <div className="relative">
                  <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                  <Input 
                    {...register('newPassword')}
                    type="password" 
                    placeholder="New Password" 
                    className="pl-10 bg-background/50 border-border/50 h-12 focus-visible:ring-primary"
                  />
                </div>
                {errors.newPassword && (
                  <p className="text-destructive text-sm font-medium pl-1">
                    {errors.newPassword.message}
                  </p>
                )}
              </div>

              <div className="space-y-2">
                <div className="relative">
                  <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                  <Input 
                    {...register('confirmPassword')}
                    type="password" 
                    placeholder="Confirm New Password" 
                    className="pl-10 bg-background/50 border-border/50 h-12 focus-visible:ring-primary"
                  />
                </div>
                {errors.confirmPassword && (
                  <p className="text-destructive text-sm font-medium pl-1">
                    {errors.confirmPassword.message}
                  </p>
                )}
              </div>
            </div>

            <Button 
              type="submit" 
              disabled={loading}
              className="w-full bg-primary hover:bg-primary/90 text-primary-foreground font-medium h-12 rounded-xl text-base transition-all shadow-md"
            >
              {loading ? <Loader2 className="w-5 h-5 animate-spin" /> : 'Reset Password'}
            </Button>
          </form>
          
          <div className="mt-6 text-center">
             <Link to="/login" className="inline-flex items-center text-sm text-muted-foreground hover:text-foreground transition-colors group">
              <ArrowLeft className="w-4 h-4 mr-2 group-hover:-translate-x-1 transition-transform" />
              Back to login
            </Link>
          </div>
        </div>
      </motion.div>
    </div>
  );
}
