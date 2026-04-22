import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

/**
 * Thin service that lets the NavbarComponent tell the SidebarComponent
 * to open/close without them needing a direct @ViewChild reference
 * across the app shell boundary.
 */
@Injectable({ providedIn: 'root' })
export class LayoutService {
  private _toggle$ = new Subject<void>();
  readonly toggle$ = this._toggle$.asObservable();

  toggle(): void { this._toggle$.next(); }
}