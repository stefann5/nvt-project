import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { FactoryService } from '../../../services/factory/factory.service';
import { FactoryListDTO } from '../../../dto/factory/FactoryDTO';
import { PageResponseDTO } from '../../../dto/common/PageResponseDTO';
import { CountryDTO } from '../../../dto/company/CountryDTO';
import { CityDTO } from '../../../dto/company/CityDTO';
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

interface StatusOption {
  label: string;
  value: boolean;
}

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
    TagModule,
    SelectModule
  ],
  providers: [ConfirmationService, MessageService],
  templateUrl: './factory-list.html',
  styleUrl: './factory-list.scss'
})
export class FactoryListComponent implements OnInit {
  factories: FactoryListDTO[] = [];
  loading = false;
  
  // Filters
  filterName = '';
  selectedCountry: CountryDTO | null = null;
  selectedCity: CityDTO | null = null;
  selectedStatus: StatusOption | null = null;
  
  countries: CountryDTO[] = [];
  cities: CityDTO[] = [];
  statusOptions: StatusOption[] = [
    { label: 'Online', value: true },
    { label: 'Offline', value: false }
  ];

  private filterTimeout: any;

  currentPage = 0;
  pageSize = 20;
  totalRecords = 0;

  constructor(
    private factoryService: FactoryService,
    private confirmationService: ConfirmationService,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    this.loadCountries();
    this.loadFactories();
  }

  loadCountries(): void {
    this.factoryService.getAllCountries().subscribe({
      next: (countries) => {
        this.countries = countries;
      },
      error: (err) => {
        console.error('Failed to load countries', err);
      }
    });
  }

  onCountryChange(event: any): void {
    this.selectedCity = null;
    this.cities = [];
    
    if (this.selectedCountry) {
      this.factoryService.getCitiesByCountry(this.selectedCountry.id).subscribe({
        next: (cities) => {
          this.cities = cities;
        },
        error: (err) => {
          console.error('Failed to load cities', err);
        }
      });
    }
    
    this.onFilterChange();
  }

  onFilterChange(): void {
    if (this.filterTimeout) {
      clearTimeout(this.filterTimeout);
    }

    this.filterTimeout = setTimeout(() => {
      this.currentPage = 0;
      this.loadFactories();
    }, 300);
  }

  loadFactories(): void {
    this.loading = true;
    
    const name = this.filterName.trim() || undefined;
    const countryId = this.selectedCountry?.id;
    const cityId = this.selectedCity?.id;
    const online = this.selectedStatus?.value;

    // Use filtered endpoint if any filter is active
    if (name || countryId || cityId || online !== undefined) {
      this.factoryService.searchFiltered(
        this.currentPage, 
        this.pageSize, 
        name, 
        countryId, 
        cityId, 
        online
      ).subscribe({
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
    } else {
      // No filters - use regular paged endpoint
      this.factoryService.getAllPaged(this.currentPage, this.pageSize).subscribe({
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
  }

  onPageChange(event: any): void {
    this.currentPage = event.page;
    this.pageSize = event.rows;
    this.loadFactories();
  }

  hasActiveFilters(): boolean {
    return !!(this.filterName.trim() || this.selectedCountry || this.selectedCity || this.selectedStatus);
  }

  clearAllFilters(): void {
    this.filterName = '';
    this.selectedCountry = null;
    this.selectedCity = null;
    this.selectedStatus = null;
    this.cities = [];
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
    if (!dateString) return 'Never';
    return new Date(dateString).toLocaleDateString('en-US', {
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
}
