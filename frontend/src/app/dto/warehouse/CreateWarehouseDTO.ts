export interface CreateWarehouseDTO {
  name: string;
  countryId: number;
  cityId: number;
  street: string;
  streetNumber: string;
  latitude: number;
  longitude: number;
  totalCapacity: number;
  sectors: CreateWarehouseSectorDTO[];
}

export interface CreateWarehouseSectorDTO {
  name: string;
  minTemperature: number;
  maxTemperature: number;
  capacity: number;
}
