export interface WarehouseResponseDTO {
  id: number;
  name: string;
  countryId: number;
  countryName: string;
  cityId: number;
  cityName: string;
  street: string;
  streetNumber: string;
  latitude: number;
  longitude: number;
  totalCapacity: number;
  active: boolean;
  online: boolean;
  lastHeartbeat: string;
  createdAt: string;
  updatedAt: string;
  sectors: SectorDTO[];
  images: ImageDTO[];
}

export interface SectorDTO {
  id: number;
  name: string;
  minTemperature: number;
  maxTemperature: number;
  currentTemperature: number;
  lastTemperatureUpdate: string;
  capacity: number;
}

export interface ImageDTO {
  id: number;
  originalName: string;
}
