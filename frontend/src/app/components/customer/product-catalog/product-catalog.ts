import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductService, ProductSearchParams } from '../../../services/product/product.service';
import { ProductListDTO } from '../../../dto/product/ProductListDTO';
import { PageResponseDTO } from '../../../dto/common/PageResponseDTO';
import { CartService } from '../../../services/cart/cart.service';
import { CardModule } from 'primeng/card';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { MessageModule } from 'primeng/message';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { TooltipModule } from 'primeng/tooltip';
import { PaginatorModule } from 'primeng/paginator';
import { SelectModule } from 'primeng/select';
import { CheckboxModule } from 'primeng/checkbox';
import { TagModule } from 'primeng/tag';
import { DividerModule } from 'primeng/divider';

@Component({
  selector: 'app-product-catalog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    CardModule,
    TableModule,
    ButtonModule,
    InputTextModule,
    InputNumberModule,
    ProgressSpinnerModule,
    MessageModule,
    ToastModule,
    IconFieldModule,
    InputIconModule,
    TooltipModule,
    PaginatorModule,
    SelectModule,
    CheckboxModule,
    TagModule,
    DividerModule
  ],
  providers: [MessageService],
  templateUrl: './product-catalog.html',
  styleUrl: './product-catalog.scss'
})
export class ProductCatalogComponent implements OnInit {
  products: ProductListDTO[] = [];
  categories: string[] = [];
  loading = false;
  searchQuery = '';
  selectedCategory: string | null = null;
  minPrice: number | null = null;
  maxPrice: number | null = null;
  inStockOnly = false;
  private searchTimeout: any;

  currentPage = 0;
  pageSize = 20;
  totalRecords = 0;
  sortField = 'name';
  sortOrder = 'asc';

  quantities: { [productId: number]: number } = {};

  constructor(
    private productService: ProductService,
    private messageService: MessageService,
    private cartService: CartService
  ) {}

  ngOnInit(): void {
    this.loadCategories();
    this.loadProducts();
  }

  loadCategories(): void {
    this.productService.getCategories().subscribe({
      next: (categories) => {
        this.categories = categories;
      },
      error: (err) => {
        console.error('Failed to load categories', err);
      }
    });
  }

  loadProducts(): void {
    this.loading = true;
    const params: ProductSearchParams = {
      search: this.searchQuery || undefined,
      category: this.selectedCategory || undefined,
      minPrice: this.minPrice ?? undefined,
      maxPrice: this.maxPrice ?? undefined,
      inStock: this.inStockOnly || undefined,
      page: this.currentPage,
      size: this.pageSize,
      sortBy: this.sortField,
      sortDir: this.sortOrder
    };

    this.productService.search(params).subscribe({
      next: (response: PageResponseDTO<ProductListDTO>) => {
        this.products = response.content;
        this.totalRecords = response.totalElements;
        this.products.forEach(p => {
          if (!this.quantities[p.id]) {
            this.quantities[p.id] = 1;
          }
        });
        this.loading = false;
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Greska', detail: 'Neuspesno ucitavanje proizvoda' });
        this.loading = false;
        console.error(err);
      }
    });
  }

  onSearch(): void {
    if (this.searchTimeout) {
      clearTimeout(this.searchTimeout);
    }

    this.searchTimeout = setTimeout(() => {
      this.currentPage = 0;
      this.loadProducts();
    }, 300);
  }

  onFilterChange(): void {
    this.currentPage = 0;
    this.loadProducts();
  }

  onPageChange(event: any): void {
    this.currentPage = event.page;
    this.pageSize = event.rows;
    this.loadProducts();
  }

  clearFilters(): void {
    this.searchQuery = '';
    this.selectedCategory = null;
    this.minPrice = null;
    this.maxPrice = null;
    this.inStockOnly = false;
    this.currentPage = 0;
    this.loadProducts();
  }

  addProductToCart(product: ProductListDTO): void {
    const quantity = this.quantities[product.id] || 1;

    if (quantity > product.totalQuantity) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Upozorenje',
        detail: `Maksimalna dostupna kolicina je ${product.totalQuantity}`
      });
      return;
    }

    if (quantity <= 0) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Upozorenje',
        detail: 'Kolicina mora biti veca od 0'
      });
      return;
    }

    this.cartService.addToCart({ product, quantity });
    this.messageService.add({
      severity: 'success',
      summary: 'Dodato',
      detail: `${product.name} (${quantity} ${product.unit}) dodato u korpu`
    });
  }

  formatPrice(price: number): string {
    return price.toFixed(2) + ' EUR';
  }

  getStockSeverity(quantity: number): "success" | "info" | "warn" | "danger" | "secondary" | "contrast" {
    if (quantity > 100) return 'success';
    if (quantity > 20) return 'info';
    if (quantity > 0) return 'warn';
    return 'danger';
  }

  getStockLabel(quantity: number): string {
    if (quantity > 100) return 'Na stanju';
    if (quantity > 20) return 'Ograniceno';
    if (quantity > 0) return 'Malo na stanju';
    return 'Nema na stanju';
  }
}
