import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CreateOrderDTO } from '../../dto/order/CreateOrderDTO';
import { OrderResponseDTO } from '../../dto/order/OrderResponseDTO';
import { OrderListDTO } from '../../dto/order/OrderListDTO';
import { PageResponseDTO } from '../../dto/common/PageResponseDTO';

@Injectable({
  providedIn: 'root'
})
export class OrderService {
  private readonly baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  createOrder(order: CreateOrderDTO): Observable<OrderResponseDTO> {
    return this.http.post<OrderResponseDTO>(`${this.baseUrl}orders`, order);
  }

  getMyOrders(page: number = 0, size: number = 20): Observable<PageResponseDTO<OrderListDTO>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponseDTO<OrderListDTO>>(`${this.baseUrl}orders/my-orders`, { params });
  }

  getMyOrderById(id: number): Observable<OrderResponseDTO> {
    return this.http.get<OrderResponseDTO>(`${this.baseUrl}orders/my-orders/${id}`);
  }

  downloadInvoice(orderId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}orders/my-orders/${orderId}/invoice`, {
      responseType: 'blob'
    });
  }

  getAllOrdersPaged(page: number = 0, size: number = 20): Observable<PageResponseDTO<OrderListDTO>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponseDTO<OrderListDTO>>(`${this.baseUrl}orders/paged`, { params });
  }

  getOrderById(id: number): Observable<OrderResponseDTO> {
    return this.http.get<OrderResponseDTO>(`${this.baseUrl}orders/${id}`);
  }

  downloadInvoiceForManager(orderId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}orders/${orderId}/invoice`, {
      responseType: 'blob'
    });
  }
}
