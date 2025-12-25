import React, { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus, Edit2, Trash2, Loader, Shield } from 'lucide-react';
import { toast } from 'react-toastify';
import { rolesApi, permissionsApi } from '../../api/roles';
import { format } from 'date-fns';

export const RoleListPage: React.FC = () => {
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [editingRole, setEditingRole] = useState<any>(null);
  const [formData, setFormData] = useState({
    name: '',
    code: '',
    permissionIds: [] as number[],
  });
  const queryClient = useQueryClient();

  // Fetch roles
  const { data: roles = [], isLoading: rolesLoading } = useQuery({
    queryKey: ['roles-full'],
    queryFn: () => rolesApi.getAllRoles(),
  });

  // Fetch permissions for dropdown
  const { data: permissions = [] } = useQuery({
    queryKey: ['permissions'],
    queryFn: () => permissionsApi.getAllPermissions(),
  });

  // Create role mutation
  const createMutation = useMutation({
    mutationFn: () => rolesApi.createRole(formData as any),
    onSuccess: () => {
      toast.success('Role created successfully');
      setShowCreateModal(false);
      resetForm();
      queryClient.invalidateQueries({ queryKey: ['roles-full'] });
    },
    onError: (error) => {
      toast.error(error instanceof Error ? error.message : 'Failed to create role');
    },
  });

  // Update role mutation
  const updateMutation = useMutation({
    mutationFn: () => rolesApi.updateRole(editingRole.id, formData as any),
    onSuccess: () => {
      toast.success('Role updated successfully');
      setEditingRole(null);
      resetForm();
      queryClient.invalidateQueries({ queryKey: ['roles-full'] });
    },
    onError: (error) => {
      toast.error(error instanceof Error ? error.message : 'Failed to update role');
    },
  });

  // Delete role mutation
  const deleteMutation = useMutation({
    mutationFn: (id: number) => rolesApi.deleteRole(id),
    onSuccess: () => {
      toast.success('Role deleted successfully');
      queryClient.invalidateQueries({ queryKey: ['roles-full'] });
    },
    onError: (error) => {
      toast.error(error instanceof Error ? error.message : 'Failed to delete role');
    },
  });

  const resetForm = () => {
    setFormData({
      name: '',
      code: '',
      permissionIds: [],
    });
  };

  const handleCreateClick = () => {
    setEditingRole(null);
    resetForm();
    setShowCreateModal(true);
  };

  const handleEditClick = (role: any) => {
    setEditingRole(role);
    setFormData({
      name: role.name,
      code: role.code,
      permissionIds: role.permissions?.map((p: any) => p.id) || [],
    });
    setShowCreateModal(true);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (!formData.name.trim()) {
      toast.error('Role name is required');
      return;
    }

    if (!formData.code.trim()) {
      toast.error('Role code is required');
      return;
    }

    if (formData.permissionIds.length === 0) {
      toast.error('At least one permission is required');
      return;
    }

    if (editingRole) {
      updateMutation.mutate();
    } else {
      createMutation.mutate();
    }
  };

  const handlePermissionToggle = (permissionId: number) => {
    setFormData(prev => ({
      ...prev,
      permissionIds: prev.permissionIds.includes(permissionId)
        ? prev.permissionIds.filter(id => id !== permissionId)
        : [...prev.permissionIds, permissionId],
    }));
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-textMain flex items-center gap-2">
            <Shield className="w-8 h-8" />
            Role Management
          </h1>
          <p className="text-textMuted">Manage system roles and permissions</p>
        </div>
        <button
          onClick={handleCreateClick}
          className="btn-primary flex items-center gap-2"
        >
          <Plus className="w-5 h-5" />
          Add Role
        </button>
      </div>

      {/* Roles Table */}
      <div className="card overflow-hidden">
        {rolesLoading ? (
          <div className="p-12 text-center">
            <Loader className="w-8 h-8 animate-spin text-primary mx-auto mb-2" />
            <p className="text-textMuted">Loading roles...</p>
          </div>
        ) : roles.length === 0 ? (
          <div className="p-12 text-center text-textMuted">
            No roles found. Click "Add Role" to create one.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-textMuted uppercase tracking-wider">
                    Name
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-textMuted uppercase tracking-wider">
                    Code
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-textMuted uppercase tracking-wider">
                    Permissions
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-textMuted uppercase tracking-wider">
                    Created
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-textMuted uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="bg-surface divide-y divide-gray-200">
                {roles.map((role: any) => (
                  <tr key={role.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm font-medium text-textMain">{role.name}</div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <code className="bg-gray-100 px-2 py-1 rounded text-xs text-textMain">
                        {role.code}
                      </code>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex gap-1 flex-wrap max-w-xs">
                        {role.permissions?.slice(0, 3).map((perm: any) => (
                          <span
                            key={perm.id}
                            className="px-2 py-1 bg-green-100 text-green-800 rounded-full text-xs"
                            title={perm.name}
                          >
                            {perm.action}
                          </span>
                        ))}
                        {role.permissions?.length > 3 && (
                          <span className="px-2 py-1 bg-gray-100 text-gray-800 rounded-full text-xs">
                            +{role.permissions.length - 3}
                          </span>
                        )}
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-textMuted">
                      {role.createdAt ? format(new Date(role.createdAt), 'dd/MM/yyyy') : '-'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-right">
                      <div className="flex items-center justify-end gap-2">
                        <button
                          onClick={() => handleEditClick(role)}
                          className="p-2 hover:bg-blue-100 rounded-lg transition-colors"
                          title="Edit"
                        >
                          <Edit2 className="w-4 h-4 text-primary" />
                        </button>
                        <button
                          onClick={() => {
                            if (confirm('Are you sure?')) {
                              deleteMutation.mutate(role.id);
                            }
                          }}
                          disabled={deleteMutation.isPending}
                          className="p-2 hover:bg-red-100 rounded-lg transition-colors disabled:opacity-50"
                          title="Delete"
                        >
                          <Trash2 className="w-4 h-4 text-statusError" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Create/Edit Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="card w-full max-w-3xl max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-2xl font-bold text-textMain">
                {editingRole ? 'Edit Role' : 'Create New Role'}
              </h2>
              <button
                onClick={() => setShowCreateModal(false)}
                className="text-textMuted hover:text-textMain"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
              {/* Name */}
              <div>
                <label className="block text-sm font-medium text-textMain mb-1">
                  Role Name <span className="text-statusError">*</span>
                </label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  className="input-field w-full"
                  placeholder="e.g., Administrator"
                  required
                />
              </div>

              {/* Code */}
              <div>
                <label className="block text-sm font-medium text-textMain mb-1">
                  Role Code <span className="text-statusError">*</span>
                </label>
                <input
                  type="text"
                  value={formData.code}
                  onChange={(e) => setFormData({ ...formData, code: e.target.value.toUpperCase() })}
                  className="input-field w-full"
                  placeholder="e.g., ADMIN"
                  required
                  disabled={!!editingRole}
                />
              </div>

              {/* Permissions */}
              <div>
                <label className="block text-sm font-medium text-textMain mb-2">
                  Permissions <span className="text-statusError">*</span>
                </label>
                <div className="border border-gray-300 rounded-lg p-3 space-y-2 max-h-64 overflow-y-auto">
                  {permissions.length === 0 ? (
                    <p className="text-sm text-textMuted">No permissions available</p>
                  ) : (
                    <div className="grid grid-cols-2 gap-3">
                      {permissions.map((perm: any) => (
                        <label key={perm.id} className="flex items-start gap-2 cursor-pointer">
                          <input
                            type="checkbox"
                            checked={formData.permissionIds.includes(perm.id)}
                            onChange={() => handlePermissionToggle(perm.id)}
                            className="w-4 h-4 rounded mt-0.5"
                          />
                          <div className="flex-1">
                            <div className="text-sm font-medium text-textMain">{perm.name}</div>
                            {perm.description && (
                              <div className="text-xs text-textMuted">{perm.description}</div>
                            )}
                          </div>
                        </label>
                      ))}
                    </div>
                  )}
                </div>
                {formData.permissionIds.length > 0 && (
                  <p className="text-xs text-textMuted mt-2">
                    {formData.permissionIds.length} permission{formData.permissionIds.length !== 1 ? 's' : ''} selected
                  </p>
                )}
              </div>

              {/* Buttons */}
              <div className="flex gap-3 pt-4 border-t border-gray-200">
                <button
                  type="submit"
                  disabled={createMutation.isPending || updateMutation.isPending}
                  className="btn-primary flex items-center gap-2"
                >
                  {createMutation.isPending || updateMutation.isPending ? (
                    <>
                      <Loader className="w-4 h-4 animate-spin" />
                      Saving...
                    </>
                  ) : (
                    'Save'
                  )}
                </button>
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="btn-secondary"
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
