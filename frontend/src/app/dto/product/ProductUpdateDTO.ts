export interface ProductUpdateDTO {
  name?: string;
  description?: string;
  price?: number;
  weight?: number;
  category?: string;
  unit?: string;
  forSale?: boolean;
  active?: boolean;
  factoryIds?: number[];
  imagesToDelete?: number[];
}
