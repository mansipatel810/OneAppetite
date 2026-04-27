import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { CartResponseDTO } from './cart.service';

@Injectable({ providedIn: 'root' })
export class OrderHistoryService {
  private http = inject(HttpClient);
  private readonly BASE = `${environment.apiBase}/api/cart`;

  getHistory(userId: number): Observable<CartResponseDTO[]> {
    return this.http.get<CartResponseDTO[]>(`${this.BASE}/history/${userId}`);
  }
}
