import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface UserProfile {
  userId: number;
  name: string;
  email: string;
  phone: string;
  role: 'EMPLOYEE' | 'VENDOR' | 'ADMIN';
  isActive: boolean;
  walletBalance: number;
  notificationsEnabled: boolean;
  buildingId: number | null;
  buildingName: string | null;
}

export interface UpdateProfilePayload {
  name?: string;
  phone?: string;
}

export interface ChangePasswordPayload {
  currentPassword: string;
  newPassword: string;
}

export interface VendorLocationBuilding {
  buildingId: number;
  buildingName: string;
  campusId: number;
  campusName: string;
  cityId: number;
  cityName: string;
}

export interface VendorLocationsResponse {
  vendorId: number;
  primaryBuilding: VendorLocationBuilding | null;
  additionalBuildings: VendorLocationBuilding[];
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private http = inject(HttpClient);
  private readonly BASE = `${environment.apiBase}/api/users`;

  getProfile(userId: number): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.BASE}/${userId}`);
  }

  updateProfile(userId: number, payload: UpdateProfilePayload): Observable<UserProfile> {
    return this.http.put<UserProfile>(`${this.BASE}/${userId}`, payload);
  }

  changePassword(userId: number, payload: ChangePasswordPayload): Observable<{ message: string }> {
    return this.http.put<{ message: string }>(`${this.BASE}/${userId}/password`, payload);
  }

  setNotifications(userId: number, enabled: boolean): Observable<UserProfile> {
    return this.http.put<UserProfile>(
      `${this.BASE}/${userId}/notifications?enabled=${enabled}`, {}
    );
  }

  /* ── Vendor stall locations ───────────────────────────── */

  getVendorLocations(vendorId: number): Observable<VendorLocationsResponse> {
    return this.http.get<VendorLocationsResponse>(`${this.BASE}/${vendorId}/locations`);
  }

  updateVendorLocations(vendorId: number, buildingIds: number[]): Observable<VendorLocationsResponse> {
    return this.http.put<VendorLocationsResponse>(
      `${this.BASE}/${vendorId}/locations`,
      { buildingIds }
    );
  }
}
