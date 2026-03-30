function normalizeRoleName(role) {
  if (typeof role !== 'string') {
    return '';
  }

  const normalized = role.trim().toUpperCase();
  if (!normalized) {
    return '';
  }

  return normalized.startsWith('ROLE_') ? normalized.slice(5) : normalized;
}

export function normalizeRoles(user) {
  if (Array.isArray(user?.roles) && user.roles.length > 0) {
    return [...new Set(user.roles.map(normalizeRoleName).filter(Boolean))];
  }
  if (user?.role) {
    const normalizedRole = normalizeRoleName(user.role);
    return normalizedRole ? [normalizedRole] : [];
  }
  return [];
}

export function defaultHomeByRoles(roles) {
  if (roles.includes('ADMIN')) return '/admin';
  if (roles.includes('EMPLOYEE')) return '/employee';
  if (roles.includes('OWNER')) return '/owner';
  return '/login';
}
