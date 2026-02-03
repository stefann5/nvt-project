import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { 
  FactorySimpleDTO, 
  FactoryListDTO, 
  FactoryResponseDTO, 
  CreateFactoryDTO, 
  UpdateFactoryDTO 
} from '../../dto/factory/FactoryDTO';
import { PageResponseDTO } from '../../dto/common/PageResponseDTO';
import { CountryDTO } from '../../dto/company/CountryDTO';
import { CityDTO } from '../../dto/company/CityDTO';

@Injectable({
  providedIn: 'root'
})
export class FactoryService {
  private readonly baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getAllSimple(): Observable<FactorySimpleDTO[]> {
    return this.http.get<FactorySimpleDTO[]>(`${this.baseUrl}factories/simple`);
  }

  getAll(): Observable<FactoryListDTO[]> {
    return this.http.get<FactoryListDTO[]>(`${this.baseUrl}factories`);
  }

  getAllPaged(page: number = 0, size: number = 20, sortBy: string = 'id', sortDir: string = 'asc'): Observable<PageResponseDTO<FactoryListDTO>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);
    return this.http.get<PageResponseDTO<FactoryListDTO>>(`${this.baseUrl}factories/paged`, { params });
  }

  getById(id: number): Observable<FactoryResponseDTO> {
    return this.http.get<FactoryResponseDTO>(`${this.baseUrl}factories/${id}`);
  }

  search(query: string): Observable<FactoryListDTO[]> {
    return this.http.get<FactoryListDTO[]>(`${this.baseUrl}factories/search`, {
      params: { query }
    });
  }

  searchPaged(query: string, page: number = 0, size: number = 20): Observable<PageResponseDTO<FactoryListDTO>> {
    const params = new HttpParams()
      .set('query', query)
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponseDTO<FactoryListDTO>>(`${this.baseUrl}factories/search/paged`, { params });
  }

  create(data: CreateFactoryDTO, images: File[]): Observable<FactoryResponseDTO> {
    const formData = new FormData();
    formData.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
    images.forEach((image) => {
      formData.append('images', image, image.name);
    });
    return this.http.post<FactoryResponseDTO>(`${this.baseUrl}factories`, formData);
  }

  update(id: number, data: UpdateFactoryDTO, newImages?: File[]): Observable<FactoryResponseDTO> {
    const formData = new FormData();
    formData.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
    if (newImages && newImages.length > 0) {
      newImages.forEach((image) => {
        formData.append('images', image, image.name);
      });
    }
    return this.http.put<FactoryResponseDTO>(`${this.baseUrl}factories/${id}`, formData);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}factories/${id}`);
  }

  getAllCountries(): Observable<CountryDTO[]> {
    return this.http.get<CountryDTO[]>(`${this.baseUrl}factories/countries`);
  }

  getCitiesByCountry(countryId: number): Observable<CityDTO[]> {
    return this.http.get<CityDTO[]>(`${this.baseUrl}factories/countries/${countryId}/cities`);
  }

  getFactoryImages(factoryId: number): Observable<{
    images: { id: number; originalName: string; url: string }[];
  }> {
    return this.http.get<{
      images: { id: number; originalName: string; url: string }[];
    }>(`${this.baseUrl}files/factory/${factoryId}/images`);
  }
}
