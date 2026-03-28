import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import axiosInstance from '../api/axiosInstance';

const EMPTY_FORM = {
  username: '',
  permissions: {
    canAdd: false,
    canEdit: false,
    canDelete: false,
    canViewFinance: false,
    canSell: false,
  },
};

const PERMISSION_GUIDE = [
  {
    key: 'canAdd',
    title: 'Add',
    description: 'Can add new medicines, restock inventory, and send new employee invitations.',
  },
  {
    key: 'canEdit',
    title: 'Edit',
    description: 'Can update medicine details and change permissions of existing employees.',
  },
  {
    key: 'canDelete',
    title: 'Delete',
    description: 'Can remove medicines from inventory and remove employees from the store.',
  },
  {
    key: 'canViewFinance',
    title: 'View Finance',
    description: 'Reserved for finance visibility. Currently stored and managed but not enforced on a dedicated finance screen yet.',
  },
  {
    key: 'canSell',
    title: 'Sell',
    description: 'Can perform sale stock reductions from inventory (negative stock adjustment).',
  },
];

const PERMISSION_LABELS = {
  canAdd: 'Add',
  canEdit: 'Edit',
  canDelete: 'Delete',
  canViewFinance: 'View Finance',
  canSell: 'Sell',
};

export default function EmployeesPage() {
  const queryClient = useQueryClient();
  const [showAdd, setShowAdd] = useState(false);
  const [form, setForm] = useState(EMPTY_FORM);
  const [fieldErrors, setFieldErrors] = useState({ username: '' });

  const employeesQuery = useQuery({
    queryKey: ['employees'],
    queryFn: async () => {
      const response = await axiosInstance.get('/api/employees');
      return response.data;
    },
  });

  const addEmployeeMutation = useMutation({
    mutationFn: async (payload) => {
      const response = await axiosInstance.post('/api/employees', payload);
      return response.data;
    },
    onSuccess: (createdEmployee) => {
      queryClient.invalidateQueries({ queryKey: ['employees'] });
      setShowAdd(false);
      setForm(EMPTY_FORM);
      sessionStorage.setItem(
        'ownerEmployeeJoinAlert',
        `Invitation sent to ${createdEmployee?.username || createdEmployee?.email || 'employee'}. They can now accept or decline joining your store.`
      );
      toast.success('Employee invitation sent');
    },
    onError: (error) => {
      const status = error.response?.status;
      if (status === 404) {
        toast.error('Username with this not found.');
        return;
      }
      toast.error(error.response?.data?.message || 'Could not add employee');
    },
  });

  const updatePermissionsMutation = useMutation({
    mutationFn: async ({ employeeUserId, permissions }) => {
      const response = await axiosInstance.put(`/api/employees/${employeeUserId}/permissions`, { permissions });
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['employees'] });
      toast.success('Permissions updated');
    },
    onError: (error) => toast.error(error.response?.data?.message || 'Could not update permissions'),
  });

  const removeEmployeeMutation = useMutation({
    mutationFn: async (employeeUserId) => axiosInstance.delete(`/api/employees/${employeeUserId}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['employees'] });
      toast.success('Employee removed');
    },
    onError: (error) => toast.error(error.response?.data?.message || 'Could not remove employee'),
  });

  function updateField(key, value) {
    setForm((prev) => ({ ...prev, [key]: value }));
    if (key === 'username') {
      setFieldErrors((prev) => ({ ...prev, [key]: '' }));
    }
  }

  function updatePermission(key, value) {
    setForm((prev) => ({
      ...prev,
      permissions: {
        ...prev.permissions,
        [key]: value,
      },
    }));
  }

  function validateAddEmployeeForm() {
    const nextErrors = { username: '' };
    const normalizedUsername = String(form.username || '').trim();

    if (!normalizedUsername || normalizedUsername.length < 3) {
      nextErrors.username = 'Enter a valid username (at least 3 characters).';
    }

    setFieldErrors(nextErrors);
    return !nextErrors.username;
  }

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 p-6 md:p-8">
      <div className="mx-auto max-w-6xl">
        <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
          <div>
            <h1 className="text-3xl font-semibold">Employees</h1>
            <p className="text-sm text-slate-300">Add employees and configure fine-grained permissions.</p>
          </div>
          <button
            type="button"
            onClick={() => setShowAdd((prev) => !prev)}
            className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium hover:bg-blue-500"
          >
            {showAdd ? 'Close Add Form' : 'Add Employee'}
          </button>
        </div>

        <div className="mt-5 rounded-lg border border-slate-800 bg-slate-900/70 p-4">
          <h2 className="text-base font-semibold text-slate-100">Permission Guide</h2>
          <p className="mt-1 text-xs text-slate-400">
            These permissions control what each employee can do across inventory and employee operations.
          </p>
          <div className="mt-3 grid gap-2 md:grid-cols-2">
            {PERMISSION_GUIDE.map((permission) => (
              <div key={permission.key} className="rounded-md border border-slate-700 bg-slate-950/60 p-3">
                <div className="text-sm font-medium text-slate-100">{permission.title}</div>
                <div className="mt-1 text-xs text-slate-300">{permission.description}</div>
              </div>
            ))}
          </div>
        </div>

        {showAdd && (
          <form
            className="mt-5 grid gap-3 rounded-lg border border-slate-800 bg-slate-900/70 p-4 md:grid-cols-2"
            onSubmit={(event) => {
              event.preventDefault();
              if (!validateAddEmployeeForm()) {
                toast.error('Please enter a valid username.');
                return;
              }
              addEmployeeMutation.mutate(form);
            }}
          >
            <div>
              <input
                className={`w-full rounded-md border bg-slate-950 px-3 py-2 ${fieldErrors.username ? 'border-rose-500' : 'border-slate-700'}`}
                placeholder="Username"
                value={form.username}
                onChange={(event) => updateField('username', event.target.value)}
                required
              />
              {fieldErrors.username && <p className="mt-1 text-xs text-rose-400">{fieldErrors.username}</p>}
            </div>

            <div className="md:col-span-2 grid grid-cols-2 md:grid-cols-5 gap-2 text-sm">
              {Object.keys(form.permissions).map((key) => (
                <label key={key} className="flex items-center gap-2 rounded border border-slate-700 px-2 py-2">
                  <input
                    type="checkbox"
                    checked={Boolean(form.permissions[key])}
                    onChange={(event) => updatePermission(key, event.target.checked)}
                  />
                  {key}
                </label>
              ))}
            </div>

            <button className="md:col-span-2 rounded-md bg-emerald-600 px-4 py-2 font-medium hover:bg-emerald-500 disabled:opacity-40" type="submit" disabled={addEmployeeMutation.isPending}>
              {addEmployeeMutation.isPending ? 'Adding...' : 'Create Employee'}
            </button>
          </form>
        )}

        <div className="mt-6 space-y-3">
          {employeesQuery.isLoading && <p className="text-slate-400">Loading employees...</p>}
          {!employeesQuery.isLoading && (employeesQuery.data || []).length === 0 && <p className="text-slate-400">No employees yet.</p>}

          {(employeesQuery.data || []).map((employee) => (
            <div key={employee.id} className="rounded-lg border border-slate-800 bg-slate-900/70 p-4">
              <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                <div>
                  <div className="font-medium">{employee.username}</div>
                  <div className="text-xs text-slate-400">{employee.email || '-'}</div>
                </div>
                <button
                  type="button"
                  className="rounded border border-rose-500/50 px-3 py-1 text-rose-300 disabled:opacity-40"
                  onClick={() => removeEmployeeMutation.mutate(employee.id)}
                  disabled={removeEmployeeMutation.isPending}
                >
                  Remove
                </button>
              </div>

              <div className="mt-3 grid grid-cols-2 md:grid-cols-5 gap-2 text-sm">
                {Object.entries(employee.permissions || {}).map(([key, value]) => (
                  <label key={key} className="flex items-center gap-2 rounded border border-slate-700 px-2 py-2">
                    <input
                      type="checkbox"
                      checked={Boolean(value)}
                      onChange={(event) => updatePermissionsMutation.mutate({
                        employeeUserId: employee.id,
                        permissions: {
                          ...employee.permissions,
                          [key]: event.target.checked,
                        },
                      })}
                    />
                    {PERMISSION_LABELS[key] || key}
                  </label>
                ))}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
