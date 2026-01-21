export interface UpdateWarehouseDTO {
  name?: string;
  countryId?: number;
  cityId?: number;
  street?: string;
  streetNumber?: string;
  latitude?: number;
  longitude?: number;
  totalCapacity?: number;
  imagesToDelete?: number[];
  sectors?: UpdateWarehouseSectorDTO[];
}

export interface UpdateWarehouseSectorDTO {
  id?: number;
  name: string;
  minTemperature: number;
  maxTemperature: number;
  capacity: number;
}
