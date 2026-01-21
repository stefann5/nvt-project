export interface TemperatureStatisticsDTO {
  warehouseId: number;
  warehouseName: string;
  sectorId: number;
  sectorName: string;
  startDate: string;
  endDate: string;
  aggregationType: string;
  averageTemperature: number;
  minTemperature: number;
  maxTemperature: number;
  dataPoints: TemperatureDataPoint[];
}

export interface TemperatureDataPoint {
  label: string;
  startDate: string;
  endDate: string;
  avgTemperature: number;
  minTemperature: number;
  maxTemperature: number;
}
