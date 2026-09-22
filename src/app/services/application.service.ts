import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, timeout, catchError } from 'rxjs';
import { environment } from '../../environments/environment';

export interface ApplicationResponse {
  id: number;
  candidateId: number;
  candidateName?: string;
  candidateEmail?: string;
  jobId: number;
  jobTitle?: string;
  resumeId: number;
  resumeFileName?: string;
  status: 'APPLIED' | 'SCREENING' | 'SHORTLISTED' | 'INTERVIEW' | 'REJECTED' | 'HIRED';
  appliedAt: string;
  updatedAt?: string;
}

export interface ShortlistedCandidateResponse {
  applicationId: number;
  candidateId: number;
  candidateName: string;
  email: string;
  phone?: string;
  jobId: number;
  jobTitle: string;
  resumeId: number;
  resumeFileName?: string;
  overallScore?: number;
  status: string;
  appliedAt?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ApplicationService {
  private readonly apiUrl = `${environment.apiUrl}/applications`;
  private readonly jobsApiUrl = `${environment.apiUrl}/jobs`;

  private readonly defaultApps: ApplicationResponse[] = [];

  constructor(private readonly http: HttpClient) {}

  /**
   * Submit an application for a job (POST /api/applications)
   */
  applyForJob(candidateId: number, jobId: number, resumeId: number): Observable<ApplicationResponse> {
    const mockApp: ApplicationResponse = {
      id: Date.now(),
      candidateId,
      jobId,
      resumeId,
      status: 'APPLIED',
      appliedAt: new Date().toISOString()
    };

    if ((environment as any).useMockData || (environment as any).useBackend === false) {
      return of(mockApp);
    }

    return this.http.post<ApplicationResponse>(this.apiUrl, { candidateId, jobId, resumeId }).pipe(
      timeout(6000),
      catchError(() => of(mockApp))
    );
  }

  /**
   * Get application by ID (GET /api/applications/{id})
   */
  getApplicationById(id: number): Observable<ApplicationResponse> {
    const found = this.defaultApps.find(a => a.id === id) || this.defaultApps[0];
    if ((environment as any).useMockData || (environment as any).useBackend === false) {
      return of(found);
    }

    return this.http.get<ApplicationResponse>(`${this.apiUrl}/${id}`).pipe(
      timeout(6000),
      catchError(() => of(found))
    );
  }

  /**
   * Get all applications submitted by candidate (GET /api/applications/candidate/{candidateId})
   */
  getApplicationsByCandidate(candidateId: number): Observable<ApplicationResponse[]> {
    if ((environment as any).useMockData || (environment as any).useBackend === false) {
      return of(this.defaultApps.filter(a => a.candidateId === candidateId || candidateId === 1));
    }

    return this.http.get<ApplicationResponse[]>(`${this.apiUrl}/candidate/${candidateId}`).pipe(
      timeout(6000),
      catchError(() => of(this.defaultApps))
    );
  }

  /**
   * Get all applications for a specific job (GET /api/applications/job/{jobId})
   */
  getApplicationsByJob(jobId: number): Observable<ApplicationResponse[]> {
    const matched = this.defaultApps.filter(a => a.jobId === jobId);
    const list = matched.length > 0 ? matched : this.defaultApps;

    if ((environment as any).useMockData || (environment as any).useBackend === false) {
      return of(list);
    }

    return this.http.get<ApplicationResponse[]>(`${this.apiUrl}/job/${jobId}`).pipe(
      timeout(6000),
      catchError(() => of(list))
    );
  }

  /**
   * Admin: Get all applications across all jobs (GET /api/applications)
   */
  getAllApplications(): Observable<ApplicationResponse[]> {
    if ((environment as any).useMockData || (environment as any).useBackend === false) {
      return of(this.defaultApps);
    }

    return this.http.get<ApplicationResponse[]>(this.apiUrl).pipe(
      timeout(6000),
      catchError(() => of(this.defaultApps))
    );
  }

  /**
   * Admin: Update status of an application (PUT /api/applications/{id}/status?status={status})
   */
  updateApplicationStatus(id: number, status: string): Observable<ApplicationResponse> {
    const found = { ...(this.defaultApps.find(a => a.id === id) || this.defaultApps[0]), status: status as any };
    if ((environment as any).useMockData || (environment as any).useBackend === false) {
      return of(found);
    }

    return this.http.put<ApplicationResponse>(`${this.apiUrl}/${id}/status?status=${status}`, {}).pipe(
      timeout(6000),
      catchError(() => of(found))
    );
  }

  /**
   * Admin: Shortlist an application (PUT /api/applications/{id}/shortlist)
   */
  shortlistApplication(id: number): Observable<ApplicationResponse> {
    const found = { ...(this.defaultApps.find(a => a.id === id) || this.defaultApps[0]), status: 'SHORTLISTED' as const };
    if ((environment as any).useMockData || (environment as any).useBackend === false) {
      return of(found);
    }

    return this.http.put<ApplicationResponse>(`${this.apiUrl}/${id}/shortlist`, {}).pipe(
      timeout(6000),
      catchError(() => of(found))
    );
  }

  /**
   * Admin: Get all shortlisted candidates for a job (GET /api/jobs/{jobId}/shortlisted)
   */
  getShortlistedForJob(jobId: number): Observable<ShortlistedCandidateResponse[]> {
    const mockShortlisted: ShortlistedCandidateResponse[] = [];

    if ((environment as any).useMockData || (environment as any).useBackend === false) {
      return of(mockShortlisted);
    }

    return this.http.get<ShortlistedCandidateResponse[]>(`${this.jobsApiUrl}/${jobId}/shortlisted`).pipe(
      timeout(6000),
      catchError(() => of(mockShortlisted))
    );
  }
}

