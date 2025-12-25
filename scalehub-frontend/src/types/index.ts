// ==================== AUTH TYPES ====================
export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  user: User;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface AuthContextType {
  user: User | null;
  token: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  login: (credentials: LoginRequest) => Promise<void>;
  logout: () => void;
  refresh: () => Promise<void>;
}

// ==================== USER & ROLE TYPES ====================
export interface User {
  id: number;
  username: string;
  fullName: string;
  status: number; // Short in Java
  roles: RoleWithPermissions[];
  createdAt: string;
  createdBy?: string;
  updatedAt: string;
  updatedBy?: string;
}

export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'LOCKED';

export interface Role {
  id: number;
  name: string;
  description?: string;
  permissions: Permission[];
  createdAt?: string;
  updatedAt?: string;
}

export interface RoleWithPermissions {
  id: number;
  name: string;
  code: string;
  permissions: Permission[];
}

export interface Permission {
  id: number;
  name: string;
  resource: string;
  action: string;
  description?: string;
}

export interface CreateUserRequest {
  username: string;
  password: string;
  fullName: string;
  email: string;
  phone?: string;
  roleIds: number[];
}

export interface UpdateUserRequest {
  fullName?: string;
  email?: string;
  phone?: string;
  status?: UserStatus;
  roleIds?: number[];
}

export interface CreateRoleRequest {
  name: string;
  description?: string;
  permissionIds: number[];
}

export interface UpdateRoleRequest {
  name?: string;
  description?: string;
  permissionIds?: number[];
}

// ==================== SCALE TYPES ====================
export interface Scale {
  id: number;
  name: string;
  model?: string;
  location_id: number;
  location_name: string;
  is_active: boolean;
  scale_config?: ScaleConfigResponse;
  createdAt?: string;
  created_at?: string;
  created_by?: string;
  updatedAt?: string;
  updated_at?: string;
  updated_by?: string;
}

export interface ScaleConfigResponse {
  protocol: string;
  scale_id: number;
  poll_interval: number;
  conn_params: {
    ip: string;
    port: number;
  };
  data_1?: ScaleDataConfig;
  data_2?: ScaleDataConfig;
}

export interface ScaleDataConfig {
  name: string;
  is_used: boolean;
  data_type: string;
  num_registers: number;
  start_registers: number;
}

export type ConnectionType = 'MODBUS_TCP' | 'SERIAL';

export type ScaleStatus = 'ONLINE' | 'OFFLINE' | 'ERROR' | 'MAINTENANCE';

export type WeightUnit = 'KG' | 'TON' | 'POUND';

export interface ScaleConfig {
  id?: number;
  scaleId: number;
  
  // Modbus TCP configuration
  ipAddress?: string;
  port?: number;
  slaveId?: number;
  
  // Serial configuration
  serialPort?: string;
  baudRate?: number;
  dataBits?: number;
  stopBits?: number;
  parity?: SerialParity;
  
  // Common configuration
  registerAddress: number;
  registerCount: number;
  dataType: DataType;
  scaleFactor: number;
  offset: number;
  pollInterval: number;
  timeout: number;
  retryAttempts: number;
  
  createdAt?: string;
  updatedAt?: string;
}

export type SerialParity = 'NONE' | 'ODD' | 'EVEN';

export type DataType = 'INT16' | 'UINT16' | 'INT32' | 'UINT32' | 'FLOAT32';

export interface CreateScaleRequest {
  name: string;
  location_id: number;
  model?: string;
  is_active?: boolean;
}

export interface UpdateScaleRequest {
  name?: string;
  description?: string;
  locationId?: number;
  status?: ScaleStatus;
  unit?: WeightUnit;
}

export interface ScaleRealtimeData {
  scaleId: number;
  lastTime: string;
  data1: string | null;
  data2: string | null;
  data3: string | null;
  data4: string | null;
  data5: string | null;
  status: ScaleStatus;
}

// ==================== LOCATION TYPES ====================
export interface Location {
  id: number;
  name: string;
  code: string;
  description?: string;
  parentId?: number;
  level: number;
  path: string;
  createdAt: string;
  updatedAt: string;
}

export interface LocationTree extends Location {
  children: LocationTree[];
  scaleCount?: number;
}

export interface CreateLocationRequest {
  name: string;
  code: string;
  description?: string;
  parentId?: number;
}

export interface UpdateLocationRequest {
  name?: string;
  description?: string;
  parentId?: number;
}

// ==================== REPORT TYPES ====================
export interface DailyReport {
  reportDate: string;
  scaleId: number;
  scaleName: string;
  locationName: string;
  totalWeighings: number;
  totalWeight: number;
  averageWeight: number;
  minWeight: number;
  maxWeight: number;
  unit: WeightUnit;
}

export interface WeighingLog {
  id: number;
  scaleId: number;
  scaleName: string;
  locationName: string;
  weight: number;
  unit: WeightUnit;
  status: WeighingStatus;
  timestamp: string;
  notes?: string;
}

export type WeighingStatus = 'SUCCESS' | 'ERROR' | 'INVALID';

export interface ReportFilter {
  startDate?: string;
  endDate?: string;
  scaleId?: number;
  locationId?: number;
  status?: WeighingStatus;
}

// ==================== COMMON TYPES ====================
export interface PaginatedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  message?: string;
  errors?: string[];
}

export interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}

export interface SelectOption {
  value: string | number;
  label: string;
}

export interface TableColumn<T> {
  key: keyof T | string;
  label: string;
  sortable?: boolean;
  render?: (value: any, row: T) => React.ReactNode;
}

export interface SortConfig {
  key: string;
  direction: 'asc' | 'desc';
}

export interface FilterConfig {
  [key: string]: any;
}

// ==================== WEBSOCKET TYPES ====================
export interface WebSocketMessage {
  type: 'SCALE_UPDATE' | 'SCALE_STATUS' | 'SYSTEM_ALERT';
  payload: any;
  timestamp: string;
}

export interface WebSocketContextType {
  connected: boolean;
  scaleData: Map<number, ScaleRealtimeData>;
  subscribe: (scaleId: number) => void;
  unsubscribe: (scaleId: number) => void;
  subscribeAll: () => void;
}

// ==================== CHART TYPES ====================
export interface ChartDataPoint {
  timestamp: string;
  value: number;
  label?: string;
}

export interface ChartConfig {
  xAxisKey: string;
  yAxisKey: string;
  lineColor?: string;
  showGrid?: boolean;
  showTooltip?: boolean;
}
