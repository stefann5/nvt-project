import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';

// PrimeNG Imports
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { PasswordModule } from 'primeng/password';
import { ToastModule } from 'primeng/toast';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { MessageService } from 'primeng/api';
import { AuthService } from '../../../services/auth/auth-service';

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    CardModule,
    ButtonModule,
    PasswordModule,
    ToastModule,
    ProgressSpinnerModule
  ],
  providers: [MessageService],
  templateUrl: 'change-password.html'
})
export class ChangePasswordComponent implements OnInit, OnDestroy {
  changePasswordForm!: FormGroup;
  isLoading = false;

  private destroy$ = new Subject<void>();

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private messageService: MessageService
  ) {
    this.initializeForm();
  }

  ngOnInit(): void {
    // Check if user needs to change password
    if (!this.authService.mustChangePassword() && !this.authService.isAuthenticated()) {
      this.router.navigate(['']);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initializeForm(): void {
    this.changePasswordForm = this.formBuilder.group({
      currentPassword: ['', [Validators.required]],
      newPassword: ['', [
        Validators.required,
        Validators.minLength(8)
      ]],
      confirmPassword: ['', [Validators.required]]
    }, { validators: this.passwordMatchValidator });
  }

  private passwordMatchValidator(form: FormGroup) {
    const newPassword = form.get('newPassword');
    const confirmPassword = form.get('confirmPassword');
    
    if (newPassword && confirmPassword && newPassword.value !== confirmPassword.value) {
      confirmPassword.setErrors({ passwordMismatch: true });
      return { passwordMismatch: true };
    }
    return null;
  }

  onSubmit(): void {
    if (this.changePasswordForm.invalid) {
      this.markFormGroupTouched();
      return;
    }

    this.performPasswordChange();
  }

  private performPasswordChange(): void {
    this.isLoading = true;
    this.messageService.clear();

    const { currentPassword, newPassword, confirmPassword } = this.changePasswordForm.value;

    this.authService.changePassword(currentPassword, newPassword, confirmPassword)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.isLoading = false;
          this.messageService.add({
            severity: 'success',
            summary: 'Success',
            detail: 'Password changed successfully! Redirecting...',
            life: 3000
          });
          
          // Redirect to app after short delay
          setTimeout(() => {
            this.router.navigate(['/app']);
          }, 1500);
        },
        error: (error) => {
          this.handleError(error);
          this.isLoading = false;
        }
      });
  }

  private handleError(error: any): void {
    let errorMessage = 'Password change failed';
    let errorDetail = 'Please try again.';

    if (error.error?.error) {
      errorDetail = error.error.error;
    } else if (error.status === 400) {
      errorDetail = 'Invalid request. Please check your input.';
    } else if (error.status === 401) {
      errorDetail = 'Session expired. Please login again.';
      this.authService.logout();
    }

    this.messageService.add({
      severity: 'error',
      summary: errorMessage,
      detail: errorDetail,
      life: 5000
    });
  }

  logout(): void {
    this.authService.logout();
  }

  private markFormGroupTouched(): void {
    Object.keys(this.changePasswordForm.controls).forEach(key => {
      const control = this.changePasswordForm.get(key);
      control?.markAsTouched();
    });
  }

  // Getters for template
  get currentPasswordControl() {
    return this.changePasswordForm.get('currentPassword');
  }

  get newPasswordControl() {
    return this.changePasswordForm.get('newPassword');
  }

  get confirmPasswordControl() {
    return this.changePasswordForm.get('confirmPassword');
  }

  get isCurrentPasswordInvalid(): boolean {
    const control = this.currentPasswordControl;
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  get isNewPasswordInvalid(): boolean {
    const control = this.newPasswordControl;
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  get isConfirmPasswordInvalid(): boolean {
    const control = this.confirmPasswordControl;
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  get currentPasswordErrorMessage(): string {
    const control = this.currentPasswordControl;
    if (control?.errors?.['required']) {
      return 'Current password is required';
    }
    return '';
  }

  get newPasswordErrorMessage(): string {
    const control = this.newPasswordControl;
    if (control?.errors?.['required']) {
      return 'New password is required';
    }
    if (control?.errors?.['minlength']) {
      return 'Password must be at least 8 characters';
    }
    return '';
  }

  get confirmPasswordErrorMessage(): string {
    const control = this.confirmPasswordControl;
    if (control?.errors?.['required']) {
      return 'Please confirm your password';
    }
    if (control?.errors?.['passwordMismatch']) {
      return 'Passwords do not match';
    }
    return '';
  }
}
