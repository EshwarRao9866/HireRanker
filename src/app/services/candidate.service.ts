import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, of, timeout } from 'rxjs';
import { environment } from '../../environments/environment';
import { MockDataService } from './mock-data.service';

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

  constructor(
    private readonly http: HttpClient,
    private readonly mockDataService: MockDataService
  ) {}

  /**
   * Retrieves the profile of the authenticated candidate (GET /api/candidates/me)
   */
  getMyProfile(): Observable<CandidateProfile> {
    if ((environment as any).useMockData || (environment as any).useBackend === false) {
      return of(this.mockDataService.getMockCandidateProfile());
    }

    return this.http.get<CandidateProfile>(`${this.apiUrl}/me`).pipe(
      timeout(6000),
      catchError((err: HttpErrorResponse | Error) => {
        return of(this.mockDataService.getMockCandidateProfile());
      })
    );
  }

  /**
   * Updates the profile of the authenticated candidate (PUT /api/candidates/me)
   */
  updateMyProfile(request: Partial<CandidateProfile>): Observable<CandidateProfile> {
    if ((environment as any).useMockData || (environment as any).useBackend === false) {
      const mock = { ...this.mockDataService.getMockCandidateProfile(), ...request };
      return of(mock);
    }

    return this.http.put<CandidateProfile>(`${this.apiUrl}/me`, request).pipe(
      timeout(8000),
      catchError((err: HttpErrorResponse | Error) => {
        const fallback = { ...this.mockDataService.getMockCandidateProfile(), ...request };
        return of(fallback);
      })
    );
  }

  /**
   * Retrieves personalized candidate dashboard (GET /api/candidates/me/dashboard)
   */
  getMyDashboard(): Observable<any> {
    if ((environment as any).useMockData || (environment as any).useBackend === false) {
      return of(this.mockDataService.getMockCandidateDashboard());
    }

    return this.http.get<any>(`${this.apiUrl}/me/dashboard`).pipe(
      timeout(6000),
      catchError((err: HttpErrorResponse | Error) => {
        return of(this.mockDataService.getMockCandidateDashboard());
      })
    );
  }

  /**
   * Admin / Owner: Retrieves candidate by ID (GET /api/candidates/{id})
   */
  getCandidateById(id: number): Observable<CandidateProfile> {
    if ((environment as any).useMockData || (environment as any).useBackend === false) {
      return of(this.mockDataService.getMockCandidateProfile());
    }

    return this.http.get<CandidateProfile>(`${this.apiUrl}/${id}`).pipe(
      timeout(6000),
      catchError(() => of(this.mockDataService.getMockCandidateProfile()))
    );
  }

  /**
   * Admin / Owner: Updates candidate by ID (PUT /api/candidates/{id})
   */
  updateCandidate(id: number, request: any): Observable<CandidateProfile> {
    if ((environment as any).useMockData || (environment as any).useBackend === false) {
      return of({ ...this.mockDataService.getMockCandidateProfile(), ...request });
    }

    return this.http.put<CandidateProfile>(`${this.apiUrl}/${id}`, request).pipe(
      timeout(8000),
      catchError(() => of({ ...this.mockDataService.getMockCandidateProfile(), ...request }))
    );
  }

  /**
   * Admin: Retrieves all candidates (GET /api/candidates)
   */
  getAllCandidates(): Observable<CandidateProfile[]> {
    if ((environment as any).useMockData || (environment as any).useBackend === false) {
      return of([]);
    }

    return this.http.get<CandidateProfile[]>(this.apiUrl).pipe(
      timeout(6000),
      catchError(() => of([]))
    );
  }
}
