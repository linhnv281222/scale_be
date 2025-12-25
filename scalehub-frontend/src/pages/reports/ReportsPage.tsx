import React, { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, LineChart, Line } from 'recharts';
import { BarChart3, Loader } from 'lucide-react';
import { toast } from 'react-toastify';
import { reportsApi } from '../../api/reports';
import { scalesApi } from '../../api/scales';
import { format, subDays } from 'date-fns';

const COLORS = ['#3B82F6', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6', '#EC4899', '#14B8A6'];

export const ReportsPage: React.FC = () => {
  const [fromDate, setFromDate] = useState(format(subDays(new Date(), 30), 'yyyy-MM-dd'));
  const [toDate, setToDate] = useState(format(new Date(), 'yyyy-MM-dd'));
  const [selectedScales, setSelectedScales] = useState<number[]>([]);
  const [dataField, setDataField] = useState('data_1');
  const [method, setMethod] = useState<'SUM' | 'AVG' | 'MAX'>('AVG');
  const [interval, setInterval] = useState<'HOUR' | 'DAY' | 'WEEK' | 'MONTH' | 'YEAR'>('DAY');
  const [chartType, setChartType] = useState<'bar' | 'line'>('bar');

  // Fetch all scales for selection
  const { data: scalesResponse } = useQuery({
    queryKey: ['scales-all'],
    queryFn: () => scalesApi.getAllScales(),
  });

  const scales = scalesResponse || [];

  // Auto-fetch report when filters change (skip first load)
  const shouldFetchReport = selectedScales.length > 0;
  const { data: reportDataMap = {}, isLoading: reportLoading } = useQuery({
    queryKey: ['reports', selectedScales, dataField, method, interval, fromDate, toDate],
    queryFn: async () => {
      // Generate report for each selected scale
      const results: Record<number, any> = {};
      
      console.log('Fetching reports for scales:', selectedScales); // DEBUG
      
      for (const scaleId of selectedScales) {
        try {
          const report = await reportsApi.generateReport({
            scaleIds: [scaleId],
            dataField,
            method,
            fromDate,
            toDate,
            interval,
          });
          results[scaleId] = report;
        } catch (error) {
          console.error(`Failed to generate report for scale ${scaleId}`, error);
        }
      }
      
      console.log('Report results:', results); // DEBUG
      return results;
    },
    enabled: shouldFetchReport,
    staleTime: 0, // Always refetch when dependencies change
  });

  const handleScaleToggle = (scaleId: number) => {
    setSelectedScales(prev => {
      const newScales = prev.includes(scaleId)
        ? prev.filter(id => id !== scaleId)
        : [...prev, scaleId];
      console.log('Scale toggled:', scaleId, 'New selection:', newScales); // DEBUG
      return newScales;
    });
  };

  // Transform report data for multi-series chart
  const transformedChartData = (() => {
    if (!reportDataMap || Object.keys(reportDataMap).length === 0) return [];
    
    // Merge all datapoints by time, preserving scale-specific values
    const timeMap: Record<string, any> = {};
    
    Object.entries(reportDataMap).forEach(([scaleIdStr, report]) => {
      const scaleId = Number(scaleIdStr);
      const scale = scales.find(s => s.id === scaleId);
      const scaleName = scale?.name || `Scale ${scaleId}`;
      
      if (report?.dataPoints) {
        report.dataPoints.forEach((point: any) => {
          if (!timeMap[point.time]) {
            timeMap[point.time] = { time: point.time };
          }
          timeMap[point.time][`${scaleName}_${scaleId}`] = point.value;
        });
      }
    });
    
    return Object.values(timeMap).sort((a, b) => {
      // Sort by time numerically if possible, otherwise alphabetically
      const aTime = parseFloat(a.time) || new Date(a.time).getTime();
      const bTime = parseFloat(b.time) || new Date(b.time).getTime();
      return aTime - bTime;
    });
  })();

  // Generate color keys for each scale series
  const seriesKeys = selectedScales.map(scaleId => {
    const scale = scales.find(s => s.id === scaleId);
    return `${scale?.name || `Scale ${scaleId}`}_${scaleId}`;
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-4">
        <div>
          <h1 className="text-3xl font-bold text-textMain">Reports</h1>
          <p className="text-textMuted">Generate and analyze scale data reports</p>
        </div>
      </div>

      {/* Report Generation Form */}
      <div className="card">
        <div className="space-y-6">
          <h2 className="text-xl font-semibold text-textMain">Generate Report</h2>

          {/* Date Range */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label htmlFor="fromDate" className="block text-sm font-medium text-textMain mb-2">
                From Date <span className="text-statusError">*</span>
              </label>
              <input
                id="fromDate"
                type="date"
                value={fromDate}
                onChange={(e) => setFromDate(e.target.value)}
                className="input-field w-full"
                required
              />
            </div>
            <div>
              <label htmlFor="toDate" className="block text-sm font-medium text-textMain mb-2">
                To Date <span className="text-statusError">*</span>
              </label>
              <input
                id="toDate"
                type="date"
                value={toDate}
                onChange={(e) => setToDate(e.target.value)}
                className="input-field w-full"
                required
              />
            </div>
          </div>

          {/* Data Field and Method */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label htmlFor="dataField" className="block text-sm font-medium text-textMain mb-2">
                Data Field <span className="text-statusError">*</span>
              </label>
              <select
                id="dataField"
                value={dataField}
                onChange={(e) => setDataField(e.target.value)}
                className="input-field w-full"
                required
              >
                <option value="data_1">Data 1</option>
                <option value="data_2">Data 2</option>
                <option value="data_3">Data 3</option>
                <option value="data_4">Data 4</option>
                <option value="data_5">Data 5</option>
              </select>
            </div>

            <div>
              <label htmlFor="method" className="block text-sm font-medium text-textMain mb-2">
                Aggregation <span className="text-statusError">*</span>
              </label>
              <select
                id="method"
                value={method}
                onChange={(e) => setMethod(e.target.value as 'SUM' | 'AVG' | 'MAX')}
                className="input-field w-full"
                required
              >
                <option value="SUM">Sum</option>
                <option value="AVG">Average</option>
                <option value="MAX">Maximum</option>
              </select>
            </div>

            <div>
              <label htmlFor="interval" className="block text-sm font-medium text-textMain mb-2">
                Time Interval <span className="text-statusError">*</span>
              </label>
              <select
                id="interval"
                value={interval}
                onChange={(e) => setInterval(e.target.value as 'HOUR' | 'DAY' | 'WEEK' | 'MONTH' | 'YEAR')}
                className="input-field w-full"
                required
              >
                <option value="HOUR">Hourly</option>
                <option value="DAY">Daily</option>
                <option value="WEEK">Weekly</option>
                <option value="MONTH">Monthly</option>
                <option value="YEAR">Yearly</option>
              </select>
            </div>
          </div>

          {/* Scale Selection */}
          <div>
            <label className="block text-sm font-medium text-textMain mb-3">
              Select Scales <span className="text-statusError">*</span>
            </label>
            <div className="border border-gray-300 rounded-lg p-3 space-y-2 max-h-48 overflow-y-auto">
              {scales.length === 0 ? (
                <p className="text-textMuted text-sm">No scales available</p>
              ) : (
                scales.map(scale => (
                  <label key={scale.id} className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={selectedScales.includes(scale.id)}
                      onChange={() => handleScaleToggle(scale.id)}
                      className="w-4 h-4 rounded"
                    />
                    <span className="text-sm text-textMain">
                      {scale.name} ({scale.location_name})
                    </span>
                  </label>
                ))
              )}
            </div>
            {selectedScales.length > 0 && (
              <p className="text-xs text-textMuted mt-2">
                {selectedScales.length} scale{selectedScales.length !== 1 ? 's' : ''} selected
              </p>
            )}
          </div>
        </div>
      </div>

      {/* Report Results */}
      {selectedScales.length > 0 && (
        <div className="space-y-6">
          {/* Loading State */}
          {reportLoading && (
            <div className="card flex items-center justify-center py-12">
              <Loader className="w-8 h-8 animate-spin text-primary mr-3" />
              <p className="text-textMuted">Generating reports...</p>
            </div>
          )}

          {/* Report Info */}
          {!reportLoading && Object.keys(reportDataMap).length > 0 && (
            <>
              <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                <div className="card">
                  <p className="text-xs text-textMuted uppercase tracking-wider mb-1">Data Field</p>
                  <p className="text-lg font-semibold text-textMain">{dataField}</p>
                </div>
                <div className="card">
                  <p className="text-xs text-textMuted uppercase tracking-wider mb-1">Method</p>
                  <p className="text-lg font-semibold text-primary">{method}</p>
                </div>
                <div className="card">
                  <p className="text-xs text-textMuted uppercase tracking-wider mb-1">Interval</p>
                  <p className="text-lg font-semibold text-primary">{interval}</p>
                </div>
                <div className="card">
                  <p className="text-xs text-textMuted uppercase tracking-wider mb-1">Scales</p>
                  <p className="text-lg font-semibold text-primary">{selectedScales.length}</p>
                </div>
              </div>

              {/* Chart Type Toggle */}
              <div className="flex gap-2">
                <button
                  onClick={() => setChartType('bar')}
                  className={`px-4 py-2 rounded-lg font-medium transition-colors ${
                    chartType === 'bar'
                      ? 'bg-primary text-white'
                      : 'bg-gray-200 text-textMain hover:bg-gray-300'
                  }`}
                >
                  Bar Chart
                </button>
                <button
                  onClick={() => setChartType('line')}
                  className={`px-4 py-2 rounded-lg font-medium transition-colors ${
                    chartType === 'line'
                      ? 'bg-primary text-white'
                      : 'bg-gray-200 text-textMain hover:bg-gray-300'
                  }`}
                >
                  Line Chart
                </button>
              </div>

              {/* Chart */}
              <div className="card">
                {transformedChartData && transformedChartData.length > 0 ? (
                  <div className="w-full h-96">
                    {chartType === 'bar' ? (
                      <ResponsiveContainer width="100%" height="100%">
                        <BarChart data={transformedChartData}>
                          <CartesianGrid strokeDasharray="3 3" />
                          <XAxis dataKey="time" />
                          <YAxis />
                          <Tooltip />
                          <Legend />
                          {seriesKeys.map((key, idx) => (
                            <Bar key={key} dataKey={key} fill={COLORS[idx % COLORS.length]} />
                          ))}
                        </BarChart>
                      </ResponsiveContainer>
                    ) : (
                      <ResponsiveContainer width="100%" height="100%">
                        <LineChart data={transformedChartData}>
                          <CartesianGrid strokeDasharray="3 3" />
                          <XAxis dataKey="time" />
                          <YAxis />
                          <Tooltip />
                          <Legend />
                          {seriesKeys.map((key, idx) => (
                            <Line
                              key={key}
                              type="monotone"
                              dataKey={key}
                              stroke={COLORS[idx % COLORS.length]}
                            />
                          ))}
                        </LineChart>
                      </ResponsiveContainer>
                    )}
                  </div>
                ) : (
                  <div className="h-96 flex items-center justify-center text-textMuted">
                    No data available for the selected parameters
                  </div>
                )}
              </div>

              {/* Data Table */}
              {transformedChartData && transformedChartData.length > 0 && (
                <div className="card overflow-hidden">
                  <h3 className="text-lg font-semibold text-textMain mb-4">Data Points</h3>
                  <div className="overflow-x-auto">
                    <table className="w-full">
                      <thead className="bg-gray-50 border-b border-gray-200">
                        <tr>
                          <th className="px-6 py-3 text-left text-xs font-medium text-textMuted uppercase tracking-wider">
                            Time
                          </th>
                          {seriesKeys.map((key, idx) => (
                            <th
                              key={key}
                              className="px-6 py-3 text-right text-xs font-medium text-textMuted uppercase tracking-wider"
                            >
                              {key.split('_')[0]}
                            </th>
                          ))}
                        </tr>
                      </thead>
                      <tbody className="bg-surface divide-y divide-gray-200">
                        {transformedChartData.map((point, index) => (
                          <tr key={index} className="hover:bg-gray-50">
                            <td className="px-6 py-4 whitespace-nowrap text-sm text-textMain">
                              {point.time}
                            </td>
                            {seriesKeys.map((key, idx) => (
                              <td
                                key={key}
                                className="px-6 py-4 whitespace-nowrap text-sm font-medium text-right"
                                style={{ color: COLORS[idx % COLORS.length] }}
                              >
                                {point[key] ? parseFloat(point[key]).toFixed(2) : '-'}
                              </td>
                            ))}
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}
            </>
          )}

          {!reportLoading && selectedScales.length > 0 && Object.keys(reportDataMap).length === 0 && (
            <div className="card text-center py-12 text-textMuted">
              No data available for the selected parameters
            </div>
          )}
        </div>
      )}

      {selectedScales.length === 0 && (
        <div className="card text-center py-12 text-textMuted">
          <p>Select at least one scale to view the report</p>
        </div>
      )}
    </div>
  );
};
