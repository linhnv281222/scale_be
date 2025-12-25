# ScaleHub IoT Frontend

Modern React-based web application for managing IoT scale devices with real-time monitoring.

## 🚀 Features

- **Real-time Dashboard**: Monitor scale status and weighing data in real-time via WebSocket
- **Scale Management**: CRUD operations for scale devices with detailed configuration
- **User Management**: Manage users with role-based access control
- **Location Hierarchy**: Organize scales in a tree-structured location system
- **Reports & Analytics**: View and export weighing logs with filtering capabilities
- **Responsive Design**: Enterprise-grade UI built with Tailwind CSS
- **Type Safety**: Full TypeScript support for better development experience

## 🛠️ Tech Stack

- **React 18** with TypeScript
- **Vite** for fast development and building
- **React Router** for routing
- **TanStack Query** (React Query) for server state management
- **Axios** for API calls with interceptors
- **Tailwind CSS** for styling
- **Recharts** for data visualization
- **SockJS + STOMP** for WebSocket real-time communication
- **React Toastify** for notifications
- **Lucide React** for icons
- **date-fns** for date formatting

## 📋 Prerequisites

- Node.js 16+ and npm
- Backend API running on `http://localhost:8080`

## 🔧 Installation

1. Navigate to the project directory:
```bash
cd scalehub-frontend
```

2. Install dependencies (đã cài đặt sẵn):
```bash
npm install
```

3. Environment variables đã được cấu hình trong `.env`

## 🚀 Development

Start the development server:
```bash
npm run dev
```

The application will be available at `http://localhost:5173`

## 🏗️ Build

Build for production:
```bash
npm run build
```

Preview production build:
```bash
npm run preview
```

## 📁 Project Structure

```
src/
├── api/              # API client and service modules
│   ├── client.ts     # Axios instance with interceptors
│   ├── auth.ts       # Authentication APIs
│   ├── scales.ts     # Scale management APIs
│   ├── users.ts      # User management APIs
│   ├── roles.ts      # Role & permission APIs
│   ├── locations.ts  # Location APIs
│   └── reports.ts    # Reporting APIs
├── components/       # Reusable components
│   ├── auth/         # Authentication components
│   ├── layout/       # Layout components (Sidebar, Header)
│   └── dashboard/    # Dashboard-specific components
├── contexts/         # React contexts
│   ├── AuthContext.tsx      # Authentication state
│   └── WebSocketContext.tsx # WebSocket connection
├── pages/            # Page components
│   ├── auth/         # Login page
│   ├── dashboard/    # Dashboard page
│   ├── scales/       # Scale management pages
│   ├── users/        # User management pages
│   ├── locations/    # Location management pages
│   └── reports/      # Reports pages
├── types/            # TypeScript type definitions
│   └── index.ts      # All types and interfaces
├── App.tsx           # Main app component with routing
├── main.tsx          # Application entry point
└── index.css         # Global styles with Tailwind
```

## 🎨 Design System

### Color Palette
- **Primary**: `#1E40AF` (Royal Blue)
- **Background**: `#F1F5F9` (Cool Gray)
- **Surface**: `#FFFFFF` (White)
- **Text Main**: `#1E293B` (Deep Slate)
- **Text Muted**: `#64748B` (Gray)
- **Success**: `#10B981` (Green)
- **Error**: `#EF4444` (Red)
- **Warning**: `#F59E0B` (Amber)

## 🔐 Authentication

Default credentials for testing:
- Username: `admin`
- Password: `admin123`

## 📊 Available Pages

1. **Dashboard** (`/dashboard`) - System overview with real-time updates
2. **Scales** (`/scales`) - Scale management
3. **Users** (`/users`) - User management (ADMIN/MANAGER only)
4. **Locations** (`/locations`) - Location hierarchy (ADMIN/MANAGER only)
5. **Reports** (`/reports`) - Weighing logs and export

## 🔌 Real-time Features

WebSocket connection for:
- Scale status changes
- Live weight readings
- System alerts

## 🛡️ Role-Based Access Control

Three roles: **ADMIN**, **MANAGER**, **USER**
Routes are protected based on user roles.
