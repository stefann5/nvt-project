import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ProductService } from '../../../services/product/product.service';
import { OrderService } from '../../../services/order/order.service';
import { CompanyRegistrationService } from '../../../services/company/company-registration.service';
import { CartService, CartItem } from '../../../services/cart/cart.service';
import { ProductListDTO } from '../../../dto/product/ProductListDTO';
import { CreateOrderDTO, OrderItemDTO } from '../../../dto/order/CreateOrderDTO';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { ToastModule } from 'primeng/toast';
import { MessageService, ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DividerModule } from 'primeng/divider';
import { StepperModule } from 'primeng/stepper';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';
import { InputNumberModule } from 'primeng/inputnumber';
import { ProductCatalogComponent } from '../product-catalog/product-catalog';
import { TextareaModule } from 'primeng/textarea';
import { InputTextModule } from 'primeng/inputtext';


interface Company {
  id: number;
  name: string;
  cityName?: string;
  countryName?: string;
}

@Component({
  selector: 'app-create-order',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    CardModule,
    ButtonModule,
    SelectModule,
    TextareaModule,
    TableModule,
    ToastModule,
    ConfirmDialogModule,
    DividerModule,
    StepperModule,
    ProgressSpinnerModule,
    TagModule,
    InputNumberModule,
    ProductCatalogComponent
  ],
  providers: [MessageService, ConfirmationService],
  templateUrl: './create-order.html',
  styleUrl: './create-order.scss'
})
export class CreateOrderComponent implements OnInit {
  currentStep = 0;
  cart: CartItem[] = [];
  companies: Company[] = [];
  selectedCompanyId: number | null = null;
  orderNotes = '';
  loading = false;
  loadingCompanies = false;
  submitting = false;

  constructor(
    private productService: ProductService,
    private orderService: OrderService,
    private companyService: CompanyRegistrationService,
    private cartService: CartService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadCompanies();
    this.loadCart();
  }

  loadCart(): void {
    // Subscribe to cart changes
    this.cartService.cart$.subscribe(cart => {
      this.cart = cart;
    });
  }

  loadCompanies(): void {
    this.loadingCompanies = true;
    this.companyService.getMyApprovedCompanies().subscribe({
      next: (companies) => {
        this.companies = companies.map(c => ({
          id: c.id,
          name: c.ownerName,
          cityName: c.cityName,
          countryName: c.countryName
        }));
        this.loadingCompanies = false;
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Greska',
          detail: 'Neuspesno ucitavanje firmi'
        });
        this.loadingCompanies = false;
        console.error(err);
      }
    });
  }

  removeFromCart(productId: number): void {
    this.cartService.removeFromCart(productId);
  }

  updateCartQuantity(productId: number, newQuantity: number): void {
    const item = this.cart.find(c => c.product.id === productId);
    if (!item) return;

    if (newQuantity <= 0) {
      this.removeFromCart(productId);
    } else if (newQuantity > item.product.totalQuantity) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Upozorenje',
        detail: `Maksimalna dostupna kolicina je ${item.product.totalQuantity}`
      });
      this.cartService.updateQuantity(productId, item.product.totalQuantity);
    } else {
      this.cartService.updateQuantity(productId, newQuantity);
    }
  }

  getCartTotal(): number {
    return this.cartService.getTotalPrice();
  }

  getCartItemCount(): number {
    return this.cartService.getItemCount();
  }

  canProceedToStep2(): boolean {
    return this.cart.length > 0;
  }

  canProceedToStep3(): boolean {
    return this.selectedCompanyId !== null;
  }

  canSubmitOrder(): boolean {
    return this.cart.length > 0 && this.selectedCompanyId !== null;
  }

  navigateToRegisterCompany(): void {
    this.router.navigate(['/app/home']);
  }

  nextStep(): void {
    if (this.currentStep === 0 && !this.canProceedToStep2()) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Upozorenje',
        detail: 'Dodajte bar jedan proizvod u korpu'
      });
      return;
    }
    if (this.currentStep === 1 && !this.canProceedToStep3()) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Upozorenje',
        detail: 'Izaberite firmu za dostavu'
      });
      return;
    }
    this.currentStep++;
  }

  prevStep(): void {
    if (this.currentStep > 0) {
      this.currentStep--;
    }
  }

  goToStep(step: number): void {
    this.currentStep = step;
  }

  submitOrder(): void {
    if (!this.canSubmitOrder()) {
      return;
    }

    this.confirmationService.confirm({
      message: `Da li ste sigurni da zelite da kreirate porudzbinu u vrednosti od ${this.formatPrice(this.getCartTotal())}?`,
      header: 'Potvrda porudzbine',
      icon: 'pi pi-shopping-cart',
      acceptLabel: 'Da, poruci',
      rejectLabel: 'Odustani',
      accept: () => {
        this.processOrder();
      }
    });
  }

  private processOrder(): void {
    this.submitting = true;

    const orderData: CreateOrderDTO = {
      companyId: this.selectedCompanyId!,
      items: this.cart.map(item => ({
        productId: item.product.id,
        quantity: item.quantity
      })),
      notes: this.orderNotes || undefined
    };

    this.orderService.createOrder(orderData).subscribe({
      next: (response) => {
        this.messageService.add({
          severity: 'success',
          summary: 'Uspeh',
          detail: `Porudzbina ${response.orderNumber} je uspesno kreirana! Faktura je poslata na vas email.`
        });
        // Clear the cart after successful order
        this.cartService.clearCart();
        this.submitting = false;
        setTimeout(() => {
          this.router.navigate(['/app/customer/orders']);
        }, 2000);
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Greska',
          detail: err.error || 'Neuspesno kreiranje porudzbine'
        });
        this.submitting = false;
        console.error(err);
      }
    });
  }

  formatPrice(price: number): string {
    return price.toFixed(2) + ' EUR';
  }

  getSelectedCompany(): Company | undefined {
    return this.companies.find(c => c.id === this.selectedCompanyId);
  }
}
