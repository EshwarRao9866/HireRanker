import { Injectable, signal, computed, inject, PLATFORM_ID, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { isPlatformBrowser } from '@angular/common';
import { environment } from '../../environments/environment';
import { catchError, of, timeout } from 'rxjs';

export interface BackendHealthInfo {
  status: 'UP' | 'DEGRADED' | 'DOWN' | 'UNKNOWN';
  backend: 'CONNECTED' | 'DISCONNECTED';
  service?: string;
  database?: 'UP' | 'DOWN' | 'UNKNOWN';
  port?: number;
  message?: string;
  timestamp?: string;
  version?: string;
}

@Injectable({
  providedIn: 'root'
})
export class BackendHealthService {
  private readonly http = inject(HttpClient);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly ngZone = inject(NgZone);

  // Reactive state signals
  readonly isOnline = signal<boolean>(false);
  readonly isChecking = signal<boolean>(false);
  readonly databaseStatus = signal<'UP' | 'DOWN' | 'UNKNOWN'>('UNKNOWN');
  readonly healthDetails = signal<BackendHealthInfo | null>(null);
  readonly lastChecked = signal<Date | null>(null);

  // Derived state
  readonly statusLabel = computed<'ONLINE' | 'DEGRADED' | 'OFFLINE'>(() => {
    if (!this.isOnline()) return 'OFFLINE';
    if (this.databaseStatus() === 'DOWN') return 'DEGRADED';
    return 'ONLINE';
  });

  private pollTimerId: any = null;
  private isRequestInFlight = false;
  private consecutiveFailures = 0;

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      if ((environment as any).useBackend === false || (environment as any).useMockData === true) {
        // Standalone Mock Mode: Mark offline gracefully without polling or emitting network errors
        this.isOnline.set(false);
        this.databaseStatus.set('UNKNOWN');
        return;
      }
      // Run initial check
      this.checkHealthNow();
      // Start adaptive periodic check
      this.scheduleNextCheck();
    }
  }

  /**
   * Immediately triggers a single health check if one is not already in flight.
   */
  checkHealthNow(): void {
    if (!isPlatformBrowser(this.platformId) || this.isRequestInFlight) {
      return;
    }

    if ((environment as any).useBackend === false || (environment as any).useMockData === true) {
      this.isOnline.set(false);
      this.isChecking.set(false);
      return;
    }

    this.isRequestInFlight = true;
    this.isChecking.set(true);

    const healthUrl = `${environment.apiUrl}/health`;

    this.http.get<BackendHealthInfo>(healthUrl)
      .pipe(
        timeout(4000),
        catchError((err) => {
          // Status 0: Network error / Connection Refused (Backend not running or port closed)
          // Status 503: Backend running but DB degraded
          if (err && err.status === 503 && err.error && err.error.database === 'DOWN') {
            return of(err.error as BackendHealthInfo);
          }
          return of(null);
        })
      )
      .subscribe({
        next: (res) => {
          this.isRequestInFlight = false;
          this.isChecking.set(false);
          this.lastChecked.set(new Date());

          if (res && (res.status === 'UP' || res.backend === 'CONNECTED')) {
            const wasOffline = !this.isOnline();
            this.consecutiveFailures = 0;
            this.isOnline.set(true);
            this.databaseStatus.set(res.database || 'UP');
            this.healthDetails.set(res);

            if (wasOffline) {
              console.info('[BackendHealthService] HireRanker Backend connection established (ONLINE).');
            }
          } else if (res && res.status === 'DEGRADED') {
            this.consecutiveFailures = 0;
            this.isOnline.set(true);
            this.databaseStatus.set('DOWN');
            this.healthDetails.set(res);
            console.warn('[BackendHealthService] HireRanker Backend is running, but MySQL database is unreachable.');
          } else {
            // Connection failed
            this.consecutiveFailures++;
            if (this.isOnline()) {
              console.warn('[BackendHealthService] HireRanker Backend is offline (Connection Refused).');
            }
            this.isOnline.set(false);
            this.databaseStatus.set('UNKNOWN');
            this.healthDetails.set(null);
          }

          this.scheduleNextCheck();
        },
        error: () => {
          this.isRequestInFlight = false;
          this.isChecking.set(false);
          this.consecutiveFailures++;
          this.isOnline.set(false);
          this.databaseStatus.set('UNKNOWN');
          this.healthDetails.set(null);
          this.scheduleNextCheck();
        }
      });
  }

  /**
   * Schedules next health check with adaptive frequency:
   * - When Online: Poll every 30 seconds (low overhead).
   * - When Offline: Poll gently every 15 seconds to detect backend start without spamming console.
   */
  private scheduleNextCheck(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    if (this.pollTimerId) {
      clearTimeout(this.pollTimerId);
      this.pollTimerId = null;
    }

    // Run outside Angular change detection zone to avoid triggering continuous change detection cycles
    this.ngZone.runOutsideAngular(() => {
      const intervalMs = this.isOnline() ? 30000 : 15000;
      this.pollTimerId = setTimeout(() => {
        this.ngZone.run(() => {
          this.checkHealthNow();
        });
      }, intervalMs);
    });
  }

  /**
   * Called by HTTP error interceptor when an API call fails with status === 0 (connection refused).
   * Instantly marks backend offline without waiting for the next periodic health check.
   */
  markOffline(): void {
    if (this.isOnline()) {
      console.warn('[BackendHealthService] Detected connection drop, marking backend OFFLINE.');
      this.isOnline.set(false);
      this.databaseStatus.set('UNKNOWN');
      this.healthDetails.set(null);
      // Immediately schedule a check to attempt fast recovery
      this.scheduleNextCheck();
    }
  }

  /**
   * Called when an API call succeeds to ensure backend is marked online.
   */
  markOnline(): void {
    if (!this.isOnline()) {
      this.isOnline.set(true);
    }
  }
}
