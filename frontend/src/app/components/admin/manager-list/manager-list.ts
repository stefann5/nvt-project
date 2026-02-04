import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, takeUntil, debounceTime, distinctUntilChanged } from 'rxjs';

// PrimeNG Imports
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { TagModule } from 'primeng/tag';
import { AvatarModule } from 'primeng/avatar';
import { TooltipModule } from 'primeng/tooltip';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { MessageService, ConfirmationService } from 'primeng/api';

import { ManagerService, ManagerResponseDTO, PageResponse } from '../../../services/user/manager.service';

@Component({
  selector: 'app-manager-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ButtonModule,
    InputTextModule,
    ToastModule,
    ConfirmDialogModule,
    TagModule,
    AvatarModule,
    TooltipModule,
    ProgressSpinnerModule
  ],
  providers: [MessageService, ConfirmationService],
  templateUrl: './manager-list.html'
})
export class ManagerListComponent implements OnInit, OnDestroy {
  managers: ManagerResponseDTO[] = [];
  totalRecords = 0;
  loading = false;
  searchQuery = '';
  
  // Pagination
  first = 0;
  rows = 10;

  private destroy$ = new Subject<void>();
  private searchSubject = new Subject<string>();

  constructor(
    private managerService: ManagerService,
    private router: Router,
    private messageService: MessageService,
    private confirmationService: ConfirmationService
  ) {}

  ngOnInit(): void {
    this.loadManagers();
    this.setupSearch();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private setupSearch(): void {
    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(query => {
      this.first = 0;
      if (query.trim()) {
        this.searchManagers(query);
      } else {
        this.loadManagers();
      }
    });
  }

  loadManagers(): void {
    this.loading = true;
    const page = this.first / this.rows;

    this.managerService.getManagersPaged(page, this.rows)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: PageResponse<ManagerResponseDTO>) => {
          this.managers = response.content;
          this.totalRecords = response.totalElements;
          this.loading = false;
        },
        error: (error) => {
          this.messageService.add({
            severity: 'error',
            summary: 'Error',
            detail: 'Failed to load managers'
          });
          this.loading = false;
        }
      });
  }

  searchManagers(query: string): void {
    this.loading = true;
    const page = this.first / this.rows;

    this.managerService.searchManagers(query, page, this.rows)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: PageResponse<ManagerResponseDTO>) => {
          this.managers = response.content;
          this.totalRecords = response.totalElements;
          this.loading = false;
        },
        error: (error) => {
          this.messageService.add({
            severity: 'error',
            summary: 'Error',
            detail: 'Failed to search managers'
          });
          this.loading = false;
        }
      });
  }

  onSearch(): void {
    this.searchSubject.next(this.searchQuery);
  }

  onPageChange(event: any): void {
    this.first = event.first;
    this.rows = event.rows;
    
    if (this.searchQuery.trim()) {
      this.searchManagers(this.searchQuery);
    } else {
      this.loadManagers();
    }
  }

  navigateToCreate(): void {
    this.router.navigate(['/app/admin/managers/new']);
  }

  confirmBlock(manager: ManagerResponseDTO): void {
    this.confirmationService.confirm({
      message: `Are you sure you want to block ${manager.name} ${manager.surname}?`,
      header: 'Confirm Block',
      icon: 'pi pi-exclamation-triangle',
      accept: () => this.blockManager(manager)
    });
  }

  confirmUnblock(manager: ManagerResponseDTO): void {
    this.confirmationService.confirm({
      message: `Are you sure you want to unblock ${manager.name} ${manager.surname}?`,
      header: 'Confirm Unblock',
      icon: 'pi pi-question-circle',
      accept: () => this.unblockManager(manager)
    });
  }

  blockManager(manager: ManagerResponseDTO): void {
    this.managerService.blockManager(manager.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updated) => {
          const index = this.managers.findIndex(m => m.id === manager.id);
          if (index !== -1) {
            this.managers[index] = updated;
          }
          this.messageService.add({
            severity: 'success',
            summary: 'Success',
            detail: `${manager.name} ${manager.surname} has been blocked`
          });
        },
        error: (error) => {
          this.messageService.add({
            severity: 'error',
            summary: 'Error',
            detail: error.error?.message || 'Failed to block manager'
          });
        }
      });
  }

  unblockManager(manager: ManagerResponseDTO): void {
    this.managerService.unblockManager(manager.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updated) => {
          const index = this.managers.findIndex(m => m.id === manager.id);
          if (index !== -1) {
            this.managers[index] = updated;
          }
          this.messageService.add({
            severity: 'success',
            summary: 'Success',
            detail: `${manager.name} ${manager.surname} has been unblocked`
          });
        },
        error: (error) => {
          this.messageService.add({
            severity: 'error',
            summary: 'Error',
            detail: error.error?.message || 'Failed to unblock manager'
          });
        }
      });
  }

  getStatusSeverity(manager: ManagerResponseDTO): 'success' | 'danger' | 'warn' {
    if (manager.blocked) return 'danger';
    if (manager.active) return 'success';
    return 'warn';
  }

  getStatusLabel(manager: ManagerResponseDTO): string {
    if (manager.blocked) return 'Blocked';
    if (manager.active) return 'Active';
    return 'Inactive';
  }

  getRoleSeverity(role: string): 'info' | 'warn' {
    return role === 'S' ? 'warn' : 'info';
  }

  getRoleLabel(role: string): string {
    return role === 'S' ? 'Super Admin' : 'Manager';
  }

  getInitials(manager: ManagerResponseDTO): string {
    return `${manager.name.charAt(0)}${manager.surname.charAt(0)}`.toUpperCase();
  }
}
