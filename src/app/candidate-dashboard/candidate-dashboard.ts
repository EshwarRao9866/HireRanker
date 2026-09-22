import { Component, OnInit, HostListener, inject } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../services/auth.service';
import { JobService, JobItem } from '../services/job.service';
import { NotificationService, AppNotification } from '../services/notification.service';
import { AiChatService } from '../services/ai-chat.service';
import { AssistantModeService } from '../services/assistant-mode.service';
import { DashboardService } from '../services/dashboard.service';
import { CandidateService } from '../services/candidate.service';
import { ResumeService } from '../services/resume.service';
import { cleanJobTitle, formatSalaryToLpa } from '../services/salary-formatter.util';

@Component({
  selector: 'app-candidate-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './candidate-dashboard.html',
  styleUrl: './candidate-dashboard.css'
})
export class CandidateDashboard implements OnInit {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly jobService = inject(JobService);
  private readonly notificationService = inject(NotificationService);
  private readonly dashboardService = inject(DashboardService);
  private readonly resumeService = inject(ResumeService);
  readonly aiChatService = inject(AiChatService);
  private readonly assistantModeService = inject(AssistantModeService);

  candidateName = '';
  candidateEmail = '';
  candidateAvatar = '';

  appliedJobs = 0;
  screenedJobs = 0;
  shortlistedJobs = 0;
  interviews = 0;

  activeResumeName = '';
  activeResumeScore = 0;
  latestApplication: any = null;
  upcomingInterview: any = null;

  applicationDateFormatted: string = '';
  shortlistedDateFormatted: string = '';

  recommendedJobs: JobItem[] = [];

  showNotificationsDropdown = false;
  notifications: AppNotification[] = [];
  unreadNotificationsCount = 0;

  get backendLive(): boolean {
    return this.authService?.backendConnected() ?? false;
  }

  get candidateInitial(): string {
    return (this.candidateName || 'C').trim().charAt(0).toUpperCase() || 'C';
  }

  getCompanyInitial(company?: string): string {
    return (company || 'H').trim().charAt(0).toUpperCase() || 'H';
  }

  openAiAssistant(): void {
    this.assistantModeService.setMode('CANDIDATE');
    this.aiChatService.openChatbot();
  }

  isBackendOffline = false;
  backendErrorMessage = '';

  ngOnInit(): void {
    this.assistantModeService.setMode('CANDIDATE');
    const saved = this.authService.getCandidateProfile();
    if (saved) {
      if (saved.fullName) this.candidateName = saved.fullName;
      if (saved.email) this.candidateEmail = saved.email;
      if (saved.profilePicture) this.candidateAvatar = saved.profilePicture;
    } else {
      const user = this.authService.currentUser();
      if (user) {
        this.candidateName = user.fullName;
        this.candidateEmail = user.email;
      }
    }

    this.recommendedJobs = this.jobService.getActiveJobs().slice(0, 3);
    this.appliedJobs = this.jobService.getAppliedJobsCount();
    this.loadNotifications();

    // Check active resume from ResumeService
    const activeRes = this.resumeService.getActiveResumeSnapshot();
    if (activeRes && activeRes.fileName) {
      this.activeResumeName = activeRes.fileName;
      this.activeResumeScore = activeRes.screeningScore || 0;
    } else {
      const savedProf = this.authService.getCandidateProfile();
      if (savedProf?.resumeName && savedProf.resumeName !== 'No resume uploaded yet') {
        this.activeResumeName = savedProf.resumeName;
      }
    }

    // Check recent application from jobService
    const candidateApps = this.jobService.getCandidateApplications();
    if (candidateApps && candidateApps.length > 0) {
      this.latestApplication = candidateApps[0];
      this.applicationDateFormatted = this.latestApplication.appliedDate || 'Recent';
      this.shortlistedDateFormatted = this.latestApplication.status === 'Shortlisted' ? 'Recent' : '';
    }

    // Fetch live candidate metrics from backend
    this.dashboardService?.getCandidateDashboard?.()?.subscribe?.({
      next: (data) => {
        this.isBackendOffline = false;
        this.backendErrorMessage = '';
        if (!data) return;
        if (data.candidateProfile) {
          if (data.candidateProfile.fullName) this.candidateName = data.candidateProfile.fullName;
          if (data.candidateProfile.email) this.candidateEmail = data.candidateProfile.email;
        }
        if (typeof data.totalApplications === 'number') this.appliedJobs = data.totalApplications;
        if (typeof data.screenedApplications === 'number') this.screenedJobs = data.screenedApplications;
        if (typeof data.shortlistedApplications === 'number') this.shortlistedJobs = data.shortlistedApplications;
        if (Array.isArray(data.upcomingInterviews)) {
          this.interviews = data.upcomingInterviews.length;
          if (data.upcomingInterviews.length > 0) {
            this.upcomingInterview = data.upcomingInterviews[0];
          }
        }
        if (Array.isArray(data.recentApplications) && data.recentApplications.length > 0) {
          const topApp = data.recentApplications[0];
          this.latestApplication = {
            jobTitle: cleanJobTitle(topApp.jobTitle || 'Applied Role'),
            company: topApp.company || 'Company',
            status: topApp.status || 'APPLIED',
            appliedDate: topApp.appliedAt ? new Date(topApp.appliedAt).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }) : 'Recent',
            matchScore: topApp.matchScore || 0
          };
          this.applicationDateFormatted = this.latestApplication.appliedDate;
        }
        if (Array.isArray(data.recommendedJobs) && data.recommendedJobs.length > 0) {
          this.recommendedJobs = data.recommendedJobs.map((j) => ({
            id: Number(j.jobId || 0),
            title: cleanJobTitle(j.title || 'Software Developer'),
            company: j.company || 'HireRanker',
            department: 'Engineering',
            location: j.location || 'Remote',
            experience: '3-5 Yrs',
            type: j.employmentType || 'Full Time',
            salary: formatSalaryToLpa(j.salaryRange),
            matchScore: j.matchScore || 85,
            tags: j.requiredSkills ? j.requiredSkills.split(',').map((s) => s.trim()) : ['Java', 'Spring Boot'],
            applicants: 0,
            status: 'Active',
            postedDate: 'Recent',
            description: ''
          }));
        }
      },
      error: (err) => {
        this.isBackendOffline = true;
        this.backendErrorMessage = 'Backend server is currently unavailable. Please start the backend service.';
        console.warn('[CANDIDATE DASHBOARD] Backend not accessible (status: ' + err?.status + '). Displaying offline status notice.');
      }
    });
  }

  loadNotifications(): void {
    this.notifications = this.notificationService.getCandidateNotifications();
    this.unreadNotificationsCount = this.notificationService.candidateUnreadCount();
  }

  toggleNotificationDropdown(event: Event): void {
    event.stopPropagation();
    this.showNotificationsDropdown = !this.showNotificationsDropdown;
    if (this.showNotificationsDropdown) {
      this.loadNotifications();
    }
  }

  @HostListener('document:click')
  closeNotificationDropdown(): void {
    this.showNotificationsDropdown = false;
  }

  markAllAsRead(): void {
    this.notificationService.markAllCandidateNotificationsAsRead();
    this.loadNotifications();
  }

  markOneAsRead(id: string, event: Event): void {
    event.stopPropagation();
    this.notificationService.markCandidateNotificationAsRead(id);
    this.loadNotifications();
  }

  clearAllNotifications(): void {
    this.notificationService.clearCandidateNotifications();
    this.loadNotifications();
  }

  handleNotificationClick(notif: AppNotification): void {
    this.notificationService.markCandidateNotificationAsRead(notif.id);
    this.loadNotifications();
    this.showNotificationsDropdown = false;
    if (notif.link) {
      this.router.navigate([notif.link]);
    } else {
      this.router.navigate(['/find-jobs']);
    }
  }

  openDashboard(): void {
    this.router.navigate(['/candidate-dashboard']);
  }

  openJobs(): void {
    this.router.navigate(['/find-jobs']);
  }

  openApplications(): void {
    this.router.navigate(['/my-applications']);
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