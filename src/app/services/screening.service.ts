import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, timeout, catchError, throwError } from 'rxjs';
import { environment } from '../../environments/environment';

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

  constructor(private readonly http: HttpClient) {}

  /**
   * Trigger AI screening for an application (POST /api/screenings/{applicationId}?force=true)
   * Enforces a 15-second timeout to prevent indefinite pending states.
   */
  screenResume(applicationId: number, force: boolean = true): Observable<ScreeningResultResponse> {
    return this.http.post<ScreeningResultResponse>(`${this.apiUrl}/${applicationId}?force=${force}`, {}).pipe(
      timeout({
        each: 30000,
        with: () => throwError(() => new Error('Resume analysis timed out. Please try again.'))
      }),
      catchError((err) => {
        if (err?.message?.includes('timed out')) {
          return throwError(() => new Error('Resume analysis timed out. Please try again.'));
        }
        const errorMsg = err?.error?.message || (typeof err?.error === 'string' ? err.error : null) || err?.message || 'Failed to complete resume screening analysis.';
        return throwError(() => new Error(errorMsg));
      })
    );
  }

  /**
   * Get screening results for an application (GET /api/screenings/application/{applicationId})
   */
  getScreeningResult(applicationId: number): Observable<ScreeningResultResponse> {
    return this.http.get<ScreeningResultResponse>(`${this.apiUrl}/application/${applicationId}`);
  }
}
