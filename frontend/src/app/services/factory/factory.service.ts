import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { FactorySimpleDTO } from '../../dto/factory/FactoryDTO';

@Injectable({
  providedIn: 'root'
})
export class FactoryService {
  private readonly baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getAllSimple(): Observable<FactorySimpleDTO[]> {
    return this.http.get<FactorySimpleDTO[]>(`${this.baseUrl}factories/simple`);
  }
}
