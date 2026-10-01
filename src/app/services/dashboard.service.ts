import { Injectable, inject, PLATFORM_ID } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, tap, catchError } from 'rxjs';
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
  applicantsTrendPercent?: number | null;
  screenedTrendPercent?: number | null;
  shortlistedTrendPercent?: number | null;
  interviewsScheduledThisWeek?: number;
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
  activeResumeOriginalFileName?: string;
  activeResumeUploadedAt?: string;
  activeResumeStatus?: string;
  activeResumeScore?: number;
  totalInterviews?: number;
  profileCompletionPercentage?: number;
  profileStrengthBadge?: string;
}

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private readonly apiUrl = environment.apiUrl;
  private readonly platformId = inject(PLATFORM_ID);
  private readonly http = inject(HttpClient);

  private readonly STORAGE_ADMIN_DASHBOARD_CACHE = 'hireRankerAdminDashboardCache';

  /**
   * Retrieves cached admin dashboard data from localStorage if available.
   */
  getCachedAdminDashboard(): AdminDashboardData | null {
    if (!isPlatformBrowser(this.platformId)) return null;
    try {
      const raw = localStorage.getItem(this.STORAGE_ADMIN_DASHBOARD_CACHE);
      if (raw) {
        return JSON.parse(raw);
      }
    } catch {
      // ignore
    }
    return null;
  }

  /**
   * Retrieves the comprehensive admin dashboard metrics (GET /api/admin/dashboard?period=...)
   * Only makes network call on browser platform where auth token is available.
   * Gracefully falls back to cached data if network request or auth fails.
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
    }).pipe(
      tap((data: AdminDashboardData) => {
        if (data && (data.totalApplications > 0 || (data.applicantsOverview && data.applicantsOverview.totalApplicants > 0))) {
          try {
            localStorage.setItem(this.STORAGE_ADMIN_DASHBOARD_CACHE, JSON.stringify(data));
          } catch {
            // ignore
          }
        }
      }),
      catchError((err) => {
        console.warn('[DashboardService] getAdminDashboard error, checking cache:', err);
        const cached = this.getCachedAdminDashboard();
        if (cached) {
          return of(cached);
        }
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
      })
    );
  }

  /**
   * Retrieves the candidate's personalized dashboard (GET /api/candidates/me/dashboard)
   * Strictly connects to MySQL backend without converting API errors into fake zeros.
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
        totalInterviews: 0,
        upcomingInterviews: [],
        recentApplications: [],
        applicationStatuses: {},
        recommendedJobs: []
      });
    }

    return this.http.get<CandidateDashboardData>(`${this.apiUrl}/candidates/me/dashboard`);
  }
}

