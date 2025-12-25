import React from 'react';
import { Scale as ScaleIcon, AlertCircle, CheckCircle, Clock } from 'lucide-react';
import type { ScaleRealtimeData, ScaleStatus } from '../../types';
import { format } from 'date-fns';

interface ScaleStatusCardProps {
  data: ScaleRealtimeData;
  onClick?: () => void;
}

export const ScaleStatusCard: React.FC<ScaleStatusCardProps> = ({ data, onClick }) => {
  const getStatusColor = (status: ScaleStatus) => {
    switch (status) {
      case "ONLINE":
        return 'text-statusSuccess bg-statusSuccess/10';
      case "ERROR":
        return 'text-statusError bg-statusError/10';
      case "OFFLINE":
        return 'text-textMuted bg-gray-100';
      case "MAINTENANCE":
        return 'text-statusWarning bg-statusWarning/10';
      default:
        return 'text-textMuted bg-gray-100';
    }
  };

  const getStatusIcon = (status: ScaleStatus) => {
    switch (status) {
      case "ONLINE":
        return <CheckCircle className="w-5 h-5" />;
      case "ERROR":
        return <AlertCircle className="w-5 h-5" />;
      case "OFFLINE":
        return <Clock className="w-5 h-5" />;
      case "MAINTENANCE":
        return <AlertCircle className="w-5 h-5" />;
      default:
        return <Clock className="w-5 h-5" />;
    }
  };

  return (
    <div 
      className="card hover:shadow-lg transition-shadow cursor-pointer"
      onClick={onClick}
    >
      <div className="flex items-start justify-between mb-4">
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 bg-primary/10 rounded-lg flex items-center justify-center">
            <ScaleIcon className="w-6 h-6 text-primary" />
          </div>
          <div>
            <h3 className="font-semibold text-textMain">{data.scaleName}</h3>
            <p className="text-sm text-textMuted">Scale #{data.scaleId}</p>
          </div>
        </div>
        <div className={`px-3 py-1 rounded-full flex items-center gap-2 ${getStatusColor(data.status)}`}>
          {getStatusIcon(data.status)}
          <span className="text-sm font-medium">{data.status}</span>
        </div>
      </div>

      <div className="space-y-2">
        <div className="flex justify-between items-center">
          <span className="text-sm text-textMuted">Current Weight:</span>
          <span className="text-2xl font-bold text-primary">
            {data.weight.toFixed(2)} {data.unit}
          </span>
        </div>
        <div className="flex justify-between items-center text-sm">
          <span className="text-textMuted">Last Update:</span>
          <span className="text-textMain">
            {format(new Date(data.timestamp), 'HH:mm:ss')}
          </span>
        </div>
      </div>
    </div>
  );
};
