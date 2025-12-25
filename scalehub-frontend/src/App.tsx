import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

import { AuthProvider } from './contexts/AuthContext';
import { WebSocketProvider } from './contexts/WebSocketContext';
import { ProtectedRoute } from './components/auth/ProtectedRoute';
import { MainLayout } from './components/layout/MainLayout';
import { LoginPage } from './pages/auth/LoginPage';
import { DashboardPage } from './pages/dashboard/DashboardPage';
import { ScaleListPage } from './pages/scales/ScaleListPage';
import { ScaleDetailPage } from './pages/scales/ScaleDetailPage';
import { ScaleEditPage } from './pages/scales/ScaleEditPage';
import { ScaleCreatePage } from './pages/scales/ScaleCreatePage';
import { MonitoringPage } from './pages/scales/MonitoringPage';
import { UserListPage } from './pages/users/UserListPage';
import { RoleListPage } from './pages/users/RoleListPage';
import { LocationManagementPage } from './pages/locations/LocationManagementPage';
import { ReportsPage } from './pages/reports/ReportsPage';

// Create a client
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
      staleTime: 30000,
    },
  },
});

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <WebSocketProvider>
          <BrowserRouter>
            <Routes>
              {/* Public routes */}
              <Route path="/login" element={<LoginPage />} />
              
              {/* Protected routes */}
              <Route
                path="/"
                element={
                  <ProtectedRoute>
                    <MainLayout />
                  </ProtectedRoute>
                }
              >
                <Route index element={<Navigate to="/dashboard" replace />} />
                <Route path="dashboard" element={<DashboardPage />} />
                <Route path="monitoring" element={<MonitoringPage />} />
                <Route path="scales" element={<ScaleListPage />} />
                <Route path="scales/create" element={<ScaleCreatePage />} />
                <Route path="scales/:id" element={<ScaleDetailPage />} />
                <Route path="scales/:id/edit" element={<ScaleEditPage />} />
                <Route 
                  path="users" 
                  element={
                    <ProtectedRoute requiredRoles={['ADMIN', 'MANAGE']}>
                      <UserListPage />
                    </ProtectedRoute>
                  } 
                />
                <Route 
                  path="roles" 
                  element={
                    <ProtectedRoute requiredRoles={['ADMIN']}>
                      <RoleListPage />
                    </ProtectedRoute>
                  } 
                />
                <Route 
                  path="locations" 
                  element={
                    <ProtectedRoute requiredRoles={['ADMIN', 'MANAGE']}>
                      <LocationManagementPage />
                    </ProtectedRoute>
                  } 
                />
                <Route path="reports" element={<ReportsPage />} />
                <Route path="*" element={<Navigate to="/dashboard" replace />} />
              </Route>
            </Routes>
          </BrowserRouter>
          <ToastContainer
            position="top-right"
            autoClose={3000}
            hideProgressBar={false}
            newestOnTop
            closeOnClick
            rtl={false}
            pauseOnFocusLoss
            draggable
            pauseOnHover
            theme="light"
          />
        </WebSocketProvider>
      </AuthProvider>
    </QueryClientProvider>
  );
}

export default App;
