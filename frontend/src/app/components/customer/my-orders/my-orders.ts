import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { OrderService } from '../../../services/order/order.service';
import { OrderListDTO } from '../../../dto/order/OrderListDTO';
import { PageResponseDTO } from '../../../dto/common/PageResponseDTO';
import { CardModule } from 'primeng/card';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { MessageModule } from 'primeng/message';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { PaginatorModule } from 'primeng/paginator';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';

@Component({
  selector: 'app-my-orders',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    CardModule,
    TableModule,
    ButtonModule,
    ProgressSpinnerModule,
    MessageModule,
    ToastModule,
    PaginatorModule,
    TagModule,
    TooltipModule
  ],
  providers: [MessageService],
  templateUrl: './my-orders.html',
  styleUrl: './my-orders.scss'
})
export class MyOrdersComponent implements OnInit {
  orders: OrderListDTO[] = [];
  loading = false;

  currentPage = 0;
  pageSize = 20;
  totalRecords = 0;

  constructor(
    private orderService: OrderService,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(): void {
    this.loading = true;
    this.orderService.getMyOrders(this.currentPage, this.pageSize).subscribe({
      next: (response: PageResponseDTO<OrderListDTO>) => {
        this.orders = response.content;
        this.totalRecords = response.totalElements;
        this.loading = false;
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Greska',
          detail: 'Neuspesno ucitavanje porudzbina'
        });
        this.loading = false;
        console.error(err);
      }
    });
  }

  onPageChange(event: any): void {
    this.currentPage = event.page;
    this.pageSize = event.rows;
    this.loadOrders();
  }

  downloadInvoice(orderId: number, orderNumber: string): void {
    this.orderService.downloadInvoice(orderId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `faktura-${orderNumber}.pdf`;
        link.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Greska',
          detail: 'Neuspesno preuzimanje fakture'
        });
        console.error(err);
      }
    });
  }

  formatDate(dateString: string): string {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleDateString('sr-RS', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  formatPrice(price: number): string {
    return price.toFixed(2) + ' EUR';
  }

  getStatusSeverity(status: string): "success" | "info" | "warn" | "danger" | "secondary" | "contrast" {
    switch (status) {
      case 'PENDING': return 'warn';
      case 'CONFIRMED': return 'info';
      case 'PROCESSING': return 'info';
      case 'SHIPPED': return 'info';
      case 'DELIVERED': return 'success';
      case 'CANCELLED': return 'danger';
      default: return 'secondary';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'PENDING': return 'Na cekanju';
      case 'CONFIRMED': return 'Potvrdjena';
      case 'PROCESSING': return 'U obradi';
      case 'SHIPPED': return 'Isporucuje se';
      case 'DELIVERED': return 'Isporuceno';
      case 'CANCELLED': return 'Otkazana';
      default: return status;
    }
  }
}
