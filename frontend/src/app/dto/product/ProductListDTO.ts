export interface ProductListDTO {
  id: number;
  name: string;
  sku: string;
  price: number;
  category: string;
  unit: string;
  active: boolean;
  totalQuantity: number;
  imageUrl?: string;
}
