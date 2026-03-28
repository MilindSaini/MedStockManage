import { useMemo, useState } from 'react';
import toast from 'react-hot-toast';
import { useNavigate } from 'react-router-dom';
import StockAdjustButton from '../components/StockAdjustButton';
import useMedicines from '../hooks/useMedicines';

function computeStatus(medicine) {
  const now = new Date();
  const expiryDate = medicine.expiryDate ? new Date(medicine.expiryDate) : null;

  if (medicine.currentStock === 0) return 'OUT_OF_STOCK';
  if (expiryDate && expiryDate < now) return 'EXPIRED';
  if (expiryDate) {
    const days = (expiryDate.getTime() - now.getTime()) / (1000 * 60 * 60 * 24);
    if (days <= 7) return 'CRITICAL';
    if (days <= 30) return 'WARNING';
  }
  if (medicine.currentStock <= medicine.lowStockThreshold) return 'LOW_STOCK';
  return 'OK';
}

function badgeClass(status) {
  if (status === 'EXPIRED') return 'bg-rose-500/20 text-rose-300 border border-rose-500/40';
  if (status === 'CRITICAL') return 'bg-orange-500/20 text-orange-300 border border-orange-500/40';
  if (status === 'WARNING') return 'bg-amber-500/20 text-amber-300 border border-amber-500/40';
  if (status === 'LOW_STOCK') return 'bg-yellow-500/20 text-yellow-200 border border-yellow-500/40';
  if (status === 'OUT_OF_STOCK') return 'bg-fuchsia-500/20 text-fuchsia-200 border border-fuchsia-500/40';
  return 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40';
}

export default function InventoryPage() {
  const navigate = useNavigate();
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);

  const { medicines, isLoading, adjustStockMutation, pagination } = useMedicines({
    search,
    page,
    size: 20,
    sortBy: 'updatedAt',
    sortDir: 'desc',
  });

  const rows = useMemo(
    () => medicines.map((medicine) => ({ ...medicine, status: computeStatus(medicine) })),
    [medicines]
  );

  function onAdjust(medicineId, delta, transactionType) {
    adjustStockMutation.mutate(
      { medicineId, delta, transactionType },
      {
        onSuccess: () => toast.success('Stock updated'),
        onError: (error) => toast.error(error.response?.data?.message || 'Could not update stock'),
      }
    );
  }

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 p-6 md:p-8">
      <div className="mx-auto max-w-7xl">
        <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
          <div>
            <h1 className="text-3xl font-semibold">Inventory</h1>
            <p className="text-slate-300 text-sm">Search, adjust stock, and open medicine for editing.</p>
          </div>
          <button
            type="button"
            onClick={() => navigate('/inventory/add')}
            className="rounded-md bg-emerald-600 px-4 py-2 text-sm font-medium hover:bg-emerald-500"
          >
            Add Medicine
          </button>
        </div>

        <div className="mt-5 rounded-lg border border-slate-800 bg-slate-900/70 p-4">
          <input
            value={search}
            onChange={(event) => {
              setSearch(event.target.value);
              setPage(0);
            }}
            placeholder="Search by name, generic name, manufacturer, SKU"
            className="w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2"
          />
        </div>

        <div className="mt-4 overflow-x-auto rounded-xl border border-slate-800">
          <table className="min-w-full bg-slate-900/70 text-sm">
            <thead className="bg-slate-900 text-slate-300">
              <tr>
                <th className="px-3 py-3 text-left">Medicine</th>
                <th className="px-3 py-3 text-left">Category</th>
                <th className="px-3 py-3 text-left">Expiry</th>
                <th className="px-3 py-3 text-left">Stock</th>
                <th className="px-3 py-3 text-left">Status</th>
                <th className="px-3 py-3 text-left">Adjust</th>
              </tr>
            </thead>
            <tbody>
              {isLoading && (
                <tr>
                  <td className="px-3 py-6 text-slate-400" colSpan={6}>Loading medicines...</td>
                </tr>
              )}

              {!isLoading && rows.length === 0 && (
                <tr>
                  <td className="px-3 py-6 text-slate-400" colSpan={6}>No medicines found.</td>
                </tr>
              )}

              {rows.map((medicine) => (
                <tr
                  key={medicine.id}
                  className="cursor-pointer border-t border-slate-800 hover:bg-slate-800/60"
                  onClick={() => navigate('/inventory/add', { state: { medicine } })}
                >
                  <td className="px-3 py-3">
                    <div className="font-medium">{medicine.name}</div>
                    <div className="text-xs text-slate-400">{medicine.genericName || '-'} · {medicine.skuCode || 'No SKU'}</div>
                  </td>
                  <td className="px-3 py-3">{medicine.category || '-'}</td>
                  <td className="px-3 py-3">{medicine.expiryDate || '-'}</td>
                  <td className="px-3 py-3">{medicine.currentStock}</td>
                  <td className="px-3 py-3">
                    <span className={`inline-flex rounded-full px-2 py-1 text-xs ${badgeClass(medicine.status)}`}>
                      {medicine.status}
                    </span>
                  </td>
                  <td className="px-3 py-3">
                    <StockAdjustButton
                      medicine={medicine}
                      busy={adjustStockMutation.isPending}
                      onAdjust={(delta, transactionType) => onAdjust(medicine.id, delta, transactionType)}
                    />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="mt-4 flex items-center justify-between text-sm text-slate-300">
          <span>Page {pagination.page + 1} of {Math.max(pagination.totalPages, 1)}</span>
          <div className="flex gap-2">
            <button
              type="button"
              className="rounded border border-slate-700 px-3 py-1 disabled:opacity-40"
              disabled={pagination.page <= 0}
              onClick={() => setPage((prev) => Math.max(prev - 1, 0))}
            >
              Previous
            </button>
            <button
              type="button"
              className="rounded border border-slate-700 px-3 py-1 disabled:opacity-40"
              disabled={pagination.page + 1 >= pagination.totalPages}
              onClick={() => setPage((prev) => prev + 1)}
            >
              Next
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
