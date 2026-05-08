import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withFetch } from '@angular/common/http';

import { routes } from './app.routes';

/**
 * Browser-only application config. SSR has been removed — see angular.json
 * (no server / outputMode / ssr keys) and main.ts which now bootstraps a
 * pure client-side Angular app. The `afterNextRender` callbacks scattered
 * across the app keep working — they just fire immediately now since there
 * is no server pass for them to defer past.
 */
export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withFetch()),
  ]
};
