export interface OrderResponseDTO {
  id: number;
  orderNumber: string;
  customerId: number;
  customerName: string;
  companyId: number;
  companyName: string;
  companyAddress: string;
  status: string;
  totalAmount: number;
  notes?: string;
  createdAt: string;
  processedAt?: string;
  items: OrderItemResponseDTO[];
}

export interface OrderItemResponseDTO {
  id: number;
  productId: number;
  productName: string;
  productSku: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
}
