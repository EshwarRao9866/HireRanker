import { inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const candidateGuard: CanActivateFn = () => {
  const platformId = inject(PLATFORM_ID);

  // On SSR (server prerendering), allow the initial page shell to load
  // Client-side hydration will check localStorage without prematurely redirecting
  if (!isPlatformBrowser(platformId)) {
    return true;
  }

  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isCandidate()) {
    return true;
  }

  // If logged in as admin, redirect to admin dashboard
  if (authService.isAdmin()) {
    const currentUrl = router.url.split('?')[0];
    if (currentUrl !== '/dashboard') {
      router.navigate(['/dashboard']);
    }
    return false;
  }

  // Not logged in, redirect to candidate login
  const currentUrl = router.url.split('?')[0];
  if (currentUrl !== '/candidate-login') {
    router.navigate(['/candidate-login']);
  }
  return false;
};
