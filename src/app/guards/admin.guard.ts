import { inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const adminGuard: CanActivateFn = () => {
  const platformId = inject(PLATFORM_ID);
  
  // On SSR (server prerendering), allow the initial page shell to load
  // Client-side hydration will check localStorage without prematurely redirecting
  if (!isPlatformBrowser(platformId)) {
    return true;
  }

  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAdmin()) {
    return true;
  }

  // If logged in as candidate, redirect to candidate dashboard
  if (authService.isCandidate()) {
    router.navigate(['/candidate-dashboard']);
    return false;
  }

  // Not logged in, redirect to admin login
  router.navigate(['/']);
  return false;
};
