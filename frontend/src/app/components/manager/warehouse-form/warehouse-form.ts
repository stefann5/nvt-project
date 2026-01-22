import { Component, OnInit, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators, FormArray } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { WarehouseService } from '../../../services/warehouse/warehouse.service';
import { CountryDTO } from '../../../dto/company/CountryDTO';
import { CityDTO } from '../../../dto/company/CityDTO';
import { WarehouseResponseDTO } from '../../../dto/warehouse/WarehouseResponseDTO';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { SelectModule } from 'primeng/select';
import { FileUploadModule } from 'primeng/fileupload';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { MessageModule } from 'primeng/message';
import { ToastModule } from 'primeng/toast';
import { DividerModule } from 'primeng/divider';
import { MessageService } from 'primeng/api';
import * as L from 'leaflet';
import { MapComponent } from '../../map/map.component';
import { AddressDTO } from '../../../dto/auth/AddressDTO';

interface ImageWithUrl {
  id: number;
  originalName: string;
  url: string;
}

@Component({
  selector: 'app-warehouse-form',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule,
    CardModule,
    ButtonModule,
    InputTextModule,
    InputNumberModule,
    SelectModule,
    FileUploadModule,
    ProgressSpinnerModule,
    MessageModule,
    ToastModule,
    DividerModule,
    MapComponent
  ],
  providers: [MessageService],
  templateUrl: './warehouse-form.html',
  styleUrl: './warehouse-form.scss'
})
export class WarehouseFormComponent implements OnInit {
  @ViewChild('mapContainer') mapContainer!: ElementRef;

  warehouseForm!: FormGroup;
  isEditMode = false;
  warehouseId: number | null = null;
  loading = false;
  submitting = false;

  countries: CountryDTO[] = [];
  cities: CityDTO[] = [];
  selectedCountry: CountryDTO | null = null;

  existingImages: ImageWithUrl[] = [];
  imagesToDelete: number[] = [];
  newImages: File[] = [];

  selectedLatitude: number | null = null;
  selectedLongitude: number | null = null;
  selectedStreet: string = '';
  
  isSubmitting = false;
  locationSelected = false;

  private map: L.Map | null = null;
  private marker: L.Marker | null = null;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private warehouseService: WarehouseService,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    this.initForm();

    const id = this.route.snapshot.paramMap.get('id');
    if (id && id !== 'new') {
      this.isEditMode = true;
      this.warehouseId = +id;
    }

    this.loadCountries();
  }

onAddressSelected(address: AddressDTO): void {
    this.selectedLatitude = address.latitude ?? null;
    this.selectedLongitude = address.longitude ?? null;
    this.selectedStreet = address.street ?? '';
    this.locationSelected = true;
    
    // Update street and street number in the form
    this.warehouseForm.patchValue({
      street: this.selectedStreet,
      streetNumber: address.number ?? ''
    });

    this.messageService.add({
      severity: 'success',
      summary: 'Location Selected',
      detail: `Address: ${this.selectedStreet || 'Location marked on map'}`
    });
  }

  initForm(): void {
    this.warehouseForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      country: [null, Validators.required],
      city: [null, Validators.required],
      street: ['', Validators.required],
      streetNumber: [''],
      totalCapacity: [null, [Validators.required, Validators.min(1)]],
      sectors: this.fb.array([])
    });
  }

  get sectorsArray(): FormArray {
    return this.warehouseForm.get('sectors') as FormArray;
  }

  addSector(): void {
    const sectorGroup = this.fb.group({
      id: [null],
      name: ['', Validators.required],
      minTemperature: [null, Validators.required],
      maxTemperature: [null, Validators.required],
      capacity: [null, [Validators.required, Validators.min(0)]]
    });
    this.sectorsArray.push(sectorGroup);
  }

  removeSector(index: number): void {
    this.sectorsArray.removeAt(index);
  }

  loadCountries(): void {
    this.loading = true;
    this.warehouseService.getAllCountries().subscribe({
      next: (countries) => {
        this.countries = countries;
        if (this.isEditMode && this.warehouseId) {
          this.loadWarehouse(this.warehouseId);
        } else {
          this.loading = false;
        }
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to load countries' });
        this.loading = false;
        console.error(err);
      }
    });
  }

  onCountryChange(event: any): void {
    this.selectedCountry = event.value;
    this.warehouseForm.patchValue({ city: null });
    this.cities = [];

    if (this.selectedCountry) {
      this.warehouseService.getCitiesByCountry(this.selectedCountry.id).subscribe({
        next: (cities) => {
          this.cities = cities;
        },
        error: (err) => {
          this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to load cities' });
          console.error(err);
        }
      });
    }
  }

  loadWarehouse(id: number): void {
    this.warehouseService.getById(id).subscribe({
      next: (warehouse) => {
        const country = this.countries.find(c => c.id === warehouse.countryId);
        if (country) {
          this.selectedCountry = country;
          this.warehouseService.getCitiesByCountry(country.id).subscribe({
            next: (cities) => {
              this.cities = cities;
              const city = cities.find(c => c.id === warehouse.cityId);

              this.warehouseForm.patchValue({
                name: warehouse.name,
                country: country,
                city: city,
                street: warehouse.street,
                streetNumber: warehouse.streetNumber,
                latitude: warehouse.latitude,
                longitude: warehouse.longitude,
                totalCapacity: warehouse.totalCapacity
              });

              this.selectedLatitude = warehouse.latitude;
              this.selectedLongitude = warehouse.longitude;

              if (this.map && this.marker) {
                this.marker.setLatLng([warehouse.latitude, warehouse.longitude]);
                this.map.setView([warehouse.latitude, warehouse.longitude], 14);
              }

              this.sectorsArray.clear();
              if (warehouse.sectors && warehouse.sectors.length > 0) {
                warehouse.sectors.forEach(sector => {
                  const sectorGroup = this.fb.group({
                    id: [sector.id],
                    name: [sector.name, Validators.required],
                    minTemperature: [sector.minTemperature, Validators.required],
                    maxTemperature: [sector.maxTemperature, Validators.required],
                    capacity: [sector.capacity, [Validators.required, Validators.min(0)]]
                  });
                  this.sectorsArray.push(sectorGroup);
                });
              }

              this.loading = false;
            },
            error: () => {
              this.loading = false;
            }
          });
        } else {
          this.warehouseForm.patchValue({
            name: warehouse.name,
            street: warehouse.street,
            streetNumber: warehouse.streetNumber,
            latitude: warehouse.latitude,
            longitude: warehouse.longitude,
            totalCapacity: warehouse.totalCapacity
          });
          this.loading = false;
        }
        this.loadExistingImages(id);
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to load warehouse' });
        this.loading = false;
        console.error(err);
      }
    });
  }

  loadExistingImages(warehouseId: number): void {
    this.warehouseService.getWarehouseImages(warehouseId).subscribe({
      next: (data) => {
        this.existingImages = data.images;
      },
      error: (err) => {
        console.error('Failed to load images', err);
      }
    });
  }

  onImageSelect(event: any): void {
    for (const file of event.files) {
      if (!this.newImages.find(f => f.name === file.name)) {
        this.newImages.push(file);
      }
    }
  }

  removeNewImage(index: number): void {
    this.newImages.splice(index, 1);
  }

  markImageForDeletion(imageId: number): void {
    if (!this.imagesToDelete.includes(imageId)) {
      this.imagesToDelete.push(imageId);
    }
  }

  restoreImage(imageId: number): void {
    const index = this.imagesToDelete.indexOf(imageId);
    if (index > -1) {
      this.imagesToDelete.splice(index, 1);
    }
  }

  isMarkedForDeletion(imageId: number): boolean {
    return this.imagesToDelete.includes(imageId);
  }

  onSubmit(): void {
    if (this.warehouseForm.invalid) {
      this.warehouseForm.markAllAsTouched();
      return;
    }

    const remainingImages = this.existingImages.filter(img => !this.imagesToDelete.includes(img.id)).length;
    if (!this.isEditMode && this.newImages.length === 0) {
      this.messageService.add({ severity: 'warn', summary: 'Warning', detail: 'At least one image is required' });
      return;
    }
    if (this.isEditMode && remainingImages + this.newImages.length === 0) {
      this.messageService.add({ severity: 'warn', summary: 'Warning', detail: 'At least one image is required' });
      return;
    }

    this.submitting = true;
    const formValue = this.warehouseForm.value;

    const sectors = formValue.sectors.map((s: any) => ({
      id: s.id || undefined,
      name: s.name,
      minTemperature: s.minTemperature,
      maxTemperature: s.maxTemperature,
      capacity: s.capacity
    }));

    if (this.isEditMode && this.warehouseId) {
      const updateData = {
        name: formValue.name,
        countryId: formValue.country.id,
        cityId: formValue.city.id,
        street: formValue.street,
        streetNumber: formValue.streetNumber,
        latitude: formValue.latitude,
        longitude: formValue.longitude,
        totalCapacity: formValue.totalCapacity,
        imagesToDelete: this.imagesToDelete,
        sectors: sectors
      };

      this.warehouseService.update(this.warehouseId, updateData, this.newImages.length > 0 ? this.newImages : undefined).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Warehouse updated successfully' });
          setTimeout(() => this.router.navigate(['/app/manager/warehouses']), 1000);
        },
        error: (err) => {
          this.messageService.add({ severity: 'error', summary: 'Error', detail: err.error || 'Failed to update warehouse' });
          this.submitting = false;
        }
      });
    } else {
      const createData = {
        name: formValue.name,
        countryId: formValue.country.id,
        cityId: formValue.city.id,
        street: formValue.street,
        streetNumber: formValue.streetNumber,
        latitude: formValue.latitude,
        longitude: formValue.longitude,
        totalCapacity: formValue.totalCapacity,
        sectors: sectors
      };

      this.warehouseService.create(createData, this.newImages).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Warehouse created successfully' });
          setTimeout(() => this.router.navigate(['/app/manager/warehouses']), 1000);
        },
        error: (err) => {
          this.messageService.add({ severity: 'error', summary: 'Error', detail: err.error || 'Failed to create warehouse' });
          this.submitting = false;
        }
      });
    }
  }

  goBack(): void {
    this.router.navigate(['/app/manager/warehouses']);
  }
}
