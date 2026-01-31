export interface ProductListDTO {
  id: number;
  name: string;
  sku: string;
  price: number;
  weight?: number;
  category: string;
  unit: string;
  forSale: boolean;
  active: boolean;
  totalQuantity: number;
  imageUrl?: string;
  factoryCount: number;
}
