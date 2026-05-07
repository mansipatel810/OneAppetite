import {
  Component, OnInit, OnDestroy, inject,
  ChangeDetectorRef, afterNextRender, HostListener, ElementRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription, interval } from 'rxjs';
import { AuthService, LoginResponse } from '../services/auth.service';
import { LayoutService } from '../services/layout.service';
import { SearchService } from '../services/search.service';
import { NotificationService, NotificationItem } from '../services/notification.service';
import { ThemeService, Theme } from '../services/theme.service';
import { ToastService } from '../services/toast.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent implements OnInit, OnDestroy {
  private authService   = inject(AuthService);
  private cdr           = inject(ChangeDetectorRef);
  private layoutService = inject(LayoutService);
  private searchSvc     = inject(SearchService);
  private notifications = inject(NotificationService);
  private themeSvc      = inject(ThemeService);
  private toast         = inject(ToastService);
  private host          = inject(ElementRef<HTMLElement>);

  user: LoginResponse | null = null;
  searchTerm = '';

  notifOpen = false;
  notifList: NotificationItem[] = [];
  unread = 0;
  marking = false;

  theme: Theme = 'light';

  private subs: Subscription[] = [];
  private pollSub?: Subscription;

  constructor() {
    afterNextRender(() => {
      this.user = this.authService.getSession();
      if (this.user?.userId) {
        this.notifications.load(this.user.userId).subscribe();
      }
      this.cdr.detectChanges();
    });
  }

  ngOnInit(): void {
    this.subs.push(
      this.searchSvc.term$.subscribe(v => {
        if (v !== this.searchTerm) {
          this.searchTerm = v;
          this.cdr.detectChanges();
        }
      }),
      this.notifications.list$.subscribe(list => {
        this.notifList = list;
        this.cdr.detectChanges();
      }),
      this.notifications.unread$.subscribe(n => {
        this.unread = n;
        this.cdr.detectChanges();
      }),
      this.themeSvc.theme$.subscribe(t => {
        this.theme = t;
        this.cdr.detectChanges();
      })
    );

    // Poll every 10s while logged in (was 30s — too slow for status flips)
    this.pollSub = interval(10000).subscribe(() => this.refetchIfLoggedIn());

    // Re-fetch immediately when the tab becomes visible / regains focus, so
    // a user who switches back to the tab sees the latest notifications
    // without waiting for the next poll tick.
    if (typeof window !== 'undefined') {
      this.boundFocus      = () => this.refetchIfLoggedIn();
      this.boundVisibility = () => {
        if (document.visibilityState === 'visible') this.refetchIfLoggedIn();
      };
      window.addEventListener('focus', this.boundFocus);
      document.addEventListener('visibilitychange', this.boundVisibility);
    }
  }

  private boundFocus?: () => void;
  private boundVisibility?: () => void;

  private refetchIfLoggedIn(): void {
    const session = this.authService.getSession();
    if (session?.userId) this.notifications.load(session.userId).subscribe();
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
    this.pollSub?.unsubscribe();
    if (typeof window !== 'undefined') {
      if (this.boundFocus)      window.removeEventListener('focus', this.boundFocus);
      if (this.boundVisibility) document.removeEventListener('visibilitychange', this.boundVisibility);
    }
  }

  hamburgerClick(): void { this.layoutService.toggle(); }

  toggleTheme(): void {
    this.themeSvc.toggle();
    const next = this.themeSvc.current;
    this.toast.success(next === 'dark' ? 'Theme updated to Dark Mode' : 'Theme updated to Light Mode');
  }

  onSearchInput(value: string): void {
    this.searchTerm = value;
    this.searchSvc.setTerm(value);
  }
  clearSearch(): void {
    this.searchTerm = '';
    this.searchSvc.clear();
  }

  /* ── Notification dropdown ───────────────────────────── */
  toggleNotifications(): void {
    this.notifOpen = !this.notifOpen;
    if (this.notifOpen && this.user?.userId) {
      this.notifications.load(this.user.userId).subscribe();
    }
  }

  closeNotifications(): void {
    this.notifOpen = false;
  }

  markAllRead(): void {
    if (!this.user?.userId || this.marking || this.unread === 0) return;
    this.marking = true;
    this.notifications.markAllRead(this.user.userId).subscribe({
      next: () => { this.marking = false; this.cdr.detectChanges(); },
      error: () => { this.marking = false; }
    });
  }

  topNotifications(): NotificationItem[] {
    return this.notifList.slice(0, 5);
  }

  formatRelative(iso: string): string {
    if (!iso) return '';
    const t = new Date(iso).getTime();
    const diff = Date.now() - t;
    if (Number.isNaN(diff)) return '';
    const m = Math.floor(diff / 60000);
    if (m < 1)   return 'Just now';
    if (m < 60)  return `${m}m ago`;
    const h = Math.floor(m / 60);
    if (h < 24)  return `${h}h ago`;
    const d = Math.floor(h / 24);
    return `${d}d ago`;
  }

  notifIcon(message: string): string {
    const m = message.toLowerCase();
    if (m.includes('ready'))                 return 'bi-check-circle-fill';
    if (m.includes('preparing'))             return 'bi-fire';
    if (m.includes('placed'))                return 'bi-bag-check-fill';
    if (m.includes('wallet') || m.includes('top'))  return 'bi-wallet2';
    if (m.includes('new order'))             return 'bi-bell-fill';
    return 'bi-info-circle-fill';
  }

  /** Close dropdown on outside click. */
  @HostListener('document:click', ['$event'])
  onDocumentClick(ev: MouseEvent): void {
    if (!this.notifOpen) return;
    if (!this.host.nativeElement.contains(ev.target as Node)) {
      this.notifOpen = false;
      this.cdr.detectChanges();
    }
  }

  /** Close on Escape. */
  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.notifOpen) {
      this.notifOpen = false;
      this.cdr.detectChanges();
    }
  }
}
