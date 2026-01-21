export interface OrderListDTO {
  id: number;
  orderNumber: string;
  companyName: string;
  status: string;
  totalAmount: number;
  itemCount: number;
  createdAt: string;
}
