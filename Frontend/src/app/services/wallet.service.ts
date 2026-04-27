import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface WalletResponse {
  userId: number;
  walletBalance: number;
}

export interface TopUpPayload {
  amount: number;
  upiId: string;
}

@Injectable({ providedIn: 'root' })
export class WalletService {
  private http = inject(HttpClient);
  private readonly BASE = `${environment.apiBase}/api/wallet`;

  getBalance(userId: number): Observable<WalletResponse> {
    return this.http.get<WalletResponse>(`${this.BASE}/${userId}`);
  }

  topUp(userId: number, payload: TopUpPayload): Observable<WalletResponse> {
    return this.http.post<WalletResponse>(`${this.BASE}/${userId}/topup`, payload);
  }
}
