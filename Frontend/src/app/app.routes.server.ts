import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  // Dynamic route — rendered on the client so we don't need to know
  // every vendor id at build time.
  {
    path: 'vendor/:id',
    renderMode: RenderMode.Client,
  },

  // Everything else: prerender at build time (default behaviour).
  {
    path: '**',
    renderMode: RenderMode.Prerender,
  },
];
