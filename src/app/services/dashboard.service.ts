import { Injectable, inject, PLATFORM_ID } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { isPlatformBrowser } from '@angular/common';
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
    resumeId?: number;
    applicationId?: number;
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
    id?: number;
    title: string;
    company: string;
    location: string;
    salaryRange?: string;
    employmentType?: string;
    requiredSkills?: string;
    matchScore?: number;
  }>;
  activeResumeFileName?: string;
  activeResumeScore?: number;
}

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private readonly apiUrl = environment.apiUrl;
  private readonly platformId = inject(PLATFORM_ID);

  constructor(private readonly http: HttpClient) {}

  /**
   * Retrieves the comprehensive admin dashboard metrics (GET /api/admin/dashboard?period=...)
   * Only makes network call on browser platform where auth token is available.
   */
  getAdminDashboard(period: string = 'This Week'): Observable<AdminDashboardData> {
    if (!isPlatformBrowser(this.platformId)) {
      return of({
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
          labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
          applicationsSeries: [0, 0, 0, 0, 0, 0, 0],
          screenedSeries: [0, 0, 0, 0, 0, 0, 0]
        },
        applicationStatus: [],
        topSkills: [],
        topRankedCandidates: []
      });
    }
    return this.http.get<AdminDashboardData>(`${this.apiUrl}/admin/dashboard`, {
      params: { period }
    });
  }

  /**
   * Retrieves the candidate's personalized dashboard (GET /api/candidates/me/dashboard)
   * Only makes network call on browser platform where candidate auth token is available.
   */
  getCandidateDashboard(): Observable<CandidateDashboardData> {
    if (!isPlatformBrowser(this.platformId)) {
      return of({
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
      });
    }
    return this.http.get<CandidateDashboardData>(`${this.apiUrl}/candidates/me/dashboard`);
  }
}
