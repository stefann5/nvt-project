import { Component, OnInit, ViewChild, ElementRef, AfterViewInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { WarehouseService } from '../../../services/warehouse/warehouse.service';
import { WarehouseResponseDTO, SectorDTO } from '../../../dto/warehouse/WarehouseResponseDTO';
import { TemperatureStatisticsDTO } from '../../../dto/warehouse/TemperatureStatisticsDTO';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { MessageModule } from 'primeng/message';
import { DividerModule } from 'primeng/divider';
import { SelectModule } from 'primeng/select';
import { DatePickerModule } from 'primeng/datepicker';
import { ToastModule } from 'primeng/toast';
import { ChartModule } from 'primeng/chart';
import { TableModule } from 'primeng/table';
import { MessageService } from 'primeng/api';
import * as L from 'leaflet';

interface PeriodOption {
  label: string;
  value: string;
}

@Component({
  selector: 'app-warehouse-detail',
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
    SelectModule,
    DatePickerModule,
    ToastModule,
    ChartModule,
    TableModule
  ],
  providers: [MessageService],
  templateUrl: './warehouse-detail.html',
  styleUrl: './warehouse-detail.scss'
})
export class WarehouseDetailComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('mapContainer') mapContainer!: ElementRef;

  warehouseId!: number;
  warehouse: WarehouseResponseDTO | null = null;
  imageUrls: { id: number; originalName: string; url: string }[] = [];
  selectedSector: SectorDTO | null = null;
  temperatureStats: TemperatureStatisticsDTO | null = null;

  loading = false;
  statsLoading = false;

  periodOptions: PeriodOption[] = [
    { label: 'Last Week', value: 'week' },
    { label: 'Last Month', value: 'month' },
    { label: 'Last 3 Months', value: '3months' },
    { label: 'Last 6 Months', value: '6months' },
    { label: 'Last Year', value: 'year' },
    { label: 'Custom Range', value: 'custom' }
  ];

  selectedPeriod: PeriodOption = this.periodOptions[0];
  customStartDate: Date | null = null;
  customEndDate: Date | null = null;
  maxDate = new Date();
  minStartDate = new Date(new Date().setFullYear(new Date().getFullYear() - 1));

  chartData: any;
  chartOptions: any;

  private map: L.Map | null = null;
  private marker: L.Marker | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private warehouseService: WarehouseService,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.warehouseId = +id;
      this.loadWarehouse();
    }
    this.initChartOptions();
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.initMap(), 100);
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
    }
  }

  private initMap(): void {
    if (!this.mapContainer?.nativeElement) return;
    if (this.map) return;

    this.map = L.map(this.mapContainer.nativeElement).setView([44.8176, 20.4633], 10);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(this.map);

    if (this.warehouse) {
      this.updateMapMarker();
    }
  }

  private updateMapMarker(): void {
    if (!this.map || !this.warehouse) return;

    if (this.marker) {
      this.map.removeLayer(this.marker);
    }

    const icon = L.divIcon({
      className: 'warehouse-marker',
      html: `<div style="background-color: ${this.warehouse.online ? '#22c55e' : '#ef4444'};
             width: 24px; height: 24px; border-radius: 50%; border: 3px solid white;
             box-shadow: 0 2px 5px rgba(0,0,0,0.3);"></div>`,
      iconSize: [24, 24],
      iconAnchor: [12, 12]
    });

    this.marker = L.marker([this.warehouse.latitude, this.warehouse.longitude], { icon })
      .addTo(this.map)
      .bindPopup(`<b>${this.warehouse.name}</b><br>
                  Status: ${this.warehouse.online ? 'Online' : 'Offline'}<br>
                  ${this.warehouse.cityName}, ${this.warehouse.countryName}`);

    this.map.setView([this.warehouse.latitude, this.warehouse.longitude], 14);
  }

  private initChartOptions(): void {
    const documentStyle = getComputedStyle(document.documentElement);
    const textColor = documentStyle.getPropertyValue('--p-text-color') || '#ffffff';
    const surfaceBorder = documentStyle.getPropertyValue('--p-content-border-color') || '#374151';

    this.chartOptions = {
      maintainAspectRatio: false,
      plugins: {
        legend: {
          display: false
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
            text: 'Temperature (°C)',
            color: textColor
          }
        }
      }
    };
  }

  loadWarehouse(): void {
    this.loading = true;
    this.warehouseService.getById(this.warehouseId).subscribe({
      next: (data) => {
        this.warehouse = data;
        this.loading = false;
        this.loadImages();
        setTimeout(() => this.initMap(), 100);
        setTimeout(() => this.tryUpdateMapMarker(), 200);

        if (data.sectors && data.sectors.length > 0) {
          this.selectSector(data.sectors[0]);
        }
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to load warehouse' });
        this.loading = false;
        console.error(err);
      }
    });
  }

  loadImages(): void {
    this.warehouseService.getWarehouseImages(this.warehouseId).subscribe({
      next: (data) => {
        this.imageUrls = data.images;
      },
      error: (err) => console.error('Failed to load images', err)
    });
  }

  private tryUpdateMapMarker(retries = 5): void {
    if (this.map && this.warehouse) {
      this.updateMapMarker();
    } else if (retries > 0) {
      setTimeout(() => this.tryUpdateMapMarker(retries - 1), 200);
    }
  }

  selectSector(sector: SectorDTO): void {
    this.selectedSector = sector;
    this.loadTemperatureStats();
  }

  loadTemperatureStats(): void {
    if (!this.selectedSector) return;

    this.statsLoading = true;

    let request$;
    if (this.selectedPeriod.value === 'custom' && this.customStartDate && this.customEndDate) {
      const startStr = this.formatDate(this.customStartDate);
      const endStr = this.formatDate(this.customEndDate);
      request$ = this.warehouseService.getTemperatureStatsByDateRange(
        this.warehouseId, this.selectedSector.id, startStr, endStr
      );
    } else if (this.selectedPeriod.value !== 'custom') {
      request$ = this.warehouseService.getTemperatureStatsByPeriod(
        this.warehouseId, this.selectedSector.id, this.selectedPeriod.value
      );
    } else {
      this.statsLoading = false;
      return;
    }

    request$.subscribe({
      next: (data) => {
        this.temperatureStats = data;
        this.updateChart();
        this.statsLoading = false;
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to load temperature statistics' });
        this.statsLoading = false;
        console.error(err);
      }
    });
  }

  private updateChart(): void {
    if (!this.temperatureStats) return;

    const labels = this.temperatureStats.dataPoints.map(dp => dp.label);
    const data = this.temperatureStats.dataPoints.map(dp => dp.avgTemperature);

    this.chartData = {
      labels,
      datasets: [
        {
          label: 'Average Temperature (°C)',
          data,
          backgroundColor: 'rgba(59, 130, 246, 0.5)',
          borderColor: 'rgb(59, 130, 246)',
          borderWidth: 2,
          fill: true,
          tension: 0.4
        }
      ]
    };
  }

  onPeriodChange(): void {
    if (this.selectedPeriod.value !== 'custom') {
      this.loadTemperatureStats();
    }
  }

  applyCustomRange(): void {
    if (!this.customStartDate || !this.customEndDate) {
      this.messageService.add({ severity: 'warn', summary: 'Warning', detail: 'Please select both start and end dates' });
      return;
    }

    if (this.customEndDate < this.customStartDate) {
      this.messageService.add({ severity: 'error', summary: 'Error', detail: 'End date must be after start date' });
      return;
    }

    const daysDiff = Math.floor((this.customEndDate.getTime() - this.customStartDate.getTime()) / (1000 * 60 * 60 * 24));
    if (daysDiff > 365) {
      this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Date range cannot exceed one year' });
      return;
    }

    this.loadTemperatureStats();
  }

  private formatDate(date: Date): string {
    return date.toISOString().split('T')[0];
  }

  formatDateTime(dateString: string): string {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  getStatusSeverity(online: boolean): 'success' | 'danger' {
    return online ? 'success' : 'danger';
  }

  getStatusLabel(online: boolean): string {
    return online ? 'Online' : 'Offline';
  }

  getTemperatureColor(sector: SectorDTO): string {
    if (sector.currentTemperature === null || sector.currentTemperature === undefined) {
      return 'gray';
    }
    if (sector.currentTemperature < sector.minTemperature || sector.currentTemperature > sector.maxTemperature) {
      return 'red';
    }
    return 'green';
  }

  goBack(): void {
    this.router.navigate(['/app/manager/warehouses']);
  }

  editWarehouse(): void {
    this.router.navigate(['/app/manager/warehouses', this.warehouseId, 'edit']);
  }
}
