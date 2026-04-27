import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export type ToastKind = 'success' | 'error' | 'info' | 'warning';

export interface Toast {
  id: number;
  message: string;
  kind: ToastKind;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private nextId = 1;
  private _toasts$ = new BehaviorSubject<Toast[]>([]);
  readonly toasts$ = this._toasts$.asObservable();

  success(message: string, durationMs = 3000): void { this.show(message, 'success', durationMs); }
  error(message: string,   durationMs = 4000): void { this.show(message, 'error',   durationMs); }
  info(message: string,    durationMs = 3000): void { this.show(message, 'info',    durationMs); }
  warning(message: string, durationMs = 3500): void { this.show(message, 'warning', durationMs); }

  private show(message: string, kind: ToastKind, durationMs: number): void {
    const toast: Toast = { id: this.nextId++, message, kind };
    this._toasts$.next([...this._toasts$.value, toast]);
    setTimeout(() => this.dismiss(toast.id), durationMs);
  }

  dismiss(id: number): void {
    this._toasts$.next(this._toasts$.value.filter(t => t.id !== id));
  }
}
