import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
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

  constructor(private readonly http: HttpClient) {}

  /**
   * Admin: Get ranked candidates for a specific job (GET /api/jobs/{jobId}/ranking)
   */
  getRankingForJob(jobId: number): Observable<CandidateRankingResponse[]> {
    return this.http.get<CandidateRankingResponse[]>(`${this.apiUrl}/${jobId}/ranking`);
  }
}
