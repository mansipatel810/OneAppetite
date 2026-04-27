import { Injectable, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { BehaviorSubject } from 'rxjs';

export type Theme = 'light' | 'dark';

const STORAGE_KEY = 'oa_theme';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private platformId = inject(PLATFORM_ID);
  private isBrowser  = isPlatformBrowser(this.platformId);

  private _theme$ = new BehaviorSubject<Theme>(this.readInitial());
  readonly theme$ = this._theme$.asObservable();

  constructor() {
    // Apply on construction so the very first paint already has the correct theme
    if (this.isBrowser) {
      this.apply(this._theme$.value);
    }
  }

  get current(): Theme { return this._theme$.value; }
  isDark(): boolean    { return this._theme$.value === 'dark'; }

  toggle(): void {
    this.set(this._theme$.value === 'dark' ? 'light' : 'dark');
  }

  set(theme: Theme): void {
    this._theme$.next(theme);
    if (!this.isBrowser) return;
    this.apply(theme);
    try { localStorage.setItem(STORAGE_KEY, theme); } catch { /* quota / private mode */ }
  }

  /** Choose initial value: persisted → OS preference → light. */
  private readInitial(): Theme {
    if (!this.isBrowser) return 'light';
    try {
      const saved = localStorage.getItem(STORAGE_KEY) as Theme | null;
      if (saved === 'dark' || saved === 'light') return saved;
    } catch { /* ignore */ }
    try {
      if (window.matchMedia?.('(prefers-color-scheme: dark)').matches) return 'dark';
    } catch { /* ignore */ }
    return 'light';
  }

  /** Flip the [data-theme] attribute on <html>. CSS overrides cascade from there. */
  private apply(theme: Theme): void {
    const root = document.documentElement;
    if (theme === 'dark') root.setAttribute('data-theme', 'dark');
    else                  root.removeAttribute('data-theme');

    // Update browser chrome color so the OS status bar matches
    const meta = document.querySelector('meta[name="theme-color"]');
    if (meta) {
      meta.setAttribute('content', theme === 'dark' ? '#0b1226' : '#2563eb');
    }
  }
}
