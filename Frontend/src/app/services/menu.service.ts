import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

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
  /** Vendor metadata returned by GET /menu/vendor/{vendorId} (MenuItemResponse) */
  vendorId?: number;
  vendorName?: string;
  vendorDescription?: string;
  vendorType?: string;
}

export interface MenuItemPayload {
  itemName: string;
  category: string;
  mealCourse: string;
  dietaryType: string;
  price: number;
  quantityAvailable: number;
  isInStock: boolean;
  imageUrl?: string;
  minPrepTime?: number;
}

@Injectable({ providedIn: 'root' })
export class MenuService {
  private http    = inject(HttpClient);
  private baseUrl = environment.apiBase;

  getMenuItems(vendorId: number): Observable<MenuItem[]> {
    return this.http.get<MenuItem[]>(`${this.baseUrl}/menu/vendor/${vendorId}`);
  }

  getVendorById(vendorId: number): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/api/vendor/${vendorId}`);
  }

  createMenuItem(vendorId: number, payload: MenuItemPayload): Observable<MenuItem> {
    return this.http.post<MenuItem>(`${this.baseUrl}/menu/vendor/${vendorId}`, payload);
  }

  updateMenuItem(vendorId: number, itemId: number, payload: MenuItemPayload): Observable<MenuItem> {
    return this.http.put<MenuItem>(`${this.baseUrl}/menu/vendor/${vendorId}/item/${itemId}`, payload);
  }

  deleteMenuItem(vendorId: number, itemId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/menu/vendor/${vendorId}/item/${itemId}`);
  }

  toggleStock(vendorId: number, itemId: number, inStock: boolean): Observable<MenuItem> {
    return this.http.put<MenuItem>(
      `${this.baseUrl}/menu/vendor/${vendorId}/item/${itemId}/stock?inStock=${inStock}`, {}
    );
  }
}
