import { Navigate, Route, Routes } from 'react-router-dom';
import AppNavbar from './components/AppNavbar';
import ProtectedRoute from './components/ProtectedRoute';
import { AuthProvider } from './context/AuthContext';
import AddMedicinePage from './pages/AddMedicinePage';
import AdminActivityPage from './pages/AdminActivityPage';
import AdminDashboard from './pages/AdminDashboard';
import AdminStoresPage from './pages/AdminStoresPage';
import AdminUsersPage from './pages/AdminUsersPage';
import AlertsPage from './pages/AlertsPage';
import EmployeeDashboard from './pages/EmployeeDashboard';
import EmployeesPage from './pages/EmployeesPage';
import InventoryPage from './pages/InventoryPage';
import LoginPage from './pages/LoginPage';
import OwnerDashboard from './pages/OwnerDashboard';
import OutOfStockPage from './pages/OutOfStockPage';
import ProfilePage from './pages/ProfilePage';
import RegisterPage from './pages/RegisterPage';

function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />

        <Route
          path="/owner"
          element={(
            <ProtectedRoute allowedRoles={["OWNER"]}>
              <>
                <AppNavbar />
                <OwnerDashboard />
              </>
            </ProtectedRoute>
          )}
        />
        <Route
          path="/employee"
          element={(
            <ProtectedRoute allowedRoles={["EMPLOYEE"]}>
              <>
                <AppNavbar />
                <EmployeeDashboard />
              </>
            </ProtectedRoute>
          )}
        />
        <Route
          path="/admin"
          element={(
            <ProtectedRoute allowedRoles={["ADMIN"]}>
              <>
                <AppNavbar />
                <AdminDashboard />
              </>
            </ProtectedRoute>
          )}
        />
        <Route
          path="/admin/stores"
          element={(
            <ProtectedRoute allowedRoles={["ADMIN"]}>
              <>
                <AppNavbar />
                <AdminStoresPage />
              </>
            </ProtectedRoute>
          )}
        />
        <Route
          path="/admin/users"
          element={(
            <ProtectedRoute allowedRoles={["ADMIN"]}>
              <>
                <AppNavbar />
                <AdminUsersPage />
              </>
            </ProtectedRoute>
          )}
        />
        <Route
          path="/admin/activity"
          element={(
            <ProtectedRoute allowedRoles={["ADMIN"]}>
              <>
                <AppNavbar />
                <AdminActivityPage />
              </>
            </ProtectedRoute>
          )}
        />
        <Route
          path="/inventory"
          element={(
            <ProtectedRoute allowedRoles={["OWNER", "EMPLOYEE"]}>
              <>
                <AppNavbar />
                <InventoryPage />
              </>
            </ProtectedRoute>
          )}
        />
        <Route
          path="/inventory/add"
          element={(
            <ProtectedRoute allowedRoles={["OWNER", "EMPLOYEE"]}>
              <>
                <AppNavbar />
                <AddMedicinePage />
              </>
            </ProtectedRoute>
          )}
        />
        <Route
          path="/alerts"
          element={(
            <ProtectedRoute allowedRoles={["OWNER", "EMPLOYEE"]}>
              <>
                <AppNavbar />
                <AlertsPage />
              </>
            </ProtectedRoute>
          )}
        />
        <Route
          path="/out-of-stock"
          element={(
            <ProtectedRoute allowedRoles={["OWNER", "EMPLOYEE"]}>
              <>
                <AppNavbar />
                <OutOfStockPage />
              </>
            </ProtectedRoute>
          )}
        />
        <Route
          path="/employees"
          element={(
            <ProtectedRoute allowedRoles={["OWNER"]}>
              <>
                <AppNavbar />
                <EmployeesPage />
              </>
            </ProtectedRoute>
          )}
        />
        <Route
          path="/profile"
          element={(
            <ProtectedRoute allowedRoles={["OWNER", "EMPLOYEE", "ADMIN"]}>
              <>
                <AppNavbar />
                <ProfilePage />
              </>
            </ProtectedRoute>
          )}
        />

        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </AuthProvider>
  );
}

export default App;
