# Xây dựng Giao diện người dùng
## Phần này là thiết kế base cho front-end
### package
```json
{
"dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-router-dom": "^6.20.0",
    "axios": "^1.6.2",
    "lucide-react": "^0.294.0",
    "recharts": "^2.10.3",
    "date-fns": "^2.30.0"
  },
  "devDependencies": {
    "@types/react": "^18.2.37",
    "@types/react-dom": "^18.2.15",
    "@vitejs/plugin-react": "^4.2.0",
    "autoprefixer": "^10.4.16",
    "postcss": "^8.4.31",
    "tailwindcss": "^3.3.5",
    "typescript": "^5.2.2",
    "vite": "^5.0.0"
  }
}
```
### Style
Theme: Enterprise Modern (Clean, Professional, Data-Rich).

Visual Language: High contrast for readability, subtle shadows for depth, rounded corners for a modern feel.

Color Palette (HEX):

Primary: #1E40AF (Royal Blue) - Key actions & Sidebar.

Background: #F1F5F9 (Cool Gray) - Page background.

Surface: #FFFFFF (White) - Card & Content containers.

Text_Main: #1E293B (Deep Slate) - Headings & Body.

Text_Muted: #64748B (Gray) - Meta-data & IDs.

Status_Success: #10B981 (Green).

Status_Error: #EF4444 (Red).

Status_Warning: #F59E0B (Amber).

2. TYPOGRAPHY & SPACING
Font: 'Inter', sans-serif (or System Default).

Scale: Base 14px (Normal), 12px (Small/Meta), 18px (Card Titles).

Grid: 12-column system, 24px gutter.

Border Radius: 12px for Cards, 8px for Buttons/Inputs.

Shadow: 0 1px 3px 0 rgb(0 0 0 / 0.1), 0 1px 2px -1px rgb(0 0 0 / 0.1).

3. CORE LAYOUT STRUCTURE
Navigation: Vertical Sidebar (Left), fixed width 260px, Primary Blue background.

Top Bar: Global Search (Full width), User Profile, Notifications.

Main Content Area: Multi-column dashboard grid

## Chi tiết

### 1. THIẾT LẬP DỰ ÁN REACT

**Bước 1.1: Tạo thư mục và khởi tạo**
```bash
mkdir scalehub-frontend
cd scalehub-frontend
npm create vite@latest . -- --template react-ts --yes
```

**Bước 1.2: Cài đặt dependencies theo package.json ở trên**
```bash
npm install
```

**Bước 1.3: Cấu hình Tailwind CSS**
- Tạo `tailwind.config.js` với theme colors theo Color Palette ở trên
- Cập nhật `index.css` với Tailwind directives
- Cấu hình PostCSS trong `vite.config.ts`

**Bước 1.4: Thiết lập TypeScript types**
- Tạo `src/types/index.ts` với interfaces dựa trên backend DTOs:
  - `User`, `Role`, `Permission` từ `/users` và `/roles`
  - `Scale`, `ScaleConfig`, `ScaleDto.Response` từ `/scales`
  - `Location` từ `/locations`
  - `AuthDto.LoginResponse` từ `/auth`

### 2. THIẾT LẬP API CLIENT

**Bước 2.1: Tạo axios client với interceptors**
- File: `src/api/client.ts`
- Base URL: `http://localhost:8080/api/v1`
- Request interceptor: Thêm Authorization header với JWT token
- Response interceptor: Handle 401 (token expired) -> auto refresh token
- Error handling: Toast notifications cho errors

**Bước 2.2: Tạo API service modules**
- `src/api/auth.ts`: login(), refreshToken(), logout()
- `src/api/users.ts`: getAllUsers(), getUserById(), createUser(), updateUser(), deleteUser()
- `src/api/roles.ts`: getAllRoles(), createRole(), updateRole(), deleteRole()
- `src/api/permissions.ts`: getAllPermissions()
- `src/api/scales.ts`: getAllScales(), getScaleById(), createScale(), updateScale(), deleteScale(), getScaleConfig(), updateScaleConfig()
- `src/api/locations.ts`: getAllLocations(), getLocationsTree(), createLocation(), updateLocation(), deleteLocation()
- `src/api/reports.ts`: getDailyReports(), getWeighingLogs()

### 3. THIẾT LẬP AUTHENTICATION

**Bước 3.1: Tạo AuthContext**
- File: `src/contexts/AuthContext.tsx`
- State: user, token, refreshToken, isAuthenticated
- Actions: login, logout, refreshToken
- Persist auth state trong localStorage

**Bước 3.2: Tạo ProtectedRoute component**
- File: `src/components/auth/ProtectedRoute.tsx`
- Check authentication status
- Redirect to `/login` nếu chưa đăng nhập
- Role-based access control dựa trên user roles

**Bước 3.3: Tạo Login page**
- File: `src/pages/auth/LoginPage.tsx`
- Form với username/password fields
- Validation với react-hook-form
- Call auth API, store tokens, redirect to dashboard

### 4. THIẾT LẬP WEBSOCKET CHO REAL-TIME DATA

**Bước 4.1: Tạo WebSocket hook**
- File: `src/hooks/useWebSocket.ts`
- Sử dụng SockJS + STOMP client
- Connect to `ws://localhost:8080/api/v1/ws`
- Subscribe to `/topic/scales` và `/topic/scale/{id}`
- Handle connection/reconnection logic
- Return scale data stream

**Bước 4.2: Tạo WebSocketContext**
- File: `src/contexts/WebSocketContext.tsx`
- Manage WebSocket connection state
- Provide scale data to components
- Handle real-time updates

### 5. THIẾT LẬP LAYOUT COMPONENTS

**Bước 5.1: Tạo Sidebar navigation**
- File: `src/components/layout/Sidebar.tsx`
- Menu items dựa trên user permissions:
  - Dashboard (all users)
  - Scales (ADMIN, MANAGER, USER)
  - Users (ADMIN, MANAGER)
  - Roles (ADMIN)
  - Locations (ADMIN, MANAGER)
  - Reports (ADMIN, MANAGER, USER)
- Active state highlighting
- Collapsible design

**Bước 5.2: Tạo Header component**
- File: `src/components/layout/Header.tsx`
- User profile dropdown (logout, profile)
- Notifications bell (future feature)
- Breadcrumb navigation

**Bước 5.3: Tạo MainLayout wrapper**
- File: `src/components/layout/MainLayout.tsx`
- Combine Sidebar + Header + Content area
- Responsive: hide sidebar on mobile

### 6. THIẾT LẬP DASHBOARD PAGE

**Bước 6.1: Tạo Dashboard overview**
- File: `src/pages/dashboard/DashboardPage.tsx`
- Real-time scale status cards (online/offline counts)
- Recent weighing activities table
- Charts: weighing trends (sử dụng recharts)
- System health indicators

**Bước 6.2: Tạo ScaleStatusCard component**
- File: `src/components/dashboard/ScaleStatusCard.tsx`
- Display scale name, current weight, status
- Real-time updates từ WebSocket
- Color coding: green (active), red (error), yellow (warning)

**Bước 6.3: Tạo WeighingChart component**
- File: `src/components/dashboard/WeighingChart.tsx`
- Line chart hiển thị weighing data over time
- Data từ `/reports/weighing-logs` API
- Time range selector (1h, 24h, 7d)

### 7. THIẾT LẬP SCALE MANAGEMENT

**Bước 7.1: Tạo ScaleListPage**
- File: `src/pages/scales/ScaleListPage.tsx`
- Data table với columns: ID, Name, Location, Status, Last Weight, Last Update
- Filters: location dropdown, status filter
- Actions: View, Edit, Delete (role-based)
- Pagination và sorting

**Bước 7.2: Tạo ScaleDetailPage**
- File: `src/pages/scales/ScaleDetailPage.tsx`
- Tabs: Overview, Configuration, Logs
- Overview: current data, real-time updates
- Configuration: Modbus/Serial settings form
- Logs: weighing history table

**Bước 7.3: Tạo ScaleForm component**
- File: `src/components/scales/ScaleForm.tsx`
- Form fields: name, location, connection type, IP/port/serial settings
- Validation rules
- Create/Edit modes

### 8. THIẾT LẬP USER MANAGEMENT

**Bước 8.1: Tạo UserListPage**
- File: `src/pages/users/UserListPage.tsx`
- Data table: username, fullName, email, roles, status
- Filters: role filter, status filter
- Actions: Edit, Delete, Reset Password

**Bước 8.2: Tạo UserForm component**
- File: `src/components/users/UserForm.tsx`
- Fields: username, password, fullName, email, phone, roles
- Role assignment với multi-select
- Password strength validation

**Bước 8.3: Tạo RoleManagementPage**
- File: `src/pages/users/RoleManagementPage.tsx`
- Role list với permissions matrix
- CRUD operations cho roles
- Permission assignment interface

### 9. THIẾT LẬP LOCATION MANAGEMENT

**Bước 9.1: Tạo LocationTree component**
- File: `src/components/locations/LocationTree.tsx`
- Hierarchical tree view từ `/locations/tree` API
- Expand/collapse nodes
- Context menu: Add child, Edit, Delete

**Bước 9.2: Tạo LocationForm component**
- File: `src/components/locations/LocationForm.tsx`
- Fields: name, parent location, description
- Tree selector cho parent

### 10. THIẾT LẬP REPORTS

**Bước 10.1: Tạo DailyReportPage**
- File: `src/pages/reports/DailyReportPage.tsx`
- Date picker cho report date
- Summary cards: total weighings, avg weight, etc.
- Detailed table với export to CSV

**Bước 10.2: Tạo WeighingLogPage**
- File: `src/pages/reports/WeighingLogPage.tsx`
- Filters: date range, scale, location
- Data table với sorting/pagination
- Export functionality

**Bước 10.3: Tạo ReportChart component**
- File: `src/components/reports/ReportChart.tsx`
- Bar/line charts cho report data
- Multiple chart types (daily totals, hourly distribution)

### 11. THIẾT LẬP ROUTING

**Bước 11.1: Cấu hình React Router**
- File: `src/App.tsx`
- Public routes: `/login`
- Protected routes với role checks:
  - `/dashboard` (all authenticated users)
  - `/scales/*` (ADMIN, MANAGER, USER)
  - `/users/*` (ADMIN, MANAGER)
  - `/locations/*` (ADMIN, MANAGER)
  - `/reports/*` (all authenticated users)

**Bước 11.2: Tạo route constants**
- File: `src/utils/routes.ts`
- Centralized route definitions
- Permission-based route filtering

### 12. THIẾT LẬP COMMON COMPONENTS

**Bước 12.1: Tạo DataTable component**
- File: `src/components/common/DataTable.tsx`
- Generic table với sorting, pagination, filtering
- Actions column với role-based buttons

**Bước 12.2: Tạo Form components**
- `src/components/common/TextField.tsx`
- `src/components/common/SelectField.tsx`
- `src/components/common/DatePicker.tsx`
- Validation integration với react-hook-form

**Bước 12.3: Tạo Modal/Dialog components**
- `src/components/common/ConfirmDialog.tsx`
- `src/components/common/FormDialog.tsx`
- Reusable cho CRUD operations

### 13. THIẾT LẬP STATE MANAGEMENT

**Bước 13.1: Sử dụng React Query**
- File: `src/main.tsx` - QueryClient setup
- API state management cho caching, background refetch
- Optimistic updates cho mutations

**Bước 13.2: Custom hooks**
- `src/hooks/useAuth.ts` - Auth state
- `src/hooks/useScales.ts` - Scale operations
- `src/hooks/useUsers.ts` - User operations

### 14. THIẾT LẬP ERROR HANDLING & LOADING

**Bước 14.1: Global error boundary**
- File: `src/components/common/ErrorBoundary.tsx`
- Catch React errors, display fallback UI

**Bước 14.2: Loading states**
- Skeleton loaders cho tables và cards
- Loading buttons với spinner
- Global loading overlay

**Bước 14.3: Toast notifications**
- Success/error/info messages
- Integration với react-toastify

### 15. TESTING & VALIDATION

**Bước 15.1: Unit tests**
- Test API functions
- Test components với React Testing Library
- Mock API responses

**Bước 15.2: Form validation**
- Yup schemas cho tất cả forms
- Client-side validation
- Server error handling

### 16. PERFORMANCE OPTIMIZATION

**Bước 16.1: Code splitting**
- Lazy load pages với React.lazy()
- Route-based code splitting

**Bước 16.2: Memoization**
- React.memo cho components
- useMemo/useCallback cho expensive operations

**Bước 16.3: Image optimization**
- Lazy loading cho images
- WebP format support

### 17. DEPLOYMENT

**Bước 17.1: Build configuration**
- `npm run build` tạo production bundle
- Environment-specific configs
- CDN setup cho assets

**Bước 17.2: CORS configuration**
- Backend Spring Boot: allow frontend origin
- WebSocket CORS settings

**Bước 17.3: Production deployment**
- Static file serving (nginx/apache)
- SSL certificate setup
- Monitoring và logging

### 18. MONITORING & MAINTENANCE

**Bước 18.1: Error tracking**
- Sentry integration cho error monitoring
- User feedback collection

**Bước 18.2: Analytics**
- User behavior tracking
- Performance metrics

**Bước 18.3: Documentation**
- Component documentation với Storybook
- API documentation updates

---

**Lưu ý quan trọng:**
- Luôn check backend API documentation tại `http://localhost:8080/api/v1/swagger-ui.html`
- Test tích hợp với Postman collection có sẵn
- Follow TypeScript strict mode
- Maintain consistent code style với ESLint/Prettier
- Regular commits và version control
