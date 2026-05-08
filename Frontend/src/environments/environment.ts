export const environment = {
  production: true,
  // Render-deployed Spring Boot backend. Frontend (Static Site) calls this
  // origin directly; CORS is whitelisted server-side via CORS_ALLOWED_ORIGINS.
  apiBase: 'https://oneappetite.onrender.com',
};
