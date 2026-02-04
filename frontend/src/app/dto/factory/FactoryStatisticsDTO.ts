export interface ProductionDataPoint {
  date: string;
  endDate: string;
  label: string;
  quantity: number;
}

export interface FactoryProductionStatisticsDTO {
  factoryId: number;
  factoryName: string;
  productId: number;
  productName: string;
  startDate: string;
  endDate: string;
  aggregationType: string;
  dataPoints: ProductionDataPoint[];
  totalQuantity: number;
  dataPointCount: number;
}

export interface AvailabilityDataPoint {
  date: string;
  endDate: string;
  label: string;
  onlinePercentage: number;
  onlineMinutes: number;
  offlineMinutes: number;
  totalMinutes: number;
}

export interface FactoryAvailabilityStatisticsDTO {
  factoryId: number;
  factoryName: string;
  startDate: string;
  endDate: string;
  aggregationType: string;
  dataPoints: AvailabilityDataPoint[];
  overallOnlinePercentage: number;
  totalOnlineMinutes: number;
  totalOfflineMinutes: number;
  totalMinutes: number;
}

export type TimePeriod = 'week' | 'month' | '3months' | '6months' | 'year' | 'custom';
