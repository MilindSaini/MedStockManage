import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import axiosInstance from '../api/axiosInstance';

function buildParams(filters = {}) {
  const params = {
    page: filters.page ?? 0,
    size: filters.size ?? 20,
    sortBy: filters.sortBy ?? 'updatedAt',
    sortDir: filters.sortDir ?? 'desc',
  };

  if (filters.search) params.search = filters.search;
  if (filters.category) params.category = filters.category;
  if (filters.expiringBefore) params.expiringBefore = filters.expiringBefore;
  if (typeof filters.outOfStock === 'boolean') params.outOfStock = filters.outOfStock;

  return params;
}

export default function useMedicines(filters = {}) {
  const queryClient = useQueryClient();
  const queryKey = ['medicines', filters];

  const medicinesQuery = useQuery({
    queryKey,
    queryFn: async () => {
      const response = await axiosInstance.get('/api/medicines', { params: buildParams(filters) });
      return response.data;
    },
  });

  const adjustStockMutation = useMutation({
    mutationFn: async ({ medicineId, delta, transactionType, notes }) => {
      const response = await axiosInstance.post('/api/stock/adjust', {
        medicineId,
        delta,
        transactionType,
        notes,
      });
      return response.data;
    },
    onMutate: async ({ medicineId, delta }) => {
      await queryClient.cancelQueries({ queryKey });
      const previous = queryClient.getQueryData(queryKey);

      queryClient.setQueryData(queryKey, (oldData) => {
        if (!oldData?.content) return oldData;
        return {
          ...oldData,
          content: oldData.content.map((medicine) => {
            if (medicine.id !== medicineId) return medicine;
            const nextAvailable = delta > 0
              ? medicine.quantityAvailable + delta
              : medicine.quantityAvailable;
            const nextSold = delta < 0
              ? medicine.quantitySold + Math.abs(delta)
              : medicine.quantitySold;
            return {
              ...medicine,
              quantityAvailable: nextAvailable,
              quantitySold: nextSold,
              currentStock: Math.max(nextAvailable - nextSold, 0),
            };
          }),
        };
      });

      return { previous };
    },
    onError: (_error, _vars, context) => {
      if (context?.previous) {
        queryClient.setQueryData(queryKey, context.previous);
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey });
      queryClient.invalidateQueries({ queryKey: ['medicines'] });
    },
  });

  const createMedicineMutation = useMutation({
    mutationFn: async (payload) => {
      const response = await axiosInstance.post('/api/medicines', payload);
      return response.data;
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['medicines'] }),
  });

  const updateMedicineMutation = useMutation({
    mutationFn: async ({ medicineId, payload }) => {
      const response = await axiosInstance.put(`/api/medicines/${medicineId}`, payload);
      return response.data;
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['medicines'] }),
  });

  const deleteMedicineMutation = useMutation({
    mutationFn: async (medicineId) => axiosInstance.delete(`/api/medicines/${medicineId}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['medicines'] }),
  });

  return {
    ...medicinesQuery,
    medicines: medicinesQuery.data?.content || [],
    pagination: {
      page: medicinesQuery.data?.number ?? 0,
      totalPages: medicinesQuery.data?.totalPages ?? 0,
      totalElements: medicinesQuery.data?.totalElements ?? 0,
    },
    adjustStockMutation,
    createMedicineMutation,
    updateMedicineMutation,
    deleteMedicineMutation,
  };
}
