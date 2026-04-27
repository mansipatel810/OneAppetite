import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class SearchService {
  private _term$ = new BehaviorSubject<string>('');
  readonly term$ = this._term$.asObservable();

  setTerm(value: string): void {
    this._term$.next(value ?? '');
  }

  clear(): void {
    this._term$.next('');
  }

  get current(): string { return this._term$.value; }
}
