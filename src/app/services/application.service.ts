import { Injectable, inject, PLATFORM_ID } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { isPlatformBrowser } from '@angular/common';
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
  phone?: string;
  location?: string;
  skills?: string;
  experience?: string;
  education?: string;
  github?: string;
  linkedin?: string;
  matchScore?: number;
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
  private readonly platformId = inject(PLATFORM_ID);

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
   * Admin: Get all shortlisted candidates across all jobs (GET /api/applications/shortlisted)
   */
  getAllShortlisted(): Observable<ShortlistedCandidateResponse[]> {
    if (!isPlatformBrowser(this.platformId)) {
      return of([]);
    }
    return this.http.get<ShortlistedCandidateResponse[]>(`${this.apiUrl}/shortlisted`);
  }

  /**
   * Admin: Get all shortlisted candidates for a job (GET /api/jobs/{jobId}/shortlisted)
   */
  getShortlistedForJob(jobId: number): Observable<ShortlistedCandidateResponse[]> {
    if (!isPlatformBrowser(this.platformId)) {
      return of([]);
    }
    return this.http.get<ShortlistedCandidateResponse[]>(`${this.jobsApiUrl}/${jobId}/shortlisted`);
  }

  /**
   * Admin: Safely clear all test applications and related interview/screening data (DELETE /api/applications/all)
   */
  clearAllApplications(): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/all`);
  }

  /**
   * Admin: Delete single application (DELETE /api/applications/{id})
   */
  deleteApplication(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}

