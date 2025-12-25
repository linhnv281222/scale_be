import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Scale, Activity, TrendingUp, AlertTriangle } from 'lucide-react';
import { scalesApi } from '../../api/scales';
import { useWebSocket } from '../../contexts/WebSocketContext';

export const DashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const { scaleData, subscribeAll, connected } = useWebSocket();
  const [currentStates, setCurrentStates] = useState<any[]>([]);

  // Fetch all scales
  const { data: scalesResponse, isLoading } = useQuery({
    queryKey: ['scales'],
    queryFn: () => scalesApi.getAllScales({ page: 0, size: 100 }),
  });

  // Fetch current states with config names
  const { data: statesResponse } = useQuery({
    queryKey: ['scales-current-states'],
    queryFn: () => scalesApi.getScalesCurrentStates(),
    refetchInterval: 5000, // Poll every 5 seconds
  });

  // Subscribe to all scale updates when connected
  useEffect(() => {
    if (connected) {
      subscribeAll();
    }
  }, [connected, subscribeAll]);

  // Update current states from response
  useEffect(() => {
    if (statesResponse) {
      setCurrentStates(statesResponse);
    }
  }, [statesResponse]);

  const scales = scalesResponse || [];
  
  // Calculate statistics from current states
  const onlineScales = currentStates.filter(s => s.status === 'ONLINE').length;
  const offlineScales = currentStates.filter(s => s.status === 'OFFLINE').length;
  const errorScales = currentStates.filter(s => s.status === 'ERROR').length;
  const totalScales = scales.length;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-3xl font-bold text-textMain mb-2">Dashboard</h1>
        <p className="text-textMuted">Monitor your scale network in real-time</p>
      </div>

      {/* Statistics Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="card">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-textMuted mb-1">Total Scales</p>
              <p className="text-3xl font-bold text-textMain">{totalScales}</p>
            </div>
            <div className="w-12 h-12 bg-primary/10 rounded-lg flex items-center justify-center">
              <Scale className="w-6 h-6 text-primary" />
            </div>
          </div>
        </div>

        <div className="card">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-textMuted mb-1">Online</p>
              <p className="text-3xl font-bold text-statusSuccess">{onlineScales}</p>
            </div>
            <div className="w-12 h-12 bg-statusSuccess/10 rounded-lg flex items-center justify-center">
              <Activity className="w-6 h-6 text-statusSuccess" />
            </div>
          </div>
        </div>

        <div className="card">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-textMuted mb-1">Offline</p>
              <p className="text-3xl font-bold text-textMuted">{offlineScales}</p>
            </div>
            <div className="w-12 h-12 bg-gray-100 rounded-lg flex items-center justify-center">
              <TrendingUp className="w-6 h-6 text-textMuted" />
            </div>
          </div>
        </div>

        <div className="card">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-textMuted mb-1">Errors</p>
              <p className="text-3xl font-bold text-statusError">{errorScales}</p>
            </div>
            <div className="w-12 h-12 bg-statusError/10 rounded-lg flex items-center justify-center">
              <AlertTriangle className="w-6 h-6 text-statusError" />
            </div>
          </div>
        </div>
      </div>

      {/* Current State Table */}
      <div>
        <h2 className="text-xl font-semibold text-textMain mb-4">Current State - All Scales</h2>
        <div className="card overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-textMuted uppercase tracking-wider">Scale Name</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-textMuted uppercase tracking-wider">Status</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-textMuted uppercase tracking-wider">Last Updated</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-textMuted uppercase tracking-wider">Data Fields</th>
                </tr>
              </thead>
              <tbody className="bg-surface divide-y divide-gray-200">
                {currentStates.map(state => (
                  <tr key={state.scaleId} className="hover:bg-gray-50">
                    <td className="px-6 py-4 text-sm font-medium text-textMain">{state.scaleName}</td>
                    <td className="px-6 py-4">
                      <span className={`px-3 py-1 rounded-full text-xs font-medium ${
                        state.status === 'ONLINE' ? 'bg-statusSuccess/10 text-statusSuccess' :
                        state.status === 'OFFLINE' ? 'bg-gray-100 text-textMuted' :
                        'bg-statusError/10 text-statusError'
                      }`}>
                        {state.status}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-sm text-textMuted">
                      {state.lastTime ? new Date(state.lastTime).toLocaleString() : '-'}
                    </td>
                    <td className="px-6 py-4">
                      <div className="space-y-1">
                        {state.dataValues && Object.entries(state.dataValues).map(([key, fieldData]: [string, any]) => (
                          <div key={key} className="text-sm">
                            {fieldData.name}: <span className="font-medium text-textMain">{fieldData.value || '-'}</span>
                          </div>
                        ))}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
};
