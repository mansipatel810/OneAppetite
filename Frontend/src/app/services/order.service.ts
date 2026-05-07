import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface OrderItem {
  orderItemId: number;
  quantity: number;
  price: number;
  menuItem: { itemName: string; category: string };
}

export type OrderStatus =
  'CART' | 'PLACED' | 'PREPARING' | 'READY' | 'PICKED_UP' | 'COMPLETED' | 'PENDING';

export interface VendorOrder {
  orderId: number;
  tokenNumber: string;
  status: OrderStatus;
  totalAmount: number;
  orderTime: string;
  user: { name: string; email: string };
  orderItems: OrderItem[];
}

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly API_BASE = '/api/orders';

  constructor(private http: HttpClient) {}

  getVendorOrders(vendorId: number): Observable<VendorOrder[]> {
    return this.http.get<VendorOrder[]>(`${this.API_BASE}/vendor/${vendorId}`);
  }

  updateOrderStatus(orderId: number, status: string): Observable<VendorOrder> {
    return this.http.put<VendorOrder>(
      `${this.API_BASE}/${orderId}/status?status=${status}`, {}
    );
  }
}