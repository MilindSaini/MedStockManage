import { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import axiosInstance from '../api/axiosInstance';

export default function AdminActivityPage() {
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [size] = useState(20);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    async function loadActivity() {
      setLoading(true);
      try {
        const response = await axiosInstance.get('/api/admin/activity', { params: { page, size } });
        const data = response.data;
        setRows(data.content || []);
        setTotalPages(data.totalPages || 0);
      } catch (error) {
        toast.error(error.response?.data?.message || 'Could not load activity');
      } finally {
        setLoading(false);
      }
    }

    loadActivity();
  }, [page, size]);

  return (
    <div className="min-h-screen bg-slate-950 p-8 text-slate-100">
      <div className="mx-auto max-w-7xl">
        <h1 className="text-3xl font-semibold">Admin Activity</h1>
        <p className="mt-2 text-slate-300">Read-only platform activity log across writes.</p>

        <div className="mt-4 overflow-x-auto rounded-xl border border-slate-800 bg-slate-900/70">
          <table className="min-w-full text-sm">
            <thead className="bg-slate-900 text-left text-slate-300">
              <tr>
                <th className="px-3 py-2">Time</th>
                <th className="px-3 py-2">Action</th>
                <th className="px-3 py-2">User ID</th>
                <th className="px-3 py-2">Store ID</th>
                <th className="px-3 py-2">Entity</th>
                <th className="px-3 py-2">Entity ID</th>
                <th className="px-3 py-2">Metadata</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td className="px-3 py-3 text-slate-400" colSpan={7}>Loading...</td></tr>
              ) : rows.length === 0 ? (
                <tr><td className="px-3 py-3 text-slate-400" colSpan={7}>No activity records found.</td></tr>
              ) : rows.map((row) => (
                <tr key={row.id} className="border-t border-slate-800 align-top">
                  <td className="px-3 py-2">{row.createdAt ? new Date(row.createdAt).toLocaleString() : '-'}</td>
                  <td className="px-3 py-2">{row.action}</td>
                  <td className="px-3 py-2">{row.userId ?? '-'}</td>
                  <td className="px-3 py-2">{row.storeId ?? '-'}</td>
                  <td className="px-3 py-2">{row.entityType ?? '-'}</td>
                  <td className="px-3 py-2">{row.entityId ?? '-'}</td>
                  <td className="px-3 py-2">
                    <pre className="max-w-xl overflow-auto whitespace-pre-wrap text-xs text-slate-300">{JSON.stringify(row.metadata || {}, null, 2)}</pre>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="mt-4 flex items-center gap-3 text-sm">
          <button
            type="button"
            className="rounded-md border border-slate-700 px-3 py-1.5 disabled:opacity-40"
            disabled={page === 0}
            onClick={() => setPage((prev) => Math.max(0, prev - 1))}
          >
            Prev
          </button>
          <span className="text-slate-300">Page {page + 1} of {Math.max(totalPages, 1)}</span>
          <button
            type="button"
            className="rounded-md border border-slate-700 px-3 py-1.5 disabled:opacity-40"
            disabled={totalPages === 0 || page + 1 >= totalPages}
            onClick={() => setPage((prev) => prev + 1)}
          >
            Next
          </button>
        </div>
      </div>
    </div>
  );
}
