import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ProductListDTO } from '../../dto/product/ProductListDTO';
import { ProductResponseDTO } from '../../dto/product/ProductResponseDTO';
import { PageResponseDTO } from '../../dto/common/PageResponseDTO';

export interface ProductSearchParams {
  search?: string;
  category?: string;
  minPrice?: number;
  maxPrice?: number;
  inStock?: boolean;
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
    if (searchParams.category) {
      params = params.set('category', searchParams.category);
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
    params = params.set('page', (searchParams.page || 0).toString());
    params = params.set('size', (searchParams.size || 20).toString());
    params = params.set('sortBy', searchParams.sortBy || 'name');
    params = params.set('sortDir', searchParams.sortDir || 'asc');

    return this.http.get<PageResponseDTO<ProductListDTO>>(`${this.baseUrl}products/search`, { params });
  }

  getCategories(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}products/categories`);
  }

  getAvailability(productId: number): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}products/${productId}/availability`);
  }
}
