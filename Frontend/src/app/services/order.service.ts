import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

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
  // Backend OrderController is mounted at /orders (no /api prefix).
  // In dev, apiBase is '' and proxy.conf.json forwards /orders → :8081.
  // In prod, apiBase is the deployed backend URL.
  private readonly API_BASE = `${environment.apiBase}/orders`;

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
