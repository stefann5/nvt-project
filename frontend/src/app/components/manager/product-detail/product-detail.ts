import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ProductService } from '../../../services/product/product.service';
import { ProductResponseDTO } from '../../../dto/product/ProductResponseDTO';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { MessageModule } from 'primeng/message';
import { DividerModule } from 'primeng/divider';
import { ToastModule } from 'primeng/toast';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MessageService, ConfirmationService } from 'primeng/api';
import { GalleriaModule } from 'primeng/galleria';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    CardModule,
    ButtonModule,
    TagModule,
    ProgressSpinnerModule,
    MessageModule,
    DividerModule,
    ToastModule,
    TableModule,
    TooltipModule,
    ConfirmDialogModule,
    GalleriaModule
  ],
  providers: [MessageService, ConfirmationService],
  templateUrl: './product-detail.html',
  styleUrl: './product-detail.scss'
})
export class ProductDetailComponent implements OnInit {
  productId!: number;
  product: ProductResponseDTO | null = null;
  imageUrls: { id: number; originalName: string; url: string }[] = [];
  
  loading = false;
  imagesLoading = false;
  
  // Galleria settings
  activeIndex = 0;
  displayGalleria = false;
  responsiveOptions = [
    {
      breakpoint: '1024px',
      numVisible: 5
    },
    {
      breakpoint: '768px',
      numVisible: 3
    },
    {
      breakpoint: '560px',
      numVisible: 1
    }
  ];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private productService: ProductService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.productId = +params['id'];
      this.loadProduct();
    });
  }

  loadProduct(): void {
    this.loading = true;
    this.productService.getById(this.productId).subscribe({
      next: (product) => {
        this.product = product;
        this.loading = false;
        this.loadImages();
      },
      error: (err) => {
        console.error('Error loading product:', err);
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Failed to load product details'
        });
        this.loading = false;
      }
    });
  }

  loadImages(): void {
    if (!this.product?.images || this.product.images.length === 0) {
      this.imageUrls = [];
      return;
    }

    this.imagesLoading = true;
    this.imageUrls = [];

    let loadedCount = 0;
    const totalImages = this.product.images.length;

    this.product.images.forEach(image => {
      this.productService.getImageUrl(image.minioPath).subscribe({
        next: (url) => {
          this.imageUrls.push({
            id: image.id,
            originalName: image.originalName,
            url: url
          });

          loadedCount++;
          if (loadedCount === totalImages) {
            this.imagesLoading = false;
          }
        },
        error: (err) => {
          console.error('Error loading image:', err);
          loadedCount++;
          if (loadedCount === totalImages) {
            this.imagesLoading = false;
          }
        }
      });
    });
  }

  goBack(): void {
    this.router.navigate(['/app/manager/products']);
  }

  editProduct(): void {
    this.router.navigate(['/app/manager/products', this.productId, 'edit']);
  }

  confirmDelete(): void {
    this.confirmationService.confirm({
      message: `Are you sure you want to delete the product "${this.product?.name}"?`,
      header: 'Confirm Delete',
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.deleteProduct();
      }
    });
  }

  deleteProduct(): void {
    this.productService.delete(this.productId).subscribe({
      next: () => {
        this.messageService.add({
          severity: 'success',
          summary: 'Success',
          detail: 'Product deleted successfully'
        });
        setTimeout(() => {
          this.router.navigate(['/app/manager/products']);
        }, 1500);
      },
      error: (err) => {
        console.error('Error deleting product:', err);
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Failed to delete product'
        });
      }
    });
  }

  toggleForSale(): void {
    if (!this.product) return;
    
    this.productService.toggleForSale(this.productId).subscribe({
      next: (response) => {
        if (this.product) {
          this.product.forSale = response.forSale;
        }
        this.messageService.add({
          severity: 'success',
          summary: 'Success',
          detail: `Product is now ${response.forSale ? 'for sale' : 'not for sale'}`
        });
      },
      error: (err) => {
        console.error('Error toggling forSale:', err);
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Failed to update product status'
        });
      }
    });
  }

  toggleActive(): void {
    if (!this.product) return;
    
    this.productService.toggleActive(this.productId).subscribe({
      next: (response) => {
        if (this.product) {
          this.product.active = response.active;
        }
        this.messageService.add({
          severity: 'success',
          summary: 'Success',
          detail: `Product is now ${response.active ? 'active' : 'inactive'}`
        });
      },
      error: (err) => {
        console.error('Error toggling active:', err);
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Failed to update product status'
        });
      }
    });
  }

  getForSaleSeverity(forSale: boolean): 'success' | 'danger' {
    return forSale ? 'success' : 'danger';
  }

  getForSaleLabel(forSale: boolean): string {
    return forSale ? 'For Sale' : 'Not For Sale';
  }

  getActiveSeverity(active: boolean): 'success' | 'warn' {
    return active ? 'success' : 'warn';
  }

  getActiveLabel(active: boolean): string {
    return active ? 'Active' : 'Inactive';
  }

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('sr-RS', {
      style: 'currency',
      currency: 'RSD',
      minimumFractionDigits: 2
    }).format(value);
  }

  formatWeight(weight: number): string {
    if (weight >= 1000) {
      return `${(weight / 1000).toFixed(2)} kg`;
    }
    return `${weight} g`;
  }

  formatDateTime(dateString: string | null | undefined): string {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleString('sr-RS', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  openGalleria(index: number): void {
    this.activeIndex = index;
    this.displayGalleria = true;
  }
}
