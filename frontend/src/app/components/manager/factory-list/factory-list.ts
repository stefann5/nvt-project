import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { FactoryService } from '../../../services/factory/factory.service';
import { FactoryListDTO } from '../../../dto/factory/FactoryDTO';
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
  selector: 'app-factory-list',
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
  templateUrl: './factory-list.html',
  styleUrl: './factory-list.scss'
})
export class FactoryListComponent implements OnInit {
  factories: FactoryListDTO[] = [];
  loading = false;
  searchQuery = '';
  private searchTimeout: any;

  currentPage = 0;
  pageSize = 20;
  totalRecords = 0;
  sortField = 'id';
  sortOrder = 'asc';

  constructor(
    private factoryService: FactoryService,
    private confirmationService: ConfirmationService,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    this.loadFactories();
  }

  loadFactories(): void {
    this.loading = true;
    this.factoryService.getAllPaged(this.currentPage, this.pageSize, this.sortField, this.sortOrder).subscribe({
      next: (response: PageResponseDTO<FactoryListDTO>) => {
        this.factories = response.content;
        this.totalRecords = response.totalElements;
        this.loading = false;
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to load factories' });
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
      this.loadFactories();
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
        this.loadFactories();
      }
    }, 300);
  }

  private performSearch(): void {
    this.loading = true;
    this.factoryService.searchPaged(this.searchQuery, this.currentPage, this.pageSize).subscribe({
      next: (response: PageResponseDTO<FactoryListDTO>) => {
        this.factories = response.content;
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
    this.loadFactories();
  }

  confirmDelete(factory: FactoryListDTO): void {
    this.confirmationService.confirm({
      message: `Are you sure you want to delete factory "${factory.name}"?`,
      header: 'Confirm Delete',
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.deleteFactory(factory.id);
      }
    });
  }

  deleteFactory(id: number): void {
    this.factoryService.delete(id).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Factory deleted successfully' });
        this.loadFactories();
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to delete factory' });
        console.error(err);
      }
    });
  }

  formatDate(dateString: string | undefined): string {
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
