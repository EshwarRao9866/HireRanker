import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
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

  constructor(private readonly http: HttpClient) {}

  /**
   * Submit an application for a job (POST /api/applications)
   */
  applyForJob(candidateId: number, jobId: number, resumeId: number): Observable<ApplicationResponse> {
    return this.http.post<ApplicationResponse>(this.apiUrl, { candidateId, jobId, resumeId });
  }

  /**
   * Get application by ID (GET /api/applications/{id})
   */
  getApplicationById(id: number): Observable<ApplicationResponse> {
    return this.http.get<ApplicationResponse>(`${this.apiUrl}/${id}`);
  }

  /**
   * Get all applications submitted by candidate (GET /api/applications/candidate/{candidateId})
   */
  getApplicationsByCandidate(candidateId: number): Observable<ApplicationResponse[]> {
    return this.http.get<ApplicationResponse[]>(`${this.apiUrl}/candidate/${candidateId}`);
  }

  /**
   * Get all applications for a specific job (GET /api/applications/job/{jobId})
   */
  getApplicationsByJob(jobId: number): Observable<ApplicationResponse[]> {
    return this.http.get<ApplicationResponse[]>(`${this.apiUrl}/job/${jobId}`);
  }

  /**
   * Admin: Get all applications across all jobs (GET /api/applications)
   */
  getAllApplications(): Observable<ApplicationResponse[]> {
    return this.http.get<ApplicationResponse[]>(this.apiUrl);
  }

  /**
   * Admin: Update status of an application (PUT /api/applications/{id}/status?status={status})
   */
  updateApplicationStatus(id: number, status: string): Observable<ApplicationResponse> {
    return this.http.put<ApplicationResponse>(`${this.apiUrl}/${id}/status?status=${status}`, {});
  }

  /**
   * Admin: Shortlist an application (PUT /api/applications/{id}/shortlist)
   */
  shortlistApplication(id: number): Observable<ApplicationResponse> {
    return this.http.put<ApplicationResponse>(`${this.apiUrl}/${id}/shortlist`, {});
  }

  /**
   * Admin: Get all shortlisted candidates for a job (GET /api/jobs/{jobId}/shortlisted)
   */
  getShortlistedForJob(jobId: number): Observable<ShortlistedCandidateResponse[]> {
    return this.http.get<ShortlistedCandidateResponse[]>(`${this.jobsApiUrl}/${jobId}/shortlisted`);
  }
}

