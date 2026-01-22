export interface WarehouseAvailabilityStatisticsDTO {
  warehouseId: number;
  warehouseName: string;
  startTime: string;
  endTime: string;
  aggregationType: string;
  totalOnlineSeconds: number;
  totalOfflineSeconds: number;
  onlinePercentage: number;
  offlinePercentage: number;
  dataPoints: AvailabilityDataPoint[];
}

export interface AvailabilityDataPoint {
  label: string;
  startTime: string;
  endTime: string;
  onlineSeconds: number;
  offlineSeconds: number;
  onlinePercentage: number;
  offlinePercentage: number;
}
