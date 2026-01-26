import { Component, OnInit, ViewChild, ElementRef, AfterViewInit, OnDestroy, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { WarehouseService } from '../../../services/warehouse/warehouse.service';
import { WarehouseResponseDTO, SectorDTO } from '../../../dto/warehouse/WarehouseResponseDTO';
import { TemperatureStatisticsDTO } from '../../../dto/warehouse/TemperatureStatisticsDTO';
import { WarehouseAvailabilityStatisticsDTO } from '../../../dto/warehouse/WarehouseAvailabilityStatisticsDTO';
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
import { TooltipModule } from 'primeng/tooltip';
import { TabsModule } from 'primeng/tabs';
import { MessageService } from 'primeng/api';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { environment } from '../../../../environments/environment';
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
    TableModule,
    TooltipModule,
    TabsModule
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
  availabilityStats: WarehouseAvailabilityStatisticsDTO | null = null;

  loading = false;
  statsLoading = false;
  availabilityLoading = false;

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

  availabilityPeriodOptions: PeriodOption[] = [
    { label: 'Last Hour', value: '1h' },
    { label: 'Last 3 Hours (Real-time)', value: '3h' },
    { label: 'Last 12 Hours', value: '12h' },
    { label: 'Last 24 Hours', value: '24h' },
    { label: 'Last 7 Days', value: '7d' },
    { label: 'Last 30 Days', value: '30d' },
    { label: 'Last 3 Months', value: '3months' },
    { label: 'Last Year', value: 'year' },
    { label: 'Custom Range', value: 'custom' }
  ];

  selectedAvailabilityPeriod: PeriodOption = this.availabilityPeriodOptions[2];
  availabilityCustomStartDate: Date | null = null;
  availabilityCustomEndDate: Date | null = null;

  chartData: any;
  chartOptions: any;
  availabilityChartData: any;
  availabilityChartOptions: any;

  private map: L.Map | null = null;
  private marker: L.Marker | null = null;
  private stompClient: Client | null = null;
  isRealTimeMode = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private warehouseService: WarehouseService,
    private messageService: MessageService,
    private ngZone: NgZone
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.warehouseId = +id;
      this.loadWarehouse();
      this.loadAvailabilityStats();
      this.connectWebSocket(); // Connect WebSocket to receive real-time status updates
    }
    this.initChartOptions();
    this.initAvailabilityChartOptions();
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.initMap(), 100);
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
    }
    this.disconnectWebSocket();
  }

  private connectWebSocket(): void {
    if (this.stompClient?.connected) return;

    const wsUrl = environment.apiUrl.replace('/api/v1/', '/ws');

    this.stompClient = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      reconnectDelay: 5000,
      debug: (str) => console.log('STOMP:', str)
    });

    this.stompClient.onConnect = () => {
      console.log('WebSocket connected for warehouse availability');

      this.stompClient?.publish({
        destination: `/app/warehouse/${this.warehouseId}/availability/subscribe`
      });

      this.stompClient?.subscribe(
        `/topic/warehouse/${this.warehouseId}/availability`,
        (message: IMessage) => {
          this.ngZone.run(() => {
            const stats = JSON.parse(message.body) as WarehouseAvailabilityStatisticsDTO;
            this.availabilityStats = stats;
            this.updateAvailabilityChart();
            console.log('Received real-time warehouse availability update');
          });
        }
      );

      // Subscribe to heartbeat updates for online status
      this.stompClient?.subscribe(
        `/topic/warehouse/${this.warehouseId}/heartbeat`,
        (message: IMessage) => {
          this.ngZone.run(() => {
            const heartbeat = JSON.parse(message.body);
            if (this.warehouse && heartbeat.status === 'ONLINE') {
              this.warehouse.online = true;
              this.warehouse.lastHeartbeat = heartbeat.timestamp;
              this.updateMapMarker();
              console.log('Warehouse status updated to ONLINE via heartbeat');
            }
          });
        }
      );

      // Subscribe to status changes (for offline notifications)
      this.stompClient?.subscribe(
        `/topic/warehouse/${this.warehouseId}/status`,
        (message: IMessage) => {
          this.ngZone.run(() => {
            const status = JSON.parse(message.body);
            if (this.warehouse) {
              this.warehouse.online = status.online;
              this.updateMapMarker();
              console.log(`Warehouse status updated to ${status.online ? 'ONLINE' : 'OFFLINE'}`);
            }
          });
        }
      );
    };

    this.stompClient.onStompError = (frame) => {
      console.error('STOMP error:', frame.headers['message'], frame.body);
    };

    this.stompClient.onWebSocketError = (event) => {
      console.error('WebSocket error:', event);
    };

    this.stompClient.activate();
    this.isRealTimeMode = true;
  }

  private disconnectWebSocket(): void {
    if (this.stompClient?.connected) {
      this.stompClient.publish({
        destination: `/app/warehouse/${this.warehouseId}/availability/unsubscribe`
      });
      this.stompClient.deactivate();
    }
    this.isRealTimeMode = false;
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

  private initAvailabilityChartOptions(): void {
    const documentStyle = getComputedStyle(document.documentElement);
    const textColor = documentStyle.getPropertyValue('--p-text-color') || '#ffffff';
    const surfaceBorder = documentStyle.getPropertyValue('--p-content-border-color') || '#374151';

    this.availabilityChartOptions = {
      maintainAspectRatio: false,
      plugins: {
        legend: {
          display: true,
          position: 'top',
          labels: { color: textColor }
        }
      },
      scales: {
        x: {
          stacked: true,
          ticks: { color: textColor },
          grid: { color: surfaceBorder }
        },
        y: {
          stacked: true,
          min: 0,
          max: 100,
          ticks: {
            color: textColor,
            callback: (value: number) => value + '%'
          },
          grid: { color: surfaceBorder },
          title: {
            display: true,
            text: 'Availability (%)',
            color: textColor
          }
        }
      }
    };
  }

  loadAvailabilityStats(): void {
    this.availabilityLoading = true;

    let request$;
    if (this.selectedAvailabilityPeriod.value === 'custom' && this.availabilityCustomStartDate && this.availabilityCustomEndDate) {
      const startStr = this.availabilityCustomStartDate.toISOString();
      const endStr = this.availabilityCustomEndDate.toISOString();
      request$ = this.warehouseService.getAvailabilityStatsByDateRange(this.warehouseId, startStr, endStr);
    } else if (this.selectedAvailabilityPeriod.value !== 'custom') {
      request$ = this.warehouseService.getAvailabilityStatsByPeriod(this.warehouseId, this.selectedAvailabilityPeriod.value);
    } else {
      this.availabilityLoading = false;
      return;
    }

    request$.subscribe({
      next: (data) => {
        this.availabilityStats = data;
        this.updateAvailabilityChart();
        this.availabilityLoading = false;
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to load availability statistics' });
        this.availabilityLoading = false;
        console.error(err);
      }
    });
  }

  private updateAvailabilityChart(): void {
    if (!this.availabilityStats) return;

    const labels = this.availabilityStats.dataPoints.map(dp => dp.label);
    const onlineData = this.availabilityStats.dataPoints.map(dp => dp.onlinePercentage);
    const offlineData = this.availabilityStats.dataPoints.map(dp => dp.offlinePercentage);

    this.availabilityChartData = {
      labels,
      datasets: [
        {
          label: 'Online',
          data: onlineData,
          backgroundColor: 'rgba(34, 197, 94, 0.7)',
          borderColor: 'rgb(34, 197, 94)',
          borderWidth: 1
        },
        {
          label: 'Offline',
          data: offlineData,
          backgroundColor: 'rgba(239, 68, 68, 0.7)',
          borderColor: 'rgb(239, 68, 68)',
          borderWidth: 1
        }
      ]
    };
  }

  onAvailabilityPeriodChange(): void {
    if (this.selectedAvailabilityPeriod.value === '3h') {
      this.connectWebSocket();
    } else {
      this.disconnectWebSocket();
    }

    if (this.selectedAvailabilityPeriod.value !== 'custom') {
      this.loadAvailabilityStats();
    }
  }

  applyAvailabilityCustomRange(): void {
    if (!this.availabilityCustomStartDate || !this.availabilityCustomEndDate) {
      this.messageService.add({ severity: 'warn', summary: 'Warning', detail: 'Please select both start and end dates' });
      return;
    }

    if (this.availabilityCustomEndDate < this.availabilityCustomStartDate) {
      this.messageService.add({ severity: 'error', summary: 'Error', detail: 'End date must be after start date' });
      return;
    }

    const daysDiff = Math.floor((this.availabilityCustomEndDate.getTime() - this.availabilityCustomStartDate.getTime()) / (1000 * 60 * 60 * 24));
    if (daysDiff > 365) {
      this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Date range cannot exceed one year' });
      return;
    }

    this.loadAvailabilityStats();
  }

  formatDuration(seconds: number): string {
    if (seconds < 60) return `${seconds}s`;
    if (seconds < 3600) return `${Math.floor(seconds / 60)}m ${seconds % 60}s`;
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    if (hours < 24) return `${hours}h ${minutes}m`;
    const days = Math.floor(hours / 24);
    const remainingHours = hours % 24;
    return `${days}d ${remainingHours}h`;
  }
}
