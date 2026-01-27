export interface ProductResponseDTO {
  id: number;
  name: string;
  description: string;
  sku: string;
  price: number;
  weight: number;
  category: string;
  unit: string;
  forSale: boolean;
  active: boolean;
  createdAt: string;
  updatedAt?: string;
  images: ProductImageDTO[];
  totalQuantity: number;
  factories: FactorySimpleDTO[];
}

export interface ProductImageDTO {
  id: number;
  originalName: string;
  minioPath: string;
}

export interface FactorySimpleDTO {
  id: number;
  name: string;
  city?: string;
  country?: string;
  online?: boolean;
}
