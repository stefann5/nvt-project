import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ProductService } from '../../../services/product/product.service';
import { FactoryService } from '../../../services/factory/factory.service';
import { ProductResponseDTO } from '../../../dto/product/ProductResponseDTO';
import { FactorySimpleDTO } from '../../../dto/factory/FactoryDTO';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { TextareaModule } from 'primeng/textarea';
import { SelectModule } from 'primeng/select';
import { MultiSelectModule } from 'primeng/multiselect';
import { FileUploadModule } from 'primeng/fileupload';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { MessageModule } from 'primeng/message';
import { ToastModule } from 'primeng/toast';
import { DividerModule } from 'primeng/divider';
import { CheckboxModule } from 'primeng/checkbox';
import { MessageService } from 'primeng/api';
import { ImageModule } from 'primeng/image';
import { TooltipModule } from 'primeng/tooltip';

interface ExistingImage {
  id: number;
  originalName: string;
  minioPath?: string;
  url?: string;
  markedForDeletion: boolean;
}

@Component({
  selector: 'app-product-form',
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
    TextareaModule,
    SelectModule,
    MultiSelectModule,
    FileUploadModule,
    ProgressSpinnerModule,
    MessageModule,
    ToastModule,
    DividerModule,
    CheckboxModule,
    ImageModule,
    TooltipModule
  ],
  providers: [MessageService],
  templateUrl: './product-form.html',
  styleUrl: './product-form.scss'
})
export class ProductFormComponent implements OnInit {
  productForm!: FormGroup;
  isEditMode = false;
  productId: number | null = null;
  loading = false;
  submitting = false;

  categories: string[] = [];
  factories: FactorySimpleDTO[] = [];

  existingImages: ExistingImage[] = [];
  newImages: File[] = [];

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private productService: ProductService,
    private factoryService: FactoryService,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id && id !== 'new') {
      this.isEditMode = true;
      this.productId = +id;
    }
    this.initForm();
    this.loadInitialData();
  }

  initForm(): void {
    this.productForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(255)]],
      description: ['', [Validators.maxLength(2000)]],
      sku: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
      price: [null, [Validators.required, Validators.min(0.01)]],
      weight: [null, [Validators.min(0.01)]],
      category: [null],
      unit: ['kom', [Validators.maxLength(20)]],
      forSale: [true],
      factories: [[]]
    });

    if (this.isEditMode) {
      this.productForm.get('sku')?.disable();
    }
  }

  loadInitialData(): void {
    this.loading = true;

    // Load categories and factories in parallel
    Promise.all([
      this.productService.getCategories().toPromise(),
      this.factoryService.getAllSimple().toPromise()
    ]).then(([categories, factories]) => {
      this.categories = categories || [];
      this.factories = factories || [];

      if (this.isEditMode && this.productId) {
        this.loadProduct(this.productId);
      } else {
        this.loading = false;
      }
    }).catch(err => {
      this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to load initial data' });
      this.loading = false;
      console.error(err);
    });
  }

  loadProduct(id: number): void {
    this.productService.getById(id).subscribe({
      next: (product) => {
        this.populateForm(product);
        this.loading = false;
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to load product' });
        this.loading = false;
        console.error(err);
      }
    });
  }

  populateForm(product: ProductResponseDTO): void {
    const selectedCategory = this.categories.find(c => c === product.category) || null;
    const selectedFactories = this.factories.filter(f => 
      product.factories?.some(pf => pf.id === f.id)
    );

    this.productForm.patchValue({
      name: product.name,
      description: product.description,
      sku: product.sku,
      price: product.price,
      weight: product.weight,
      category: selectedCategory,
      unit: product.unit,
      forSale: product.forSale,
      factories: selectedFactories
    });

    // Set existing images
    this.existingImages = product.images.map(img => ({
      id: img.id,
      originalName: img.originalName,
      minioPath: img.minioPath,
      markedForDeletion: false
    }));
  }

  onImagesSelect(event: any): void {
    for (let file of event.files) {
      if (!this.newImages.some(f => f.name === file.name)) {
        this.newImages.push(file);
      }
    }
  }

  removeNewImage(index: number): void {
    this.newImages.splice(index, 1);
  }

  toggleImageDeletion(image: ExistingImage): void {
    image.markedForDeletion = !image.markedForDeletion;
  }

  onSubmit(): void {
    if (this.productForm.invalid) {
      this.productForm.markAllAsTouched();
      this.messageService.add({ severity: 'warn', summary: 'Validation', detail: 'Please fill all required fields correctly' });
      return;
    }

    this.submitting = true;
    const formValue = this.productForm.getRawValue();

    if (this.isEditMode && this.productId) {
      this.updateProduct(formValue);
    } else {
      this.createProduct(formValue);
    }
  }

  createProduct(formValue: any): void {
    const productData = {
      name: formValue.name,
      description: formValue.description,
      sku: formValue.sku,
      price: formValue.price,
      weight: formValue.weight,
      category: formValue.category,
      unit: formValue.unit,
      forSale: formValue.forSale,
      factoryIds: formValue.factories?.map((f: FactorySimpleDTO) => f.id) || []
    };

    this.productService.create(productData, this.newImages.length > 0 ? this.newImages : undefined).subscribe({
      next: (response) => {
        this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Product created successfully' });
        this.submitting = false;
        setTimeout(() => {
          this.router.navigate(['/app/manager/products', response.id]);
        }, 1000);
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: err.error?.message || 'Failed to create product' });
        this.submitting = false;
        console.error(err);
      }
    });
  }

  updateProduct(formValue: any): void {
    const imagesToDelete = this.existingImages
      .filter(img => img.markedForDeletion)
      .map(img => img.id);

    const productData = {
      name: formValue.name,
      description: formValue.description,
      price: formValue.price,
      weight: formValue.weight,
      category: formValue.category,
      unit: formValue.unit,
      forSale: formValue.forSale,
      factoryIds: formValue.factories?.map((f: FactorySimpleDTO) => f.id) || [],
      imagesToDelete: imagesToDelete.length > 0 ? imagesToDelete : undefined
    };

    this.productService.update(this.productId!, productData, this.newImages.length > 0 ? this.newImages : undefined).subscribe({
      next: (response) => {
        this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Product updated successfully' });
        this.submitting = false;
        setTimeout(() => {
          this.router.navigate(['/app/manager/products', response.id]);
        }, 1000);
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: err.error?.message || 'Failed to update product' });
        this.submitting = false;
        console.error(err);
      }
    });
  }

  cancel(): void {
    if (this.isEditMode && this.productId) {
      this.router.navigate(['/app/manager/products', this.productId]);
    } else {
      this.router.navigate(['/app/manager/products']);
    }
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.productForm.get(fieldName);
    return field ? field.invalid && field.touched : false;
  }

  getFieldError(fieldName: string): string {
    const field = this.productForm.get(fieldName);
    if (field?.errors) {
      if (field.errors['required']) return `${fieldName} is required`;
      if (field.errors['minlength']) return `${fieldName} must be at least ${field.errors['minlength'].requiredLength} characters`;
      if (field.errors['maxlength']) return `${fieldName} cannot exceed ${field.errors['maxlength'].requiredLength} characters`;
      if (field.errors['min']) return `${fieldName} must be at least ${field.errors['min'].min}`;
    }
    return '';
  }
}
