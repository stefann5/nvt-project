import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { VehicleBrandDTO } from '../../dto/vehicle/VehicleBrandDTO';
import { VehicleModelDTO } from '../../dto/vehicle/VehicleModelDTO';
import { VehicleResponseDTO } from '../../dto/vehicle/VehicleResponseDTO';
import { CreateVehicleDTO } from '../../dto/vehicle/CreateVehicleDTO';
import { UpdateVehicleDTO } from '../../dto/vehicle/UpdateVehicleDTO';

@Injectable({
  providedIn: 'root'
})
export class VehicleService {
  private readonly baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getAllBrands(): Observable<VehicleBrandDTO[]> {
    return this.http.get<VehicleBrandDTO[]>(`${this.baseUrl}vehicles/brands`);
  }

  getModelsByBrand(brandId: number): Observable<VehicleModelDTO[]> {
    return this.http.get<VehicleModelDTO[]>(`${this.baseUrl}vehicles/brands/${brandId}/models`);
  }

  getAll(): Observable<VehicleResponseDTO[]> {
    return this.http.get<VehicleResponseDTO[]>(`${this.baseUrl}vehicles`);
  }

  getById(id: number): Observable<VehicleResponseDTO> {
    return this.http.get<VehicleResponseDTO>(`${this.baseUrl}vehicles/${id}`);
  }

  search(query: string): Observable<VehicleResponseDTO[]> {
    return this.http.get<VehicleResponseDTO[]>(`${this.baseUrl}vehicles/search`, {
      params: { query }
    });
  }

  create(data: CreateVehicleDTO, images: File[]): Observable<VehicleResponseDTO> {
    const formData = new FormData();
    formData.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
    images.forEach((image) => {
      formData.append('images', image, image.name);
    });
    return this.http.post<VehicleResponseDTO>(`${this.baseUrl}vehicles`, formData);
  }

  update(id: number, data: UpdateVehicleDTO, newImages?: File[]): Observable<VehicleResponseDTO> {
    const formData = new FormData();
    formData.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
    if (newImages && newImages.length > 0) {
      newImages.forEach((image) => {
        formData.append('images', image, image.name);
      });
    }
    return this.http.put<VehicleResponseDTO>(`${this.baseUrl}vehicles/${id}`, formData);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}vehicles/${id}`);
  }

  getVehicleImages(vehicleId: number): Observable<{
    images: { id: number; originalName: string; url: string }[];
  }> {
    return this.http.get<{
      images: { id: number; originalName: string; url: string }[];
    }>(`${this.baseUrl}files/vehicle/${vehicleId}/images`);
  }
}
