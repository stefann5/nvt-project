import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { WarehouseService } from '../../../services/warehouse/warehouse.service';
import { WarehouseListDTO } from '../../../dto/warehouse/WarehouseListDTO';
import { PageResponseDTO } from '../../../dto/common/PageResponseDTO';
import { CardModule } from 'primeng/card';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { MessageModule } from 'primeng/message';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { ConfirmationService, MessageService } from 'primeng/api';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { TooltipModule } from 'primeng/tooltip';
import { PaginatorModule } from 'primeng/paginator';
import { TagModule } from 'primeng/tag';

@Component({
  selector: 'app-warehouse-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    CardModule,
    TableModule,
    ButtonModule,
    InputTextModule,
    ProgressSpinnerModule,
    MessageModule,
    ConfirmDialogModule,
    ToastModule,
    IconFieldModule,
    InputIconModule,
    TooltipModule,
    PaginatorModule,
    TagModule
  ],
  providers: [ConfirmationService, MessageService],
  templateUrl: './warehouse-list.html',
  styleUrl: './warehouse-list.scss'
})
export class WarehouseListComponent implements OnInit {
  warehouses: WarehouseListDTO[] = [];
  loading = false;
  searchQuery = '';
  private searchTimeout: any;

  currentPage = 0;
  pageSize = 20;
  totalRecords = 0;
  sortField = 'id';
  sortOrder = 'asc';

  constructor(
    private warehouseService: WarehouseService,
    private confirmationService: ConfirmationService,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    this.loadWarehouses();
  }

  loadWarehouses(): void {
    this.loading = true;
    this.warehouseService.getAllPaged(this.currentPage, this.pageSize, this.sortField, this.sortOrder).subscribe({
      next: (response: PageResponseDTO<WarehouseListDTO>) => {
        this.warehouses = response.content;
        this.totalRecords = response.totalElements;
        this.loading = false;
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to load warehouses' });
        this.loading = false;
        console.error(err);
      }
    });
  }

  onPageChange(event: any): void {
    this.currentPage = event.page;
    this.pageSize = event.rows;
    if (this.searchQuery.trim()) {
      this.performSearch();
    } else {
      this.loadWarehouses();
    }
  }

  onSearch(): void {
    if (this.searchTimeout) {
      clearTimeout(this.searchTimeout);
    }

    this.searchTimeout = setTimeout(() => {
      this.currentPage = 0;
      if (this.searchQuery.trim()) {
        this.performSearch();
      } else {
        this.loadWarehouses();
      }
    }, 300);
  }

  private performSearch(): void {
    this.loading = true;
    this.warehouseService.searchPaged(this.searchQuery, this.currentPage, this.pageSize).subscribe({
      next: (response: PageResponseDTO<WarehouseListDTO>) => {
        this.warehouses = response.content;
        this.totalRecords = response.totalElements;
        this.loading = false;
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Search failed' });
        this.loading = false;
        console.error(err);
      }
    });
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.currentPage = 0;
    this.loadWarehouses();
  }

  confirmDelete(warehouse: WarehouseListDTO): void {
    this.confirmationService.confirm({
      message: `Are you sure you want to delete warehouse "${warehouse.name}"?`,
      header: 'Confirm Delete',
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.deleteWarehouse(warehouse.id);
      }
    });
  }

  deleteWarehouse(id: number): void {
    this.warehouseService.delete(id).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Warehouse deleted successfully' });
        this.loadWarehouses();
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to delete warehouse' });
        console.error(err);
      }
    });
  }

  formatDate(dateString: string): string {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }

  getStatusSeverity(online: boolean): 'success' | 'danger' {
    return online ? 'success' : 'danger';
  }

  getStatusLabel(online: boolean): string {
    return online ? 'Online' : 'Offline';
  }
}
