import { Component, OnInit, ViewChild, ElementRef, AfterViewInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FactoryService } from '../../../services/factory/factory.service';
import { FactoryResponseDTO, FactoryProductDTO } from '../../../dto/factory/FactoryDTO';
import { FactoryProductionStatisticsDTO, TimePeriod } from '../../../dto/factory/FactoryStatisticsDTO';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { MessageModule } from 'primeng/message';
import { DividerModule } from 'primeng/divider';
import { ToastModule } from 'primeng/toast';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { TabsModule } from 'primeng/tabs';
import { SelectModule } from 'primeng/select';
import { DatePickerModule } from 'primeng/datepicker';
import { ChartModule } from 'primeng/chart';
import { MessageService, ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import * as L from 'leaflet';

interface PeriodOption {
  label: string;
  value: TimePeriod;
}

@Component({
  selector: 'app-factory-detail',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
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
    TabsModule,
    SelectModule,
    DatePickerModule,
    ChartModule,
    ConfirmDialogModule
  ],
  providers: [MessageService, ConfirmationService],
  templateUrl: './factory-detail.html',
  styleUrl: './factory-detail.scss'
})
export class FactoryDetailComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('mapContainer') mapContainer!: ElementRef;

  factoryId!: number;
  factory: FactoryResponseDTO | null = null;
  imageUrls: { id: number; originalName: string; url: string }[] = [];
  loading = false;

  // Production Statistics
  selectedProduct: FactoryProductDTO | null = null;
  productionStats: FactoryProductionStatisticsDTO | null = null;
  productionLoading = false;
  productionChartData: any;
  productionChartOptions: any;

  // Period Options
  periodOptions: PeriodOption[] = [
    { label: 'Last Week', value: 'week' },
    { label: 'Last Month', value: 'month' },
    { label: 'Last 3 Months', value: '3months' },
    { label: 'Last 6 Months', value: '6months' },
    { label: 'Last Year', value: 'year' },
    { label: 'Custom Range', value: 'custom' }
  ];

  selectedProductionPeriod: PeriodOption = this.periodOptions[0];
  productionCustomStartDate: Date | null = null;
  productionCustomEndDate: Date | null = null;

  maxDate = new Date();
  minStartDate = new Date(new Date().setFullYear(new Date().getFullYear() - 1));

  private map: L.Map | null = null;
  private marker: L.Marker | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private factoryService: FactoryService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.factoryId = +id;
      this.loadFactory();
    }
    this.initProductionChartOptions();
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.initMap(), 100);
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
    }
  }

  loadFactory(): void {
    this.loading = true;
    this.factoryService.getById(this.factoryId).subscribe({
      next: (factory) => {
        this.factory = factory;
        this.loading = false;
        this.loadImages();
        setTimeout(() => this.updateMapMarker(), 100);
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to load factory' });
        this.loading = false;
        console.error(err);
      }
    });
  }

  loadImages(): void {
    this.factoryService.getFactoryImages(this.factoryId).subscribe({
      next: (data) => {
        this.imageUrls = data.images;
      },
      error: (err) => {
        console.error('Failed to load images', err);
      }
    });
  }

  private initMap(): void {
    if (!this.mapContainer?.nativeElement) return;

    const defaultLat = 45.2671;
    const defaultLng = 19.8335;

    this.map = L.map(this.mapContainer.nativeElement, {
      center: [defaultLat, defaultLng],
      zoom: 13,
      zoomControl: true
    });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap contributors'
    }).addTo(this.map);

    if (this.factory) {
      this.updateMapMarker();
    }
  }

  private updateMapMarker(): void {
    if (!this.map || !this.factory) return;

    const lat = this.factory.latitude;
    const lng = this.factory.longitude;

    if (this.marker) {
      this.marker.remove();
    }

    const markerColor = this.factory.online ? '#22c55e' : '#ef4444';
    const icon = L.divIcon({
      className: 'custom-marker',
      html: `<div style="background-color: ${markerColor}; width: 24px; height: 24px; border-radius: 50%; border: 3px solid white; box-shadow: 0 2px 5px rgba(0,0,0,0.3);"></div>`,
      iconSize: [24, 24],
      iconAnchor: [12, 12]
    });

    this.marker = L.marker([lat, lng], { icon })
      .addTo(this.map)
      .bindPopup(`<b>${this.factory.name}</b><br>Status: ${this.factory.online ? 'Online' : 'Offline'}`);

    this.map.setView([lat, lng], 14);
  }

  getStatusSeverity(online: boolean): 'success' | 'danger' {
    return online ? 'success' : 'danger';
  }

  getStatusLabel(online: boolean): string {
    return online ? 'Online' : 'Offline';
  }

  formatDate(dateString: string | undefined): string {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  confirmDelete(): void {
    this.confirmationService.confirm({
      message: `Are you sure you want to delete factory "${this.factory?.name}"?`,
      header: 'Confirm Delete',
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.deleteFactory();
      }
    });
  }

  deleteFactory(): void {
    this.factoryService.delete(this.factoryId).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Factory deleted successfully' });
        setTimeout(() => this.router.navigate(['/app/manager/factories']), 1000);
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to delete factory' });
        console.error(err);
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/app/manager/factories']);
  }

  editFactory(): void {
    this.router.navigate(['/app/manager/factories', this.factoryId, 'edit']);
  }

  // Chart Initialization
  private initProductionChartOptions(): void {
    const documentStyle = getComputedStyle(document.documentElement);
    const textColor = documentStyle.getPropertyValue('--p-text-color') || '#ffffff';
    const surfaceBorder = documentStyle.getPropertyValue('--p-content-border-color') || '#374151';

    this.productionChartOptions = {
      maintainAspectRatio: false,
      plugins: {
        legend: {
          display: false
        },
        tooltip: {
          callbacks: {
            label: (context: any) => `${context.raw} units`
          }
        }
      },
      scales: {
        x: {
          ticks: { color: textColor },
          grid: { color: surfaceBorder }
        },
        y: {
          ticks: { color: textColor },
          grid: { color: surfaceBorder },
          title: {
            display: true,
            text: 'Units Produced',
            color: textColor
          },
          beginAtZero: true
        }
      }
    };
  }

  // Product Selection
  onProductSelect(product: FactoryProductDTO): void {
    this.selectedProduct = product;
    this.loadProductionStats();
  }

  // Production Statistics
  onProductionPeriodChange(): void {
    if (this.selectedProductionPeriod.value !== 'custom') {
      this.loadProductionStats();
    }
  }

  applyProductionCustomRange(): void {
    if (this.productionCustomStartDate && this.productionCustomEndDate) {
      this.loadProductionStats();
    } else {
      this.messageService.add({ severity: 'warn', summary: 'Warning', detail: 'Please select both start and end dates' });
    }
  }

  loadProductionStats(): void {
    if (!this.selectedProduct) return;

    this.productionLoading = true;
    
    const period = this.selectedProductionPeriod.value;
    let startDate: string | undefined;
    let endDate: string | undefined;
    
    if (period === 'custom' && this.productionCustomStartDate && this.productionCustomEndDate) {
      startDate = this.formatDateForApi(this.productionCustomStartDate);
      endDate = this.formatDateForApi(this.productionCustomEndDate);
    }

    this.factoryService.getProductionStatistics(
      this.factoryId, 
      this.selectedProduct.id, 
      period, 
      startDate, 
      endDate
    ).subscribe({
      next: (stats) => {
        this.productionStats = stats;
        this.updateProductionChart();
        this.productionLoading = false;
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to load production statistics' });
        this.productionLoading = false;
        console.error(err);
      }
    });
  }

  private updateProductionChart(): void {
    if (!this.productionStats || this.productionStats.dataPoints.length === 0) {
      this.productionChartData = null;
      return;
    }

    const documentStyle = getComputedStyle(document.documentElement);
    const primaryColor = documentStyle.getPropertyValue('--p-primary-color') || '#818cf8';

    this.productionChartData = {
      labels: this.productionStats.dataPoints.map(dp => dp.label),
      datasets: [
        {
          label: 'Units Produced',
          data: this.productionStats.dataPoints.map(dp => dp.quantity),
          backgroundColor: primaryColor,
          borderColor: primaryColor,
          borderWidth: 2,
          fill: false,
          tension: 0.4
        }
      ]
    };
  }

  private formatDateForApi(date: Date): string {
    return date.toISOString().split('T')[0];
  }
}
