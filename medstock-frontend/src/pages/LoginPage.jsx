import { useEffect, useRef, useState } from 'react';
import toast from 'react-hot-toast';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { API_BASE_URL } from '../config/env';
import { defaultHomeByRoles, normalizeRoles } from '../utils/roleRouting';

export default function LoginPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { login, completeOAuthLogin, user, isAuthenticated } = useAuth();
  const [tab, setTab] = useState('password');
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const exchangedOAuthCodeRef = useRef(null);

  useEffect(() => {
    if (isAuthenticated && user) {
      navigate(defaultHomeByRoles(normalizeRoles(user)), { replace: true });
    }
  }, [isAuthenticated, user, navigate]);

  useEffect(() => {
    const code = searchParams.get('code');
    if (!code || exchangedOAuthCodeRef.current === code) {
      return;
    }
    exchangedOAuthCodeRef.current = code;

    (async () => {
      try {
        const response = await completeOAuthLogin(code);
        window.history.replaceState({}, document.title, window.location.pathname);
        navigate(defaultHomeByRoles(normalizeRoles(response.user)), { replace: true });
        toast.success('Google login successful');
      } catch {
        exchangedOAuthCodeRef.current = null;
        toast.error('Google login failed');
      }
    })();
  }, [searchParams, completeOAuthLogin]);

  async function onSubmit(event) {
    event.preventDefault();
    setLoading(true);
    try {
      const response = await login(identifier, password);
      toast.success('Welcome back');
      navigate(defaultHomeByRoles(normalizeRoles(response.user)), { replace: true });
    } catch (error) {
      toast.error(error.response?.data?.message || 'Invalid credentials');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 px-4 py-10">
      <div className="mx-auto max-w-md rounded-xl border border-slate-700 bg-slate-900 p-6">
        <h1 className="text-2xl font-semibold">Login</h1>
        <p className="mt-1 text-sm text-slate-300">Use username or email with password.</p>

        <div className="mt-5 grid grid-cols-2 gap-2 rounded-lg border border-slate-700 p-1">
          <button
            type="button"
            onClick={() => setTab('password')}
            className={`rounded-md px-3 py-2 text-sm ${tab === 'password' ? 'bg-blue-600 text-white' : 'text-slate-300'}`}
          >
            Password
          </button>
          <button
            type="button"
            onClick={() => setTab('google')}
            className={`rounded-md px-3 py-2 text-sm ${tab === 'google' ? 'bg-blue-600 text-white' : 'text-slate-300'}`}
          >
            Google
          </button>
        </div>

        {tab === 'password' ? (
          <form className="mt-6 space-y-4" onSubmit={onSubmit}>
            <div>
              <label className="mb-1 block text-sm">Username or Email</label>
              <input
                className="w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2"
                value={identifier}
                onChange={(e) => setIdentifier(e.target.value)}
                required
              />
            </div>
            <div>
              <label className="mb-1 block text-sm">Password</label>
              <input
                className="w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>
            <button
              className="w-full rounded-md bg-blue-600 px-3 py-2 font-medium hover:bg-blue-500 disabled:opacity-60"
              type="submit"
              disabled={loading}
            >
              {loading ? 'Signing in...' : 'Sign In'}
            </button>
          </form>
        ) : (
          <div className="mt-6">
            <a
              href={`${API_BASE_URL}/oauth2/authorization/google`}
              className="block w-full rounded-md border border-slate-600 px-3 py-2 text-center font-medium hover:border-slate-400"
            >
              Continue with Google
            </a>
          </div>
        )}

        <p className="mt-4 text-sm text-slate-300">
          New store owner?{' '}
          <Link to="/register" className="text-blue-400 hover:text-blue-300">
            Create account
          </Link>
        </p>
      </div>
    </div>
  );
}
