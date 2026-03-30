import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const NAV_ITEMS = [
  { to: '/owner', label: 'Owner Dashboard', roles: ['OWNER'] },
  { to: '/employee', label: 'Employee Dashboard', roles: ['EMPLOYEE'] },
  { to: '/admin', label: 'Admin Dashboard', roles: ['ADMIN'] },
  { to: '/admin/stores', label: 'Admin Stores', roles: ['ADMIN'] },
  { to: '/inventory', label: 'Inventory', roles: ['OWNER', 'EMPLOYEE'] },
  { to: '/inventory/add', label: 'Add Medicine', roles: ['OWNER', 'EMPLOYEE'] },
  { to: '/alerts', label: 'Alerts', roles: ['OWNER', 'EMPLOYEE'] },
  { to: '/out-of-stock', label: 'Out Of Stock', roles: ['OWNER', 'EMPLOYEE'] },
  { to: '/employees', label: 'Employees', roles: ['OWNER'] },
  { to: '/profile', label: 'Profile', roles: ['OWNER', 'EMPLOYEE', 'ADMIN'] },
];

function canAccess(itemRoles, roles) {
  return itemRoles.some((role) => roles.includes(role));
}

function homeRouteForRole(role) {
  if (role === 'OWNER') return '/owner';
  if (role === 'EMPLOYEE') return '/employee';
  if (role === 'ADMIN') return '/admin';
  return '/login';
}

export default function AppNavbar() {
  const navigate = useNavigate();
  const { role, roles = [] } = useAuth();

  const visibleItems = NAV_ITEMS.filter((item) => canAccess(item.roles, roles));

  return (
    <header className="sticky top-0 z-40 border-b border-slate-800 bg-slate-950/95 backdrop-blur">
      <div className="mx-auto flex max-w-7xl flex-wrap items-center gap-2 px-4 py-3 md:px-8">
        <button
          type="button"
          onClick={() => navigate(-1)}
          className="rounded-md border border-slate-700 px-3 py-1.5 text-sm text-slate-200 hover:border-slate-500"
        >
          Back
        </button>
        <button
          type="button"
          onClick={() => navigate(homeRouteForRole(role), { replace: true })}
          className="rounded-md bg-emerald-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-emerald-500"
        >
          Home
        </button>

        <nav className="flex flex-wrap items-center gap-2">
          {visibleItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => (
                `rounded-md px-3 py-1.5 text-sm ${isActive ? 'bg-slate-700 text-white' : 'bg-slate-900 text-slate-300 hover:bg-slate-800'}`
              )}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </div>
    </header>
  );
}
