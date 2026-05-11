import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

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
  private readonly BASE = `${environment.apiBase}/api/chat`;

  send(message: string, history: ChatTurn[]): Observable<{ reply: string }> {
    return this.http.post<{ reply: string }>(this.BASE, { message, history });
  }
}
