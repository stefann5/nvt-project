import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { VehicleService } from '../../../services/vehicle/vehicle.service';
import { VehicleResponseDTO } from '../../../dto/vehicle/VehicleResponseDTO';
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

@Component({
  selector: 'app-vehicle-list',
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
    InputIconModule
  ],
  providers: [ConfirmationService, MessageService],
  templateUrl: './vehicle-list.html',
  styleUrl: './vehicle-list.scss'
})
export class VehicleListComponent implements OnInit {
  vehicles: VehicleResponseDTO[] = [];
  loading = false;
  searchQuery = '';
  private searchTimeout: any;

  constructor(
    private vehicleService: VehicleService,
    private confirmationService: ConfirmationService,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    this.loadVehicles();
  }

  loadVehicles(): void {
    this.loading = true;
    this.vehicleService.getAll().subscribe({
      next: (data) => {
        this.vehicles = data;
        this.loading = false;
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to load vehicles' });
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
      if (this.searchQuery.trim()) {
        this.loading = true;
        this.vehicleService.search(this.searchQuery).subscribe({
          next: (data) => {
            this.vehicles = data;
            this.loading = false;
          },
          error: (err) => {
            this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Search failed' });
            this.loading = false;
            console.error(err);
          }
        });
      } else {
        this.loadVehicles();
      }
    }, 300);
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.loadVehicles();
  }

  confirmDelete(vehicle: VehicleResponseDTO): void {
    this.confirmationService.confirm({
      message: `Are you sure you want to delete vehicle "${vehicle.licensePlate}"?`,
      header: 'Confirm Delete',
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.deleteVehicle(vehicle.id);
      }
    });
  }

  deleteVehicle(id: number): void {
    this.vehicleService.delete(id).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Vehicle deleted successfully' });
        this.loadVehicles();
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to delete vehicle' });
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
}
