import apiClient from './client';

export interface ReportRequest {
  scaleIds: number[];
  dataField: string;
  method: 'SUM' | 'AVG' | 'MAX';
  fromDate: string; // 'YYYY-MM-DD'
  toDate: string; // 'YYYY-MM-DD'
  interval: 'HOUR' | 'DAY' | 'WEEK' | 'MONTH' | 'YEAR';
}

export interface DataPoint {
  time: string;
  value: number;
}

export interface ReportResponse {
  reportName: string;
  method: string;
  dataField: string;
  interval: string;
  dataPoints: DataPoint[];
}

export const reportsApi = {
  generateReport: async (request: ReportRequest): Promise<ReportResponse> => {
    const response = await apiClient.post('/reports/generate', request);
    return response.data.data;
  },

  aggregateDailyData: async (): Promise<string> => {
    const response = await apiClient.post('/reports/aggregate-daily');
    return response.data.data;
  },
};
