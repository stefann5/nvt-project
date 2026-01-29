export interface ProductCreateDTO {
  name: string;
  description?: string;
  sku: string;
  price: number;
  weight?: number;
  category: string;
  unit?: string;
  forSale?: boolean;
  factoryIds?: number[];
}
