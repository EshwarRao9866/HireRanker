import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, timeout, catchError } from 'rxjs';
import { environment } from '../../environments/environment';

export interface AdminDashboardData {
  totalCandidates: number;
  totalApplications: number;
  totalJobs: number;
  screenedResumes: number;
  shortlistedCandidates: number;
  averageMatchScore: number;
  totalInterviews: number;
  totalUsers: number;
  applicantsOverview: {
    totalApplicants: number;
    screenedResumes: number;
    shortlisted: number;
    interviewsScheduled: number;
    labels: string[];
    applicationsSeries: number[];
    screenedSeries: number[];
  };
  applicationStatus: Array<{
    status: string;
    label: string;
    count: number;
    percent: number;
    cssClass: string;
  }>;
  topSkills: Array<{
    name: string;
    count: number;
    percent: number;
    cssClass: string;
  }>;
  topRankedCandidates: Array<{
    rank: number;
    candidateId: number;
    candidateName: string;
    email: string;
    appliedRole: string;
    matchScore: number;
    skillsScore: number;
    experienceScore: number;
    educationScore: number;
    applicationStatus: string;
    resumeFileName: string;
  }>;
}

export interface CandidateDashboardData {
  candidateProfile: {
    id: number;
    userId: number;
    email: string;
    fullName: string;
    phone: string;
    location: string;
    skills: string;
    experience: string;
    education: string;
  };
  totalApplications: number;
  totalResumes: number;
  screenedApplications: number;
  shortlistedApplications: number;
  upcomingInterviews: Array<{
    id: number;
    applicationId: number;
    candidateId: number;
    candidateName: string;
    jobTitle: string;
    scheduledDateTime: string;
    type: string;
    status: string;
    meetingLink?: string;
    notes?: string;
  }>;
  recentApplications: Array<{
    applicationId: number;
    jobId: number;
    jobTitle: string;
    company: string;
    location: string;
    appliedAt: string;
    status: string;
    matchScore?: number;
    resumeFileName?: string;
  }>;
  applicationStatuses: Record<string, number>;
  recommendedJobs: Array<{
    jobId: number;
    title: string;
    company: string;
    location: string;
    salaryRange?: string;
    employmentType?: string;
    requiredSkills?: string;
    matchScore?: number;
  }>;
}

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private readonly apiUrl = environment.apiUrl;

  constructor(private readonly http: HttpClient) {}

  private getMockAdminDashboard(): AdminDashboardData {
    return {
      totalCandidates: 0,
      totalApplications: 0,
      totalJobs: 0,
      screenedResumes: 0,
      shortlistedCandidates: 0,
      averageMatchScore: 0,
      totalInterviews: 0,
      totalUsers: 0,
      applicantsOverview: {
        totalApplicants: 0,
        screenedResumes: 0,
        shortlisted: 0,
        interviewsScheduled: 0,
        labels: [],
        applicationsSeries: [],
        screenedSeries: []
      },
      applicationStatus: [],
      topSkills: [],
      topRankedCandidates: []
    };
  }

  private getMockCandidateDashboard(): CandidateDashboardData {
    return {
      candidateProfile: {
        id: 0,
        userId: 0,
        email: '',
        fullName: '',
        phone: '',
        location: '',
        skills: '',
        experience: '',
        education: ''
      },
      totalApplications: 0,
      totalResumes: 0,
      screenedApplications: 0,
      shortlistedApplications: 0,
      upcomingInterviews: [],
      recentApplications: [],
      applicationStatuses: {},
      recommendedJobs: []
    };
  }

  /**
   * Retrieves the comprehensive admin dashboard metrics (GET /api/admin/dashboard)
   */
  getAdminDashboard(period?: string): Observable<AdminDashboardData> {
    if ((environment as any).useMockData || (environment as any).useBackend === false) {
      return of(this.getMockAdminDashboard());
    }

    const query = period ? `?period=${encodeURIComponent(period)}` : '';
    return this.http.get<AdminDashboardData>(`${this.apiUrl}/admin/dashboard${query}`).pipe(
      timeout(6000),
      catchError(() => of(this.getMockAdminDashboard()))
    );
  }

  /**
   * Retrieves the candidate's personalized dashboard (GET /api/candidates/me/dashboard)
   */
  getCandidateDashboard(): Observable<CandidateDashboardData> {
    if ((environment as any).useMockData || (environment as any).useBackend === false) {
      return of(this.getMockCandidateDashboard());
    }

    return this.http.get<CandidateDashboardData>(`${this.apiUrl}/candidates/me/dashboard`).pipe(
      timeout(6000),
      catchError(() => of(this.getMockCandidateDashboard()))
    );
  }
}
