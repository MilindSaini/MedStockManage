import { useState } from 'react';
import toast from 'react-hot-toast';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { defaultHomeByRoles, normalizeRoles } from '../utils/roleRouting';

export default function RegisterPage() {
  const navigate = useNavigate();
  const { register } = useAuth();
  const [form, setForm] = useState({
    username: '',
    email: '',
    password: '',
  });
  const [loading, setLoading] = useState(false);

  function updateField(key, value) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  async function onSubmit(event) {
    event.preventDefault();
    setLoading(true);
    try {
      const response = await register(form);
      toast.success('Account created successfully');
      navigate(defaultHomeByRoles(normalizeRoles(response.user)), { replace: true });
    } catch (error) {
      const status = error.response?.status;
      const backendMessage = String(error.response?.data?.message || '').toLowerCase();

      if (status === 409) {
        if (backendMessage.includes('email')) {
          toast.error('This email is already taken. Please use another email.');
        } else if (backendMessage.includes('username')) {
          toast.error('This username is already taken. Please choose another username.');
        } else {
          toast.error('Email or username is already taken.');
        }
      } else {
        toast.error(error.response?.data?.message || 'Could not create account');
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 px-4 py-10">
      <div className="mx-auto max-w-md rounded-xl border border-slate-700 bg-slate-900 p-6">
        <h1 className="text-2xl font-semibold">Create Account</h1>
        <p className="mt-1 text-sm text-slate-300">Sign up as an employee. You can upgrade to owner later.</p>

        <form className="mt-6 space-y-4" onSubmit={onSubmit}>
          <div>
            <label className="mb-1 block text-sm">Username</label>
            <input
              className="w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2"
              placeholder="e.g. john_store_01"
              value={form.username}
              onChange={(e) => updateField('username', e.target.value)}
              minLength={3}
              maxLength={60}
              pattern="[A-Za-z0-9_]+"
              title="Username must be 3-60 characters and can contain letters, numbers, and underscore only."
              autoComplete="username"
              required
            />
            <p className="mt-1 text-xs text-slate-400">Use 3-60 characters: letters, numbers, and underscore only.</p>
          </div>

          <div>
            <label className="mb-1 block text-sm">Email</label>
            <input
              className="w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2"
              type="email"
              placeholder="you@example.com"
              value={form.email}
              onChange={(e) => updateField('email', e.target.value)}
              maxLength={180}
              autoComplete="email"
              required
            />
            <p className="mt-1 text-xs text-slate-400">Enter a valid and unique email address.</p>
          </div>

          <div>
            <label className="mb-1 block text-sm">Password</label>
            <input
              className="w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2"
              type="password"
              placeholder="Create a strong password"
              value={form.password}
              onChange={(e) => updateField('password', e.target.value)}
              minLength={8}
              maxLength={128}
              pattern="(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,128}"
              title="Password must be 8-128 characters and include at least one uppercase letter, one lowercase letter, and one number."
              autoComplete="new-password"
              required
            />
            <p className="mt-1 text-xs text-slate-400">Must include at least 8 characters, one uppercase, one lowercase, and one number.</p>
          </div>

          <button
            className="w-full rounded-md bg-blue-600 px-3 py-2 font-medium hover:bg-blue-500 disabled:opacity-60"
            type="submit"
            disabled={loading}
          >
            {loading ? 'Creating...' : 'Create Account'}
          </button>
        </form>

        <p className="mt-4 text-sm text-slate-300">
          Already have an account?{' '}
          <Link to="/login" className="text-blue-400 hover:text-blue-300">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
}
