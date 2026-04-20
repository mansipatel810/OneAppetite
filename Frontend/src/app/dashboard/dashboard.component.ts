import {
  Component,
  OnInit,
  ChangeDetectorRef,
  afterNextRender,
  inject
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService, LoginResponse } from '../services/auth.service';
import { FoodService } from '../services/food.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  private authService = inject(AuthService);
  private foodService = inject(FoodService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  user: LoginResponse | null = null;
  cities: any[] = [];
  campuses: any[] = [];
  buildings: any[] = [];
  vendors: any[] = [];

  selectedBuildingId: number | null = null;
  currentBuilding: string = '';
  isSearching: boolean = false;
  mobileMenuOpen: boolean = false;

  constructor() {
    afterNextRender(() => {
      setTimeout(() => {
        this.user = this.authService.getSession();
        if (!this.user) {
          this.router.navigate(['/login']);
          return;
        }
        this.loadCities();
      }, 0);
    });
  }

  ngOnInit(): void {}

  loadCities() {
    this.foodService.getCities().subscribe({
      next: (data) => {
        this.cities = data;
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Failed to load cities', err)
    });
  }

  onCityChange(event: any) {
    const cityId = event.target.value;
    this.resetSelection(true, true, true);
    if (cityId) {
      this.foodService.getCampuses(+cityId).subscribe(data => {
        this.campuses = data;
        this.cdr.detectChanges();
      });
    }
  }

  onCampusChange(event: any) {
    const campusId = event.target.value;
    this.resetSelection(false, true, true);
    if (campusId) {
      this.foodService.getBuildings(+campusId).subscribe(data => {
        this.buildings = data;
        this.cdr.detectChanges();
      });
    }
  }

  onBuildingChange(event: any) {
    const id = event.target.value;
    this.selectedBuildingId = id ? Number(id) : null;
    this.vendors = [];
    this.cdr.detectChanges();
  }

  /** Clicking a building card directly selects it */
  selectBuilding(building: any) {
    this.selectedBuildingId = Number(building.buildingId || building.building_id);
    this.currentBuilding = building.buildingName || building.building_name;
    this.vendors = [];
    this.cdr.detectChanges();
  }

  searchVendors() {
    if (!this.selectedBuildingId) return;
    this.isSearching = true;
    this.foodService.getVendors(this.selectedBuildingId).subscribe({
      next: (data) => {
        this.vendors = data;
        this.isSearching = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Vendor search error:', err);
        this.isSearching = false;
        this.cdr.detectChanges();
      }
    });
  }

  /** Go back from vendor grid to building selection */
  resetVendors() {
    this.vendors = [];
    this.cdr.detectChanges();
  }

  /** Mobile sidebar toggle */
  toggleMobileMenu() {
    this.mobileMenuOpen = !this.mobileMenuOpen;
    this.cdr.detectChanges();
  }

  /** Close sidebar (called on nav link click) */
  closeMobileMenu() {
    this.mobileMenuOpen = false;
    this.cdr.detectChanges();
  }

  private resetSelection(campus: boolean, building: boolean, vend: boolean) {
    if (campus) this.campuses = [];
    if (building) {
      this.buildings = [];
      this.selectedBuildingId = null;
      this.currentBuilding = '';
    }
    if (vend) this.vendors = [];
    this.cdr.detectChanges();
  }

  logout(): void {
    this.authService.clearSession();
    this.router.navigate(['/login']);
  }
}
