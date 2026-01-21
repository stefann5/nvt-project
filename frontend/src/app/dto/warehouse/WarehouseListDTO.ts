export interface WarehouseListDTO {
  id: number;
  name: string;
  countryName: string;
  cityName: string;
  street: string;
  streetNumber: string;
  totalCapacity: number;
  sectorCount: number;
  active: boolean;
  online: boolean;
  lastHeartbeat: string;
  createdAt: string;
}
