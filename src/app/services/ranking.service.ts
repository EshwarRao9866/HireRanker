import { Injectable, inject, PLATFORM_ID } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { isPlatformBrowser } from '@angular/common';
import { environment } from '../../environments/environment';

export interface CandidateRankingResponse {
  rank: number;
  candidateId: number;
  candidateName: string;
  candidateEmail?: string;
  jobId?: number;
  jobTitle?: string;
  applicationId: number;
  overallScore: number;
  skillsScore: number;
  experienceScore: number;
  educationScore: number;
  applicationStatus: string;
  screenedAt?: string;
  resumeFileName?: string;
}

export type CandidateRankingItem = CandidateRankingResponse;

@Injectable({
  providedIn: 'root'
})
export class RankingService {
  private readonly apiUrl = `${environment.apiUrl}/jobs`;
  private readonly platformId = inject(PLATFORM_ID);

  constructor(private readonly http: HttpClient) {}

  /**
   * Admin: Get ranked candidates for a specific job (GET /api/jobs/{jobId}/ranking)
   */
  getRankingForJob(jobId: number): Observable<CandidateRankingResponse[]> {
    if (!isPlatformBrowser(this.platformId)) {
      return of([]);
    }
    return this.http.get<CandidateRankingResponse[]>(`${this.apiUrl}/${jobId}/ranking`);
  }

  /**
   * Admin: Get all ranked candidates across all jobs (GET /api/ranking)
   */
  getAllRankings(): Observable<CandidateRankingResponse[]> {
    if (!isPlatformBrowser(this.platformId)) {
      return of([]);
    }
    return this.http.get<CandidateRankingResponse[]>(`${environment.apiUrl}/ranking`);
  }
}
