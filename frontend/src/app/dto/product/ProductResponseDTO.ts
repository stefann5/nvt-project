export interface ProductResponseDTO {
  id: number;
  name: string;
  description: string;
  sku: string;
  price: number;
  weight: number;
  category: string;
  unit: string;
  active: boolean;
  createdAt: string;
  images: ProductImageDTO[];
  totalQuantity: number;
}

export interface ProductImageDTO {
  id: number;
  originalName: string;
}
