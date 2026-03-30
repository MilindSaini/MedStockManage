import { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import axiosInstance from '../api/axiosInstance';

export default function AdminStoresPage() {
  const [stores, setStores] = useState([]);
  const [loading, setLoading] = useState(true);
  const [savingStoreId, setSavingStoreId] = useState(null);

  useEffect(() => {
    async function loadSchedules() {
      setLoading(true);
      try {
        const response = await axiosInstance.get('/api/admin/stores/schedules');
        setStores(response.data || []);
      } catch (error) {
        toast.error(error.response?.data?.message || 'Could not load store schedules');
      } finally {
        setLoading(false);
      }
    }

    loadSchedules();
  }, []);

  function updateStoreField(storeId, key, value) {
    setStores((prev) => prev.map((item) => (item.storeId === storeId ? { ...item, [key]: value } : item)));
  }

  async function saveSchedule(store) {
    setSavingStoreId(store.storeId);
    try {
      const response = await axiosInstance.put(`/api/admin/stores/${store.storeId}/schedules`, {
        expiryAlertTime: store.expiryAlertTime,
        lowStockAlertTime: store.lowStockAlertTime,
        outOfStockAlertTime: store.outOfStockAlertTime,
        batchPromotionTime: store.batchPromotionTime,
      });

      setStores((prev) => prev.map((item) => (item.storeId === store.storeId ? response.data : item)));
      toast.success(`Schedule updated for ${store.storeName}`);
    } catch (error) {
      toast.error(error.response?.data?.message || 'Could not update schedule');
    } finally {
      setSavingStoreId(null);
    }
  }

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 p-8">
      <h1 className="text-3xl font-semibold">Admin Stores Dashboard</h1>
      <p className="mt-2 text-slate-300">View all stores and control per-store alert schedules from one page.</p>

      <div className="mt-8">
        <h2 className="text-xl font-semibold">Store Alert Schedule Control</h2>
        <p className="mt-1 text-sm text-slate-300">
          Set when each store runs alert checks. Expiry schedule sends 3 email categories: EXPIRED, CRITICAL, and WARNING.
        </p>
        <p className="mt-1 text-xs text-slate-400">
          Low Stock and Out Of Stock have separate schedules. Batch Promotion is separate from medicine status alerts.
        </p>

        {loading ? (
          <div className="mt-4 rounded-md border border-slate-700 bg-slate-900/60 p-4 text-slate-300">Loading stores...</div>
        ) : stores.length === 0 ? (
          <div className="mt-4 rounded-md border border-slate-700 bg-slate-900/60 p-4 text-slate-300">No stores found.</div>
        ) : (
          <div className="mt-4 grid gap-4">
            {stores.map((store) => (
              <div key={store.storeId} className="rounded-xl border border-slate-800 bg-slate-900/70 p-4">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div>
                    <h3 className="text-lg font-semibold text-slate-100">{store.storeName}</h3>
                    <p className="text-xs text-slate-400">Store ID: {store.storeId} | Plan: {store.subscriptionStatus}</p>
                    {store.address && <p className="text-xs text-slate-400">Address: {store.address}</p>}
                  </div>
                </div>

                <div className="mt-4 grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-4">
                  <div>
                    <label className="mb-1 block text-xs uppercase tracking-wide text-slate-400">Batch Promotion</label>
                    <input
                      type="time"
                      className="w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2"
                      value={store.batchPromotionTime || '06:00'}
                      onChange={(event) => updateStoreField(store.storeId, 'batchPromotionTime', event.target.value)}
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-xs uppercase tracking-wide text-slate-400">Expiry + Critical + Warning</label>
                    <input
                      type="time"
                      className="w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2"
                      value={store.expiryAlertTime || '08:00'}
                      onChange={(event) => updateStoreField(store.storeId, 'expiryAlertTime', event.target.value)}
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-xs uppercase tracking-wide text-slate-400">Low Stock Alerts</label>
                    <input
                      type="time"
                      className="w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2"
                      value={store.lowStockAlertTime || '08:30'}
                      onChange={(event) => updateStoreField(store.storeId, 'lowStockAlertTime', event.target.value)}
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-xs uppercase tracking-wide text-slate-400">Out Of Stock Alerts</label>
                    <input
                      type="time"
                      className="w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2"
                      value={store.outOfStockAlertTime || '09:00'}
                      onChange={(event) => updateStoreField(store.storeId, 'outOfStockAlertTime', event.target.value)}
                    />
                  </div>
                </div>

                <button
                  type="button"
                  className="mt-4 rounded-md border border-blue-500/60 bg-blue-500/20 px-4 py-2 text-sm font-medium text-blue-200 hover:bg-blue-500/30 disabled:opacity-60"
                  disabled={savingStoreId === store.storeId}
                  onClick={() => saveSchedule(store)}
                >
                  {savingStoreId === store.storeId ? 'Saving...' : 'Save Schedule'}
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}