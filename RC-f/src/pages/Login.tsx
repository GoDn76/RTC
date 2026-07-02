import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { authApi } from '../api/auth';
import { toast } from 'sonner';
import { Loader2, Mail, Lock } from 'lucide-react';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { GoogleLogin } from '@react-oauth/google';
import { motion } from 'framer-motion';

const schema = z.object({
  email: z.string().email('Please enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
});

type FormData = z.infer<typeof schema>;

export function Login() {
  const navigate = useNavigate();
  const { setAuth } = useAuthStore();
  const [loading, setLoading] = useState(false);

  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema)
  });

  const onSubmit = async (data: FormData) => {
    setLoading(true);
    try {
      const authRes = await authApi.login(data);
      localStorage.setItem('chat_token', authRes.accessToken);
      const profile = await authApi.getMe();
      
      const user = {
        id: profile.uuid,
        name: profile.name,
        email: profile.email,
        emailVerified: profile.emailVerified
      };

      setAuth(authRes.accessToken, user);
      toast.success('Logged in successfully');
      navigate('/');
    } catch (error: any) {
      toast.error(error.message || 'Failed to login');
      localStorage.removeItem('chat_token');
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleSuccess = async (credentialResponse: any) => {
    if (!credentialResponse.credential) {
      toast.error('Google Sign In failed. No credential received.');
      return;
    }
    setLoading(true);
    try {
      const authRes = await authApi.loginWithGoogle({ idToken: credentialResponse.credential });
      localStorage.setItem('chat_token', authRes.accessToken);
      const profile = await authApi.getMe();
      const user = {
        id: profile.uuid,
        name: profile.name,
        email: profile.email,
        emailVerified: profile.emailVerified
      };
      setAuth(authRes.accessToken, user);
      toast.success('Successfully logged in with Google');
      navigate('/');
    } catch (error: any) {
      toast.error(error.message || 'Failed to login with Google');
      localStorage.removeItem('chat_token');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col md:flex-row items-center justify-center bg-background text-foreground p-4 overflow-hidden relative">
      <div className="absolute inset-0 pointer-events-none">
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
          <div className="text-center mb-8">
            <h1 className="text-4xl font-bold tracking-tight mb-2 bg-gradient-to-br from-foreground to-foreground/60 bg-clip-text text-transparent">
              Welcome Back
            </h1>
            <p className="text-muted-foreground">Sign in to continue chatting</p>
          </div>

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
            <div className="space-y-1">
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                <Input 
                  {...register('email')}
                  type="email" 
                  placeholder="Email" 
                  className="pl-10 bg-background/50 border-border/50 h-12 focus-visible:ring-primary"
                />
              </div>
              {errors.email && <p className="text-destructive text-sm font-medium pl-1">{errors.email.message}</p>}
            </div>

            <div className="space-y-1">
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                <Input 
                  {...register('password')}
                  type="password" 
                  placeholder="Password" 
                  className="pl-10 bg-background/50 border-border/50 h-12 focus-visible:ring-primary"
                />
              </div>
              {errors.password && <p className="text-destructive text-sm font-medium pl-1">{errors.password.message}</p>}
              
              <div className="flex justify-end pt-1">
                <Link to="/forgot-password" className="text-sm font-medium text-muted-foreground hover:text-primary transition-colors">
                  Forgot password?
                </Link>
              </div>
            </div>

            <Button 
              type="submit" 
              disabled={loading}
              className="w-full bg-primary hover:bg-primary/90 text-primary-foreground font-medium h-12 rounded-xl text-base transition-all shadow-md"
            >
              {loading ? <Loader2 className="w-5 h-5 animate-spin" /> : 'Sign In'}
            </Button>
          </form>

          <div className="relative my-8">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-border/50" />
            </div>
            <div className="relative flex justify-center text-sm">
              <span className="px-2 glass-button text-muted-foreground rounded-full text-xs py-1">
                Or continue with
              </span>
            </div>
          </div>

          <div className="flex justify-center w-full">
            <GoogleLogin 
              onSuccess={handleGoogleSuccess} 
              onError={() => toast.error('Google Sign In failed')} 
              theme="filled_black"
              shape="pill"
            />
          </div>

          <p className="mt-8 text-center text-sm text-muted-foreground">
            Don't have an account?{' '}
            <Link to="/register" className="font-medium text-primary hover:text-primary/80 transition-colors">
              Sign up
            </Link>
          </p>
        </div>
      </motion.div>
    </div>
  );
}
