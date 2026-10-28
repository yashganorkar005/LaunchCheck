import React, { useState } from 'react';
import { LogIn, UserPlus } from 'lucide-react';
import { Modal } from '../ui/Modal';
import { Input } from '../ui/Input';
import { Button } from '../ui/Button';
import { api, ApiError } from '../../lib/api';
import { AuthResponse } from '../../types';

interface AuthModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (data: AuthResponse) => void;
  initialMode?: 'login' | 'register';
}

export function AuthModal({
  isOpen,
  onClose,
  onSuccess,
  initialMode = 'login'
}: AuthModalProps) {
  const [mode, setMode] = useState<'login' | 'register'>(initialMode);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      const data = mode === 'login'
        ? await api.auth.login({ email, password })
        : await api.auth.register({ email, password });

      localStorage.setItem('lc_token', data.token);
      localStorage.setItem('lc_email', data.user.email);
      onSuccess(data);
      onClose();
    } catch (err: any) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError(err.message || 'Authentication failed. Please verify credentials.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={mode === 'login' ? 'Operator Sign In' : 'Create Operator Account'}
      eyebrow="IDENTITY GATEWAY"
      maxWidth="md"
    >
      <div className="flex border border-[#25352B] mb-5 rounded-[2px] overflow-hidden">
        <button
          type="button"
          onClick={() => { setMode('login'); setError(null); }}
          className={`flex-1 py-2 text-xs font-mono uppercase tracking-wider transition-colors ${
            mode === 'login'
              ? 'bg-[#38FF8E]/10 text-[#38FF8E] font-bold border-r border-[#25352B]'
              : 'text-[#849188] hover:text-[#F4F8F4] border-r border-[#25352B]'
          }`}
        >
          Sign In
        </button>
        <button
          type="button"
          onClick={() => { setMode('register'); setError(null); }}
          className={`flex-1 py-2 text-xs font-mono uppercase tracking-wider transition-colors ${
            mode === 'register'
              ? 'bg-[#38FF8E]/10 text-[#38FF8E] font-bold'
              : 'text-[#849188] hover:text-[#F4F8F4]'
          }`}
        >
          Register
        </button>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        <Input
          label="Email Address"
          type="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="operator@company.com"
          monospace
        />
        <Input
          label="Password"
          type="password"
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="••••••••••••"
          monospace
        />

        {error && (
          <div className="p-3 bg-[#FF5A78]/10 border border-[#FF5A78]/40 text-[#FF5A78] text-xs font-mono rounded-[2px]">
            {error}
          </div>
        )}

        <div className="pt-2">
          <Button
            type="submit"
            variant="primary"
            size="md"
            isLoading={loading}
            icon={mode === 'login' ? <LogIn className="w-3.5 h-3.5" /> : <UserPlus className="w-3.5 h-3.5" />}
            className="w-full"
          >
            {mode === 'login' ? 'Authenticate' : 'Create Account'}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
