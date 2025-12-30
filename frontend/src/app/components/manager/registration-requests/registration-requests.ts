import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { CompanyRegistrationService } from '../../../services/company/company-registration.service';
import { RegistrationRequestDTO } from '../../../dto/company/RegistrationRequestDTO';

@Component({
  selector: 'app-registration-requests',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './registration-requests.html',
  styleUrl: './registration-requests.scss'
})
export class RegistrationRequestsComponent implements OnInit {
  requests: RegistrationRequestDTO[] = [];
  loading = false;
  error = '';
  showAll = false;

  constructor(private companyService: CompanyRegistrationService) {}

  ngOnInit(): void {
    this.loadRequests();
  }

  loadRequests(): void {
    this.loading = true;
    this.error = '';
    
    const request$ = this.showAll 
      ? this.companyService.getAllRequests()
      : this.companyService.getPendingRequests();

    request$.subscribe({
      next: (data) => {
        this.requests = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load registration requests';
        this.loading = false;
        console.error(err);
      }
    });
  }

  toggleFilter(): void {
    this.showAll = !this.showAll;
    this.loadRequests();
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'PENDING': return 'status-pending';
      case 'APPROVED': return 'status-approved';
      case 'REJECTED': return 'status-rejected';
      default: return '';
    }
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }
}
