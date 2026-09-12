import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface CandidateProfile {
  id?: number;
  userId?: number;
  email?: string;
  fullName: string;
  phone?: string;
  location?: string;
  skills?: string;
  experience?: string;
  education?: string;
  github?: string;
  linkedin?: string;
  createdAt?: string;
  updatedAt?: string;
}

@Injectable({
  providedIn: 'root'
})
export class CandidateService {
  private readonly apiUrl = `${environment.apiUrl}/candidates`;

  constructor(private readonly http: HttpClient) {}

  /**
   * Retrieves the profile of the authenticated candidate (GET /api/candidates/me)
   */
  getMyProfile(): Observable<CandidateProfile> {
    return this.http.get<CandidateProfile>(`${this.apiUrl}/me`);
  }

  /**
   * Updates the profile of the authenticated candidate (PUT /api/candidates/me)
   */
  updateMyProfile(request: Partial<CandidateProfile>): Observable<CandidateProfile> {
    return this.http.put<CandidateProfile>(`${this.apiUrl}/me`, request);
  }

  /**
   * Retrieves personalized candidate dashboard (GET /api/candidates/me/dashboard)
   */
  getMyDashboard(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/me/dashboard`);
  }

  /**
   * Admin / Owner: Retrieves candidate by ID (GET /api/candidates/{id})
   */
  getCandidateById(id: number): Observable<CandidateProfile> {
    return this.http.get<CandidateProfile>(`${this.apiUrl}/${id}`);
  }

  /**
   * Admin / Owner: Updates candidate by ID (PUT /api/candidates/{id})
   */
  updateCandidate(id: number, request: any): Observable<CandidateProfile> {
    return this.http.put<CandidateProfile>(`${this.apiUrl}/${id}`, request);
  }

  /**
   * Admin: Retrieves all candidates (GET /api/candidates)
   */
  getAllCandidates(): Observable<CandidateProfile[]> {
    return this.http.get<CandidateProfile[]>(this.apiUrl);
  }
}
