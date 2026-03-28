import { useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import useMedicines from '../hooks/useMedicines';

function statusOf(medicine) {
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

const ORDER = ['CRITICAL', 'EXPIRED', 'OUT_OF_STOCK', 'WARNING', 'LOW_STOCK'];

const STATUS_INFO = {
  CRITICAL: {
    label: 'Critical Expiry',
    meaning: 'Medicine will expire within 7 days.',
    action: 'Prioritize sale/usage, reduce reorder, and plan replacement stock immediately.',
  },
  EXPIRED: {
    label: 'Expired',
    meaning: 'Medicine expiry date has passed.',
    action: 'Stop dispensing this batch, isolate stock, and follow your return/disposal process.',
  },
  OUT_OF_STOCK: {
    label: 'Out Of Stock',
    meaning: 'Current stock is 0.',
    action: 'Create a purchase order or transfer stock from another store as soon as possible.',
  },
  WARNING: {
    label: 'Expiry Warning',
    meaning: 'Medicine will expire within 30 days.',
    action: 'Monitor closely, promote usage, and avoid over-purchasing this item.',
  },
  LOW_STOCK: {
    label: 'Low Stock',
    meaning: 'Current stock is at or below the low stock threshold.',
    action: 'Plan replenishment now and verify that threshold value is set correctly.',
  },
};

export default function AlertsPage() {
  const navigate = useNavigate();
  const { medicines, isLoading } = useMedicines({ size: 100, sortBy: 'updatedAt', sortDir: 'desc' });

  const grouped = useMemo(() => {
    const acc = {
      CRITICAL: [],
      EXPIRED: [],
      OUT_OF_STOCK: [],
      WARNING: [],
      LOW_STOCK: [],
    };

    medicines.forEach((medicine) => {
      const status = statusOf(medicine);
      if (status !== 'OK') {
        acc[status].push(medicine);
      }
    });

    return acc;
  }, [medicines]);

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 p-6 md:p-8">
      <div className="mx-auto max-w-7xl">
        <h1 className="text-3xl font-semibold">Alerts</h1>
        <p className="mt-1 text-sm text-slate-300">Grouped by severity. Click any medicine card to open it for editing.</p>
        <div className="mt-4 rounded-xl border border-sky-700/60 bg-sky-900/20 p-4">
          <h2 className="text-sm font-semibold text-sky-200">How to use these warnings</h2>
          <p className="mt-1 text-sm text-sky-100/90">
            Each alert group includes what the warning means and the recommended action. Resolve CRITICAL, EXPIRED, and OUT OF STOCK first.
          </p>
        </div>

        {isLoading && <p className="mt-6 text-slate-400">Loading alerts...</p>}

        <div className="mt-6 grid gap-5 md:grid-cols-2">
          {ORDER.map((status) => (
            <section key={status} className="rounded-xl border border-slate-800 bg-slate-900/70 p-4">
              <div className="mb-3 flex items-center justify-between">
                <h2 className="text-lg font-semibold">{STATUS_INFO[status].label}</h2>
                <span className="rounded-full bg-slate-800 px-2 py-1 text-xs">{grouped[status].length}</span>
              </div>

              <div className="mb-3 rounded-lg border border-slate-700 bg-slate-950/70 p-3 text-xs text-slate-300">
                <p>
                  <span className="font-semibold text-slate-200">Meaning:</span> {STATUS_INFO[status].meaning}
                </p>
                <p className="mt-1">
                  <span className="font-semibold text-emerald-300">Recommended action:</span> {STATUS_INFO[status].action}
                </p>
              </div>

              <div className="space-y-2">
                {grouped[status].length === 0 && <p className="text-sm text-slate-400">No medicines</p>}
                {grouped[status].map((medicine) => (
                  <button
                    key={medicine.id}
                    type="button"
                    onClick={() => navigate('/inventory/add', { state: { medicine } })}
                    className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-left hover:border-slate-500"
                  >
                    <div className="font-medium">{medicine.name}</div>
                    <div className="text-xs text-slate-400">Stock: {medicine.currentStock} · Expiry: {medicine.expiryDate || '-'}</div>
                  </button>
                ))}
              </div>
            </section>
          ))}
        </div>
      </div>
    </div>
  );
}
