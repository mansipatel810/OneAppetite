// src/app/services/food.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

/** Menu item shape returned by GET /menu/vendor/{vendorId}. Mirrors MenuItemResponse.java. */
export interface MenuItemResponse {
  itemId: number;
  itemName: string;
  category: string;
  mealCourse: string;      // "Breakfast" | "Lunch" | "Dinner" | other
  dietaryType: string;     // "VEG" | "NON_VEG" etc.
  price: number;
  quantityAvailable: number;
  isInStock: boolean;
  imageUrl: string | null;
  vendorId: number;
  vendorName: string;
  vendorDescription: string;
  vendorType: string;
}

@Injectable({ providedIn: 'root' })
export class FoodService {
  private readonly API = environment.apiBase;

  constructor(private http: HttpClient) {}

  getCities(): Observable<any[]> {
    return this.http.get<any[]>(`${this.API}/api/v1/cities`);
  }

  getCampuses(cityId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.API}/api/v1/campuses/city/${cityId}`);
  }

  getBuildings(campusId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.API}/api/v1/buildings/${campusId}`);
  }

  getVendors(buildingId: number): Observable<any[]> {
    const url = `${this.API}/vendors/building/${buildingId}`;
    console.log('[FoodService] GET', url, '(buildingId type:', typeof buildingId, ')');
    return this.http.get<any[]>(url);
  }

  /** Menu items for a single vendor. Grouped client-side by meal course. */
  getMenuItemsByVendor(vendorId: number): Observable<MenuItemResponse[]> {
    return this.http.get<MenuItemResponse[]>(`${this.API}/menu/vendor/${vendorId}`);
  }
}
