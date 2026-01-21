import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { DividerModule } from 'primeng/divider';
import { OrderService } from '../../../services/order/order.service';
import { OrderResponseDTO } from '../../../dto/order/OrderResponseDTO';

@Component({
    selector: 'app-order-detail',
    standalone: true,
    imports: [CommonModule, CardModule, ButtonModule, TableModule, TagModule, DividerModule],
    templateUrl: './order-detail.html',
    styleUrl: './order-detail.scss'
})
export class OrderDetailComponent implements OnInit {
    order: OrderResponseDTO | null = null;
    loading = true;
    orderId: number = 0;

    constructor(
        private orderService: OrderService,
        private route: ActivatedRoute,
        private router: Router
    ) {}

    ngOnInit(): void {
        this.orderId = Number(this.route.snapshot.paramMap.get('id'));
        this.loadOrder();
    }

    loadOrder(): void {
        this.loading = true;
        this.orderService.getOrderById(this.orderId).subscribe({
            next: (order) => {
                this.order = order;
                this.loading = false;
            },
            error: (err) => {
                console.error('Error loading order:', err);
                this.loading = false;
            }
        });
    }

    goBack(): void {
        this.router.navigate(['/app/manager/orders']);
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
