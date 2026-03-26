import { useState } from 'react';
import toast from 'react-hot-toast';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function EmployeeDashboard() {
  const navigate = useNavigate();
  const { logout, roles, switchRole, completeOwnerProfile } = useAuth();
  const [form, setForm] = useState({
    storeName: '',
    phone: '',
    fullName: '',
  });
  const [submitting, setSubmitting] = useState(false);
  const canSwitchToOwner = roles.includes('OWNER');
  const canViewAdmin = roles.includes('ADMIN');
  const needsOwnerProfile = !roles.includes('OWNER') && !roles.includes('ADMIN');

  async function onLogout() {
    await logout();
    navigate('/login', { replace: true });
  }

  async function onBecomeOwner(event) {
    event.preventDefault();
    setSubmitting(true);
    try {
      await completeOwnerProfile(form);
      switchRole('OWNER');
      toast.success('Owner profile completed successfully');
      navigate('/owner', { replace: true });
    } catch (error) {
      const status = error.response?.status;
      const backendMessage = String(error.response?.data?.message || '').toLowerCase();

      if (status === 409 && backendMessage.includes('phone')) {
        toast.error('This phone number is already taken. Please use another phone number.');
      } else {
        toast.error(error.response?.data?.message || 'Could not complete owner profile');
      }
    } finally {
      setSubmitting(false);
    }
  }

  function updateField(key, value) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 p-8">
      <div className="flex items-center justify-between gap-4">
        <h1 className="text-3xl font-semibold">Employee Dashboard</h1>
        <div className="flex items-center gap-2">
          {canSwitchToOwner && (
            <button
              type="button"
              onClick={() => {
                switchRole('OWNER');
                navigate('/owner');
              }}
              className="rounded-md border border-blue-500/60 px-4 py-2 text-sm font-medium text-blue-300 hover:border-blue-400"
            >
              Switch to Owner
            </button>
          )}
          {canViewAdmin && (
            <button
              type="button"
              onClick={() => {
                switchRole('ADMIN');
                navigate('/admin');
              }}
              className="rounded-md border border-amber-500/60 px-4 py-2 text-sm font-medium text-amber-300 hover:border-amber-400"
            >
              Admin View
            </button>
          )}
          <button
            type="button"
            onClick={onLogout}
            className="rounded-md border border-slate-600 px-4 py-2 text-sm font-medium hover:border-slate-400"
          >
            Logout
          </button>
        </div>
      </div>

      <p className="mt-2 text-slate-300">You are logged in as employee.</p>

      {needsOwnerProfile && (
        <div className="mt-8 max-w-xl rounded-lg border border-slate-700 bg-slate-900 p-5">
          <h2 className="text-xl font-semibold">Become a Store Owner</h2>
          <p className="mt-1 text-sm text-slate-300">Complete these fields to get OWNER + EMPLOYEE roles.</p>

          <form className="mt-5 space-y-3" onSubmit={onBecomeOwner}>
            <input
              className="w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2"
              placeholder="Store name"
              value={form.storeName}
              onChange={(event) => updateField('storeName', event.target.value)}
              required
            />
            <input
              className="w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2"
              placeholder="Phone number"
              value={form.phone}
              onChange={(event) => updateField('phone', event.target.value)}
              required
            />
            <input
              className="w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2"
              placeholder="Full name"
              value={form.fullName}
              onChange={(event) => updateField('fullName', event.target.value)}
              required
            />

            <button
              className="w-full rounded-md bg-blue-600 px-3 py-2 font-medium hover:bg-blue-500 disabled:opacity-60"
              type="submit"
              disabled={submitting}
            >
              {submitting ? 'Submitting...' : 'Become Owner'}
            </button>
          </form>
        </div>
      )}
    </div>
  );
}
