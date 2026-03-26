import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

function dashboardByRoles(roles) {
  if (roles.includes('ADMIN')) return '/admin';
  if (roles.includes('EMPLOYEE')) return '/employee';
  if (roles.includes('OWNER')) return '/owner';
  return '/owner';
}

export default function ProtectedRoute({ children, allowedRoles }) {
  const { isAuthenticated, roles, isAuthReady } = useAuth();

  if (!isAuthReady) {
    return null;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  const hasAccess = !allowedRoles || allowedRoles.length === 0
    || roles.includes('ADMIN')
    || allowedRoles.some((allowedRole) => roles.includes(allowedRole));

  if (!hasAccess) {
    return <Navigate to={dashboardByRoles(roles)} replace />;
  }

  return children;
}
