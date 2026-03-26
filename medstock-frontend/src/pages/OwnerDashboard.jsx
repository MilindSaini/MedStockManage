import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function OwnerDashboard() {
  const navigate = useNavigate();
  const { logout, roles, switchRole } = useAuth();
  const canSwitchToEmployee = roles.includes('EMPLOYEE');
  const canViewAdmin = roles.includes('ADMIN');

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
      <p className="mt-2 text-slate-300">Phase 2 complete: authenticated owner route.</p>
    </div>
  );
}
