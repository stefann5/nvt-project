import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FactoryService } from '../../../services/factory/factory.service';
import { CountryDTO } from '../../../dto/company/CountryDTO';
import { CityDTO } from '../../../dto/company/CityDTO';
import { FactoryResponseDTO } from '../../../dto/factory/FactoryDTO';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { SelectModule } from 'primeng/select';
import { FileUploadModule } from 'primeng/fileupload';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { MessageModule } from 'primeng/message';
import { ToastModule } from 'primeng/toast';
import { DividerModule } from 'primeng/divider';
import { MessageService } from 'primeng/api';
import { MapComponent } from '../../map/map.component';
import { AddressDTO } from '../../../dto/auth/AddressDTO';

interface ImageWithUrl {
  id: number;
  originalName: string;
  url: string;
}

@Component({
  selector: 'app-factory-form',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule,
    CardModule,
    ButtonModule,
    InputTextModule,
    TextareaModule,
    SelectModule,
    FileUploadModule,
    ProgressSpinnerModule,
    MessageModule,
    ToastModule,
    DividerModule,
    MapComponent
  ],
  providers: [MessageService],
  templateUrl: './factory-form.html',
  styleUrl: './factory-form.scss'
})
export class FactoryFormComponent implements OnInit {
  factoryForm!: FormGroup;
  isEditMode = false;
  factoryId: number | null = null;
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
  locationSelected = false;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private factoryService: FactoryService,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    this.initForm();

    const id = this.route.snapshot.paramMap.get('id');
    if (id && id !== 'new') {
      this.isEditMode = true;
      this.factoryId = +id;
    }

    this.loadInitialData();
  }

  initForm(): void {
    this.factoryForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      description: [''],
      country: [null, Validators.required],
      city: [null, Validators.required],
      street: ['', Validators.required],
      streetNumber: ['']
    });
  }

  loadInitialData(): void {
    this.loading = true;
    
    // Load countries
    this.factoryService.getAllCountries().subscribe({
      next: (countries) => {
        this.countries = countries;
        
        if (this.isEditMode && this.factoryId) {
          this.loadFactory(this.factoryId);
        } else {
          this.loading = false;
        }
      },
      error: (err: any) => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to load countries' });
        this.loading = false;
        console.error(err);
      }
    });
  }

  onCountryChange(event: any): void {
    this.selectedCountry = event.value;
    this.factoryForm.patchValue({ city: null });
    this.cities = [];

    if (this.selectedCountry) {
      this.factoryService.getCitiesByCountry(this.selectedCountry.id).subscribe({
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

  onAddressSelected(address: AddressDTO): void {
    this.selectedLatitude = address.latitude ?? null;
    this.selectedLongitude = address.longitude ?? null;
    this.selectedStreet = address.street ?? '';
    this.locationSelected = true;
    
    this.factoryForm.patchValue({
      street: this.selectedStreet,
      streetNumber: address.number ?? ''
    });

    this.messageService.add({
      severity: 'success',
      summary: 'Location Selected',
      detail: `Address: ${this.selectedStreet || 'Location marked on map'}`
    });
  }

  loadFactory(id: number): void {
    this.factoryService.getById(id).subscribe({
      next: (factory: FactoryResponseDTO) => {
        const country = this.countries.find(c => c.id === factory.countryId);
        if (country) {
          this.selectedCountry = country;
          this.factoryService.getCitiesByCountry(country.id).subscribe({
            next: (cities) => {
              this.cities = cities;
              const city = cities.find(c => c.id === factory.cityId);

              this.factoryForm.patchValue({
                name: factory.name,
                description: factory.description,
                country: country,
                city: city,
                street: factory.street,
                streetNumber: factory.streetNumber
              });

              this.selectedLatitude = factory.latitude;
              this.selectedLongitude = factory.longitude;
              this.selectedStreet = factory.street;
              this.locationSelected = true;

              this.loading = false;
            },
            error: () => {
              this.loading = false;
            }
          });
        } else {
          this.factoryForm.patchValue({
            name: factory.name,
            description: factory.description,
            street: factory.street,
            streetNumber: factory.streetNumber
          });
          this.loading = false;
        }
        this.loadExistingImages(id);
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to load factory' });
        this.loading = false;
        console.error(err);
      }
    });
  }

  loadExistingImages(factoryId: number): void {
    this.factoryService.getFactoryImages(factoryId).subscribe({
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
    if (this.factoryForm.invalid) {
      this.factoryForm.markAllAsTouched();
      this.messageService.add({ severity: 'warn', summary: 'Warning', detail: 'Please fill all required fields' });
      return;
    }

    if (!this.locationSelected || this.selectedLatitude === null || this.selectedLongitude === null) {
      this.messageService.add({ severity: 'warn', summary: 'Warning', detail: 'Please select a location on the map' });
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
    const formValue = this.factoryForm.value;

    if (this.isEditMode && this.factoryId) {
      const updateData = {
        name: formValue.name,
        description: formValue.description,
        countryId: formValue.country.id,
        cityId: formValue.city.id,
        street: formValue.street,
        streetNumber: formValue.streetNumber,
        latitude: this.selectedLatitude!,
        longitude: this.selectedLongitude!,
        imagesToDelete: this.imagesToDelete
      };

      this.factoryService.update(this.factoryId, updateData, this.newImages.length > 0 ? this.newImages : undefined).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Factory updated successfully' });
          setTimeout(() => this.router.navigate(['/app/manager/factories']), 1000);
        },
        error: (err) => {
          this.messageService.add({ severity: 'error', summary: 'Error', detail: err.error || 'Failed to update factory' });
          this.submitting = false;
        }
      });
    } else {
      const createData = {
        name: formValue.name,
        description: formValue.description,
        countryId: formValue.country.id,
        cityId: formValue.city.id,
        street: formValue.street,
        streetNumber: formValue.streetNumber,
        latitude: this.selectedLatitude!,
        longitude: this.selectedLongitude!
      };

      this.factoryService.create(createData, this.newImages).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Factory created successfully' });
          setTimeout(() => this.router.navigate(['/app/manager/factories']), 1000);
        },
        error: (err) => {
          this.messageService.add({ severity: 'error', summary: 'Error', detail: err.error || 'Failed to create factory' });
          this.submitting = false;
        }
      });
    }
  }

  goBack(): void {
    this.router.navigate(['/app/manager/factories']);
  }
}
