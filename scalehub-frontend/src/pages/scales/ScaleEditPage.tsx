import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, Save, Loader, ChevronDown, ChevronUp } from 'lucide-react';
import { toast } from 'react-toastify';
import { scalesApi } from '../../api/scales';
import { locationsApi } from '../../api/locations';
import type { Scale, ScaleConfigResponse, ScaleDataConfig } from '../../types';

export const ScaleEditPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [formData, setFormData] = useState({
    name: '',
    model: '',
    location_id: 0,
    is_active: true,
  });

  const [configData, setConfigData] = useState<Partial<ScaleConfigResponse>>({
    protocol: '',
    poll_interval: 1000,
    conn_params: {
      ip: '',
      port: 502,
    },
  });

  const [expandedDataChannels, setExpandedDataChannels] = useState<Record<number, boolean>>({
    1: true,
    2: true,
    3: false,
    4: false,
    5: false,
  });

  // Fetch scale data
  const { data: scale, isLoading: scaleLoading, error: scaleError } = useQuery({
    queryKey: ['scale', id],
    queryFn: () => scalesApi.getScaleById(Number(id)),
    enabled: !!id,
  });

  // Fetch locations for dropdown
  const { data: locations = [] } = useQuery({
    queryKey: ['locations'],
    queryFn: () => locationsApi.getAllLocations(),
  });

  // Populate form when scale data loads
  useEffect(() => {
    if (scale) {
      setFormData({
        name: scale.name,
        model: scale.model || '',
        location_id: scale.location_id,
        is_active: scale.is_active,
      });

      if (scale.scale_config) {
        setConfigData({
          protocol: scale.scale_config.protocol,
          poll_interval: scale.scale_config.poll_interval,
          conn_params: scale.scale_config.conn_params,
          data_1: scale.scale_config.data_1,
          data_2: scale.scale_config.data_2,
        });
      }
    }
  }, [scale]);

  // Update mutation
  const updateMutation = useMutation({
    mutationFn: () => scalesApi.updateScale(Number(id), formData),
    onSuccess: () => {
      toast.success('Scale updated successfully');
      queryClient.invalidateQueries({ queryKey: ['scale', id] });
      queryClient.invalidateQueries({ queryKey: ['scales'] });
    },
    onError: (error) => {
      toast.error(error instanceof Error ? error.message : 'Failed to update scale');
    },
  });

  // Update config mutation
  const updateConfigMutation = useMutation({
    mutationFn: () => scalesApi.updateScaleConfig(Number(id), configData),
    onSuccess: () => {
      toast.success('Scale configuration updated successfully');
      queryClient.invalidateQueries({ queryKey: ['scale', id] });
      queryClient.invalidateQueries({ queryKey: ['scales'] });
    },
    onError: (error) => {
      toast.error(error instanceof Error ? error.message : 'Failed to update configuration');
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

    updateMutation.mutate();
  };

  const handleConfigSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (!configData.protocol?.trim()) {
      toast.error('Protocol is required');
      return;
    }

    if (!configData.poll_interval || configData.poll_interval < 100) {
      toast.error('Poll interval must be at least 100ms');
      return;
    }

    if (!configData.conn_params?.ip?.trim()) {
      toast.error('IP address is required');
      return;
    }

    if (!configData.conn_params?.port) {
      toast.error('Port is required');
      return;
    }

    updateConfigMutation.mutate();
  };

  if (scaleLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <Loader className="w-8 h-8 animate-spin text-primary mx-auto mb-2" />
          <p className="text-textMuted">Loading scale details...</p>
        </div>
      </div>
    );
  }

  if (scaleError || !scale) {
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
          <p className="text-textMain">{scaleError instanceof Error ? scaleError.message : 'Scale not found'}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-4">
        <button
          onClick={() => navigate(`/scales/${id}`)}
          className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
          title="Back"
        >
          <ArrowLeft className="w-5 h-5 text-primary" />
        </button>
        <div>
          <h1 className="text-3xl font-bold text-textMain">Edit Scale</h1>
          <p className="text-textMuted">{scale.name}</p>
        </div>
      </div>

      {/* Edit Form */}
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
          </div>

          {/* Model */}
          <div>
            <label htmlFor="model" className="block text-sm font-medium text-textMain mb-2">
              Model
            </label>
            <input
              id="model"
              type="text"
              value={formData.model}
              onChange={(e) => setFormData({ ...formData, model: e.target.value })}
              className="input-field w-full"
              placeholder="e.g., IND570"
            />
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
          </div>

          {/* Status */}
          <div className="flex items-center gap-3">
            <input
              id="is_active"
              type="checkbox"
              checked={formData.is_active}
              onChange={(e) => setFormData({ ...formData, is_active: e.target.checked })}
              className="w-4 h-4 rounded border-gray-300 text-primary focus:ring-primary"
            />
            <label htmlFor="is_active" className="text-sm font-medium text-textMain">
              Active
            </label>
          </div>
        </div>

        {/* Buttons for Scale Info */}
        <div className="flex gap-3 pt-6 border-t border-gray-200 pb-6">
          <button
            type="submit"
            disabled={updateMutation.isPending}
            className="btn-primary flex items-center gap-2"
          >
            {updateMutation.isPending ? (
              <>
                <Loader className="w-4 h-4 animate-spin" />
                Saving...
              </>
            ) : (
              <>
                <Save className="w-4 h-4" />
                Save Scale Info
              </>
            )}
          </button>
          <button
            type="button"
            onClick={() => navigate(`/scales/${id}`)}
            className="btn-secondary"
          >
            Cancel
          </button>
        </div>
      </form>

      {/* Configuration Form */}
      {scale.scale_config && (
        <form onSubmit={handleConfigSubmit} className="card">
          <div className="space-y-6">
            <h2 className="text-2xl font-bold text-textMain">Scale Configuration</h2>

            <div className="space-y-4">
              {/* Protocol */}
              <div>
                <label htmlFor="protocol" className="block text-sm font-medium text-textMain mb-2">
                  Protocol <span className="text-statusError">*</span>
                </label>
                <select
                  id="protocol"
                  value={configData.protocol || ''}
                  onChange={(e) => setConfigData({ ...configData, protocol: e.target.value })}
                  className="input-field w-full"
                  required
                >
                  <option value="">Select protocol</option>
                  <option value="MODBUS_TCP">MODBUS TCP</option>
                  <option value="MODBUS_RTU">MODBUS RTU</option>
                  <option value="SERIAL">SERIAL</option>
                </select>
              </div>

              {/* Poll Interval */}
              <div>
                <label htmlFor="pollInterval" className="block text-sm font-medium text-textMain mb-2">
                  Poll Interval (ms) <span className="text-statusError">*</span>
                </label>
                <input
                  id="pollInterval"
                  type="number"
                  min="100"
                  value={configData.poll_interval || 1000}
                  onChange={(e) => setConfigData({ ...configData, poll_interval: parseInt(e.target.value) || 1000 })}
                  className="input-field w-full"
                  placeholder="1000"
                  required
                />
                <p className="text-xs text-textMuted mt-1">Minimum 100ms</p>
              </div>

              {/* Connection Parameters */}
              <div className="bg-blue-50 p-4 rounded-lg border border-blue-200">
                <h4 className="font-medium text-textMain mb-3">Connection Parameters</h4>
                
                {configData.protocol === 'MODBUS_TCP' ? (
                  <div className="grid grid-cols-2 gap-4">
                    {/* IP Address */}
                    <div>
                      <label htmlFor="ip" className="block text-sm font-medium text-textMain mb-2">
                        IP Address <span className="text-statusError">*</span>
                      </label>
                      <input
                        id="ip"
                        type="text"
                        value={configData.conn_params?.ip || ''}
                        onChange={(e) => setConfigData({
                          ...configData,
                          conn_params: { ...configData.conn_params, ip: e.target.value }
                        })}
                        className="input-field w-full"
                        placeholder="192.168.1.10"
                        required
                      />
                    </div>

                    {/* Port */}
                    <div>
                      <label htmlFor="port" className="block text-sm font-medium text-textMain mb-2">
                        Port <span className="text-statusError">*</span>
                      </label>
                      <input
                        id="port"
                        type="number"
                        min="1"
                        max="65535"
                        value={configData.conn_params?.port || 502}
                        onChange={(e) => setConfigData({
                          ...configData,
                          conn_params: { ...configData.conn_params, port: parseInt(e.target.value) || 502 }
                        })}
                        className="input-field w-full"
                        placeholder="502"
                        required
                      />
                    </div>
                  </div>
                ) : configData.protocol === 'MODBUS_RTU' ? (
                  <div className="space-y-4">
                    {/* Serial Port */}
                    <div>
                      <label htmlFor="port" className="block text-sm font-medium text-textMain mb-2">
                        Serial Port <span className="text-statusError">*</span>
                      </label>
                      <input
                        id="port"
                        type="text"
                        value={configData.conn_params?.port || ''}
                        onChange={(e) => setConfigData({
                          ...configData,
                          conn_params: { ...configData.conn_params, port: e.target.value }
                        })}
                        className="input-field w-full"
                        placeholder="COM1 or /dev/ttyUSB0"
                        required
                      />
                      <p className="text-xs text-textMuted mt-1">e.g., COM1, COM3, or /dev/ttyUSB0 on Linux</p>
                    </div>

                    {/* Baud Rate */}
                    <div>
                      <label htmlFor="baudRate" className="block text-sm font-medium text-textMain mb-2">
                        Baud Rate <span className="text-statusError">*</span>
                      </label>
                      <select
                        id="baudRate"
                        value={configData.conn_params?.baud_rate || 9600}
                        onChange={(e) => setConfigData({
                          ...configData,
                          conn_params: { ...configData.conn_params, baud_rate: parseInt(e.target.value) }
                        })}
                        className="input-field w-full"
                        required
                      >
                        <option value={9600}>9600</option>
                        <option value={19200}>19200</option>
                        <option value={38400}>38400</option>
                        <option value={57600}>57600</option>
                        <option value={115200}>115200</option>
                      </select>
                    </div>

                    {/* Data Bits */}
                    <div className="grid grid-cols-3 gap-3">
                      <div>
                        <label htmlFor="dataBits" className="block text-sm font-medium text-textMain mb-2">
                          Data Bits
                        </label>
                        <select
                          id="dataBits"
                          value={configData.conn_params?.data_bits || 8}
                          onChange={(e) => setConfigData({
                            ...configData,
                            conn_params: { ...configData.conn_params, data_bits: parseInt(e.target.value) }
                          })}
                          className="input-field w-full"
                        >
                          <option value={7}>7</option>
                          <option value={8}>8</option>
                        </select>
                      </div>

                      {/* Stop Bits */}
                      <div>
                        <label htmlFor="stopBits" className="block text-sm font-medium text-textMain mb-2">
                          Stop Bits
                        </label>
                        <select
                          id="stopBits"
                          value={configData.conn_params?.stop_bits || 1}
                          onChange={(e) => setConfigData({
                            ...configData,
                            conn_params: { ...configData.conn_params, stop_bits: parseInt(e.target.value) }
                          })}
                          className="input-field w-full"
                        >
                          <option value={1}>1</option>
                          <option value={2}>2</option>
                        </select>
                      </div>

                      {/* Parity */}
                      <div>
                        <label htmlFor="parity" className="block text-sm font-medium text-textMain mb-2">
                          Parity
                        </label>
                        <select
                          id="parity"
                          value={configData.conn_params?.parity || 'NONE'}
                          onChange={(e) => setConfigData({
                            ...configData,
                            conn_params: { ...configData.conn_params, parity: e.target.value }
                          })}
                          className="input-field w-full"
                        >
                          <option value="NONE">None</option>
                          <option value="ODD">Odd</option>
                          <option value="EVEN">Even</option>
                        </select>
                      </div>
                    </div>
                  </div>
                ) : configData.protocol === 'SERIAL' ? (
                  <div className="space-y-4">
                    {/* Serial Port */}
                    <div>
                      <label htmlFor="port" className="block text-sm font-medium text-textMain mb-2">
                        Serial Port <span className="text-statusError">*</span>
                      </label>
                      <input
                        id="port"
                        type="text"
                        value={configData.conn_params?.port || ''}
                        onChange={(e) => setConfigData({
                          ...configData,
                          conn_params: { ...configData.conn_params, port: e.target.value }
                        })}
                        className="input-field w-full"
                        placeholder="COM1 or /dev/ttyUSB0"
                        required
                      />
                    </div>

                    {/* Baud Rate */}
                    <div>
                      <label htmlFor="baudRate" className="block text-sm font-medium text-textMain mb-2">
                        Baud Rate <span className="text-statusError">*</span>
                      </label>
                      <select
                        id="baudRate"
                        value={configData.conn_params?.baud_rate || 9600}
                        onChange={(e) => setConfigData({
                          ...configData,
                          conn_params: { ...configData.conn_params, baud_rate: parseInt(e.target.value) }
                        })}
                        className="input-field w-full"
                        required
                      >
                        <option value={9600}>9600</option>
                        <option value={19200}>19200</option>
                        <option value={38400}>38400</option>
                        <option value={57600}>57600</option>
                        <option value={115200}>115200</option>
                      </select>
                    </div>
                  </div>
                ) : (
                  <p className="text-textMuted text-sm">Please select a protocol first</p>
                )}
              </div>

              {/* Data Channels Configuration */}
              <div className="border-t pt-4 mt-6">
                <h4 className="font-medium text-textMain mb-3">Data Channels</h4>
                <div className="space-y-3">
                  {[1, 2, 3, 4, 5].map((channelNum) => {
                    const channelKey = `data_${channelNum}` as const;
                    const channelData = configData[channelKey] as ScaleDataConfig | undefined;
                    const isExpanded = expandedDataChannels[channelNum];

                    return (
                      <div key={channelNum} className="border border-gray-300 rounded-lg">
                        {/* Channel Header */}
                        <button
                          type="button"
                          onClick={() => setExpandedDataChannels({
                            ...expandedDataChannels,
                            [channelNum]: !isExpanded
                          })}
                          className="w-full flex items-center justify-between p-3 hover:bg-gray-50 transition-colors"
                        >
                          <div className="flex items-center gap-3">
                            <input
                              type="checkbox"
                              checked={channelData?.is_used || false}
                              onChange={(e) => {
                                e.stopPropagation();
                                setConfigData({
                                  ...configData,
                                  [channelKey]: {
                                    ...(channelData || {}),
                                    is_used: e.target.checked,
                                  }
                                });
                              }}
                              className="w-4 h-4 rounded"
                            />
                            <span className="font-medium text-textMain">
                              Data Channel {channelNum}: {channelData?.name || '(Not configured)'}
                            </span>
                          </div>
                          {isExpanded ? <ChevronUp className="w-5 h-5" /> : <ChevronDown className="w-5 h-5" />}
                        </button>

                        {/* Channel Content */}
                        {isExpanded && (
                          <div className="border-t p-3 bg-gray-50 space-y-3">
                            {/* Name */}
                            <div>
                              <label className="block text-sm font-medium text-textMain mb-1">
                                Name
                              </label>
                              <input
                                type="text"
                                value={channelData?.name || ''}
                                onChange={(e) => {
                                  setConfigData({
                                    ...configData,
                                    [channelKey]: {
                                      ...(channelData || { is_used: false, data_type: 'integer', num_registers: 1, start_registers: 0 }),
                                      name: e.target.value,
                                    }
                                  });
                                }}
                                className="input-field w-full text-sm"
                                placeholder="e.g., Weight"
                              />
                            </div>

                            {/* Data Type */}
                            <div>
                              <label className="block text-sm font-medium text-textMain mb-1">
                                Data Type
                              </label>
                              <select
                                value={channelData?.data_type || 'integer'}
                                onChange={(e) => {
                                  setConfigData({
                                    ...configData,
                                    [channelKey]: {
                                      ...(channelData || { is_used: false, num_registers: 1, start_registers: 0 }),
                                      data_type: e.target.value,
                                    }
                                  });
                                }}
                                className="input-field w-full text-sm"
                              >
                                <option value="integer">Integer</option>
                                <option value="float">Float</option>
                                <option value="double">Double</option>
                                <option value="boolean">Boolean</option>
                              </select>
                            </div>

                            {/* Registers Grid */}
                            <div className="grid grid-cols-2 gap-3">
                              {/* Start Registers */}
                              <div>
                                <label className="block text-sm font-medium text-textMain mb-1">
                                  Start Register
                                </label>
                                <input
                                  type="number"
                                  min="0"
                                  value={channelData?.start_registers || 0}
                                  onChange={(e) => {
                                    setConfigData({
                                      ...configData,
                                      [channelKey]: {
                                        ...(channelData || { is_used: false, data_type: 'integer', num_registers: 1 }),
                                        start_registers: parseInt(e.target.value) || 0,
                                      }
                                    });
                                  }}
                                  className="input-field w-full text-sm"
                                  placeholder="0"
                                />
                              </div>

                              {/* Number of Registers */}
                              <div>
                                <label className="block text-sm font-medium text-textMain mb-1">
                                  Num Registers
                                </label>
                                <input
                                  type="number"
                                  min="1"
                                  value={channelData?.num_registers || 1}
                                  onChange={(e) => {
                                    setConfigData({
                                      ...configData,
                                      [channelKey]: {
                                        ...(channelData || { is_used: false, data_type: 'integer', start_registers: 0 }),
                                        num_registers: parseInt(e.target.value) || 1,
                                      }
                                    });
                                  }}
                                  className="input-field w-full text-sm"
                                  placeholder="1"
                                />
                              </div>
                            </div>
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>
            </div>

            {/* Config Buttons */}
            <div className="flex gap-3 pt-6 border-t border-gray-200">
              <button
                type="submit"
                disabled={updateConfigMutation.isPending}
                className="btn-primary flex items-center gap-2"
              >
                {updateConfigMutation.isPending ? (
                  <>
                    <Loader className="w-4 h-4 animate-spin" />
                    Saving...
                  </>
                ) : (
                  <>
                    <Save className="w-4 h-4" />
                    Save Configuration
                  </>
                )}
              </button>
              <button
                type="button"
                onClick={() => setConfigData({
                  protocol: scale.scale_config?.protocol,
                  poll_interval: scale.scale_config?.poll_interval,
                  conn_params: scale.scale_config?.conn_params,
                })}
                className="btn-secondary"
              >
                Reset
              </button>
            </div>
          </div>
        </form>
      )}
    </div>
  );
};
