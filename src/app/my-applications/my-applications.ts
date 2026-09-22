import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { JobService, CandidateApplication } from '../services/job.service';
import { DashboardService } from '../services/dashboard.service';
import { cleanJobTitle } from '../services/salary-formatter.util';

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

  constructor(
    private readonly router: Router,
    private readonly authService: AuthService,
    private readonly jobService: JobService,
    private readonly dashboardService: DashboardService
  ) {}

  ngOnInit(): void {
    this.loadApplications();
  }

  loadApplications(): void {
    this.applications = this.jobService.getCandidateApplications();

    // Fetch live applications from backend candidate dashboard
    this.dashboardService.getCandidateDashboard().subscribe({
      next: (dash) => {
        if (dash && Array.isArray(dash.recentApplications) && dash.recentApplications.length > 0) {
          this.applications = dash.recentApplications.map((app) => ({
            id: app.applicationId,
            jobId: app.jobId,
            jobTitle: cleanJobTitle(app.jobTitle),
            company: app.company,
            candidateName: dash.candidateProfile?.fullName || 'Candidate',
            candidateEmail: dash.candidateProfile?.email || '',
            appliedDate: app.appliedAt ? new Date(app.appliedAt).toLocaleDateString() : 'Recent',
            matchScore: Math.round(app.matchScore || 85),
            status: this.mapApplicationStatus(app.status),
            location: app.location || 'Hyderabad',
            salary: '₹10 - 16 LPA'
          }));
        }
      },
      error: () => {}
    });
  }

  private mapApplicationStatus(status?: string): 'Shortlisted' | 'Under Review' | 'Interview Scheduled' | 'Application Sent' {
    if (!status) return 'Under Review';
    const s = status.toUpperCase();
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

  get shortlistedCount(): number {
    return this.applications.filter(a => a.status === 'Shortlisted').length;
  }

  get interviewCount(): number {
    return this.applications.filter(a => a.status === 'Interview Scheduled').length;
  }

  get reviewCount(): number {
    return this.applications.filter(a => a.status === 'Under Review' || a.status === 'Application Sent').length;
  }

  setFilter(filter: 'ALL' | 'SHORTLISTED' | 'REVIEW' | 'INTERVIEW'): void {
    this.activeFilter = filter;
  }

  showDetailsModal = false;
  selectedApp: CandidateApplication | null = null;
  selectedJob: any = null;

  viewDetails(app: CandidateApplication): void {
    this.selectedApp = app;
    const allJobs = this.jobService.getJobs();
    this.selectedJob = allJobs.find(j => j.id === app.jobId || j.title.toLowerCase() === app.jobTitle.toLowerCase()) || null;
    this.showDetailsModal = true;
  }

  closeDetailsModal(): void {
    this.showDetailsModal = false;
    this.selectedApp = null;
    this.selectedJob = null;
  }

  withdrawApplication(app: CandidateApplication): void {
    this.applications = this.applications.filter(a => a.id !== app.id);
    this.jobService.saveCandidateApplications(this.applications);
    if (this.selectedApp?.id === app.id) {
      this.closeDetailsModal();
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

  openProfile(): void {
    this.router.navigate(['/my-profile']);
  }

  logout(): void {
    this.authService.logout('/candidate-login');
  }
}
