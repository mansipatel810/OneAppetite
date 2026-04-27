import { Injectable, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { BehaviorSubject } from 'rxjs';

export interface SavedLocation {
  cityId: number;
  cityName: string;
  campusId: number;
  campusName: string;
  buildingId: number;
  buildingName: string;
}

const STORAGE_KEY = 'oa_location';

@Injectable({ providedIn: 'root' })
export class LocationService {
  private platformId = inject(PLATFORM_ID);
  private isBrowser  = isPlatformBrowser(this.platformId);

  private _location$ = new BehaviorSubject<SavedLocation | null>(this.read());
  /** Subscribe in any component to react to location changes. */
  readonly location$ = this._location$.asObservable();

  /** Synchronous read. Returns null if nothing saved yet. */
  get current(): SavedLocation | null {
    return this._location$.value;
  }

  hasLocation(): boolean {
    return this._location$.value !== null;
  }

  /** Persist a new location and broadcast it. */
  set(loc: SavedLocation): void {
    this._location$.next(loc);
    if (!this.isBrowser) return;
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(loc));
    } catch { /* quota / private mode — ignore */ }
  }

  /** Wipe the saved location (forces selection screen on next dashboard visit). */
  clear(): void {
    this._location$.next(null);
    if (!this.isBrowser) return;
    try { localStorage.removeItem(STORAGE_KEY); } catch { /* ignore */ }
  }

  /** Load from localStorage on construction. */
  private read(): SavedLocation | null {
    if (!this.isBrowser) return null;
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) return null;
      const parsed = JSON.parse(raw) as SavedLocation;
      if (parsed?.buildingId && parsed?.buildingName) return parsed;
      return null;
    } catch {
      return null;
    }
  }
}
