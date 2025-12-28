import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CountryDTO } from '../../dto/company/CountryDTO';
import { CityDTO } from '../../dto/company/CityDTO';
import { CreateRequestDTO } from '../../dto/company/CreateRequestDTO';

@Injectable({
  providedIn: 'root'
})
export class CompanyRegistrationService {
  private readonly baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getCountries(): Observable<CountryDTO[]> {
    return this.http.get<CountryDTO[]>(`${this.baseUrl}locations/countries`);
  }

  getCitiesByCountry(countryId: number): Observable<CityDTO[]> {
    return this.http.get<CityDTO[]>(`${this.baseUrl}locations/countries/${countryId}/cities`);
  }

  createRegistrationRequest(
    data: CreateRequestDTO,
    images: File[],
    documents: File[]
  ): Observable<number> {
    const formData = new FormData();
    
    // Append the JSON data as a blob with correct content type
    formData.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
    
    // Append images
    images.forEach((image) => {
      formData.append('images', image, image.name);
    });
    
    // Append documents
    documents.forEach((document) => {
      formData.append('documents', document, document.name);
    });

    return this.http.post<number>(`${this.baseUrl}registration-requests`, formData);
  }
}
