import { useState } from 'react';
import toast from 'react-hot-toast';
import { useAuth } from '../context/AuthContext';

export default function ProfilePage() {
  const { user, updateProfile } = useAuth();
  const isOwner = user?.role === 'OWNER';
  const [form, setForm] = useState({
    storeName: user?.storeName || '',
    fullName: user?.fullName || '',
    phone: user?.phone || '',
  });
  const [saving, setSaving] = useState(false);

  function updateField(key, value) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  async function onSubmit(event) {
    event.preventDefault();
    setSaving(true);
    try {
      const payload = {
        fullName: form.fullName,
        phone: form.phone,
      };
      if (isOwner) {
        payload.storeName = form.storeName;
      }

      await updateProfile(payload);
      toast.success('Profile updated');
    } catch (error) {
      toast.error(error.response?.data?.message || 'Could not update profile');
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="min-h-screen bg-slate-950 p-6 text-slate-100 md:p-8">
      <div className="mx-auto max-w-2xl rounded-xl border border-slate-800 bg-slate-900/70 p-5">
        <h1 className="text-3xl font-semibold">Profile</h1>
        <p className="mt-1 text-sm text-slate-300">Username and email are fixed. You can update other details.</p>

        <form className="mt-6 space-y-3" onSubmit={onSubmit}>
          <div>
            <label className="mb-1 block text-sm text-slate-300">Username</label>
            <input
              className="w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2 text-slate-400"
              value={user?.username || ''}
              readOnly
            />
          </div>

          <div>
            <label className="mb-1 block text-sm text-slate-300">Email</label>
            <input
              className="w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2 text-slate-400"
              value={user?.email || ''}
              readOnly
            />
          </div>

          <div>
            <label className="mb-1 block text-sm text-slate-300">Store name</label>
            <input
              className={`w-full rounded-md border px-3 py-2 ${isOwner ? 'border-slate-700 bg-slate-950' : 'border-slate-700 bg-slate-950 text-slate-400'}`}
              placeholder="Store name"
              value={form.storeName}
              onChange={(event) => updateField('storeName', event.target.value)}
              readOnly={!isOwner}
            />
            {!isOwner && <p className="mt-1 text-xs text-slate-400">Assigned store name is read-only for employees.</p>}
          </div>

          <div>
            <label className="mb-1 block text-sm text-slate-300">Full name</label>
            <input
              className="w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2"
              placeholder="Your full name"
              value={form.fullName}
              onChange={(event) => updateField('fullName', event.target.value)}
            />
          </div>

          <div>
            <label className="mb-1 block text-sm text-slate-300">Phone</label>
            <input
              className="w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2"
              placeholder="Your phone number"
              value={form.phone}
              onChange={(event) => updateField('phone', event.target.value)}
            />
          </div>

          <button
            type="submit"
            className="w-full rounded-md bg-blue-600 px-4 py-2 font-medium hover:bg-blue-500 disabled:opacity-60"
            disabled={saving}
          >
            {saving ? 'Saving...' : 'Save Profile'}
          </button>
        </form>
      </div>
    </div>
  );
}
