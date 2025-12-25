# ScaleHub IoT Frontend - Architecture & Extension Guide

## 📐 Kiến trúc tổng quan

Frontend được xây dựng theo kiến trúc phân lớp rõ ràng:

```
┌─────────────────────────────────────────┐
│          Presentation Layer             │
│     (Pages & Components - React)        │
├─────────────────────────────────────────┤
│         Application Layer               │
│    (Contexts, Hooks, State Mgmt)        │
├─────────────────────────────────────────┤
│           Service Layer                 │
│        (API Services - Axios)           │
├─────────────────────────────────────────┤
│          Infrastructure                 │
│   (WebSocket, HTTP Client, Storage)     │
└─────────────────────────────────────────┘
```

## 🔧 Các thành phần chính

### 1. API Layer (`src/api/`)

Tất cả API calls đều thông qua các service modules:
- `client.ts`: Axios instance với interceptors
- `auth.ts`, `scales.ts`, `users.ts`, etc.: Specific API services

**Cách thêm API mới:**

```typescript
// src/api/newFeature.ts
import apiClient from './client';
import { NewFeatureType } from '../types';

export const newFeatureApi = {
  getAll: async (): Promise<NewFeatureType[]> => {
    const response = await apiClient.get('/new-feature');
    return response.data.data;
  },
  
  create: async (data: CreateRequest): Promise<NewFeatureType> => {
    const response = await apiClient.post('/new-feature', data);
    return response.data.data;
  },
};
```

### 2. Type Definitions (`src/types/`)

Tất cả TypeScript types được định nghĩa tập trung trong `index.ts`:

```typescript
// Thêm type mới
export interface NewFeature {
  id: number;
  name: string;
  // ... other fields
}

export interface CreateNewFeatureRequest {
  name: string;
  // ...
}
```

### 3. Context Providers (`src/contexts/`)

**AuthContext**: Quản lý authentication state
- Login/logout
- Token management
- User info

**WebSocketContext**: Quản lý WebSocket connections
- Real-time data subscription
- Connection state
- Auto-reconnect

**Cách thêm Context mới:**

```typescript
// src/contexts/NewContext.tsx
import React, { createContext, useContext, useState } from 'react';

interface NewContextType {
  // Define context interface
}

const NewContext = createContext<NewContextType | undefined>(undefined);

export const NewProvider: React.FC<{children: ReactNode}> = ({children}) => {
  // Context logic
  
  return (
    <NewContext.Provider value={value}>
      {children}
    </NewContext.Provider>
  );
};

export const useNew = () => {
  const context = useContext(NewContext);
  if (!context) throw new Error('useNew must be used within NewProvider');
  return context;
};
```

### 4. Components (`src/components/`)

Cấu trúc:
- `auth/`: Authentication-related components
- `layout/`: Layout components (Sidebar, Header, MainLayout)
- `dashboard/`: Dashboard-specific components
- `common/`: Reusable components (buttons, modals, tables, etc.)

**Component Guidelines:**
- Mỗi component trong 1 file riêng
- Export named exports
- Props interface rõ ràng
- Sử dụng TypeScript strict mode

### 5. Pages (`src/pages/`)

Mỗi feature có folder riêng:
- `auth/`: Login, Register
- `dashboard/`: Dashboard overview
- `scales/`: Scale management
- `users/`: User management
- `locations/`: Location management
- `reports/`: Reporting pages

**Cách thêm page mới:**

```typescript
// src/pages/newFeature/NewFeaturePage.tsx
import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { newFeatureApi } from '../../api/newFeature';

export const NewFeaturePage: React.FC = () => {
  const { data, isLoading } = useQuery({
    queryKey: ['new-feature'],
    queryFn: () => newFeatureApi.getAll(),
  });

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold">New Feature</h1>
      {/* Page content */}
    </div>
  );
};
```

## 🎨 Styling Guidelines

### Tailwind CSS Classes

**Đã định nghĩa sẵn:**
- `.btn-primary`: Primary button
- `.btn-secondary`: Secondary button
- `.card`: Card container
- `.input-field`: Input field
- `.sidebar-link`: Sidebar menu item
- `.sidebar-link-active`: Active sidebar item

### Color Variables

Sử dụng trong Tailwind config:
- `bg-primary`: Primary color
- `text-textMain`: Main text color
- `text-textMuted`: Muted text
- `bg-statusSuccess`: Success state
- `bg-statusError`: Error state
- `bg-statusWarning`: Warning state

### Typography Scale

- `text-xs`: 12px
- `text-sm`: 14px
- `text-base`: 16px (default)
- `text-lg`: 18px
- `text-xl`: 20px
- `text-2xl`: 24px
- `text-3xl`: 30px

## 🔄 State Management

### Server State (React Query)

Sử dụng cho data từ API:

```typescript
// Fetching data
const { data, isLoading, error } = useQuery({
  queryKey: ['key'],
  queryFn: () => api.fetch(),
});

// Mutations
const mutation = useMutation({
  mutationFn: (data) => api.create(data),
  onSuccess: () => {
    queryClient.invalidateQueries({ queryKey: ['key'] });
  },
});
```

### Client State

- React Context cho global state (Auth, WebSocket)
- useState cho local component state
- useReducer cho complex state logic

## 🔐 Authentication Flow

```
1. User enters credentials
2. Call authApi.login()
3. Store tokens in localStorage
4. Update AuthContext
5. Redirect to dashboard
6. API client auto-adds token to requests
7. On 401: Auto refresh token
8. On refresh fail: Logout and redirect to login
```

## 🔌 WebSocket Integration

### Subscription Pattern

```typescript
const { scaleData, subscribe, connected } = useWebSocket();

useEffect(() => {
  if (connected) {
    subscribe(scaleId); // Subscribe to specific scale
    // or
    subscribeAll(); // Subscribe to all scales
  }
}, [connected]);

// Access real-time data
const realtimeData = scaleData.get(scaleId);
```

## 🛣️ Routing

Routes được định nghĩa trong `App.tsx`:

```typescript
<Routes>
  <Route path="/login" element={<LoginPage />} />
  
  <Route path="/" element={<ProtectedRoute><MainLayout /></ProtectedRoute>}>
    <Route path="dashboard" element={<DashboardPage />} />
    
    {/* Protected by role */}
    <Route 
      path="admin" 
      element={
        <ProtectedRoute requiredRoles={['ADMIN']}>
          <AdminPage />
        </ProtectedRoute>
      } 
    />
  </Route>
</Routes>
```

## 📝 Best Practices

### 1. Component Organization
- Keep components small and focused
- Extract reusable logic to custom hooks
- Use composition over inheritance

### 2. Type Safety
- Always define TypeScript interfaces
- Avoid `any` type
- Use generics for reusable components

### 3. Error Handling
- Use try-catch for async operations
- Show user-friendly error messages
- Log errors for debugging

### 4. Performance
- Use React.memo for expensive renders
- Lazy load routes with React.lazy()
- Debounce search inputs
- Paginate large data sets

### 5. Accessibility
- Use semantic HTML
- Add ARIA labels
- Keyboard navigation support
- Color contrast compliance

## 🧪 Testing (TODO)

```typescript
// Unit tests with Vitest
import { render, screen } from '@testing-library/react';
import { Component } from './Component';

test('renders component', () => {
  render(<Component />);
  expect(screen.getByText('Hello')).toBeInTheDocument();
});
```

## 🚀 Deployment

### Build cho production

```bash
npm run build
```

### Environment Variables

Development: `.env`
Production: `.env.production`

```env
VITE_API_BASE_URL=https://api.production.com/api/v1
VITE_WS_URL=wss://api.production.com/api/v1/ws
```

### Serve static files

Build output trong `dist/` có thể serve bằng:
- Nginx
- Apache
- Vercel
- Netlify
- AWS S3 + CloudFront

### Nginx Configuration Example

```nginx
server {
  listen 80;
  server_name app.scalehub.com;
  root /var/www/scalehub-frontend/dist;
  index index.html;

  location / {
    try_files $uri $uri/ /index.html;
  }

  location /api {
    proxy_pass http://backend:8080;
  }
}
```

## 📚 Resources

- [React Documentation](https://react.dev)
- [Vite Documentation](https://vitejs.dev)
- [TanStack Query](https://tanstack.com/query)
- [Tailwind CSS](https://tailwindcss.com)
- [TypeScript Handbook](https://www.typescriptlang.org/docs)

## 🐛 Common Issues

### WebSocket không kết nối
- Kiểm tra backend WebSocket endpoint
- Verify CORS configuration
- Check network/firewall settings

### Token refresh loop
- Clear localStorage
- Check refresh token endpoint
- Verify token expiration times

### Build errors
- Delete node_modules và reinstall
- Clear Vite cache: `rm -rf node_modules/.vite`
- Check TypeScript errors: `npx tsc --noEmit`

## 🎯 Roadmap

### Phase 1 (Completed)
- ✅ Basic authentication
- ✅ Dashboard với real-time data
- ✅ Scale management
- ✅ User management
- ✅ Location management
- ✅ Reports

### Phase 2 (Future)
- [ ] Scale detail page with tabs
- [ ] Create/Edit forms cho tất cả entities
- [ ] Advanced filtering và sorting
- [ ] Bulk operations
- [ ] File upload/download
- [ ] Role permissions matrix UI

### Phase 3 (Future)
- [ ] Dark mode
- [ ] Mobile responsive optimization
- [ ] PWA support
- [ ] Offline mode
- [ ] Push notifications
- [ ] Multi-language support (i18n)
