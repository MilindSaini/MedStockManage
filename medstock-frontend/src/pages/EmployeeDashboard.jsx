import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { useNavigate } from 'react-router-dom';
import axiosInstance from '../api/axiosInstance';
import { useAuth } from '../context/AuthContext';

export default function EmployeeDashboard() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { logout, roles, switchRole, completeOwnerProfile, refreshMe } = useAuth();
  const [form, setForm] = useState({
    storeName: '',
    storeAddress: '',
  });
  const [submitting, setSubmitting] = useState(false);
  const canSwitchToOwner = roles.includes('OWNER');
  const canViewAdmin = roles.includes('ADMIN');
  const needsOwnerProfile = !roles.includes('OWNER') && !roles.includes('ADMIN');

  const invitationsQuery = useQuery({
    queryKey: ['employeeInvitations'],
    queryFn: async () => {
      const response = await axiosInstance.get('/api/employee-invitations/me');
      return response.data || [];
    },
    staleTime: 20_000,
  });

  const respondMutation = useMutation({
    mutationFn: async ({ invitationId, accept }) => axiosInstance.post(`/api/employee-invitations/${invitationId}/respond`, { accept }),
    onSuccess: async (_, variables) => {
      await queryClient.invalidateQueries({ queryKey: ['employeeInvitations'] });
      if (variables.accept) {
        await refreshMe();
        toast.success('Joined store successfully');
      } else {
        toast.success('Invitation declined');
      }
    },
    onError: (error) => toast.error(error.response?.data?.message || 'Could not respond to invitation'),
  });

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
      toast.error(error.response?.data?.message || 'Could not complete owner profile');
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

      {(invitationsQuery.data || []).length > 0 && (
        <div className="mt-6 space-y-3">
          {(invitationsQuery.data || []).map((invitation) => (
            <div key={invitation.id} className="rounded-lg border border-blue-500/40 bg-blue-900/20 p-4">
              <div className="text-sm text-blue-100">
                You received a request to join {invitation.storeName || 'a store'}. Do you want to join?
              </div>
              <div className="mt-3 flex gap-2">
                <button
                  type="button"
                  className="rounded-md bg-emerald-600 px-3 py-1 text-sm font-medium hover:bg-emerald-500 disabled:opacity-60"
                  onClick={() => respondMutation.mutate({ invitationId: invitation.id, accept: true })}
                  disabled={respondMutation.isPending}
                >
                  Join
                </button>
                <button
                  type="button"
                  className="rounded-md border border-rose-500/60 px-3 py-1 text-sm text-rose-200 hover:border-rose-400 disabled:opacity-60"
                  onClick={() => respondMutation.mutate({ invitationId: invitation.id, accept: false })}
                  disabled={respondMutation.isPending}
                >
                  Decline
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      <div className="mt-8 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        <button
          type="button"
          onClick={() => navigate('/inventory')}
          className="rounded-lg border border-slate-700 bg-slate-900 p-4 text-left hover:border-slate-500"
        >
          <div className="font-medium">Inventory</div>
          <div className="text-xs text-slate-400">Search medicines and adjust stock</div>
        </button>
        <button
          type="button"
          onClick={() => navigate('/alerts')}
          className="rounded-lg border border-slate-700 bg-slate-900 p-4 text-left hover:border-slate-500"
        >
          <div className="font-medium">Alerts</div>
          <div className="text-xs text-slate-400">See expiry and low-stock alerts</div>
        </button>
        <button
          type="button"
          onClick={() => navigate('/out-of-stock')}
          className="rounded-lg border border-slate-700 bg-slate-900 p-4 text-left hover:border-slate-500"
        >
          <div className="font-medium">Out of Stock</div>
          <div className="text-xs text-slate-400">Restock depleted medicines</div>
        </button>
      </div>

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
              placeholder="Store address"
              value={form.storeAddress}
              onChange={(event) => updateField('storeAddress', event.target.value)}
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
