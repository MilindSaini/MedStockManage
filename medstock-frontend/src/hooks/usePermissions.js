import { useQuery } from '@tanstack/react-query';
import axiosInstance from '../api/axiosInstance';
import { useAuth } from '../context/AuthContext';

const ALL_TRUE = {
  canAdd: true,
  canEdit: true,
  canDelete: true,
  canViewFinance: true,
  canSell: true,
};

const ALL_FALSE = {
  canAdd: false,
  canEdit: false,
  canDelete: false,
  canViewFinance: false,
  canSell: false,
};

export default function usePermissions() {
  const { roles, isAuthenticated } = useAuth();
  const isOwnerOrAdmin = roles.includes('OWNER') || roles.includes('ADMIN');

  const query = useQuery({
    queryKey: ['my-permissions'],
    queryFn: async () => {
      const response = await axiosInstance.get('/api/employees/my-permissions');
      return response.data;
    },
    enabled: isAuthenticated && !isOwnerOrAdmin,
    staleTime: 30_000,
  });

  if (!isAuthenticated) {
    return { ...ALL_FALSE, isLoading: false };
  }

  if (isOwnerOrAdmin) {
    return { ...ALL_TRUE, isLoading: false };
  }

  return {
    canAdd: Boolean(query.data?.canAdd),
    canEdit: Boolean(query.data?.canEdit),
    canDelete: Boolean(query.data?.canDelete),
    canViewFinance: Boolean(query.data?.canViewFinance),
    canSell: Boolean(query.data?.canSell),
    isLoading: query.isLoading,
  };
}
