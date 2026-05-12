import { Component, inject } from '@angular/core';
import { RouterOutlet, NavigationEnd, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { filter } from 'rxjs/operators';
import { ToastHostComponent } from './toast/toast-host.component';
import { ChatbotComponent }   from './chatbot/chatbot.component';
import { AuthService }        from './services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, ToastHostComponent, ChatbotComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  private auth   = inject(AuthService);
  private router = inject(Router);

  /**
   * Show the floating chatbot on every authenticated page (employee,
   * vendor, admin). Hidden on /login and /register where there's no
   * session. We re-evaluate on every route change so logout immediately
   * hides the widget.
   */
  showChatbot = false;

  constructor() {
    this.updateChatbotVisibility();
    this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe(() => this.updateChatbotVisibility());
  }

  private updateChatbotVisibility(): void {
    const url = this.router.url.toLowerCase();
    const isAuthPage = url.startsWith('/login') || url.startsWith('/register');
    this.showChatbot = this.auth.isLoggedIn() && !isAuthPage;
  }
}
