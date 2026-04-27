import {
  Component,
  OnInit,
  OnDestroy,
  ChangeDetectorRef,
  afterNextRender,
  inject
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService, LoginResponse } from '../services/auth.service';
import { FoodService } from '../services/food.service';
import { LocationService, SavedLocation } from '../services/location.service';
import { ToastService } from '../services/toast.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit, OnDestroy {
  private authService = inject(AuthService);
  private foodService = inject(FoodService);
  private locationSvc = inject(LocationService);
  private toast       = inject(ToastService);
  private router      = inject(Router);
  private route       = inject(ActivatedRoute);
  private cdr         = inject(ChangeDetectorRef);

  user: LoginResponse | null = null;
  cities: any[]    = [];
  campuses: any[]  = [];
  buildings: any[] = [];
  vendors: any[]   = [];

  selectedCityId: number | null = null;
  selectedCityName = '';
  selectedCampusId: number | null = null;
  selectedCampusName = '';
  selectedBuildingId: number | null = null;
  currentBuilding: string = '';
  isSearching = false;

  /** When true, the City→Campus→Building selector is shown.
   *  When false, we jump straight to the vendor list using a saved location. */
  selectionMode = true;

  /** ALL | VEG | NON_VEG — filters vendors by their declared vendorType */
  dietaryFilter: 'ALL' | 'VEG' | 'NON_VEG' = 'ALL';
  /** Local search term within the vendor grid */
  vendorSearch = '';

  private querySub?: Subscription;

  constructor() {
    afterNextRender(() => {
      setTimeout(() => {
        this.user = this.authService.getSession();
        if (!this.user) {
          this.router.navigate(['/login']);
          return;
        }

        // Smart navigation: ?change=1 forces the selector,
        // otherwise jump to vendor list if we have a saved location.
        const change = this.route.snapshot.queryParamMap.get('change') === '1';
        const saved  = this.locationSvc.current;

        if (saved && !change) {
          this.applySaved(saved);
          this.searchVendors();
        } else {
          this.selectionMode = true;
          this.loadCities();
        }
        this.cdr.detectChanges();
      }, 0);
    });
  }

  ngOnInit(): void {
    // React to query-param changes (e.g. user clicks "Change Location"
    // from the sidebar while already on /dashboard).
    this.querySub = this.route.queryParamMap.subscribe(params => {
      if (params.get('change') === '1') {
        this.openSelector();
      }
    });
  }

  ngOnDestroy(): void {
    this.querySub?.unsubscribe();
  }

  private applySaved(loc: SavedLocation): void {
    this.selectedCityId      = loc.cityId;
    this.selectedCityName    = loc.cityName;
    this.selectedCampusId    = loc.campusId;
    this.selectedCampusName  = loc.campusName;
    this.selectedBuildingId  = loc.buildingId;
    this.currentBuilding     = loc.buildingName;
    this.selectionMode       = false;
  }

  /* ── Selectors ─────────────────────────────────────────── */
  loadCities() {
    this.foodService.getCities().subscribe({
      next: (data) => { this.cities = data; this.cdr.detectChanges(); },
      error: ()    => console.error('Failed to load cities')
    });
  }

  onCityChange(event: any) {
    const cityId = event.target.value;
    const cityOption = event.target.options[event.target.selectedIndex];
    this.selectedCityName = cityOption?.text || '';
    this.selectedCityId   = cityId ? +cityId : null;
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
    const campusOption = event.target.options[event.target.selectedIndex];
    this.selectedCampusName = campusOption?.text || '';
    this.selectedCampusId   = campusId ? +campusId : null;
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
    const opt = event.target.options[event.target.selectedIndex];
    this.currentBuilding = opt?.text || '';
    this.vendors = [];
    this.cdr.detectChanges();
  }

  selectBuilding(building: any) {
    this.selectedBuildingId = Number(building.buildingId || building.building_id);
    this.currentBuilding    = building.buildingName || building.building_name;
    this.vendors = [];
    this.cdr.detectChanges();
  }

  searchVendors() {
    console.log('[Dashboard] searchVendors() — selectedBuildingId =',
                this.selectedBuildingId,
                '| building =', this.currentBuilding,
                '| city =', this.selectedCityName,
                '| campus =', this.selectedCampusName);

    if (!this.selectedBuildingId) {
      console.warn('[Dashboard] No buildingId — aborting vendor search');
      return;
    }
    this.isSearching = true;
    this.foodService.getVendors(this.selectedBuildingId).subscribe({
      next: (data) => {
        console.log('[Dashboard] vendor API returned', data?.length ?? 0, 'item(s)', data);

        this.vendors      = data ?? [];
        this.isSearching  = false;
        this.selectionMode = false;

        // Persist the location whenever the user successfully lands on a vendor list.
        if (this.selectedCityId && this.selectedCampusId && this.selectedBuildingId) {
          this.locationSvc.set({
            cityId:       this.selectedCityId,
            cityName:     this.selectedCityName,
            campusId:     this.selectedCampusId,
            campusName:   this.selectedCampusName,
            buildingId:   this.selectedBuildingId,
            buildingName: this.currentBuilding,
          });
        }

        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('[Dashboard] vendor API error', err);
        this.isSearching = false;
        this.toast.error("Couldn't load vendors for this building.");
        this.cdr.detectChanges();
      }
    });
  }

  /** "Change Location" — clears state and re-opens the selector. */
  openSelector(): void {
    this.locationSvc.clear();
    this.selectionMode = true;
    this.vendors = [];
    this.campuses = [];
    this.buildings = [];
    this.selectedCityId = null;
    this.selectedCityName = '';
    this.selectedCampusId = null;
    this.selectedCampusName = '';
    this.selectedBuildingId = null;
    this.currentBuilding = '';
    this.dietaryFilter = 'ALL';
    this.vendorSearch = '';
    this.loadCities();
    this.cdr.detectChanges();
  }

  /** Used by the in-page back button on the vendor list. */
  resetVendors() {
    this.openSelector();
  }

  setDietaryFilter(f: 'ALL' | 'VEG' | 'NON_VEG'): void {
    this.dietaryFilter = f;
    this.cdr.detectChanges();
  }

  onVendorSearch(event: Event): void {
    this.vendorSearch = (event.target as HTMLInputElement).value;
    this.cdr.detectChanges();
  }

  /** Vendors after combined dietary + name search filters */
  get visibleVendors(): any[] {
    const term = this.vendorSearch.trim().toLowerCase();
    return this.vendors.filter(v => {
      const vt = (v.vendorType || '').toUpperCase().replace('-', '_');
      if (this.dietaryFilter === 'VEG' && (!vt.includes('VEG') || vt.includes('NON'))) return false;
      if (this.dietaryFilter === 'NON_VEG' && !vt.includes('NON')) return false;
      if (!term) return true;
      const name = (v.vendorName || v.name || '').toLowerCase();
      const desc = (v.vendorDescription || '').toLowerCase();
      return name.includes(term) || desc.includes(term);
    });
  }

  private resetSelection(campus: boolean, building: boolean, vend: boolean) {
    if (campus)   { this.campuses = []; this.selectedCampusId = null; this.selectedCampusName = ''; }
    if (building) { this.buildings = []; this.selectedBuildingId = null; this.currentBuilding = ''; }
    if (vend)     { this.vendors = []; }
    this.cdr.detectChanges();
  }

  logout(): void {
    this.authService.clearSession();
    this.router.navigate(['/login']);
  }
}
