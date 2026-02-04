import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';

// PrimeNG Imports
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { ToastModule } from 'primeng/toast';
import { FileUploadModule } from 'primeng/fileupload';
import { ImageModule } from 'primeng/image';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { MessageService } from 'primeng/api';

import { ManagerService, CreateManagerDTO } from '../../../services/user/manager.service';

@Component({
  selector: 'app-manager-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    CardModule,
    ButtonModule,
    InputTextModule,
    ToastModule,
    FileUploadModule,
    ImageModule,
    ProgressSpinnerModule
  ],
  providers: [MessageService],
  templateUrl: './manager-form.html'
})
export class ManagerFormComponent implements OnInit, OnDestroy {
  managerForm!: FormGroup;
  isLoading = false;
  selectedFile: File | null = null;
  imagePreview: string | null = null;

  private destroy$ = new Subject<void>();

  constructor(
    private formBuilder: FormBuilder,
    private managerService: ManagerService,
    private router: Router,
    private messageService: MessageService
  ) {
    this.initializeForm();
  }

  ngOnInit(): void {}

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initializeForm(): void {
    this.managerForm = this.formBuilder.group({
      username: ['', [Validators.required, Validators.email]],
      name: ['', [Validators.required, Validators.minLength(2)]],
      surname: ['', [Validators.required, Validators.minLength(2)]],
      phoneNumber: ['']
    });
  }

  onFileSelect(event: any): void {
    const file = event.files[0];
    if (file) {
      // Validate file type
      if (!file.type.startsWith('image/')) {
        this.messageService.add({
          severity: 'error',
          summary: 'Invalid File',
          detail: 'Please select an image file'
        });
        return;
      }

      // Validate file size (max 5MB)
      if (file.size > 5 * 1024 * 1024) {
        this.messageService.add({
          severity: 'error',
          summary: 'File Too Large',
          detail: 'Image must be less than 5MB'
        });
        return;
      }

      this.selectedFile = file;

      // Create preview
      const reader = new FileReader();
      reader.onload = () => {
        this.imagePreview = reader.result as string;
      };
      reader.readAsDataURL(file);
    }
  }

  removeImage(): void {
    this.selectedFile = null;
    this.imagePreview = null;
  }

  onSubmit(): void {
    if (this.managerForm.invalid) {
      this.markFormGroupTouched();
      return;
    }

    this.createManager();
  }

  private createManager(): void {
    this.isLoading = true;

    const managerData: CreateManagerDTO = {
      username: this.managerForm.value.username.trim(),
      name: this.managerForm.value.name.trim(),
      surname: this.managerForm.value.surname.trim(),
      phoneNumber: this.managerForm.value.phoneNumber?.trim() || undefined
    };

    this.managerService.createManager(managerData, this.selectedFile || undefined)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.isLoading = false;
          this.messageService.add({
            severity: 'success',
            summary: 'Success',
            detail: `Manager ${response.name} ${response.surname} created successfully. Login credentials have been sent to their email.`,
            life: 5000
          });
          
          setTimeout(() => {
            this.router.navigate(['/app/admin/managers']);
          }, 2000);
        },
        error: (error) => {
          this.isLoading = false;
          this.messageService.add({
            severity: 'error',
            summary: 'Error',
            detail: error.error?.message || 'Failed to create manager'
          });
        }
      });
  }

  cancel(): void {
    this.router.navigate(['/app/admin/managers']);
  }

  private markFormGroupTouched(): void {
    Object.keys(this.managerForm.controls).forEach(key => {
      const control = this.managerForm.get(key);
      control?.markAsTouched();
    });
  }

  // Getters for template
  get usernameControl() { return this.managerForm.get('username'); }
  get nameControl() { return this.managerForm.get('name'); }
  get surnameControl() { return this.managerForm.get('surname'); }

  get isUsernameInvalid(): boolean {
    const control = this.usernameControl;
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  get isNameInvalid(): boolean {
    const control = this.nameControl;
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  get isSurnameInvalid(): boolean {
    const control = this.surnameControl;
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  get usernameErrorMessage(): string {
    const control = this.usernameControl;
    if (control?.errors?.['required']) return 'Email is required';
    if (control?.errors?.['email']) return 'Please enter a valid email';
    return '';
  }

  get nameErrorMessage(): string {
    const control = this.nameControl;
    if (control?.errors?.['required']) return 'Name is required';
    if (control?.errors?.['minlength']) return 'Name must be at least 2 characters';
    return '';
  }

  get surnameErrorMessage(): string {
    const control = this.surnameControl;
    if (control?.errors?.['required']) return 'Surname is required';
    if (control?.errors?.['minlength']) return 'Surname must be at least 2 characters';
    return '';
  }
}
