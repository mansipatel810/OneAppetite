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
   * Send the user's message to the campus food guide. Per the zero-data
   * privacy policy we DO NOT send userId — the backend operates as if every
   * caller is anonymous. We still send the role so the server can hard-block
   * ADMIN as a second line of defense (the chatbot is hidden in the UI for
   * admins too).
   */
  send(message: string, history: ChatTurn[]): Observable<{ reply: string }> {
    const session = this.auth.getSession();
    const body = {
      message,
      history,
      role: session?.role ?? null,
    };
    return this.http.post<{ reply: string }>(this.BASE, body);
  }
}
