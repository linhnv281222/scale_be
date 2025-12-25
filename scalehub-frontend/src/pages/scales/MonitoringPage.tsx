import React, { useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Activity, AlertCircle } from 'lucide-react';
import { useWebSocket } from '../../contexts/WebSocketContext';
import { useQuery } from '@tanstack/react-query';
import { scalesApi } from '../../api/scales';
import type { Scale, ScaleRealtimeData } from '../../types';

export const MonitoringPage: React.FC = () => {
  const { scaleData, subscribeAll, connected } = useWebSocket();

  // Subscribe to all scales when component mounts
  useEffect(() => {
    if (connected) {
      subscribeAll();
    }
  }, [connected, subscribeAll]);

  const { data: scales = [], isLoading } = useQuery({
    queryKey: ['scales'],
    queryFn: () => scalesApi.getAllScales(),
  });

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <Activity className="w-8 h-8 animate-spin text-primary mx-auto mb-2" />
          <p className="text-textMuted">Loading scales...</p>
        </div>
      </div>
    );
  }

  const getScaleRealtimeData = (scaleId: number): ScaleRealtimeData | null => {
    return scaleData.has(scaleId) ? scaleData.get(scaleId) || null : null;
  };

  const onlineScales = scales.filter(s => {
    const data = getScaleRealtimeData(s.id);
    return data && data.status === 'ONLINE';
  }).length;

  const offlineScales = scales.filter(s => {
    const data = getScaleRealtimeData(s.id);
    return data && data.status === 'OFFLINE';
  }).length;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-3xl font-bold text-textMain flex items-center gap-3">
          <Activity className="w-8 h-8 text-primary" />
          Scale Monitoring
        </h1>
        <p className="text-textMuted mt-1">Real-time monitoring of all scales</p>
      </div>

      {/* Statistics */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="card">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-textMuted text-sm">Total Scales</p>
              <p className="text-3xl font-bold text-textMain mt-1">{scales.length}</p>
            </div>
            <div className="w-12 h-12 rounded-lg bg-blue-100 flex items-center justify-center">
              <Activity className="w-6 h-6 text-blue-600" />
            </div>
          </div>
        </div>

        <div className="card">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-textMuted text-sm">Online</p>
              <p className="text-3xl font-bold text-statusSuccess mt-1">{onlineScales}</p>
            </div>
            <div className="w-12 h-12 rounded-lg bg-statusSuccess/10 flex items-center justify-center">
              <span className="w-3 h-3 rounded-full bg-statusSuccess animate-pulse"></span>
            </div>
          </div>
        </div>

        <div className="card">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-textMuted text-sm">Offline</p>
              <p className="text-3xl font-bold text-statusError mt-1">{offlineScales}</p>
            </div>
            <div className="w-12 h-12 rounded-lg bg-statusError/10 flex items-center justify-center">
              <AlertCircle className="w-6 h-6 text-statusError" />
            </div>
          </div>
        </div>

        <div className="card">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-textMuted text-sm">Active Rate</p>
              <p className="text-3xl font-bold text-primary mt-1">
                {scales.length > 0 ? Math.round((onlineScales / scales.length) * 100) : 0}%
              </p>
            </div>
            <div className="w-12 h-12 rounded-lg bg-primary/10 flex items-center justify-center">
              <Activity className="w-6 h-6 text-primary" />
            </div>
          </div>
        </div>
      </div>

      {/* Scales Grid */}
      <div>
        <h2 className="text-xl font-semibold text-textMain mb-4">Active Scales</h2>
        {scales.length === 0 ? (
          <div className="card bg-gray-50 text-center py-8">
            <AlertCircle className="w-12 h-12 text-textMuted mx-auto mb-3" />
            <p className="text-textMuted">No scales available</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {scales.map((scale) => {
              const realtimeData = getScaleRealtimeData(scale.id);
              const isOnline = realtimeData?.status === 'ONLINE';

              return (
                <Link
                  key={scale.id}
                  to={`/scales/${scale.id}`}
                  className="card hover:shadow-lg transition-shadow cursor-pointer group"
                >
                  {/* Header */}
                  <div className="flex items-start justify-between mb-3">
                    <div className="flex-1 min-w-0">
                      <h3 className="font-semibold text-textMain text-lg group-hover:text-primary transition-colors truncate">
                        {scale.name}
                      </h3>
                      <p className="text-sm text-textMuted truncate">
                        {scale.model || 'Unknown Model'}
                      </p>
                    </div>
                    <div className={`flex-shrink-0 ml-2 px-3 py-1 rounded-full text-xs font-medium flex items-center gap-1 ${
                      isOnline
                        ? 'bg-statusSuccess/10 text-statusSuccess'
                        : 'bg-statusError/10 text-statusError'
                    }`}>
                      <span className={`w-2 h-2 rounded-full ${isOnline ? 'bg-statusSuccess' : 'bg-statusError'} ${isOnline ? 'animate-pulse' : ''}`}></span>
                      {isOnline ? 'ONLINE' : 'OFFLINE'}
                    </div>
                  </div>

                  {/* Location & Status */}
                  <div className="flex items-center justify-between text-sm mb-4 pb-4 border-b border-gray-200">
                    <div>
                      <p className="text-textMuted text-xs">Location</p>
                      <p className="text-textMain font-medium">{scale.location_name}</p>
                    </div>
                    <div>
                      <p className="text-textMuted text-xs">Protocol</p>
                      <p className="text-textMain font-medium">{scale.scale_config?.protocol || '-'}</p>
                    </div>
                  </div>

                  {/* Real-time Data */}
                  {realtimeData ? (
                    <div className="space-y-2">
                      <p className="text-xs font-semibold text-textMuted uppercase">Data Channels</p>
                      <div className="grid grid-cols-3 gap-2">
                        {[1, 2, 3].map((num) => {
                          const dataKey = `data${num}` as keyof ScaleRealtimeData;
                          const value = realtimeData[dataKey];
                          const config = scale.scale_config?.[`data_${num}` as keyof typeof scale.scale_config];

                          return (
                            <div key={num} className="bg-gray-50 rounded p-2 text-center">
                              <p className="text-xs text-textMuted truncate">
                                {config && 'name' in config ? (config as any).name : `Ch ${num}`}
                              </p>
                              <p className={`text-sm font-bold ${
                                value !== null ? 'text-primary' : 'text-textMuted'
                              }`}>
                                {value ?? '-'}
                              </p>
                            </div>
                          );
                        })}
                      </div>
                      <p className="text-xs text-textMuted text-right mt-2">
                        Updated: {new Date(realtimeData.lastTime).toLocaleTimeString()}
                      </p>
                    </div>
                  ) : (
                    <div className="bg-yellow-50 border border-yellow-200 rounded p-3 text-center">
                      <p className="text-xs text-yellow-700">No data available</p>
                    </div>
                  )}
                </Link>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};
