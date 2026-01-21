export interface CreateOrderDTO {
  companyId: number;
  items: OrderItemDTO[];
  notes?: string;
}

export interface OrderItemDTO {
  productId: number;
  quantity: number;
}
