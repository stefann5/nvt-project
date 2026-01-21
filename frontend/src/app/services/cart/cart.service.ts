import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface CartItem {
  product: {
    id: number;
    name: string;
    sku: string;
    price: number;
    category: string;
    unit: string;
    active: boolean;
    totalQuantity: number;
    imageUrl?: string;
  };
  quantity: number;
}

@Injectable({
  providedIn: 'root'
})
export class CartService {
  private readonly CART_STORAGE_KEY = 'shopping_cart';
  private cartSubject = new BehaviorSubject<CartItem[]>(this.loadCartFromStorage());

  public cart$: Observable<CartItem[]> = this.cartSubject.asObservable();

  constructor() {}

  /**
   * Load cart from localStorage
   */
  private loadCartFromStorage(): CartItem[] {
    try {
      const cartJson = localStorage.getItem(this.CART_STORAGE_KEY);
      if (cartJson) {
        return JSON.parse(cartJson);
      }
    } catch (error) {
      console.error('Error loading cart from storage:', error);
    }
    return [];
  }

  /**
   * Save cart to localStorage
   */
  private saveCartToStorage(cart: CartItem[]): void {
    try {
      localStorage.setItem(this.CART_STORAGE_KEY, JSON.stringify(cart));
    } catch (error) {
      console.error('Error saving cart to storage:', error);
    }
  }

  /**
   * Get current cart items
   */
  getCart(): CartItem[] {
    return this.cartSubject.value;
  }

  /**
   * Add item to cart or update quantity if already exists
   */
  addToCart(item: CartItem): void {
    const currentCart = this.getCart();
    const existingItemIndex = currentCart.findIndex(
      cartItem => cartItem.product.id === item.product.id
    );

    if (existingItemIndex > -1) {
      // Update quantity if item already exists
      currentCart[existingItemIndex].quantity += item.quantity;
    } else {
      // Add new item
      currentCart.push(item);
    }

    this.updateCart(currentCart);
  }

  /**
   * Update quantity of a specific item
   */
  updateQuantity(productId: number, quantity: number): void {
    const currentCart = this.getCart();
    const itemIndex = currentCart.findIndex(
      item => item.product.id === productId
    );

    if (itemIndex > -1) {
      if (quantity <= 0) {
        // Remove item if quantity is 0 or negative
        currentCart.splice(itemIndex, 1);
      } else {
        currentCart[itemIndex].quantity = quantity;
      }
      this.updateCart(currentCart);
    }
  }

  /**
   * Remove item from cart
   */
  removeFromCart(productId: number): void {
    const currentCart = this.getCart().filter(
      item => item.product.id !== productId
    );
    this.updateCart(currentCart);
  }

  /**
   * Clear entire cart
   */
  clearCart(): void {
    this.updateCart([]);
  }

  /**
   * Get total number of items in cart
   */
  getItemCount(): number {
    return this.getCart().reduce((total, item) => total + item.quantity, 0);
  }

  /**
   * Get total price of all items in cart
   */
  getTotalPrice(): number {
    return this.getCart().reduce(
      (total, item) => total + (item.product.price * item.quantity),
      0
    );
  }

  /**
   * Check if a product is in the cart
   */
  isInCart(productId: number): boolean {
    return this.getCart().some(item => item.product.id === productId);
  }

  /**
   * Get quantity of a specific product in cart
   */
  getProductQuantity(productId: number): number {
    const item = this.getCart().find(item => item.product.id === productId);
    return item ? item.quantity : 0;
  }

  /**
   * Update cart and notify subscribers
   */
  private updateCart(cart: CartItem[]): void {
    this.saveCartToStorage(cart);
    this.cartSubject.next(cart);
  }
}
