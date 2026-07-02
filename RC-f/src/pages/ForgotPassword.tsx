import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { authApi } from '../api/auth';
import { toast } from 'sonner';
import { Loader2, ArrowLeft, Mail } from 'lucide-react';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { motion } from 'framer-motion';

const schema = z.object({
  email: z.string().email('Please enter a valid email address'),
});

type FormData = z.infer<typeof schema>;

export function ForgotPassword() {
  const navigate = useNavigate();
  const { setForgotPasswordEmail } = useAuthStore();
  const [loading, setLoading] = useState(false);

  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema)
  });

  const onSubmit = async (data: FormData) => {
    setLoading(true);
    try {
      await authApi.requestPasswordReset({ email: data.email });
      setForgotPasswordEmail(data.email);
      toast.success('OTP sent to your email.');
      navigate('/reset-password');
    } catch (error: any) {
      toast.error(error.message || 'Failed to send OTP');
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
          <Link to="/login" className="inline-flex items-center text-sm text-muted-foreground hover:text-foreground transition-colors mb-6 group">
            <ArrowLeft className="w-4 h-4 mr-2 group-hover:-translate-x-1 transition-transform" />
            Back to login
          </Link>
          
          <div className="mb-8">
            <h1 className="text-3xl font-bold tracking-tight mb-3">Reset Password</h1>
            <p className="text-muted-foreground text-sm">
              Enter your email address and we'll send you an OTP to reset your password.
            </p>
          </div>

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
            <div className="space-y-2">
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                <Input 
                  {...register('email')}
                  type="email" 
                  placeholder="Email address" 
                  className="pl-10 bg-background/50 border-border/50 h-12 focus-visible:ring-primary"
                />
              </div>
              {errors.email && (
                <p className="text-destructive text-sm font-medium pl-1 animate-in slide-in-from-top-1">
                  {errors.email.message}
                </p>
              )}
            </div>

            <Button 
              type="submit" 
              disabled={loading}
              className="w-full bg-primary hover:bg-primary/90 text-primary-foreground font-medium h-12 rounded-xl text-base transition-all shadow-md"
            >
              {loading ? <Loader2 className="w-5 h-5 animate-spin" /> : 'Send Reset Code'}
            </Button>
          </form>
        </div>
      </motion.div>
    </div>
  );
}
