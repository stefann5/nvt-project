import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface CreateManagerDTO {
  username: string;
  name: string;
  surname: string;
  phoneNumber?: string;
}

export interface ManagerResponseDTO {
  id: number;
  username: string;
  name: string;
  surname: string;
  phoneNumber: string;
  photo: string | null;
  active: boolean;
  blocked: boolean;
  role: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({
  providedIn: 'root'
})
export class ManagerService {

  private baseUrl = `${environment.apiUrl}managers`;

  constructor(private http: HttpClient) {}

  /**
   * Create a new manager with optional profile image
   */
  createManager(manager: CreateManagerDTO, profileImage?: File): Observable<ManagerResponseDTO> {
    const formData = new FormData();
    formData.append('manager', JSON.stringify(manager));
    
    if (profileImage) {
      formData.append('profileImage', profileImage);
    }

    return this.http.post<ManagerResponseDTO>(this.baseUrl, formData);
  }

  /**
   * Get all managers with pagination
   */
  getManagersPaged(page: number = 0, size: number = 10, sortBy: string = 'name', sortDir: string = 'asc'): Observable<PageResponse<ManagerResponseDTO>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);

    return this.http.get<PageResponse<ManagerResponseDTO>>(`${this.baseUrl}/paged`, { params });
  }

  /**
   * Get all managers
   */
  getAllManagers(): Observable<ManagerResponseDTO[]> {
    return this.http.get<ManagerResponseDTO[]>(this.baseUrl);
  }

  /**
   * Search managers
   */
  searchManagers(query: string, page: number = 0, size: number = 10): Observable<PageResponse<ManagerResponseDTO>> {
    const params = new HttpParams()
      .set('query', query)
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PageResponse<ManagerResponseDTO>>(`${this.baseUrl}/search`, { params });
  }

  /**
   * Get manager by ID
   */
  getManagerById(id: number): Observable<ManagerResponseDTO> {
    return this.http.get<ManagerResponseDTO>(`${this.baseUrl}/${id}`);
  }

  /**
   * Block a manager
   */
  blockManager(id: number): Observable<ManagerResponseDTO> {
    return this.http.put<ManagerResponseDTO>(`${this.baseUrl}/${id}/block`, {});
  }

  /**
   * Unblock a manager
   */
  unblockManager(id: number): Observable<ManagerResponseDTO> {
    return this.http.put<ManagerResponseDTO>(`${this.baseUrl}/${id}/unblock`, {});
  }
}
