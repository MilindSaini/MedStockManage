export function normalizeRoles(user) {
  if (Array.isArray(user?.roles) && user.roles.length > 0) {
    return user.roles;
  }
  if (user?.role) {
    return [user.role];
  }
  return [];
}

export function defaultHomeByRoles(roles) {
  if (roles.includes('ADMIN')) return '/admin';
  if (roles.includes('EMPLOYEE')) return '/employee';
  if (roles.includes('OWNER')) return '/owner';
  return '/login';
}
