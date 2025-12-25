import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';

interface ProtectedRouteProps {
  children: React.ReactNode;
  requiredPermissions?: string[];
  requiredRoles?: string[];
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({
  children,
  requiredPermissions = [],
  requiredRoles = [],
}) => {
  const { isAuthenticated, user } = useAuth();

  // Check if user is authenticated
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  // Check if user has required roles
  // User object from getCurrentUser() has nested roles structure: user.roles[].name
  // But we should also support JWT which may have direct roles array
  if (requiredRoles.length > 0 && user) {
    let userRoles: string[] = [];
    
    // Check if roles is array of objects (from API)
    if (user.roles && Array.isArray(user.roles)) {
      userRoles = user.roles.map(role => {
        // Handle both { id, name, code } from API and plain string from JWT
        // Prioritize role.code over role.name since code is the identifier
        return typeof role === 'string' ? role : role.code || role.name;
      });
    }
    
    const hasRequiredRole = requiredRoles.some(role => 
      userRoles.some(userRole => 
        userRole.toUpperCase() === role.toUpperCase()
      )
    );
    
    if (!hasRequiredRole) {
      return <Navigate to="/unauthorized" replace />;
    }
  }

  // Check if user has required permissions
  if (requiredPermissions.length > 0 && user) {
    const userPermissions = user.roles && Array.isArray(user.roles)
      ? user.roles.flatMap(role => {
          // Only process if role is an object with permissions
          if (typeof role === 'object' && role.permissions) {
            return role.permissions.map(p => `${p.resource}:${p.action}`);
          }
          return [];
        })
      : [];
    
    const hasRequiredPermissions = requiredPermissions.every(permission =>
      userPermissions.includes(permission)
    );
    
    if (!hasRequiredPermissions) {
      return <Navigate to="/unauthorized" replace />;
    }
  }

  return <>{children}</>;
};
