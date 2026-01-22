import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { WarehouseResponseDTO, SectorDTO } from '../../dto/warehouse/WarehouseResponseDTO';
import { WarehouseListDTO } from '../../dto/warehouse/WarehouseListDTO';
import { CreateWarehouseDTO } from '../../dto/warehouse/CreateWarehouseDTO';
import { UpdateWarehouseDTO } from '../../dto/warehouse/UpdateWarehouseDTO';
import { TemperatureStatisticsDTO } from '../../dto/warehouse/TemperatureStatisticsDTO';
import { WarehouseAvailabilityStatisticsDTO } from '../../dto/warehouse/WarehouseAvailabilityStatisticsDTO';
import { PageResponseDTO } from '../../dto/common/PageResponseDTO';
import { CountryDTO } from '../../dto/company/CountryDTO';
import { CityDTO } from '../../dto/company/CityDTO';

@Injectable({
  providedIn: 'root'
})
export class WarehouseService {
  private readonly baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getAllCountries(): Observable<CountryDTO[]> {
    return this.http.get<CountryDTO[]>(`${this.baseUrl}warehouses/countries`);
  }

  getCitiesByCountry(countryId: number): Observable<CityDTO[]> {
    return this.http.get<CityDTO[]>(`${this.baseUrl}warehouses/countries/${countryId}/cities`);
  }

  getAll(): Observable<WarehouseListDTO[]> {
    return this.http.get<WarehouseListDTO[]>(`${this.baseUrl}warehouses`);
  }

  getAllPaged(page: number = 0, size: number = 20, sortBy: string = 'id', sortDir: string = 'asc'): Observable<PageResponseDTO<WarehouseListDTO>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);
    return this.http.get<PageResponseDTO<WarehouseListDTO>>(`${this.baseUrl}warehouses/paged`, { params });
  }

  getById(id: number): Observable<WarehouseResponseDTO> {
    return this.http.get<WarehouseResponseDTO>(`${this.baseUrl}warehouses/${id}`);
  }

  search(query: string): Observable<WarehouseListDTO[]> {
    return this.http.get<WarehouseListDTO[]>(`${this.baseUrl}warehouses/search`, {
      params: { query }
    });
  }

  searchPaged(query: string, page: number = 0, size: number = 20): Observable<PageResponseDTO<WarehouseListDTO>> {
    const params = new HttpParams()
      .set('query', query)
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponseDTO<WarehouseListDTO>>(`${this.baseUrl}warehouses/search/paged`, { params });
  }

  create(data: CreateWarehouseDTO, images: File[]): Observable<WarehouseResponseDTO> {
    const formData = new FormData();
    formData.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
    images.forEach((image) => {
      formData.append('images', image, image.name);
    });
    return this.http.post<WarehouseResponseDTO>(`${this.baseUrl}warehouses`, formData);
  }

  update(id: number, data: UpdateWarehouseDTO, newImages?: File[]): Observable<WarehouseResponseDTO> {
    const formData = new FormData();
    formData.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
    if (newImages && newImages.length > 0) {
      newImages.forEach((image) => {
        formData.append('images', image, image.name);
      });
    }
    return this.http.put<WarehouseResponseDTO>(`${this.baseUrl}warehouses/${id}`, formData);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}warehouses/${id}`);
  }

  getWarehouseImages(warehouseId: number): Observable<{
    images: { id: number; originalName: string; url: string }[];
  }> {
    return this.http.get<{
      images: { id: number; originalName: string; url: string }[];
    }>(`${this.baseUrl}files/warehouse/${warehouseId}/images`);
  }

  getSectorWithCurrentTemperature(warehouseId: number, sectorId: number): Observable<SectorDTO> {
    return this.http.get<SectorDTO>(`${this.baseUrl}warehouses/${warehouseId}/sectors/${sectorId}`);
  }

  getTemperatureStatsByPeriod(warehouseId: number, sectorId: number, period: string): Observable<TemperatureStatisticsDTO> {
    const params = new HttpParams().set('period', period);
    return this.http.get<TemperatureStatisticsDTO>(
      `${this.baseUrl}warehouses/${warehouseId}/sectors/${sectorId}/temperature/stats`,
      { params }
    );
  }

  getTemperatureStatsByDateRange(warehouseId: number, sectorId: number, startDate: string, endDate: string): Observable<TemperatureStatisticsDTO> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);
    return this.http.get<TemperatureStatisticsDTO>(
      `${this.baseUrl}warehouses/${warehouseId}/sectors/${sectorId}/temperature/stats`,
      { params }
    );
  }

  getAvailabilityStatsByPeriod(warehouseId: number, period: string): Observable<WarehouseAvailabilityStatisticsDTO> {
    const params = new HttpParams().set('period', period);
    return this.http.get<WarehouseAvailabilityStatisticsDTO>(
      `${this.baseUrl}warehouses/${warehouseId}/availability/stats`,
      { params }
    );
  }

  getAvailabilityStatsByDateRange(warehouseId: number, startTime: string, endTime: string): Observable<WarehouseAvailabilityStatisticsDTO> {
    const params = new HttpParams()
      .set('startTime', startTime)
      .set('endTime', endTime);
    return this.http.get<WarehouseAvailabilityStatisticsDTO>(
      `${this.baseUrl}warehouses/${warehouseId}/availability/stats`,
      { params }
    );
  }
}
