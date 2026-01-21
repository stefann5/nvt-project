import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import { OrderService } from '../../../services/order/order.service';
import { OrderListDTO } from '../../../dto/order/OrderListDTO';

@Component({
    selector: 'app-manager-orders',
    standalone: true,
    imports: [CommonModule, TableModule, ButtonModule, CardModule, TagModule],
    templateUrl: './manager-orders.html',
    styleUrl: './manager-orders.scss'
})
export class ManagerOrdersComponent implements OnInit {
    orders: OrderListDTO[] = [];
    loading = false;
    totalRecords = 0;
    rows = 20;
    first = 0;

    constructor(
        private orderService: OrderService,
        private router: Router
    ) {}

    ngOnInit(): void {
        this.loadOrders();
    }

    loadOrders(event?: any): void {
        this.loading = true;
        const page = event ? event.first / event.rows : 0;
        const size = event ? event.rows : this.rows;

        this.orderService.getAllOrdersPaged(page, size).subscribe({
            next: (response) => {
                this.orders = response.content;
                this.totalRecords = response.totalElements;
                this.loading = false;
            },
            error: (err) => {
                console.error('Error loading orders:', err);
                this.loading = false;
            }
        });
    }

    viewOrder(order: OrderListDTO): void {
        this.router.navigate(['/app/manager/orders', order.id]);
    }

    getStatusSeverity(status: string): "success" | "secondary" | "info" | "warn" | "danger" | "contrast" | undefined {
        switch (status) {
            case 'PENDING':
                return 'warn';
            case 'CONFIRMED':
                return 'info';
            case 'SHIPPED':
                return 'info';
            case 'DELIVERED':
                return 'success';
            case 'CANCELLED':
                return 'danger';
            default:
                return 'secondary';
        }
    }
}
