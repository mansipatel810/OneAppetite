import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface UserSettings {
  userId: number;
  name: string;
  email: string;
  phone: string;
  walletBalance: number;
  dietaryPreference: string | null;
  notificationsEnabled: boolean;
}

@Injectable({ providedIn: 'root' })
export class UserSettingsService {
  private readonly API_BASE = '/api/users';

  constructor(private http: HttpClient) {}

  getSettings(userId: number): Observable<UserSettings> {
    return this.http.get<UserSettings>(`${this.API_BASE}/${userId}/settings`);
  }

  updateSettings(userId: number, settings: Partial<UserSettings>): Observable<UserSettings> {
    return this.http.put<UserSettings>(`${this.API_BASE}/${userId}/settings`, settings);
  }

  topUpWallet(userId: number, amount: number): Observable<UserSettings> {
    return this.http.put<UserSettings>(
      `${this.API_BASE}/${userId}/wallet/topup?amount=${amount}`, {}
    );
  }

  getFavorites(userId: number): Observable<any[]> {
    return this.http.get<any[]>(`/api/users/${userId}/favorites`);
  }

  addFavorite(userId: number, itemId: number): Observable<string> {
    return this.http.post(`/api/users/${userId}/favorites/${itemId}`, {}, { responseType: 'text' });
  }

  removeFavorite(userId: number, itemId: number): Observable<string> {
    return this.http.delete(`/api/users/${userId}/favorites/${itemId}`, { responseType: 'text' });
  }
}