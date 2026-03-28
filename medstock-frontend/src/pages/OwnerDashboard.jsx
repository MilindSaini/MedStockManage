import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function OwnerDashboard() {
  const navigate = useNavigate();
  const { logout, roles, switchRole } = useAuth();
  const canSwitchToEmployee = roles.includes('EMPLOYEE');
  const canViewAdmin = roles.includes('ADMIN');
  const [joinAlertMessage, setJoinAlertMessage] = useState('');

  useEffect(() => {
    const message = sessionStorage.getItem('ownerEmployeeJoinAlert') || '';
    setJoinAlertMessage(message);
  }, []);

  async function onLogout() {
    await logout();
    navigate('/login', { replace: true });
  }

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 p-8">
      <div className="flex items-center justify-between gap-4">
        <h1 className="text-3xl font-semibold">Owner Dashboard</h1>
        <div className="flex items-center gap-2">
          {canSwitchToEmployee && (
            <button
              type="button"
              onClick={() => {
                switchRole('EMPLOYEE');
                navigate('/employee');
              }}
              className="rounded-md border border-emerald-500/60 px-4 py-2 text-sm font-medium text-emerald-300 hover:border-emerald-400"
            >
              Switch to Employee
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

      <div className="mt-8 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <button
          type="button"
          onClick={() => navigate('/inventory')}
          className="rounded-lg border border-slate-700 bg-slate-900 p-4 text-left hover:border-slate-500"
        >
          <div className="font-medium">Inventory</div>
          <div className="text-xs text-slate-400">Manage medicines and stock actions</div>
        </button>
        <button
          type="button"
          onClick={() => navigate('/alerts')}
          className="rounded-lg border border-slate-700 bg-slate-900 p-4 text-left hover:border-slate-500"
        >
          <div className="font-medium">Alerts</div>
          <div className="text-xs text-slate-400">Expiry and low-stock grouped views</div>
        </button>
        <button
          type="button"
          onClick={() => navigate('/out-of-stock')}
          className="rounded-lg border border-slate-700 bg-slate-900 p-4 text-left hover:border-slate-500"
        >
          <div className="font-medium">Out of Stock</div>
          <div className="text-xs text-slate-400">Restock or remove zero-stock medicines</div>
        </button>
        <button
          type="button"
          onClick={() => navigate('/employees')}
          className="rounded-lg border border-slate-700 bg-slate-900 p-4 text-left hover:border-slate-500"
        >
          <div className="font-medium">Employees</div>
          <div className="text-xs text-slate-400">Add employees and set permissions</div>
        </button>
      </div>

      {joinAlertMessage && (
        <div className="mt-6 rounded-lg border border-amber-500/40 bg-amber-500/10 p-4 text-amber-100">
          <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
            <p className="text-sm font-medium">{joinAlertMessage}</p>
            <button
              type="button"
              className="rounded border border-amber-400/60 px-3 py-1 text-xs hover:border-amber-300"
              onClick={() => {
                sessionStorage.removeItem('ownerEmployeeJoinAlert');
                setJoinAlertMessage('');
              }}
            >
              Dismiss
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
