import {
  Component, OnInit, inject,
  ChangeDetectorRef, afterNextRender
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService, LoginResponse } from '../services/auth.service';
import { LayoutService } from '../services/layout.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent implements OnInit {
  private authService   = inject(AuthService);
  private cdr           = inject(ChangeDetectorRef);
  private layoutService = inject(LayoutService);

  user: LoginResponse | null = null;

  constructor() {
    afterNextRender(() => {
      this.user = this.authService.getSession();
      this.cdr.detectChanges();
    });
  }

  ngOnInit(): void {}

  hamburgerClick(): void { this.layoutService.toggle(); }
}