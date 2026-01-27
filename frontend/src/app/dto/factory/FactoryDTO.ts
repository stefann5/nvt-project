export interface FactorySimpleDTO {
  id: number;
  name: string;
}

export interface FactoryListDTO {
  id: number;
  name: string;
  countryName?: string;
  cityName?: string;
  isOnline: boolean;
}
