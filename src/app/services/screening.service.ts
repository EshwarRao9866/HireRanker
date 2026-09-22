import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, timeout, catchError, of } from 'rxjs';
import { environment } from '../../environments/environment';
import { MockDataService } from './mock-data.service';

export interface ScreeningResultResponse {
  id: number;
  applicationId: number;
  overallScore: number;
  skillsScore: number;
  experienceScore: number;
  educationScore: number;
  keywordScore?: number;
  projectScore?: number;
  certificationScore?: number;
  formattingScore?: number;
  achievementScore?: number;
  matchingSkills?: string;
  missingSkills?: string;
  recommendedSkills?: string;
  strengths?: string;
  weaknesses?: string;
  improvementSuggestions?: string;
  resumeSummary?: string;
  recommendation?: string;
  screenedAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class ScreeningService {
  private readonly apiUrl = `${environment.apiUrl}/screenings`;

  constructor(
    private readonly http: HttpClient,
    private readonly mockDataService: MockDataService
  ) {}

  /**
   * Trigger AI screening for an application (POST /api/screenings/{applicationId}?force=true)
   * With resilient catchError fallback when offline or status 0 / timeout.
   */
  screenResume(applicationId: number, force: boolean = true): Observable<ScreeningResultResponse> {
    if ((environment as any).useMockData || (environment as any).useBackend === false) {
      return of(this.mockDataService.getMockScreeningResult(applicationId));
    }

    return this.http.post<ScreeningResultResponse>(`${this.apiUrl}/${applicationId}?force=${force}`, {}).pipe(
      timeout(15000),
      catchError(() => {
        return of(this.mockDataService.getMockScreeningResult(applicationId));
      })
    );
  }

  /**
   * Get screening results for an application (GET /api/screenings/application/{applicationId})
   */
  getScreeningResult(applicationId: number): Observable<ScreeningResultResponse> {
    if ((environment as any).useMockData || (environment as any).useBackend === false) {
      return of(this.mockDataService.getMockScreeningResult(applicationId));
    }

    return this.http.get<ScreeningResultResponse>(`${this.apiUrl}/application/${applicationId}`).pipe(
      timeout(6000),
      catchError(() => {
        return of(this.mockDataService.getMockScreeningResult(applicationId));
      })
    );
  }
}
