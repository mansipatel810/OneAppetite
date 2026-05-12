import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';

export interface ChatTurn {
  role: 'user' | 'model';
  text: string;
}

export interface ChatMessage {
  from: 'user' | 'bot';
  text: string;
  loading?: boolean;
}

@Injectable({ providedIn: 'root' })
export class ChatService {
  private http = inject(HttpClient);
  private auth = inject(AuthService);
  private readonly BASE = `${environment.apiBase}/api/chat`;

  /**
   * Send the user's message to the chatbot. We attach the logged-in user's
   * role + userId so the backend can scope SQL queries appropriately:
   *   - VENDOR: their own sales / menu / orders
   *   - ADMIN: cross-cutting analytics on users/vendors/orders
   *   - EMPLOYEE: their own cart / orders / menu browsing
   */
  send(message: string, history: ChatTurn[]): Observable<{ reply: string }> {
    const session = this.auth.getSession();
    const body = {
      message,
      history,
      role: session?.role ?? null,
      userId: session?.userId ?? null,
    };
    return this.http.post<{ reply: string }>(this.BASE, body);
  }
}
