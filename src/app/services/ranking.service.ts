import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, timeout, catchError } from 'rxjs';
import { environment } from '../../environments/environment';

export interface CandidateRankingResponse {
  rank: number;
  candidateId: number;
  candidateName: string;
  applicationId: number;
  overallScore: number;
  skillsScore: number;
  experienceScore: number;
  educationScore: number;
  applicationStatus: string;
}

export type CandidateRankingItem = CandidateRankingResponse;

@Injectable({
  providedIn: 'root'
})
export class RankingService {
  private readonly apiUrl = `${environment.apiUrl}/jobs`;

  private readonly defaultRankings: CandidateRankingResponse[] = [];

  constructor(private readonly http: HttpClient) {}

  /**
   * Admin: Get ranked candidates for a specific job (GET /api/jobs/{jobId}/ranking)
   */
  getRankingForJob(jobId: number): Observable<CandidateRankingResponse[]> {
    if ((environment as any).useMockData || (environment as any).useBackend === false) {
      return of(this.defaultRankings);
    }

    return this.http.get<CandidateRankingResponse[]>(`${this.apiUrl}/${jobId}/ranking`).pipe(
      timeout(6000),
      catchError(() => of(this.defaultRankings))
    );
  }
}
