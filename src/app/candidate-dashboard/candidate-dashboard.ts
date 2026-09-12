import { Component, OnInit, HostListener } from '@angular/core';
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
  candidateName = 'Eshwar Rao';
  candidateEmail = 'eshwar@candidate.com';
  candidateAvatar = '';

  appliedJobs = 5;
  screenedJobs = 3;
  shortlistedJobs = 1;
  interviews = 1;

  readonly applicationDateFormatted: string = new Date(Date.now() - 3 * 86400000).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
  readonly shortlistedDateFormatted: string = new Date(Date.now() - 1 * 86400000).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });

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
    readonly aiChatService: AiChatService,
    private readonly assistantModeService: AssistantModeService
  ) {}

  openAiAssistant(): void {
    this.assistantModeService.setMode('CANDIDATE');
    this.aiChatService.openChatbot();
  }

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

    // Fetch live candidate metrics from backend
    this.dashboardService.getCandidateDashboard().subscribe({
      next: (data) => {
        if (data.candidateProfile) {
          if (data.candidateProfile.fullName) this.candidateName = data.candidateProfile.fullName;
          if (data.candidateProfile.email) this.candidateEmail = data.candidateProfile.email;
        }
        if (typeof data.totalApplications === 'number') this.appliedJobs = data.totalApplications;
        if (typeof data.screenedApplications === 'number') this.screenedJobs = data.screenedApplications;
        if (typeof data.shortlistedApplications === 'number') this.shortlistedJobs = data.shortlistedApplications;
        if (Array.isArray(data.upcomingInterviews)) this.interviews = data.upcomingInterviews.length;
        if (data.recommendedJobs && data.recommendedJobs.length > 0) {
          this.recommendedJobs = data.recommendedJobs.map((j) => ({
            id: Number(j.jobId),
            title: cleanJobTitle(j.title),
            company: j.company,
            department: 'Engineering',
            location: j.location,
            experience: '3-5 Yrs',
            type: j.employmentType || 'Full Time',
            salary: formatSalaryToLpa(j.salaryRange),
            matchScore: j.matchScore || 85,
            tags: j.requiredSkills ? j.requiredSkills.split(',').map((s) => s.trim()) : ['Java', 'Spring Boot'],
            applicants: 12,
            status: 'Active',
            postedDate: 'Recent',
            description: ''
          }));
        }
      },
      error: (err) => {
        // Fallback gracefully to default local data if backend is offline or unauthorized
        console.warn('Backend candidate dashboard not accessible, using cached state:', err?.status);
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