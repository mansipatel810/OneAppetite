import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface MenuItem {
  itemId: number;
  itemName: string;
  category: string;
  price: number;
  quantityAvailable: number;
  isInStock: boolean;
  dietaryType?: string;
  mealCourse?: string;
  description?: string;
  imageUrl?: string;
  minPrepTime?: number;
}

@Injectable({ providedIn: 'root' })
export class MenuService {
  private http    = inject(HttpClient);
  private baseUrl = 'http://localhost:8081';

  getMenuItems(vendorId: number): Observable<MenuItem[]> {
    return this.http.get<MenuItem[]>(`${this.baseUrl}/menu/vendor/${vendorId}`);
  }

  getVendorById(vendorId: number): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/api/vendor/${vendorId}`);
  }

  toggleStock(vendorId: number, itemId: number): Observable<MenuItem> {
    return this.http.put<MenuItem>(
      `/api/menu/vendor/${vendorId}/item/${itemId}/toggle-stock`, {}
    );
  }
}