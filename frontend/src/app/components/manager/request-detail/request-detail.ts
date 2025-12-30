import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CompanyRegistrationService } from '../../../services/company/company-registration.service';
import { RegistrationRequestDTO } from '../../../dto/company/RegistrationRequestDTO';

interface FileWithUrl {
  id: number;
  originalName: string;
  url: string;
  contentType?: string;
}

@Component({
  selector: 'app-request-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './request-detail.html',
  styleUrl: './request-detail.scss'
})
export class RequestDetailComponent implements OnInit {
  request: RegistrationRequestDTO | null = null;
  loading = false;
  processing = false;
  error = '';
  successMessage = '';
  
  showRejectModal = false;
  rejectionReason = '';

  imageUrls: FileWithUrl[] = [];
  documentUrls: FileWithUrl[] = [];
  filesLoading = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private companyService: CompanyRegistrationService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadRequest(+id);
    }
  }

  loadRequest(id: number): void {
    this.loading = true;
    this.error = '';

    this.companyService.getRequestById(id).subscribe({
      next: (data) => {
        this.request = data;
        this.loading = false;
        this.loadFiles(id);
      },
      error: (err) => {
        this.error = 'Failed to load request details';
        this.loading = false;
        console.error(err);
      }
    });
  }

  loadFiles(requestId: number): void {
    this.filesLoading = true;
    this.companyService.getRequestFiles(requestId).subscribe({
      next: (data) => {
        this.imageUrls = data.images;
        this.documentUrls = data.documents;
        this.filesLoading = false;
      },
      error: (err) => {
        console.error('Failed to load files', err);
        this.filesLoading = false;
      }
    });
  }

  approveRequest(): void {
    if (!this.request) return;

    this.processing = true;
    this.error = '';

    this.companyService.processRequest(this.request.id, { approved: true }).subscribe({
      next: (result) => {
        this.request = result;
        this.successMessage = 'Request approved successfully. Email notification sent to the owner.';
        this.processing = false;
      },
      error: (err) => {
        this.error = err.error || 'Failed to approve request';
        this.processing = false;
      }
    });
  }

  openRejectModal(): void {
    this.showRejectModal = true;
    this.rejectionReason = '';
  }

  closeRejectModal(): void {
    this.showRejectModal = false;
    this.rejectionReason = '';
  }

  confirmReject(): void {
    if (!this.request || !this.rejectionReason.trim()) {
      this.error = 'Please provide a rejection reason';
      return;
    }

    this.processing = true;
    this.error = '';

    this.companyService.processRequest(this.request.id, {
      approved: false,
      rejectionReason: this.rejectionReason
    }).subscribe({
      next: (result) => {
        this.request = result;
        this.successMessage = 'Request rejected. Email notification sent to the owner.';
        this.processing = false;
        this.closeRejectModal();
      },
      error: (err) => {
        this.error = err.error || 'Failed to reject request';
        this.processing = false;
      }
    });
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

  goBack(): void {
    this.router.navigate(['/app/manager/requests']);
  }
}
