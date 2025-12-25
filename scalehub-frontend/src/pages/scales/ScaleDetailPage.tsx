import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { ArrowLeft, Loader, Edit2 } from 'lucide-react';
import { scalesApi } from '../../api/scales';
import { useWebSocket } from '../../contexts/WebSocketContext';
import type { Scale, ScaleRealtimeData } from '../../types';

export const ScaleDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { scaleData, subscribe } = useWebSocket();
  const [selectedTab, setSelectedTab] = useState<'info' | 'config'>('info');

  const { data: scale, isLoading, error } = useQuery({
    queryKey: ['scale', id],
    queryFn: () => scalesApi.getScaleById(Number(id)),
    enabled: !!id,
  });

  // Subscribe to this scale's realtime data updates
  useEffect(() => {
    const scaleId = Number(id);
    if (scaleId) {
      subscribe(scaleId);
    }
  }, [id, subscribe]);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <Loader className="w-8 h-8 animate-spin text-primary mx-auto mb-2" />
          <p className="text-textMuted">Loading scale details...</p>
        </div>
      </div>
    );
  }

  if (error || !scale) {
    return (
      <div className="space-y-6">
        <button
          onClick={() => navigate('/scales')}
          className="flex items-center gap-2 text-primary hover:text-primary/80"
        >
          <ArrowLeft className="w-4 h-4" />
          Back to Scales
        </button>
        <div className="card bg-statusError/10 border border-statusError">
          <h3 className="text-lg font-semibold text-statusError mb-2">Error Loading Scale</h3>
          <p className="text-textMain">{error instanceof Error ? error.message : 'Scale not found'}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <button
            onClick={() => navigate('/scales')}
            className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
            title="Back"
          >
            <ArrowLeft className="w-5 h-5 text-primary" />
          </button>
          <div>
            <h1 className="text-3xl font-bold text-textMain">{scale.name}</h1>
            <p className="text-textMuted">Scale ID: {scale.id}</p>
          </div>
        </div>
        <span className={`px-4 py-2 rounded-full font-medium ${
          scale.is_active 
            ? 'bg-statusSuccess/10 text-statusSuccess' 
            : 'bg-gray-100 text-textMuted'
        }`}>
          {scale.is_active ? 'ACTIVE' : 'INACTIVE'}
        </span>
      </div>

      {/* Tab Navigation */}
      <div className="border-b border-gray-200">
        <div className="flex gap-6">
          <button
            onClick={() => setSelectedTab('info')}
            className={`px-4 py-3 font-medium border-b-2 transition-colors ${
              selectedTab === 'info'
                ? 'border-primary text-primary'
                : 'border-transparent text-textMuted hover:text-textMain'
            }`}
          >
            Information
          </button>
          <button
            onClick={() => setSelectedTab('config')}
            className={`px-4 py-3 font-medium border-b-2 transition-colors ${
              selectedTab === 'config'
                ? 'border-primary text-primary'
                : 'border-transparent text-textMuted hover:text-textMain'
            }`}
          >
            Configuration
          </button>
        </div>
      </div>

      {/* Information Tab */}
      {selectedTab === 'info' && (
        <>
          {/* Basic Information */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="card">
          <h2 className="text-lg font-semibold text-textMain mb-4">Basic Information</h2>
          <div className="space-y-3">
            <div>
              <label className="text-sm font-medium text-textMuted">Name</label>
              <p className="text-textMain">{scale.name}</p>
            </div>
            {scale.model && (
              <div>
                <label className="text-sm font-medium text-textMuted">Model</label>
                <p className="text-textMain">{scale.model}</p>
              </div>
            )}
            <div>
              <label className="text-sm font-medium text-textMuted">Location</label>
              <p className="text-textMain">{scale.location_name}</p>
            </div>
            <div>
              <label className="text-sm font-medium text-textMuted">Created By</label>
              <p className="text-textMain">{scale.created_by || '-'}</p>
            </div>
            {scale.created_at && (
              <div>
                <label className="text-sm font-medium text-textMuted">Created At</label>
                <p className="text-textMain">{new Date(scale.created_at).toLocaleString()}</p>
              </div>
            )}
          </div>
        </div>

        {/* Configuration */}
        {scale.scale_config && (
          <div className="card">
            <h2 className="text-lg font-semibold text-textMain mb-4">Configuration</h2>
            <div className="space-y-3">
              <div>
                <label className="text-sm font-medium text-textMuted">Protocol</label>
                <p className="text-textMain">{scale.scale_config.protocol}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-textMuted">Poll Interval</label>
                <p className="text-textMain">{scale.scale_config.poll_interval} ms</p>
              </div>
              {scale.scale_config.conn_params && (
                <>
                  <div>
                    <label className="text-sm font-medium text-textMuted">IP Address</label>
                    <p className="text-textMain">{scale.scale_config.conn_params.ip}</p>
                  </div>
                  <div>
                    <label className="text-sm font-medium text-textMuted">Port</label>
                    <p className="text-textMain">{scale.scale_config.conn_params.port}</p>
                  </div>
                </>
              )}
            </div>
          </div>
        )}
      </div>

      {/* Data Configuration */}
      {scale.scale_config && (
        <div className="card">
          <h2 className="text-lg font-semibold text-textMain mb-4">Data Configuration</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {scale.scale_config.data_1 && (
              <div className="border border-gray-200 rounded-lg p-4">
                <h3 className="font-semibold text-textMain mb-3">{scale.scale_config.data_1.name}</h3>
                <div className="space-y-2 text-sm">
                  <div className="flex justify-between">
                    <span className="text-textMuted">Status:</span>
                    <span className={`font-medium ${scale.scale_config.data_1.is_used ? 'text-statusSuccess' : 'text-textMuted'}`}>
                      {scale.scale_config.data_1.is_used ? 'Active' : 'Inactive'}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-textMuted">Data Type:</span>
                    <span className="text-textMain">{scale.scale_config.data_1.data_type}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-textMuted">Num Registers:</span>
                    <span className="text-textMain">{scale.scale_config.data_1.num_registers}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-textMuted">Start Registers:</span>
                    <span className="text-textMain">{scale.scale_config.data_1.start_registers}</span>
                  </div>
                </div>
              </div>
            )}
            {scale.scale_config.data_2 && (
              <div className="border border-gray-200 rounded-lg p-4">
                <h3 className="font-semibold text-textMain mb-3">{scale.scale_config.data_2.name}</h3>
                <div className="space-y-2 text-sm">
                  <div className="flex justify-between">
                    <span className="text-textMuted">Status:</span>
                    <span className={`font-medium ${scale.scale_config.data_2.is_used ? 'text-statusSuccess' : 'text-textMuted'}`}>
                      {scale.scale_config.data_2.is_used ? 'Active' : 'Inactive'}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-textMuted">Data Type:</span>
                    <span className="text-textMain">{scale.scale_config.data_2.data_type}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-textMuted">Num Registers:</span>
                    <span className="text-textMain">{scale.scale_config.data_2.num_registers}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-textMuted">Start Registers:</span>
                    <span className="text-textMain">{scale.scale_config.data_2.start_registers}</span>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Audit Info */}
      {(scale.updated_at || scale.updated_by) && (
        <div className="card">
          <h2 className="text-lg font-semibold text-textMain mb-4">Audit Information</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {scale.updated_by && (
              <div>
                <label className="text-sm font-medium text-textMuted">Last Updated By</label>
                <p className="text-textMain">{scale.updated_by}</p>
              </div>
            )}
            {scale.updated_at && (
              <div>
                <label className="text-sm font-medium text-textMuted">Last Updated At</label>
                <p className="text-textMain">{new Date(scale.updated_at).toLocaleString()}</p>
              </div>
            )}
          </div>
        </div>
      )}
        </>
      )}

      {/* Configuration Tab */}
      {selectedTab === 'config' && (
        <>
          {/* Configuration Details */}
          {scale.scale_config && (
            <div className="space-y-6">
              {/* Connection Config */}
              <div className="card">
                <h2 className="text-lg font-semibold text-textMain mb-4">Connection Configuration</h2>
                <div className="space-y-3">
                  <div>
                    <label className="text-sm font-medium text-textMuted">Protocol</label>
                    <p className="text-textMain">{scale.scale_config.protocol}</p>
                  </div>
                  <div>
                    <label className="text-sm font-medium text-textMuted">Poll Interval</label>
                    <p className="text-textMain">{scale.scale_config.poll_interval} ms</p>
                  </div>
                  {scale.scale_config.conn_params && (
                    <>
                      <div>
                        <label className="text-sm font-medium text-textMuted">IP Address</label>
                        <p className="text-textMain">{scale.scale_config.conn_params.ip}</p>
                      </div>
                      <div>
                        <label className="text-sm font-medium text-textMuted">Port</label>
                        <p className="text-textMain">{scale.scale_config.conn_params.port}</p>
                      </div>
                    </>
                  )}
                </div>
              </div>

              {/* Data Channels Config */}
              <div className="card">
                <h2 className="text-lg font-semibold text-textMain mb-4">Data Channels Configuration</h2>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {scale.scale_config.data_1 && (
                    <div className="border border-gray-200 rounded-lg p-4">
                      <h3 className="font-semibold text-textMain mb-3">{scale.scale_config.data_1.name}</h3>
                      <div className="space-y-2 text-sm">
                        <div className="flex justify-between">
                          <span className="text-textMuted">Status:</span>
                          <span className={`font-medium ${scale.scale_config.data_1.is_used ? 'text-statusSuccess' : 'text-textMuted'}`}>
                            {scale.scale_config.data_1.is_used ? 'Active' : 'Inactive'}
                          </span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-textMuted">Data Type:</span>
                          <span className="text-textMain">{scale.scale_config.data_1.data_type}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-textMuted">Num Registers:</span>
                          <span className="text-textMain">{scale.scale_config.data_1.num_registers}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-textMuted">Start Registers:</span>
                          <span className="text-textMain">{scale.scale_config.data_1.start_registers}</span>
                        </div>
                      </div>
                    </div>
                  )}
                  {scale.scale_config.data_2 && (
                    <div className="border border-gray-200 rounded-lg p-4">
                      <h3 className="font-semibold text-textMain mb-3">{scale.scale_config.data_2.name}</h3>
                      <div className="space-y-2 text-sm">
                        <div className="flex justify-between">
                          <span className="text-textMuted">Status:</span>
                          <span className={`font-medium ${scale.scale_config.data_2.is_used ? 'text-statusSuccess' : 'text-textMuted'}`}>
                            {scale.scale_config.data_2.is_used ? 'Active' : 'Inactive'}
                          </span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-textMuted">Data Type:</span>
                          <span className="text-textMain">{scale.scale_config.data_2.data_type}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-textMuted">Num Registers:</span>
                          <span className="text-textMain">{scale.scale_config.data_2.num_registers}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-textMuted">Start Registers:</span>
                          <span className="text-textMain">{scale.scale_config.data_2.start_registers}</span>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
};
