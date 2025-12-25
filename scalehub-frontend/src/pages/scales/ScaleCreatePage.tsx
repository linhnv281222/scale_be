import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, Save, Loader } from 'lucide-react';
import { toast } from 'react-toastify';
import { scalesApi } from '../../api/scales';
import { locationsApi } from '../../api/locations';
import type { CreateScaleRequest } from '../../types';

export const ScaleCreatePage: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [formData, setFormData] = useState<CreateScaleRequest>({
    name: '',
    location_id: 0,
    model: '',
    is_active: true,
  });

  // Fetch locations for dropdown
  const { data: locations = [] } = useQuery({
    queryKey: ['locations'],
    queryFn: () => locationsApi.getAllLocations(),
  });

  // Create mutation
  const createMutation = useMutation({
    mutationFn: () => scalesApi.createScale(formData),
    onSuccess: (newScale) => {
      toast.success('Scale created successfully');
      queryClient.invalidateQueries({ queryKey: ['scales'] });
      navigate(`/scales/${newScale.id}/edit`);
    },
    onError: (error) => {
      toast.error(error instanceof Error ? error.message : 'Failed to create scale');
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (!formData.name.trim()) {
      toast.error('Scale name is required');
      return;
    }

    if (formData.location_id === 0) {
      toast.error('Location is required');
      return;
    }

    createMutation.mutate();
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-4">
        <button
          onClick={() => navigate('/scales')}
          className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
          title="Back"
        >
          <ArrowLeft className="w-5 h-5 text-primary" />
        </button>
        <div>
          <h1 className="text-3xl font-bold text-textMain">Create New Scale</h1>
          <p className="text-textMuted">Add a new scale to the system</p>
        </div>
      </div>

      {/* Create Form */}
      <form onSubmit={handleSubmit} className="card">
        <div className="space-y-6">
          {/* Name */}
          <div>
            <label htmlFor="name" className="block text-sm font-medium text-textMain mb-2">
              Scale Name <span className="text-statusError">*</span>
            </label>
            <input
              id="name"
              type="text"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              className="input-field w-full"
              placeholder="e.g., Cân 01"
              required
            />
            <p className="text-xs text-textMuted mt-1">Tên thiết bị cân (bắt buộc)</p>
          </div>

          {/* Model */}
          <div>
            <label htmlFor="model" className="block text-sm font-medium text-textMain mb-2">
              Model
            </label>
            <input
              id="model"
              type="text"
              value={formData.model || ''}
              onChange={(e) => setFormData({ ...formData, model: e.target.value })}
              className="input-field w-full"
              placeholder="e.g., IND570"
            />
            <p className="text-xs text-textMuted mt-1">Model của thiết bị cân (tùy chọn)</p>
          </div>

          {/* Location */}
          <div>
            <label htmlFor="location" className="block text-sm font-medium text-textMain mb-2">
              Location <span className="text-statusError">*</span>
            </label>
            <select
              id="location"
              value={formData.location_id}
              onChange={(e) => setFormData({ ...formData, location_id: parseInt(e.target.value) })}
              className="input-field w-full"
              required
            >
              <option value={0}>Select a location</option>
              {locations.map((location: any) => (
                <option key={location.id} value={location.id}>
                  {location.name}
                </option>
              ))}
            </select>
            <p className="text-xs text-textMuted mt-1">Vị trí đặt thiết bị cân (bắt buộc)</p>
          </div>

          {/* Status */}
          <div className="flex items-center gap-3">
            <input
              id="is_active"
              type="checkbox"
              checked={formData.is_active !== false}
              onChange={(e) => setFormData({ ...formData, is_active: e.target.checked })}
              className="w-4 h-4 rounded border-gray-300 text-primary focus:ring-primary"
            />
            <label htmlFor="is_active" className="text-sm font-medium text-textMain">
              Active
            </label>
          </div>

          {/* Info Box */}
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <h3 className="font-semibold text-blue-900 mb-2">Note</h3>
            <ul className="text-sm text-blue-800 space-y-1 list-disc list-inside">
              <li>Configuration của scale sẽ được thêm sau khi tạo</li>
              <li>Bạn có thể chỉnh sửa model, location, và status bất kỳ lúc nào</li>
              <li>Để cấu hình protocol, connection params và data channels, hãy vào trang Edit Scale</li>
            </ul>
          </div>

          {/* Buttons */}
          <div className="flex gap-3 pt-6 border-t border-gray-200">
            <button
              type="submit"
              disabled={createMutation.isPending}
              className="btn-primary flex items-center gap-2"
            >
              {createMutation.isPending ? (
                <>
                  <Loader className="w-4 h-4 animate-spin" />
                  Creating...
                </>
              ) : (
                <>
                  <Save className="w-4 h-4" />
                  Create Scale
                </>
              )}
            </button>
            <button
              type="button"
              onClick={() => navigate('/scales')}
              className="btn-secondary"
            >
              Cancel
            </button>
          </div>
        </div>
      </form>
    </div>
  );
};
