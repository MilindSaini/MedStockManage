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

      <div className="mt-8 grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
        <button
          type="button"
          className="rounded-lg border border-slate-700 bg-slate-900 p-4 text-left hover:border-slate-500"
          onClick={() => navigate('/admin/users')}
        >
          <div className="font-medium">Users</div>
          <div className="text-xs text-slate-400">Read-only paginated user directory</div>
        </button>
        <button
          type="button"
          className="rounded-lg border border-slate-700 bg-slate-900 p-4 text-left hover:border-slate-500"
          onClick={() => navigate('/admin/stores')}
        >
          <div className="font-medium">Stores</div>
          <div className="text-xs text-slate-400">Manage per-store alert schedules</div>
        </button>
        <button
          type="button"
          className="rounded-lg border border-slate-700 bg-slate-900 p-4 text-left hover:border-slate-500"
          onClick={() => navigate('/admin/activity')}
        >
          <div className="font-medium">Activity</div>
          <div className="text-xs text-slate-400">Platform-wide write action history</div>
        </button>
      </div>
    </div>
  );
}
