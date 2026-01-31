import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ProductService, ProductSearchParams } from '../../../services/product/product.service';
import { ProductListDTO } from '../../../dto/product/ProductListDTO';
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
import { SelectModule } from 'primeng/select';
import { InputNumberModule } from 'primeng/inputnumber';
import { CheckboxModule } from 'primeng/checkbox';

@Component({
  selector: 'app-product-list',
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
    TagModule,
    SelectModule,
    InputNumberModule,
    CheckboxModule
  ],
  providers: [ConfirmationService, MessageService],
  templateUrl: './product-list.html',
  styleUrl: './product-list.scss'
})
export class ProductListComponent implements OnInit {
  products: ProductListDTO[] = [];
  categories: string[] = [];
  loading = false;
  
  // Search/Filter
  searchQuery = '';
  selectedCategory: string | null = null;
  minPrice: number | null = null;
  maxPrice: number | null = null;
  forSaleFilter: boolean | null = null;
  activeFilter: boolean | null = null;
  
  private searchTimeout: any;

  // Pagination
  currentPage = 0;
  pageSize = 20;
  totalRecords = 0;
  sortField = 'id';
  sortOrder = 'desc';

  forSaleOptions = [
    { label: 'All', value: null },
    { label: 'For Sale', value: true },
    { label: 'Not For Sale', value: false }
  ];

  activeOptions = [
    { label: 'All', value: null },
    { label: 'Active', value: true },
    { label: 'Inactive', value: false }
  ];

  constructor(
    private productService: ProductService,
    private confirmationService: ConfirmationService,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    this.loadCategories();
    this.loadProducts();
  }

  loadCategories(): void {
    this.productService.getCategories().subscribe({
      next: (categories) => {
        this.categories = categories;
        console.log(categories);
      },
      error: (err) => {
        console.error('Failed to load categories', err);
      }
    });
  }

  loadProducts(): void {
    this.loading = true;
    this.productService.getAllPagedForManager(
      this.currentPage,
      this.pageSize,
      this.sortField,
      this.sortOrder
    ).subscribe({
      next: (response: PageResponseDTO<ProductListDTO>) => {
        this.products = response.content;
        this.totalRecords = response.totalElements;
        this.loading = false;
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to load products' });
        this.loading = false;
        console.error(err);
      }
    });
  }

  onPageChange(event: any): void {
    this.currentPage = event.page;
    this.pageSize = event.rows;
    this.performSearch();
  }

  onSearch(): void {
    if (this.searchTimeout) {
      clearTimeout(this.searchTimeout);
    }

    this.searchTimeout = setTimeout(() => {
      this.currentPage = 0;
      this.performSearch();
    }, 300);
  }

  onFilterChange(): void {
    this.currentPage = 0;
    this.performSearch();
  }

  private performSearch(): void {
    this.loading = true;

    const searchParams: ProductSearchParams = {
      search: this.searchQuery || undefined,
      category: this.selectedCategory || undefined,
      minPrice: this.minPrice ?? undefined,
      maxPrice: this.maxPrice ?? undefined,
      forSale: this.forSaleFilter ?? undefined,
      active: this.activeFilter ?? undefined,
      page: this.currentPage,
      size: this.pageSize,
      sortBy: this.sortField,
      sortDir: this.sortOrder
    };

    this.productService.search(searchParams).subscribe({
      next: (response: PageResponseDTO<ProductListDTO>) => {
        this.products = response.content;
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

  clearFilters(): void {
    this.searchQuery = '';
    this.selectedCategory = null;
    this.minPrice = null;
    this.maxPrice = null;
    this.forSaleFilter = null;
    this.activeFilter = null;
    this.currentPage = 0;
    this.loadProducts();
  }

  confirmDelete(product: ProductListDTO): void {
    this.confirmationService.confirm({
      message: `Are you sure you want to delete product "${product.name}"?`,
      header: 'Confirm Delete',
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.deleteProduct(product.id);
      }
    });
  }

  deleteProduct(id: number): void {
    this.productService.delete(id).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Product deleted successfully' });
        this.performSearch();
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to delete product' });
        console.error(err);
      }
    });
  }

  toggleForSale(product: ProductListDTO): void {
    this.productService.toggleForSale(product.id).subscribe({
      next: (updated) => {
        product.forSale = updated.forSale;
        this.messageService.add({
          severity: 'success',
          summary: 'Success',
          detail: `Product is now ${updated.forSale ? 'for sale' : 'not for sale'}`
        });
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to update product' });
        console.error(err);
      }
    });
  }

  toggleActive(product: ProductListDTO): void {
    this.productService.toggleActive(product.id).subscribe({
      next: (updated) => {
        product.active = updated.active;
        this.messageService.add({
          severity: 'success',
          summary: 'Success',
          detail: `Product is now ${updated.active ? 'active' : 'inactive'}`
        });
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to update product' });
        console.error(err);
      }
    });
  }

  formatPrice(price: number): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'EUR'
    }).format(price);
  }

  getForSaleSeverity(forSale: boolean): 'success' | 'warn' {
    return forSale ? 'success' : 'warn';
  }

  getActiveSeverity(active: boolean): 'success' | 'danger' {
    return active ? 'success' : 'danger';
  }

  getStockSeverity(quantity: number): 'success' | 'warn' | 'danger' {
    if (quantity > 50) return 'success';
    if (quantity > 0) return 'warn';
    return 'danger';
  }
}
