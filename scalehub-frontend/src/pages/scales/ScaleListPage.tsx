import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus, Edit, Trash2, Eye, Search } from 'lucide-react';
import { scalesApi } from '../../api/scales';
import { locationsApi } from '../../api/locations';
// ...existing code...
import { toast } from 'react-toastify';
import { format } from 'date-fns';

export const ScaleListPage: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [locationFilter, setLocationFilter] = useState<string>('');
  const [page, setPage] = useState(0);
  const pageSize = 10;

  // Fetch scales
  const { data: scales, isLoading } = useQuery({
    queryKey: ['scales', page, statusFilter, locationFilter],
    queryFn: () => scalesApi.getAllScales({
      page,
      size: pageSize,
      status: statusFilter || undefined,
      locationId: locationFilter ? parseInt(locationFilter) : undefined,
    }),
  });

  // Fetch locations for filter
  const { data: locations } = useQuery({
    queryKey: ['locations'],
    queryFn: () => locationsApi.getAllLocations(),
  });

  // Delete mutation
  const deleteMutation = useMutation({
    mutationFn: (id: number) => scalesApi.deleteScale(id),
    onSuccess: () => {
      toast.success('Scale deleted successfully');
      queryClient.invalidateQueries({ queryKey: ['scales'] });
    },
    onError: () => {
      toast.error('Failed to delete scale');
    },
  });

  const handleDelete = (id: number, name: string) => {
    if (window.confirm(`Are you sure you want to delete scale "${name}"?`)) {
      deleteMutation.mutate(id);
    }
  };

  const scalesList = scales || [];

  // Filter scales by search term
  const filteredScales = scalesList.filter(scale =>
    scale.name.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const getStatusBadge = (status: "ONLINE" | "OFFLINE" | "ERROR" | "MAINTENANCE") => {
    const styles = {
      ONLINE: 'bg-statusSuccess/10 text-statusSuccess',
      OFFLINE: 'bg-gray-100 text-textMuted',
      ERROR: 'bg-statusError/10 text-statusError',
      MAINTENANCE: 'bg-statusWarning/10 text-statusWarning',
    };

    return (
      <span className={`px-3 py-1 rounded-full text-sm font-medium ${styles[status]}`}>
        {status}
      </span>
    );
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-textMain mb-2">Scales</h1>
          <p className="text-textMuted">Manage your scale devices</p>
        </div>
        <button
          onClick={() => navigate('/scales/create')}
          className="btn-primary flex items-center gap-2"
        >
          <Plus className="w-5 h-5" />
          Add Scale
        </button>
      </div>

      {/* Filters */}
      <div className="card">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-textMuted" />
            <input
              type="text"
              placeholder="Search scales..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="input-field pl-10"
            />
          </div>

          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="input-field"
          >
            <option value="">All Statuses</option>
            <option value="ONLINE">Online</option>
            <option value="OFFLINE">Offline</option>
            <option value="ERROR">Error</option>
            <option value="MAINTENANCE">Maintenance</option>
          </select>

          <select
            value={locationFilter}
            onChange={(e) => setLocationFilter(e.target.value)}
            className="input-field"
          >
            <option value="">All Locations</option>
            {locations?.map(loc => (
              <option key={loc.id} value={loc.id}>{loc.name}</option>
            ))}
          </select>
        </div>
      </div>

      {/* Table */}
      <div className="card overflow-hidden">
        {isLoading ? (
          <div className="p-12 text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto"></div>
          </div>
        ) : filteredScales.length === 0 ? (
          <div className="p-12 text-center text-textMuted">
            No scales found
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-textMuted uppercase tracking-wider">
                    ID
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-textMuted uppercase tracking-wider">
                    Name
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-textMuted uppercase tracking-wider">
                    Model
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-textMuted uppercase tracking-wider">
                    Location
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-textMuted uppercase tracking-wider">
                    Protocol
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-textMuted uppercase tracking-wider">
                    Status
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-textMuted uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="bg-surface divide-y divide-gray-200">
                {filteredScales.map((scale) => (
                  <tr key={scale.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-textMuted">
                      #{scale.id}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-textMain">
                      {scale.name}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-textMain">
                      {scale.model || '-'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-textMain">
                      {scale.location_name}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-textMain">
                      {scale.scale_config?.protocol || '-'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className={`px-3 py-1 rounded-full text-sm font-medium ${
                        scale.is_active 
                          ? 'bg-statusSuccess/10 text-statusSuccess' 
                          : 'bg-gray-100 text-textMuted'
                      }`}>
                        {scale.is_active ? 'ACTIVE' : 'INACTIVE'}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                      <div className="flex items-center justify-end gap-2">
                        <button
                          onClick={() => navigate(`/scales/${scale.id}`)}
                          className="p-2 text-primary hover:bg-primary/10 rounded-lg transition-colors"
                          title="View"
                        >
                          <Eye className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => navigate(`/scales/${scale.id}/edit`)}
                          className="p-2 text-statusWarning hover:bg-statusWarning/10 rounded-lg transition-colors"
                          title="Edit"
                        >
                          <Edit className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => handleDelete(scale.id, scale.name)}
                          className="p-2 text-statusError hover:bg-statusError/10 rounded-lg transition-colors"
                          title="Delete"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* No pagination needed - API returns full list */}
      </div>
    </div>
  );
};
