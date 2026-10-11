import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { forkJoin, of, catchError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { JobService, CandidateApplication } from '../services/job.service';
import { DashboardService } from '../services/dashboard.service';
import { ApplicationService, ApplicationResponse } from '../services/application.service';
import { cleanJobTitle, formatSalaryToLpa } from '../services/salary-formatter.util';

@Component({
  selector: 'app-my-applications',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './my-applications.html',
  styleUrl: './my-applications.css',
})
export class MyApplications implements OnInit {
  activeFilter: 'ALL' | 'SHORTLISTED' | 'REVIEW' | 'INTERVIEW' = 'ALL';

  applications: CandidateApplication[] = [];

  // Filter tab counts derived directly from candidate's real applications
  totalApplied: number = 0;
  shortlistedCount: number = 0;
  interviewsCount: number = 0;
  inReviewCount: number = 0;

  // Loading & Error states
  isLoading: boolean = true;
  isError: boolean = false;
  errorMessage: string = '';

  showDetailsModal = false;
  selectedApp: CandidateApplication | null = null;
  selectedJob: any = null;

  constructor(
    private readonly router: Router,
    private readonly authService: AuthService,
    private readonly jobService: JobService,
    private readonly dashboardService: DashboardService,
    private readonly applicationService: ApplicationService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadApplications();
  }

  loadApplications(): void {
    this.isLoading = true;
    this.isError = false;
    this.errorMessage = '';
    this.cdr.markForCheck();
    this.cdr.detectChanges();

    // Fetch strictly the authenticated candidate's applications from MySQL
    this.applicationService.getMyApplications().subscribe({
      next: (apps) => {
        this.isLoading = false;
        this.isError = false;

        const mappedApps: CandidateApplication[] = (apps || []).map((app) => ({
          id: app.id,
          jobId: app.jobId,
          jobTitle: cleanJobTitle(app.jobTitle || 'Role'),
          company: app.company && app.company.trim().length > 0 ? app.company : 'HireRanker Technologies',
          candidateName: app.candidateName || 'Candidate',
          candidateEmail: app.candidateEmail || '',
          appliedDate: app.appliedAt
            ? new Date(app.appliedAt).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })
            : 'Recent',
          matchScore: app.matchScore !== undefined && app.matchScore !== null ? Math.round(app.matchScore) : 85,
          status: this.mapApplicationStatus(app.status),
          location: app.jobLocation || app.location || 'Hyderabad',
          salary: app.salary ? formatSalaryToLpa(app.salary) : 'Competitive'
        }));

        this.applications = mappedApps;
        this.recalculateFilterCounts();

        // Notify zoneless change detection
        this.cdr.markForCheck();
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load applications from backend:', err);
        this.isLoading = false;
        this.isError = true;
        this.errorMessage = err?.error?.message || 'Unable to retrieve your applications. Please verify your connection.';
        this.applications = [];
        this.recalculateFilterCounts();

        this.cdr.markForCheck();
        this.cdr.detectChanges();
      }
    });
  }

  recalculateFilterCounts(): void {
    this.totalApplied = this.applications.length;
    this.shortlistedCount = this.applications.filter(a => a.status === 'Shortlisted').length;
    this.interviewsCount = this.applications.filter(a => a.status === 'Interview Scheduled').length;
    this.inReviewCount = this.applications.filter(a => a.status === 'Under Review' || a.status === 'Application Sent').length;
  }

  private mapApplicationStatus(status?: string): 'Shortlisted' | 'Under Review' | 'Interview Scheduled' | 'Application Sent' {
    if (!status) return 'Under Review';
    const s = status.toUpperCase().trim();
    if (s === 'SHORTLISTED') return 'Shortlisted';
    if (s === 'INTERVIEW' || s === 'INTERVIEW_SCHEDULED') return 'Interview Scheduled';
    if (s === 'APPLIED') return 'Application Sent';
    return 'Under Review';
  }

  get filteredApplications(): CandidateApplication[] {
    if (this.activeFilter === 'SHORTLISTED') {
      return this.applications.filter(a => a.status === 'Shortlisted');
    }
    if (this.activeFilter === 'REVIEW') {
      return this.applications.filter(a => a.status === 'Under Review' || a.status === 'Application Sent');
    }
    if (this.activeFilter === 'INTERVIEW') {
      return this.applications.filter(a => a.status === 'Interview Scheduled');
    }
    return this.applications;
  }

  setFilter(filter: 'ALL' | 'SHORTLISTED' | 'REVIEW' | 'INTERVIEW'): void {
    this.activeFilter = filter;
    this.cdr.markForCheck();
    this.cdr.detectChanges();
  }

  viewDetails(app: CandidateApplication): void {
    this.selectedApp = app;
    const allJobs = this.jobService.getJobs();
    this.selectedJob = allJobs.find(j => j.id === app.jobId || j.title.toLowerCase() === app.jobTitle.toLowerCase()) || null;
    this.showDetailsModal = true;
    this.cdr.markForCheck();
    this.cdr.detectChanges();
  }

  closeDetailsModal(): void {
    this.showDetailsModal = false;
    this.selectedApp = null;
    this.selectedJob = null;
    this.cdr.markForCheck();
    this.cdr.detectChanges();
  }

  withdrawApplication(app: CandidateApplication): void {
    if (confirm(`Are you sure you want to withdraw your application for "${app.jobTitle}"?`)) {
      if (app.id) {
        this.applicationService.deleteApplication(app.id).subscribe({
          next: () => {
            this.applications = this.applications.filter(a => a.id !== app.id);
            if (this.totalApplied > 0) this.totalApplied--;
            if (app.status === 'Shortlisted' && this.shortlistedCount > 0) this.shortlistedCount--;
            if (app.status === 'Interview Scheduled' && this.interviewsCount > 0) this.interviewsCount--;
            if ((app.status === 'Under Review' || app.status === 'Application Sent') && this.inReviewCount > 0) this.inReviewCount--;
            this.jobService.saveCandidateApplications(this.applications);
            if (this.selectedApp?.id === app.id) {
              this.closeDetailsModal();
            }
            this.cdr.markForCheck();
            this.cdr.detectChanges();
          },
          error: (err) => {
            console.error('Failed to withdraw application on backend:', err);
            this.applications = this.applications.filter(a => a.id !== app.id);
            this.jobService.saveCandidateApplications(this.applications);
            if (this.selectedApp?.id === app.id) {
              this.closeDetailsModal();
            }
            this.cdr.markForCheck();
            this.cdr.detectChanges();
          }
        });
      } else {
        this.applications = this.applications.filter(a => a.id !== app.id);
        this.jobService.saveCandidateApplications(this.applications);
        if (this.selectedApp?.id === app.id) {
          this.closeDetailsModal();
        }
        this.cdr.markForCheck();
        this.cdr.detectChanges();
      }
    }
  }

  goDashboard(): void {
    this.router.navigate(['/candidate-dashboard']);
  }

  openJobs(): void {
    this.router.navigate(['/find-jobs']);
  }

  openResume(): void {
    this.router.navigate(['/my-resume']);
  }

  openInterviews(): void {
    this.router.navigate(['/interviews']);
  }

  openMessages(): void {
    this.router.navigate(['/candidate-messages']);
  }

  contactRecruiter(app?: CandidateApplication): void {
    if (app) {
      this.router.navigate(['/candidate-messages'], {
        queryParams: {
          jobId: app.jobId,
          applicationId: app.id,
          jobTitle: app.jobTitle,
          company: app.company,
          contactRecruiter: 'true'
        }
      });
    } else {
      this.router.navigate(['/candidate-messages']);
    }
  }

  openProfile(): void {
    this.router.navigate(['/my-profile']);
  }

  logout(): void {
    this.authService.logout('/candidate-login');
  }
}
