import { Component, OnInit, HostListener, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../services/auth.service';
import { JobService, JobItem } from '../services/job.service';
import { NotificationService, AppNotification } from '../services/notification.service';
import { AiChatService } from '../services/ai-chat.service';
import { AssistantModeService } from '../services/assistant-mode.service';
import { DashboardService } from '../services/dashboard.service';
import { cleanJobTitle, formatSalaryToLpa } from '../services/salary-formatter.util';

@Component({
  selector: 'app-candidate-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './candidate-dashboard.html',
  styleUrl: './candidate-dashboard.css'
})
export class CandidateDashboard implements OnInit {
  candidateName = '';
  candidateEmail = '';
  candidateAvatar = '';

  appliedJobs = 0;
  screenedJobs = 0;
  shortlistedJobs = 0;
  interviews = 0;

  profileStrength = 0;
  profileStrengthBadge = 'Incomplete';

  isLoading = true;
  isError = false;
  errorMessage = '';

  activeApplication: {
    jobTitle: string;
    company: string;
    status: string;
    appliedDate: string;
    screenedDate?: string;
    shortlistedDate?: string;
    matchScore?: number;
    timelineStep: number;
    interviewDate?: string;
  } | null = null;

  upcomingInterview: {
    jobTitle: string;
    scheduledDateTime: string;
    type: string;
    meetingLink?: string;
  } | null = null;

  resumeStatus: {
    fileName: string;
    screeningScore?: number;
    screeningStatus: string;
    uploadedDate?: string;
  } | null = null;

  recommendedJobs: JobItem[] = [];

  showNotificationsDropdown = false;
  notifications: AppNotification[] = [];
  unreadNotificationsCount = 0;

  get backendLive(): boolean {
    return this.authService.backendConnected();
  }

  constructor(
    private readonly router: Router,
    private readonly authService: AuthService,
    private readonly jobService: JobService,
    private readonly notificationService: NotificationService,
    private readonly dashboardService: DashboardService,
    private readonly cdr: ChangeDetectorRef,
    readonly aiChatService: AiChatService,
    private readonly assistantModeService: AssistantModeService
  ) {}

  openAiAssistant(): void {
    this.assistantModeService.setMode('CANDIDATE');
    this.aiChatService.openChatbot();
  }

  ngOnInit(): void {
    this.assistantModeService.setMode('CANDIDATE');
    const user = this.authService.currentUser();
    if (user) {
      this.candidateName = user.fullName || 'Candidate';
      this.candidateEmail = user.email || '';
    }
    const saved = this.authService.getCandidateProfile();
    if (saved) {
      if (saved.fullName) this.candidateName = saved.fullName;
      if (saved.email) this.candidateEmail = saved.email;
      if (saved.profilePicture) this.candidateAvatar = saved.profilePicture;
    }

    this.loadNotifications();
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    this.isLoading = true;
    this.isError = false;
    this.errorMessage = '';
    this.cdr.markForCheck();
    this.cdr.detectChanges();

    // Fetch live candidate metrics from backend (strictly candidate-specific)
    this.dashboardService.getCandidateDashboard().subscribe({
      next: (data) => {
        this.isLoading = false;
        this.isError = false;
        if (!data) {
          this.cdr.markForCheck();
          this.cdr.detectChanges();
          return;
        }

        if (data.candidateProfile) {
          if (data.candidateProfile.fullName) this.candidateName = data.candidateProfile.fullName;
          if (data.candidateProfile.email) this.candidateEmail = data.candidateProfile.email;
        }

        this.appliedJobs = typeof data.totalApplications === 'number' ? data.totalApplications : 0;
        this.screenedJobs = typeof data.screenedApplications === 'number' ? data.screenedApplications : 0;
        this.shortlistedJobs = typeof data.shortlistedApplications === 'number' ? data.shortlistedApplications : 0;
        this.interviews = typeof data.totalInterviews === 'number'
          ? data.totalInterviews
          : (Array.isArray(data.upcomingInterviews) ? data.upcomingInterviews.length : 0);

        // Dynamic profile strength calculated from database
        this.profileStrength = typeof data.profileCompletionPercentage === 'number'
          ? data.profileCompletionPercentage
          : (this.candidateName && this.candidateEmail ? 60 : 20);
        this.profileStrengthBadge = data.profileStrengthBadge || (this.profileStrength >= 80 ? 'Profile Ready' : this.profileStrength >= 50 ? 'Good Progress' : 'Incomplete');

        // Populate active application tracker from latest real submission
        if (data.recentApplications && data.recentApplications.length > 0) {
          const latest = data.recentApplications[0];
          const st = (latest.status || 'APPLIED').toUpperCase();
          let step = 1;
          if (st === 'SCREENING' || st === 'SCREENED') step = 2;
          else if (st === 'SHORTLISTED') step = 3;
          else if (st === 'INTERVIEW' || st === 'HIRED') step = 4;

          const appliedFormatted = latest.appliedAt
            ? new Date(latest.appliedAt).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })
            : 'Submitted';

          this.activeApplication = {
            jobTitle: cleanJobTitle(latest.jobTitle || 'Role'),
            company: latest.company && latest.company.trim().length > 0 ? latest.company : 'HireRanker Technologies',
            status: latest.status || 'Applied',
            appliedDate: appliedFormatted,
            matchScore: latest.matchScore ? Math.round(latest.matchScore) : undefined,
            timelineStep: step
          };
        } else {
          this.activeApplication = null;
        }

        // Populate upcoming interview
        if (data.upcomingInterviews && data.upcomingInterviews.length > 0) {
          const iv = data.upcomingInterviews[0];
          const dtFormatted = iv.scheduledDateTime
            ? new Date(iv.scheduledDateTime).toLocaleString('en-US', { dateStyle: 'medium', timeStyle: 'short' })
            : 'Date to be confirmed';
          this.upcomingInterview = {
            jobTitle: cleanJobTitle(iv.jobTitle || 'Technical Interview'),
            scheduledDateTime: dtFormatted,
            type: iv.type || 'Technical Round',
            meetingLink: iv.meetingLink
          };
        } else {
          this.upcomingInterview = null;
        }

        // Populate active resume status
        if (data.activeResumeFileName) {
          let formattedDate: string | undefined = undefined;
          if (data.activeResumeUploadedAt) {
            try {
              formattedDate = new Date(data.activeResumeUploadedAt).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
            } catch {
              // ignore
            }
          }
          this.resumeStatus = {
            fileName: data.activeResumeOriginalFileName || data.activeResumeFileName,
            screeningScore: data.activeResumeScore ? Math.round(data.activeResumeScore) : undefined,
            screeningStatus: data.activeResumeStatus || (data.activeResumeScore ? 'SCREENED' : 'UPLOADED'),
            uploadedDate: formattedDate
          };
        } else {
          this.resumeStatus = null;
        }

        // Populate AI recommended jobs (excluding jobs already applied for)
        if (data.recommendedJobs && data.recommendedJobs.length > 0) {
          this.recommendedJobs = data.recommendedJobs.map((j) => ({
            id: Number(j.id || j.jobId),
            title: cleanJobTitle(j.title),
            company: j.company && j.company.trim().length > 0 ? j.company : 'HireRanker Technologies',
            department: 'Engineering',
            location: j.location || 'Remote',
            experience: '3-5 Yrs',
            type: j.employmentType || 'Full Time',
            salary: formatSalaryToLpa(j.salaryRange),
            matchScore: j.matchScore ? Math.round(j.matchScore) : 80,
            tags: j.requiredSkills ? j.requiredSkills.split(',').map((s) => s.trim()) : [],
            applicants: 0,
            status: 'Active',
            postedDate: 'Verified',
            description: ''
          }));
        } else {
          this.recommendedJobs = [];
        }

        this.cdr.markForCheck();
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Backend candidate dashboard error:', err);
        this.isLoading = false;
        this.isError = true;
        this.errorMessage = err?.status === 401
          ? 'Authentication expired. Please log in again to access candidate data.'
          : 'Unable to connect to backend server. Please verify your connection.';
        this.cdr.markForCheck();
        this.cdr.detectChanges();
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

  refreshDashboard(): void {
    this.loadDashboardData();
  }

  logout(): void {
    this.authService.logout('/candidate-login');
  }
}