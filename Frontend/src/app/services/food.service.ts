// src/app/services/food.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class FoodService {
  constructor(private http: HttpClient) {}

  getCities(): Observable<any[]> {
    return this.http.get<any[]>('/api/v1/cities');
  }

  getCampuses(cityId: number): Observable<any[]> {
    return this.http.get<any[]>(`/api/v1/campuses/city/${cityId}`);
  }

  getBuildings(campusId: number): Observable<any[]> {
    return this.http.get<any[]>(`/api/v1/buildings/${campusId}`);
  }

  getVendors(buildingId: number): Observable<any[]> {
    return this.http.get<any[]>(`/vendors/building/${buildingId}`);
  }
}