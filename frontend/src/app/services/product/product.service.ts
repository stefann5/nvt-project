import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ProductListDTO } from '../../dto/product/ProductListDTO';
import { ProductResponseDTO } from '../../dto/product/ProductResponseDTO';
import { ProductCreateDTO } from '../../dto/product/ProductCreateDTO';
import { ProductUpdateDTO } from '../../dto/product/ProductUpdateDTO';
import { PageResponseDTO } from '../../dto/common/PageResponseDTO';

export interface ProductSearchParams {
  search?: string;
  category?: string;
  minPrice?: number;
  maxPrice?: number;
  inStock?: boolean;
  forSale?: boolean;
  active?: boolean;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ProductService {
  private readonly baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  // ==================== PUBLIC/CUSTOMER METHODS ====================

  getAllPaged(page: number = 0, size: number = 20, sortBy: string = 'name', sortDir: string = 'asc'): Observable<PageResponseDTO<ProductListDTO>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);
    return this.http.get<PageResponseDTO<ProductListDTO>>(`${this.baseUrl}products/paged`, { params });
  }

  getById(id: number): Observable<ProductResponseDTO> {
    return this.http.get<ProductResponseDTO>(`${this.baseUrl}products/${id}`);
  }

  search(searchParams: ProductSearchParams): Observable<PageResponseDTO<ProductListDTO>> {
    let params = new HttpParams();

    if (searchParams.search) {
      params = params.set('search', searchParams.search);
    }
    if (searchParams.category !== undefined && searchParams.category !== null) {
      params = params.set('category', searchParams.category.toString());
    }
    if (searchParams.minPrice !== undefined && searchParams.minPrice !== null) {
      params = params.set('minPrice', searchParams.minPrice.toString());
    }
    if (searchParams.maxPrice !== undefined && searchParams.maxPrice !== null) {
      params = params.set('maxPrice', searchParams.maxPrice.toString());
    }
    if (searchParams.inStock !== undefined && searchParams.inStock !== null) {
      params = params.set('inStock', searchParams.inStock.toString());
    }
    if (searchParams.forSale !== undefined && searchParams.forSale !== null) {
      params = params.set('forSale', searchParams.forSale.toString());
    }
    if (searchParams.active !== undefined && searchParams.active !== null) {
      params = params.set('active', searchParams.active.toString());
    }
    params = params.set('page', (searchParams.page || 0).toString());
    params = params.set('size', (searchParams.size || 20).toString());
    params = params.set('sortBy', searchParams.sortBy || 'name');
    params = params.set('sortDir', searchParams.sortDir || 'asc');

    return this.http.get<PageResponseDTO<ProductListDTO>>(`${this.baseUrl}products/search`, { params });
  }

  getCategories(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}products/categories`);
  }

  // getCategoriesTree(): Observable<CategoryDTO[]> {
  //   return this.http.get<CategoryDTO[]>(`${this.baseUrl}products/categories/tree`);
  // }

  getAvailability(productId: number): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}products/${productId}/availability`);
  }

  // ==================== MANAGER METHODS ====================

  getAllPagedForManager(page: number = 0, size: number = 20, sortBy: string = 'id', sortDir: string = 'desc'): Observable<PageResponseDTO<ProductListDTO>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);
    return this.http.get<PageResponseDTO<ProductListDTO>>(`${this.baseUrl}products/manager/paged`, { params });
  }

  create(product: ProductCreateDTO, images?: File[]): Observable<ProductResponseDTO> {
    const formData = new FormData();
    formData.append('product', new Blob([JSON.stringify(product)], { type: 'application/json' }));

    if (images && images.length > 0) {
      images.forEach(image => {
        formData.append('images', image);
      });
    }

    return this.http.post<ProductResponseDTO>(`${this.baseUrl}products`, formData);
  }

  update(id: number, product: ProductUpdateDTO, images?: File[]): Observable<ProductResponseDTO> {
    const formData = new FormData();
    formData.append('product', new Blob([JSON.stringify(product)], { type: 'application/json' }));

    if (images && images.length > 0) {
      images.forEach(image => {
        formData.append('images', image);
      });
    }

    return this.http.put<ProductResponseDTO>(`${this.baseUrl}products/${id}`, formData);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}products/${id}`);
  }

  toggleForSale(id: number): Observable<ProductResponseDTO> {
    return this.http.patch<ProductResponseDTO>(`${this.baseUrl}products/${id}/toggle-for-sale`, {});
  }

  toggleActive(id: number): Observable<ProductResponseDTO> {
    return this.http.patch<ProductResponseDTO>(`${this.baseUrl}products/${id}/toggle-active`, {});
  }

  getImageUrl(imagePath: string): Observable<string> {
    const params = new HttpParams()
      .set('imagePath', imagePath);
    return this.http.get(`${this.baseUrl}products/images/`, { params, responseType: 'text' });
  }
}
