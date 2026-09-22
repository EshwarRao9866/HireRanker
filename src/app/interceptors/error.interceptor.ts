import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { BackendHealthService } from '../services/backend-health.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const platformId = inject(PLATFORM_ID);
  const router = inject(Router);
  const healthService = inject(BackendHealthService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (isPlatformBrowser(platformId)) {
        if (error.status === 0) {
          // Connection refused / Network failure: Backend server is offline
          healthService.markOffline();
        } else if (error.status === 401) {
          // Token expired or invalid: clean session and redirect
          localStorage.removeItem('hireRankerToken');
          localStorage.removeItem('isLoggedIn');
          localStorage.removeItem('userRole');
          localStorage.removeItem('hireRankerSession');

          // Do not redirect if user was already on login/register pages
          const currentUrl = router.url;
          if (!currentUrl.includes('login') && !currentUrl.includes('register') && currentUrl !== '/') {
            const isCandidateRoute = currentUrl.includes('candidate') ||
                                     currentUrl.includes('my-resume') ||
                                     currentUrl.includes('my-profile') ||
                                     currentUrl.includes('my-applications') ||
                                     currentUrl.includes('interviews') ||
                                     currentUrl.includes('find-jobs');
            if (isCandidateRoute) {
              router.navigate(['/candidate-login']);
            } else {
              router.navigate(['/']);
            }
          }
        } else if (error.status === 403) {
          console.warn('[ErrorInterceptor] 403 Forbidden - Access denied for resource:', req.url);
        } else if (error.status === 503) {
          console.warn('[ErrorInterceptor] 503 Service Unavailable for resource:', req.url);
        } else if (error.status === 500) {
          console.warn('[ErrorInterceptor] 500 Internal Server Error for resource:', req.url);
        }
      }

      return throwError(() => error);
    })
  );
};
