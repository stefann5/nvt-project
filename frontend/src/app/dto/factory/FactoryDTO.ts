export interface FactorySimpleDTO {
  id: number;
  name: string;
}

export interface FactoryListDTO {
  id: number;
  name: string;
  description?: string;
  countryName?: string;
  cityName?: string;
  street?: string;
  streetNumber?: string;
  productCount: number;
  active: boolean;
  online: boolean;
  lastHeartbeat?: string;
  createdAt?: string;
}

export interface FactoryResponseDTO {
  id: number;
  name: string;
  description?: string;
  countryId?: number;
  countryName?: string;
  cityId?: number;
  cityName?: string;
  street: string;
  streetNumber?: string;
  latitude: number;
  longitude: number;
  active: boolean;
  online: boolean;
  lastHeartbeat?: string;
  createdAt?: string;
  updatedAt?: string;
  products: FactoryProductDTO[];
  images: FactoryImageDTO[];
}

export interface FactoryProductDTO {
  id: number;
  name: string;
  categoryName?: string;
}

export interface FactoryImageDTO {
  id: number;
  originalName: string;
}

export interface CreateFactoryDTO {
  name: string;
  description?: string;
  countryId: number;
  cityId: number;
  street: string;
  streetNumber?: string;
  latitude: number;
  longitude: number;
  productIds?: number[];
}

export interface UpdateFactoryDTO {
  name: string;
  description?: string;
  countryId: number;
  cityId: number;
  street: string;
  streetNumber?: string;
  latitude: number;
  longitude: number;
  productIds?: number[];
  imagesToDelete?: number[];
}
