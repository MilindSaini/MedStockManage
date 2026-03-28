import toast from 'react-hot-toast';
import { useState } from 'react';
import useMedicines from '../hooks/useMedicines';
import usePermissions from '../hooks/usePermissions';

const RESTOCK_OPTIONS = [1, 5, 10, 25, 50, 100];

export default function OutOfStockPage() {
  const { canAdd, canDelete } = usePermissions();
  const { medicines, isLoading, adjustStockMutation, deleteMedicineMutation } = useMedicines({ outOfStock: true, size: 100 });
  const [restockByMedicine, setRestockByMedicine] = useState({});

  function getRestockQty(medicineId) {
    const value = Number(restockByMedicine[medicineId]);
    if (!Number.isFinite(value) || value <= 0) {
      return 1;
    }
    return value;
  }

  function restock(medicineId) {
    if (!canAdd) {
      toast.error('No permission to restock');
      return;
    }

    const qty = getRestockQty(medicineId);

    adjustStockMutation.mutate(
      { medicineId, delta: qty, transactionType: 'RESTOCK' },
      {
        onSuccess: () => toast.success(`Restocked by ${qty} unit${qty > 1 ? 's' : ''}`),
        onError: (error) => toast.error(error.response?.data?.message || 'Could not restock'),
      }
    );
  }

  function remove(medicineId) {
    if (!canDelete) {
      toast.error('No permission to delete');
      return;
    }

    deleteMedicineMutation.mutate(medicineId, {
      onSuccess: () => toast.success('Medicine deleted'),
      onError: (error) => toast.error(error.response?.data?.message || 'Could not delete medicine'),
    });
  }

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 p-6 md:p-8">
      <div className="mx-auto max-w-5xl">
        <h1 className="text-3xl font-semibold">Out of Stock</h1>
        <p className="mt-1 text-sm text-slate-300">Medicines with zero stock. Restock instantly or remove from active inventory.</p>

        <div className="mt-6 space-y-3">
          {isLoading && <p className="text-slate-400">Loading out-of-stock medicines...</p>}
          {!isLoading && medicines.length === 0 && <p className="text-slate-400">No out-of-stock medicines.</p>}

          {medicines.map((medicine) => (
            <div key={medicine.id} className="flex flex-col gap-3 rounded-lg border border-slate-800 bg-slate-900/70 p-4 md:flex-row md:items-center md:justify-between">
              <div>
                <div className="font-medium">{medicine.name}</div>
                <div className="text-xs text-slate-400">Category: {medicine.category || '-'} · SKU: {medicine.skuCode || '-'}</div>
              </div>
              <div className="flex flex-wrap items-center gap-2">
                <label className="flex items-center gap-2 text-xs text-slate-300">
                  <span>Qty</span>
                  <select
                    value={getRestockQty(medicine.id)}
                    onChange={(event) => {
                      const nextQty = Number(event.target.value);
                      setRestockByMedicine((prev) => ({
                        ...prev,
                        [medicine.id]: nextQty,
                      }));
                    }}
                    className="rounded-md border border-slate-700 bg-slate-950 px-2 py-1 text-slate-100"
                    disabled={adjustStockMutation.isPending}
                  >
                    {RESTOCK_OPTIONS.map((qtyOption) => (
                      <option key={qtyOption} value={qtyOption}>{qtyOption}</option>
                    ))}
                  </select>
                </label>
                <button
                  type="button"
                  onClick={() => restock(medicine.id)}
                  className="rounded-md border border-emerald-500/50 px-3 py-1 text-emerald-300 disabled:opacity-40"
                  disabled={adjustStockMutation.isPending}
                >
                  Restock +{getRestockQty(medicine.id)}
                </button>
                <button
                  type="button"
                  onClick={() => remove(medicine.id)}
                  className="rounded-md border border-rose-500/50 px-3 py-1 text-rose-300 disabled:opacity-40"
                  disabled={deleteMedicineMutation.isPending}
                >
                  Delete
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
