import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function AdminDashboard() {
  const navigate = useNavigate();
  const { logout, switchRole, roles } = useAuth();

  async function onLogout() {
    await logout();
    navigate('/login', { replace: true });
  }

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 p-8">
      <div className="flex items-center justify-between gap-4">
        <h1 className="text-3xl font-semibold">Admin Dashboard</h1>
        <div className="flex items-center gap-2">
          {roles.includes('EMPLOYEE') && (
            <button
              type="button"
              onClick={() => {
                switchRole('EMPLOYEE');
                navigate('/employee');
              }}
              className="rounded-md border border-emerald-500/60 px-4 py-2 text-sm font-medium text-emerald-300 hover:border-emerald-400"
            >
              Employee View
            </button>
          )}
          {roles.includes('OWNER') && (
            <button
              type="button"
              onClick={() => {
                switchRole('OWNER');
                navigate('/owner');
              }}
              className="rounded-md border border-blue-500/60 px-4 py-2 text-sm font-medium text-blue-300 hover:border-blue-400"
            >
              Owner View
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
      <p className="mt-2 text-slate-300">Admin can access all dashboards and permissions.</p>

      <div className="mt-8 rounded-xl border border-slate-800 bg-slate-900/70 p-6">
        <h2 className="text-xl font-semibold">Manage Other Stores</h2>
        <p className="mt-2 text-sm text-slate-300">
          Open a dedicated stores dashboard to see all stores and control their alert schedules.
        </p>
        <button
          type="button"
          className="mt-4 rounded-md border border-cyan-500/60 bg-cyan-500/20 px-4 py-2 text-sm font-medium text-cyan-200 hover:bg-cyan-500/30"
          onClick={() => navigate('/admin/stores')}
        >
          Open Admin Stores Dashboard
        </button>
      </div>
    </div>
  );
}
