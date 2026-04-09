import { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import axiosInstance from '../api/axiosInstance';

export default function AdminUsersPage() {
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [size] = useState(20);
  const [totalPages, setTotalPages] = useState(0);
  const [search, setSearch] = useState('');
  const [appliedSearch, setAppliedSearch] = useState('');

  useEffect(() => {
    async function loadUsers() {
      setLoading(true);
      try {
        const response = await axiosInstance.get('/api/admin/users', {
          params: { page, size, search: appliedSearch },
        });
        const data = response.data;
        setRows(data.content || []);
        setTotalPages(data.totalPages || 0);
      } catch (error) {
        toast.error(error.response?.data?.message || 'Could not load users');
      } finally {
        setLoading(false);
      }
    }

    loadUsers();
  }, [page, size, appliedSearch]);

  return (
    <div className="min-h-screen bg-slate-950 p-8 text-slate-100">
      <div className="mx-auto max-w-7xl">
        <h1 className="text-3xl font-semibold">Admin Users</h1>
        <p className="mt-2 text-slate-300">Read-only paginated directory of all users.</p>

        <form
          className="mt-4 flex gap-2"
          onSubmit={(event) => {
            event.preventDefault();
            setPage(0);
            setAppliedSearch(search.trim());
          }}
        >
          <input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Search username, email, full name"
            className="w-full rounded-md border border-slate-700 bg-slate-900 px-3 py-2"
          />
          <button type="submit" className="rounded-md border border-cyan-500/60 bg-cyan-500/20 px-4 py-2 text-cyan-100">
            Search
          </button>
        </form>

        <div className="mt-4 overflow-x-auto rounded-xl border border-slate-800 bg-slate-900/70">
          <table className="min-w-full text-sm">
            <thead className="bg-slate-900 text-left text-slate-300">
              <tr>
                <th className="px-3 py-2">ID</th>
                <th className="px-3 py-2">Username</th>
                <th className="px-3 py-2">Email</th>
                <th className="px-3 py-2">Role</th>
                <th className="px-3 py-2">Store ID</th>
                <th className="px-3 py-2">Active</th>
                <th className="px-3 py-2">Created</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td className="px-3 py-3 text-slate-400" colSpan={7}>Loading...</td></tr>
              ) : rows.length === 0 ? (
                <tr><td className="px-3 py-3 text-slate-400" colSpan={7}>No users found.</td></tr>
              ) : rows.map((row) => (
                <tr key={row.id} className="border-t border-slate-800">
                  <td className="px-3 py-2">{row.id}</td>
                  <td className="px-3 py-2">{row.username}</td>
                  <td className="px-3 py-2">{row.email}</td>
                  <td className="px-3 py-2">{row.role}</td>
                  <td className="px-3 py-2">{row.storeId ?? '-'}</td>
                  <td className="px-3 py-2">{row.isActive ? 'Yes' : 'No'}</td>
                  <td className="px-3 py-2">{row.createdAt ? new Date(row.createdAt).toLocaleString() : '-'}</td>
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
