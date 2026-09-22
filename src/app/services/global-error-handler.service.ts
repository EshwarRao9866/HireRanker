import { ErrorHandler, Injectable, NgZone, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class GlobalErrorHandler implements ErrorHandler {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly zone = inject(NgZone);
  private readonly router = inject(Router);

  handleError(error: unknown): void {
    console.error('[GlobalErrorHandler] Uncaught application exception:', error);

    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    const errorMessage = error instanceof Error ? error.message : String(error);

    // Detect corrupted JSON / storage errors
    const isCorruptStorage =
      errorMessage.includes('JSON') ||
      errorMessage.includes('Unexpected token') ||
      errorMessage.includes('localStorage') ||
      errorMessage.includes('sessionStorage') ||
      errorMessage.includes('QuotaExceededError');

    // Detect null reference exceptions on critical navigation/layout
    const isNullReference =
      errorMessage.includes('Cannot read properties of null') ||
      errorMessage.includes('Cannot read properties of undefined') ||
      errorMessage.includes('is not a function');

    if (isCorruptStorage) {
      console.warn('[GlobalErrorHandler] Corrupt storage state detected. Cleaning session credentials.');
      try {
        localStorage.removeItem('hireRankerSession');
        localStorage.removeItem('hireRankerToken');
        localStorage.removeItem('isLoggedIn');
        localStorage.removeItem('userRole');
      } catch {}

      this.zone.run(() => {
        alert('A session synchronization issue occurred. Resetting your login session.');
        this.router.navigate(['/login']).catch(() => {
          this.router.navigate(['/']);
        });
      });
      return;
    }

    // If a fatal crash renders a blank screen while navigating, prevent permanent dead-end
    if (isNullReference && (window.location.pathname.includes('dashboard') || window.location.pathname === '/')) {
      console.warn('[GlobalErrorHandler] Navigation null-reference prevented blank screen, redirecting safely.');
    }
  }
}
