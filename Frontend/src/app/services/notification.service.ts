import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface NotificationItem {
  id: number;
  userId: number;
  message: string;
  timestamp: string;
  isRead: boolean;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private http = inject(HttpClient);
  private readonly BASE = `${environment.apiBase}/api/notifications`;

  private _list$ = new BehaviorSubject<NotificationItem[]>([]);
  readonly list$ = this._list$.asObservable();

  private _unread$ = new BehaviorSubject<number>(0);
  readonly unread$ = this._unread$.asObservable();

  /** Replace internal cache with the latest 20 from the server. */
  load(userId: number): Observable<NotificationItem[]> {
    return this.http.get<NotificationItem[]>(`${this.BASE}/${userId}`).pipe(
      tap(list => {
        const sorted = [...list].sort(
          (a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
        );
        this._list$.next(sorted);
        this._unread$.next(sorted.filter(n => !n.isRead).length);
      }),
      catchError(() => {
        this._list$.next([]);
        this._unread$.next(0);
        return of([] as NotificationItem[]);
      })
    );
  }

  markAllRead(userId: number): Observable<{ updated: number }> {
    return this.http
      .put<{ updated: number }>(`${this.BASE}/${userId}/mark-all-read`, {})
      .pipe(
        tap(() => {
          const next = this._list$.value.map(n => ({ ...n, isRead: true }));
          this._list$.next(next);
          this._unread$.next(0);
        }),
        catchError(() => of({ updated: 0 }))
      );
  }

  /** Take the latest N items (default 5). */
  recent(n = 5): NotificationItem[] {
    return this._list$.value.slice(0, n);
  }
}
