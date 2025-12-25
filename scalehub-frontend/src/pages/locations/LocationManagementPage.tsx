import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus, Edit, Trash2, ChevronRight, ChevronDown, X, Loader } from 'lucide-react';
import { locationsApi } from '../../api/locations';
import type { LocationTree, CreateLocationRequest, UpdateLocationRequest } from '../../types';
import { toast } from 'react-toastify';

interface TreeNodeProps {
  node: LocationTree;
  onEdit: (id: number, name: string, code: string, description?: string, parentId?: number) => void;
  onDelete: (id: number, name: string) => void;
}

// Export named for testing
export { TreeNodeProps };

const TreeNode: React.FC<TreeNodeProps> = ({ node, onEdit, onDelete }) => {
  const [expanded, setExpanded] = useState(true);
  const hasChildren = node.children && node.children.length > 0;

  return (
    <div className="ml-4">
      <div className="flex items-center gap-2 py-2 px-3 hover:bg-gray-50 rounded-lg group">
        {hasChildren ? (
          <button
            onClick={() => setExpanded(!expanded)}
            className="p-1 hover:bg-gray-200 rounded"
          >
            {expanded ? (
              <ChevronDown className="w-4 h-4 text-textMuted" />
            ) : (
              <ChevronRight className="w-4 h-4 text-textMuted" />
            )}
          </button>
        ) : (
          <div className="w-6"></div>
        )}
        
        <div className="flex-1 flex items-center gap-3">
          <div>
            <div className="font-medium text-textMain">{node.name}</div>
            <div className="text-sm text-textMuted">
              Code: {node.code} | Level: {node.level}
              {node.scaleCount !== undefined && ` | Scales: ${node.scaleCount}`}
            </div>
          </div>
        </div>

        <div className="opacity-0 group-hover:opacity-100 flex items-center gap-1 transition-opacity">
          <button
            onClick={() => onEdit(node.id, node.name, node.code, node.description, node.parentId)}
            className="p-2 text-statusWarning hover:bg-statusWarning/10 rounded-lg transition-colors"
            title="Edit"
          >
            <Edit className="w-4 h-4" />
          </button>
          <button
            onClick={() => onDelete(node.id, node.name)}
            className="p-2 text-statusError hover:bg-statusError/10 rounded-lg transition-colors"
            title="Delete"
          >
            <Trash2 className="w-4 h-4" />
          </button>
        </div>
      </div>

      {expanded && hasChildren && (
        <div className="ml-2 border-l-2 border-gray-200">
          {node.children.map(child => (
            <TreeNode
              key={child.id}
              node={child}
              onEdit={onEdit}
              onDelete={onDelete}
            />
          ))}
        </div>
      )}
    </div>
  );
};

export const LocationManagementPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [parentId, setParentId] = useState<number | null>(null);
  const [parentName, setParentName] = useState<string | null>(null);
  const [formData, setFormData] = useState({
    name: '',
    code: '',
    description: '',
  });

  // Fetch location tree
  const { data: locationTree, isLoading } = useQuery({
    queryKey: ['locations-tree'],
    queryFn: () => locationsApi.getLocationsTree(),
  });

  // Create mutation
  const createMutation = useMutation({
    mutationFn: (data: CreateLocationRequest) => locationsApi.createLocation(data),
    onSuccess: () => {
      toast.success('Location created successfully');
      setShowCreateModal(false);
      resetForm();
      queryClient.invalidateQueries({ queryKey: ['locations-tree'] });
    },
    onError: (error) => {
      toast.error(error instanceof Error ? error.message : 'Failed to create location');
    },
  });

  // Update mutation
  const updateMutation = useMutation({
    mutationFn: (data: UpdateLocationRequest) => 
      locationsApi.updateLocation(editingId!, data),
    onSuccess: () => {
      toast.success('Location updated successfully');
      setShowEditModal(false);
      resetForm();
      queryClient.invalidateQueries({ queryKey: ['locations-tree'] });
    },
    onError: (error) => {
      toast.error(error instanceof Error ? error.message : 'Failed to update location');
    },
  });

  // Delete mutation
  const deleteMutation = useMutation({
    mutationFn: (id: number) => locationsApi.deleteLocation(id),
    onSuccess: () => {
      toast.success('Location deleted successfully');
      queryClient.invalidateQueries({ queryKey: ['locations-tree'] });
    },
    onError: (error) => {
      toast.error(error instanceof Error ? error.message : 'Failed to delete location');
    },
  });

  const resetForm = () => {
    setFormData({ name: '', code: '', description: '' });
    setEditingId(null);
    setParentId(null);
    setParentName(null);
  };

  const handleCreateRoot = () => {
    resetForm();
    setParentId(null);
    setParentName(null);
    setShowCreateModal(true);
  };

  const handleEdit = (id: number, name: string, code: string, description?: string, parentId?: number) => {
    setEditingId(id);
    setFormData({ name, code, description: description || '' });
    setParentId(parentId || null);
    setShowEditModal(true);
  };

  const handleDelete = (id: number, name: string) => {
    if (confirm(`Are you sure you want to delete "${name}"?`)) {
      deleteMutation.mutate(id);
    }
  };

  const handleCreateSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!formData.name.trim()) {
      toast.error('Location name is required');
      return;
    }

    if (!formData.code.trim()) {
      toast.error('Location code is required');
      return;
    }

    const payload: CreateLocationRequest = {
      name: formData.name.trim(),
      code: formData.code.trim().toUpperCase(),
      description: formData.description.trim() || undefined,
      parentId: parentId || undefined,
    };

    createMutation.mutate(payload);
  };

  const handleEditSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (!formData.name.trim()) {
      toast.error('Location name is required');
      return;
    }

    const payload: UpdateLocationRequest = {
      name: formData.name.trim(),
      description: formData.description.trim() || undefined,
      parentId: parentId || undefined,
    };

    updateMutation.mutate(payload);
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-screen">
        <div className="flex flex-col items-center gap-3">
          <Loader className="w-6 h-6 text-primary animate-spin" />
          <p className="text-textMuted">Loading locations...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="p-8">
      <div className="max-w-4xl mx-auto">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-3xl font-bold text-textMain">Location Management</h1>
            <p className="text-textMuted mt-2">Manage your location hierarchy</p>
          </div>
          <button
            onClick={handleCreateRoot}
            className="flex items-center gap-2 px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 transition-colors"
          >
            <Plus className="w-4 h-4" />
            Add Location
          </button>
        </div>

        {locationTree && locationTree.length > 0 ? (
          <div className="bg-white rounded-lg border border-borderColor">
            <div className="p-6 border-b border-borderColor">
              <h2 className="text-lg font-semibold text-textMain">Locations</h2>
            </div>
            <div className="p-4">
              {locationTree.map(location => (
                <TreeNode
                  key={location.id}
                  node={location}
                  onEdit={handleEdit}
                  onDelete={handleDelete}
                />
              ))}
            </div>
          </div>
        ) : (
          <div className="text-center py-12 bg-white rounded-lg border border-borderColor">
            <p className="text-textMuted">No locations found</p>
          </div>
        )}
      </div>

      {/* Create Location Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-md">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-bold text-textMain">Add Location</h2>
              <button
                onClick={() => setShowCreateModal(false)}
                className="p-1 hover:bg-gray-100 rounded-lg"
              >
                <X className="w-5 h-5 text-textMuted" />
              </button>
            </div>

            <form onSubmit={handleCreateSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-textMain mb-2">
                  Location Name *
                </label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  placeholder="e.g., Warehouse A"
                  className="w-full px-3 py-2 border border-borderColor rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20"
                  disabled={createMutation.isPending}
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-textMain mb-2">
                  Location Code *
                </label>
                <input
                  type="text"
                  value={formData.code}
                  onChange={(e) => setFormData({ ...formData, code: e.target.value.toUpperCase() })}
                  placeholder="e.g., WH_A"
                  className="w-full px-3 py-2 border border-borderColor rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20 uppercase"
                  disabled={createMutation.isPending}
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-textMain mb-2">
                  Parent Location (Optional)
                </label>
                <select
                  value={parentId || ''}
                  onChange={(e) => setParentId(e.target.value ? Number(e.target.value) : null)}
                  className="w-full px-3 py-2 border border-borderColor rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20"
                  disabled={createMutation.isPending}
                >
                  <option value="">No Parent (Root Location)</option>
                  {locationTree?.map(location => (
                    <option key={location.id} value={location.id}>
                      {location.name} ({location.code})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-textMain mb-2">
                  Description (Optional)
                </label>
                <textarea
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  placeholder="Add a description..."
                  rows={3}
                  className="w-full px-3 py-2 border border-borderColor rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20"
                  disabled={createMutation.isPending}
                />
              </div>

              <div className="flex gap-3 pt-4">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="flex-1 px-4 py-2 text-textMain bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
                  disabled={createMutation.isPending}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="flex-1 px-4 py-2 text-white bg-primary hover:bg-primary/90 rounded-lg transition-colors disabled:opacity-50 flex items-center justify-center gap-2"
                  disabled={createMutation.isPending}
                >
                  {createMutation.isPending && <Loader className="w-4 h-4 animate-spin" />}
                  Create
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Edit Location Modal */}
      {showEditModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-md">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-bold text-textMain">Edit Location</h2>
              <button
                onClick={() => setShowEditModal(false)}
                className="p-1 hover:bg-gray-100 rounded-lg"
              >
                <X className="w-5 h-5 text-textMuted" />
              </button>
            </div>

            <form onSubmit={handleEditSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-textMain mb-2">
                  Location Name *
                </label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  placeholder="e.g., Warehouse A"
                  className="w-full px-3 py-2 border border-borderColor rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20"
                  disabled={updateMutation.isPending}
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-textMuted mb-2">
                  Location Code (Read-only)
                </label>
                <input
                  type="text"
                  value={formData.code}
                  className="w-full px-3 py-2 border border-borderColor rounded-lg bg-gray-50 text-textMuted cursor-not-allowed"
                  disabled
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-textMain mb-2">
                  Parent Location (Optional)
                </label>
                <select
                  value={parentId || ''}
                  onChange={(e) => setParentId(e.target.value ? Number(e.target.value) : null)}
                  className="w-full px-3 py-2 border border-borderColor rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20"
                  disabled={updateMutation.isPending}
                >
                  <option value="">No Parent (Root Location)</option>
                  {locationTree?.map(location => (
                    <option key={location.id} value={location.id}>
                      {location.name} ({location.code})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-textMain mb-2">
                  Description (Optional)
                </label>
                <textarea
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  placeholder="Add a description..."
                  rows={3}
                  className="w-full px-3 py-2 border border-borderColor rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20"
                  disabled={updateMutation.isPending}
                />
              </div>

              <div className="flex gap-3 pt-4">
                <button
                  type="button"
                  onClick={() => setShowEditModal(false)}
                  className="flex-1 px-4 py-2 text-textMain bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
                  disabled={updateMutation.isPending}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="flex-1 px-4 py-2 text-white bg-statusWarning hover:bg-statusWarning/90 rounded-lg transition-colors disabled:opacity-50 flex items-center justify-center gap-2"
                  disabled={updateMutation.isPending}
                >
                  {updateMutation.isPending && <Loader className="w-4 h-4 animate-spin" />}
                  Update
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
