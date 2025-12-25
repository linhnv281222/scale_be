import React from 'react';
import { NavLink } from 'react-router-dom';
import { 
  LayoutDashboard, 
  Scale, 
  Users, 
  Shield, 
  MapPin, 
  FileText,
  Activity
} from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';

interface MenuItem {
  path: string;
  label: string;
  icon: React.ReactNode;
  requiredRoles?: string[];
}

export const Sidebar: React.FC = () => {
  const { user } = useAuth();

  const menuItems: MenuItem[] = [
    {
      path: '/dashboard',
      label: 'Dashboard',
      icon: <LayoutDashboard className="w-5 h-5" />,
    },
    {
      path: '/monitoring',
      label: 'Monitoring',
      icon: <Activity className="w-5 h-5" />,
    },
    {
      path: '/scales',
      label: 'Scales',
      icon: <Scale className="w-5 h-5" />,
    },
    {
      path: '/users',
      label: 'Users',
      icon: <Users className="w-5 h-5" />,
      requiredRoles: ['ADMIN', 'MANAGE'],
    },
    {
      path: '/roles',
      label: 'Roles & Permissions',
      icon: <Shield className="w-5 h-5" />,
      requiredRoles: ['ADMIN'],
    },
    {
      path: '/locations',
      label: 'Locations',
      icon: <MapPin className="w-5 h-5" />,
      requiredRoles: ['ADMIN', 'MANAGE'],
    },
    {
      path: '/reports',
      label: 'Reports',
      icon: <FileText className="w-5 h-5" />,
    },
  ];

  const hasRole = (requiredRoles?: string[]): boolean => {
    if (!requiredRoles || requiredRoles.length === 0) return true;
    if (!user) return false;
    
    const userRoles = user.roles.map(role => {
      // Handle both { id, name, code } from API and plain string from JWT
      // Prioritize role.code over role.name since code is the identifier
      return typeof role === 'string' ? role : role.code || role.name;
    });
    
    return requiredRoles.some(role => 
      userRoles.some(userRole => 
        userRole.toUpperCase() === role.toUpperCase()
      )
    );
  };

  const filteredMenuItems = menuItems.filter(item => hasRole(item.requiredRoles));

  return (
    <aside className="w-64 bg-primary min-h-screen flex flex-col">
      <div className="p-6">
        <div className="flex items-center gap-3">
          <Scale className="w-8 h-8 text-white" />
          <div>
            <h1 className="text-xl font-bold text-white">ScaleHub IoT</h1>
            <p className="text-xs text-white/70">Scale Management</p>
          </div>
        </div>
      </div>

      <nav className="flex-1 px-3 space-y-1">
        {filteredMenuItems.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            className={({ isActive }) =>
              `sidebar-link ${isActive ? 'sidebar-link-active' : ''}`
            }
          >
            {item.icon}
            <span>{item.label}</span>
          </NavLink>
        ))}
      </nav>

      <div className="p-4 border-t border-white/10">
        <div className="flex items-center gap-3 text-white">
          <div className="w-10 h-10 rounded-full bg-white/20 flex items-center justify-center">
            <Users className="w-5 h-5" />
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-sm font-medium truncate">{user?.fullName}</p>
            <p className="text-xs text-white/70 truncate">
              {user?.roles.map(r => r.name).join(', ')}
            </p>
          </div>
        </div>
      </div>
    </aside>
  );
};
