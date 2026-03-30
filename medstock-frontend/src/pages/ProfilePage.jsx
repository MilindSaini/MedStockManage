import { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import axiosInstance from '../api/axiosInstance';
import { useAuth } from '../context/AuthContext';

export default function ProfilePage() {
  const { user, updateProfile, refreshMe } = useAuth();
  const isOwner = user?.role === 'OWNER';
  const [form, setForm] = useState({
    storeName: user?.storeName || '',
    fullName: user?.fullName || '',
    phone: user?.phone || '',
  });
  const [saving, setSaving] = useState(false);
  const [sendingOtp, setSendingOtp] = useState(false);
  const [verifyingOtp, setVerifyingOtp] = useState(false);
  const [otp, setOtp] = useState('');
  const [otpStatus, setOtpStatus] = useState(null);

  useEffect(() => {
    setForm({
      storeName: user?.storeName || '',
      fullName: user?.fullName || '',
      phone: user?.phone || '',
    });
  }, [user]);

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

  async function onSendOtp() {
    setSendingOtp(true);
    setOtpStatus(null);
    try {
      const response = await axiosInstance.post('/api/auth/phone/send-otp', {
        phone: form.phone,
      });

      if (response.data?.accepted === true) {
        toast.success('OTP sent to phone');
        setOtpStatus({ ok: true, message: `OTP sent to ${response.data?.to || 'your phone'}` });
      } else {
        const reason = response.data?.reason || 'unknown reason';
        toast.error(`OTP not sent: ${reason}`);
        setOtpStatus({ ok: false, message: `OTP not sent: ${reason}` });
      }
    } catch (error) {
      const message = error.response?.data?.message || 'Could not send OTP';
      toast.error(message);
      setOtpStatus({ ok: false, message });
    } finally {
      setSendingOtp(false);
    }
  }

  async function onVerifyOtp() {
    if (!otp.trim()) {
      toast.error('Enter OTP first');
      return;
    }

    setVerifyingOtp(true);
    try {
      await axiosInstance.post('/api/auth/phone/verify-otp', { otp });
      await refreshMe();
      toast.success('Phone number verified');
      setOtp('');
      setOtpStatus({ ok: true, message: 'Phone verified successfully' });
    } catch (error) {
      const message = error.response?.data?.message || 'Could not verify OTP';
      toast.error(message);
      setOtpStatus({ ok: false, message });
    } finally {
      setVerifyingOtp(false);
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
            <p className={`mt-1 text-xs ${user?.phoneVerified ? 'text-emerald-300' : 'text-amber-300'}`}>
              {user?.phoneVerified ? 'Phone is verified' : 'Phone is not verified'}
            </p>
          </div>

          <button
            type="submit"
            className="w-full rounded-md bg-blue-600 px-4 py-2 font-medium hover:bg-blue-500 disabled:opacity-60"
            disabled={saving}
          >
            {saving ? 'Saving...' : 'Save Profile'}
          </button>

          <div className="rounded-md border border-slate-700 bg-slate-950/50 p-3">
            <div className="mb-2 text-sm font-medium text-slate-200">Phone Verification</div>
            <div className="flex flex-col gap-2 md:flex-row">
              <button
                type="button"
                className="rounded-md border border-cyan-500/60 bg-cyan-500/15 px-4 py-2 font-medium text-cyan-200 hover:bg-cyan-500/25 disabled:opacity-60"
                disabled={sendingOtp}
                onClick={onSendOtp}
              >
                {sendingOtp ? 'Sending OTP...' : 'Send OTP'}
              </button>
              <input
                className="flex-1 rounded-md border border-slate-700 bg-slate-950 px-3 py-2"
                placeholder="Enter OTP"
                value={otp}
                onChange={(event) => setOtp(event.target.value)}
              />
              <button
                type="button"
                className="rounded-md border border-emerald-500/60 bg-emerald-500/15 px-4 py-2 font-medium text-emerald-200 hover:bg-emerald-500/25 disabled:opacity-60"
                disabled={verifyingOtp}
                onClick={onVerifyOtp}
              >
                {verifyingOtp ? 'Verifying...' : 'Verify OTP'}
              </button>
            </div>

            {otpStatus && (
              <div className={`mt-2 rounded-md border px-3 py-2 text-sm ${otpStatus.ok ? 'border-emerald-500/50 bg-emerald-500/10 text-emerald-300' : 'border-rose-500/50 bg-rose-500/10 text-rose-300'}`}>
                {otpStatus.message}
              </div>
            )}
          </div>

          {!user?.phoneVerified && (
            <div className="rounded-md border border-amber-500/40 bg-amber-500/10 px-3 py-2 text-sm text-amber-200">
              Verify phone to enable OTP-based phone verification.
            </div>
          )}
        </form>
      </div>
    </div>
  );
}
