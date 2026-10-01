import { Component, OnInit, HostListener, ChangeDetectorRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../services/auth.service';
import { NotificationService, AppNotification } from '../services/notification.service';
import { JobService, ApplicantRecord } from '../services/job.service';
import { AiChatService } from '../services/ai-chat.service';
import { AssistantModeService } from '../services/assistant-mode.service';
import { DashboardService } from '../services/dashboard.service';
import { ResumeService } from '../services/resume.service';
import { ApplicationService } from '../services/application.service';

interface CandidateRow {
  id: number;
  name: string;
  email: string;
  appliedRole: string;
  jobId?: number;
  matchScore: number;
  skills: number;
  experience: string;
  education: number;
  resumeFileName: string;
  resumeId?: number;
  applicationId?: number;
  status: 'Shortlisted' | 'Under Review' | 'Interview Scheduled' | 'Rejected' | string;
}

interface SkillStat {
  name: string;
  percent: number;
  class: string;
}

interface StatusStat {
  label: string;
  percent: number;
  count: number;
  class: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard implements OnInit {
  adminName = 'Admin Recruiter';
  adminAvatar = '';
  searchText: string = '';
  selectedStatusFilter = 'All';
  selectedPeriod = 'This Week';
  selectedCandidateTab: 'all' | 'topListed' = 'all';

  get backendLive(): boolean {
    return this.authService.backendConnected();
  }

  stats = {
    totalApplicants: 0,
    screenedResumes: 0,
    shortlisted: 0,
    interviewsScheduled: 0,
    avgMatchRate: 0,
    applicantsTrend: null as number | null,
    screenedTrend: null as number | null,
    shortlistedTrend: null as number | null,
    interviewsThisWeek: 0
  };

  topSkills: SkillStat[] = [];

  statusBreakdown: StatusStat[] = [];

  candidates: CandidateRow[] = [];

  showNotificationsDropdown = false;
  notifications: AppNotification[] = [];
  unreadNotificationsCount = 0;

  chartLabels: string[] = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
  appliedSvgPoints = '0,220 700,220';
  appliedLinePoints = '0,220 700,220';
  screenedSvgPoints = '0,220 700,220';
  screenedLinePoints = '0,220 700,220';
  chartYAxisMax = 10;
  chartYAxisLabels = ['10', '8', '6', '4', '2', '0'];
  skillsConicGradient = '#e2e8f0 0% 100%';
  statusConicGradient = '#e2e8f0 0% 100%';

  selectedLeaderboardJobId: number = 0; // 0 = All Jobs (Global Leaderboard)
  availableJobs: Array<{ id: number; title: string }> = [];
  selectedProfileCandidate: CandidateRow | null = null;
  toastMessage = '';
  private toastTimeout: any;

  constructor(
    private readonly router: Router,
    private readonly authService: AuthService,
    private readonly notificationService: NotificationService,
    private readonly jobService: JobService,
    readonly aiChatService: AiChatService,
    private readonly assistantModeService: AssistantModeService,
    private readonly dashboardService: DashboardService,
    private readonly resumeService: ResumeService,
    private readonly applicationService: ApplicationService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  openAiAssistant(): void {
    this.assistantModeService.setMode('RECRUITER');
    this.aiChatService.openChatbot();
  }

  ngOnInit(): void {
    this.assistantModeService.setMode('RECRUITER');
    const user = this.authService.currentUser();
    if (user && user.fullName) {
      this.adminName = user.fullName;
    }
    const adminSettings = this.authService.getAdminSettings();
    if (adminSettings) {
      if (adminSettings.adminName) this.adminName = adminSettings.adminName;
      if (adminSettings.adminAvatar) this.adminAvatar = adminSettings.adminAvatar;
    }

    this.loadNotifications();

    // Preload available jobs for the dynamic Leaderboard Job filter
    this.jobService.fetchJobsFromBackend().subscribe({
      next: (jobs) => {
        if (jobs && jobs.length > 0) {
          this.availableJobs = jobs.map(j => ({ id: j.id, title: j.title }));
          this.cdr.detectChanges();
        }
      },
      error: () => {}
    });

    // Immediately restore cached dashboard data so the UI displays instantly on page refresh
    const cached = this.dashboardService.getCachedAdminDashboard();
    if (cached) {
      this.populateDashboardData(cached);
    }

    this.loadDashboardData(this.selectedPeriod);
  }

  onPeriodChange(): void {
    this.loadDashboardData(this.selectedPeriod);
  }

  private populateDashboardData(data: any): void {
    if (!data) return;

    this.stats = {
      totalApplicants: data.totalApplications ?? data.applicantsOverview?.totalApplicants ?? 0,
      screenedResumes: data.screenedResumes ?? 0,
      shortlisted: data.shortlistedCandidates ?? 0,
      interviewsScheduled: data.totalInterviews ?? 0,
      avgMatchRate: Math.round(data.averageMatchScore ?? 0),
      applicantsTrend: data.applicantsTrendPercent !== undefined ? data.applicantsTrendPercent : null,
      screenedTrend: data.screenedTrendPercent !== undefined ? data.screenedTrendPercent : null,
      shortlistedTrend: data.shortlistedTrendPercent !== undefined ? data.shortlistedTrendPercent : null,
      interviewsThisWeek: data.interviewsScheduledThisWeek ?? 0
    };

    if (data.topSkills && data.topSkills.length > 0) {
      this.topSkills = data.topSkills.map((s: any) => ({
        name: s.name,
        percent: Math.round(s.percent),
        class: s.cssClass || 'other'
      }));
      this.computeSkillsConicGradient(this.topSkills);
    } else if (this.topSkills.length === 0) {
      this.skillsConicGradient = '#e2e8f0 0% 100%';
    }

    if (data.applicationStatus && data.applicationStatus.length > 0) {
      this.statusBreakdown = data.applicationStatus.map((s: any) => ({
        label: s.label,
        count: s.count,
        percent: Math.round(s.percent),
        class: s.cssClass || 'screened'
      }));
      this.computeStatusConicGradient(this.statusBreakdown);
    } else if (this.statusBreakdown.length === 0) {
      this.statusConicGradient = '#e2e8f0 0% 100%';
    }

    if (data.topRankedCandidates && data.topRankedCandidates.length > 0) {
      this.candidates = data.topRankedCandidates.map((c: any) => ({
        id: c.candidateId,
        name: c.candidateName,
        email: c.email,
        appliedRole: c.appliedRole,
        jobId: c.jobId,
        matchScore: Math.round(c.matchScore),
        skills: Math.round(c.skillsScore),
        experience: `${c.experienceScore || 3} yrs`,
        education: Math.round(c.educationScore),
        resumeFileName: c.resumeFileName || 'Resume.pdf',
        resumeId: c.resumeId,
        applicationId: c.applicationId,
        status: c.applicationStatus === 'SHORTLISTED' ? 'Shortlisted' :
                c.applicationStatus === 'INTERVIEW' ? 'Interview Scheduled' :
                c.applicationStatus === 'REJECTED' ? 'Rejected' : 'Under Review'
      }));
    } else {
      this.candidates = [];
    }

    if (data.applicantsOverview) {
      this.renderDynamicChart(data.applicantsOverview);
    }
  }

  loadDashboardData(period: string = 'This Week'): void {
    this.dashboardService.getAdminDashboard(period).subscribe({
      next: (data) => {
        if (data) {
          const hasMetrics = (data.totalApplications > 0) ||
                             (data.applicantsOverview && data.applicantsOverview.totalApplicants > 0) ||
                             (data.topRankedCandidates && data.topRankedCandidates.length > 0) ||
                             (data.topSkills && data.topSkills.length > 0);
          // If the new payload has data, or if we have no existing stats yet, populate
          if (hasMetrics || this.stats.totalApplicants === 0) {
            this.populateDashboardData(data);
          }
        }
      },
      error: (err) => {
        console.error('Failed to load dashboard data:', err);
      }
    });
  }

  private renderDynamicChart(overview: { labels: string[]; applicationsSeries: number[]; screenedSeries: number[] }): void {
    if (!overview.labels || overview.labels.length === 0) {
      return;
    }

    this.chartLabels = overview.labels;
    const appVals = overview.applicationsSeries || [];
    const screenVals = overview.screenedSeries || [];

    const maxVal = Math.max(...appVals, ...screenVals, 0);
    const yMax = maxVal === 0 ? 10 : Math.ceil(maxVal * 1.25);
    this.chartYAxisMax = yMax;
    this.chartYAxisLabels = [
      String(yMax),
      String(Math.round(yMax * 0.8)),
      String(Math.round(yMax * 0.6)),
      String(Math.round(yMax * 0.4)),
      String(Math.round(yMax * 0.2)),
      '0'
    ];

    const count = overview.labels.length;
    const step = count > 1 ? 700 / (count - 1) : 700;
    const chartHeight = 220;

    const appPoints: string[] = [];
    const screenPoints: string[] = [];

    for (let i = 0; i < count; i++) {
      const x = Math.round(i * step);
      const aVal = appVals[i] || 0;
      const sVal = screenVals[i] || 0;

      const aY = Math.round(chartHeight - (aVal / yMax) * (chartHeight - 30));
      const sY = Math.round(chartHeight - (sVal / yMax) * (chartHeight - 30));

      appPoints.push(`${x},${aY}`);
      screenPoints.push(`${x},${sY}`);
    }

    this.appliedLinePoints = appPoints.join(' ');
    this.appliedSvgPoints = `0,${chartHeight} ` + appPoints.join(' ') + ` 700,${chartHeight}`;

    this.screenedLinePoints = screenPoints.join(' ');
    this.screenedSvgPoints = `0,${chartHeight} ` + screenPoints.join(' ') + ` 700,${chartHeight}`;
  }

  private computeSkillsConicGradient(skills: SkillStat[]): void {
    if (!skills || skills.length === 0) {
      this.skillsConicGradient = '#e2e8f0 0% 100%';
      return;
    }
    const colorPalette = ['#3b82f6', '#10b981', '#f59e0b', '#6366f1', '#06b6d4', '#94a3b8'];
    let currentPct = 0;
    const stops: string[] = [];
    skills.forEach((s, idx) => {
      const color = colorPalette[idx % colorPalette.length];
      const start = currentPct;
      currentPct += s.percent;
      stops.push(`${color} ${start}% ${currentPct}%`);
    });
    if (currentPct < 100) {
      stops.push(`#e2e8f0 ${currentPct}% 100%`);
    }
    this.skillsConicGradient = `conic-gradient(${stops.join(', ')})`;
  }

  private computeStatusConicGradient(statuses: StatusStat[]): void {
    if (!statuses || statuses.length === 0 || this.stats.totalApplicants === 0) {
      this.statusConicGradient = '#e2e8f0 0% 100%';
      return;
    }
    const colorMap: Record<string, string> = {
      'new': '#3b82f6',
      'screened': '#10b981',
      'shortlisted': '#8b5cf6',
      'interview': '#f59e0b',
      'rejected': '#f43f5e',
      'hired': '#14b8a6'
    };
    let currentPct = 0;
    const stops: string[] = [];
    statuses.forEach((st) => {
      const color = colorMap[st.class] || '#6366f1';
      const start = currentPct;
      currentPct += st.percent;
      stops.push(`${color} ${start}% ${currentPct}%`);
    });
    if (currentPct < 100) {
      stops.push(`#e2e8f0 ${currentPct}% 100%`);
    }
    this.statusConicGradient = `conic-gradient(${stops.join(', ')})`;
  }

  loadNotifications(): void {
    this.notifications = this.notificationService.getAdminNotifications();
    this.unreadNotificationsCount = this.notificationService.adminUnreadCount();
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
    this.notificationService.markAllAdminNotificationsAsRead();
    this.loadNotifications();
  }

  markOneAsRead(id: string, event: Event): void {
    event.stopPropagation();
    this.notificationService.markAdminNotificationAsRead(id);
    this.loadNotifications();
  }

  clearAllNotifications(): void {
    this.notificationService.clearAdminNotifications();
    this.loadNotifications();
  }

  handleNotificationClick(notif: AppNotification): void {
    this.notificationService.markAdminNotificationAsRead(notif.id);
    this.loadNotifications();
    this.showNotificationsDropdown = false;
    if (notif.link) {
      this.router.navigate([notif.link]);
    } else {
      this.router.navigate(['/job-applicants']);
    }
  }

  get sortedCandidates(): CandidateRow[] {
    const list = this.selectedLeaderboardJobId === 0
      ? this.candidates
      : this.candidates.filter(c => c.jobId === this.selectedLeaderboardJobId);

    return [...list].sort((a, b) => {
      const scoreDiff = b.matchScore - a.matchScore;
      if (scoreDiff !== 0) return scoreDiff;
      const skillsDiff = b.skills - a.skills;
      if (skillsDiff !== 0) return skillsDiff;
      const eduDiff = b.education - a.education;
      if (eduDiff !== 0) return eduDiff;
      return (a.applicationId || 0) - (b.applicationId || 0);
    });
  }

  get topThreeCandidates(): CandidateRow[] {
    return this.sortedCandidates.slice(0, 3);
  }

  get filteredCandidates(): CandidateRow[] {
    return this.sortedCandidates.filter(c => {
      const matchesSearch = !this.searchText ||
        c.name.toLowerCase().includes(this.searchText.toLowerCase()) ||
        c.email.toLowerCase().includes(this.searchText.toLowerCase()) ||
        c.appliedRole.toLowerCase().includes(this.searchText.toLowerCase());

      const matchesStatus = this.selectedStatusFilter === 'All' || c.status === this.selectedStatusFilter;

      const matchesTab = this.selectedCandidateTab === 'all' || (c.matchScore >= 84 || c.status === 'Shortlisted');

      return matchesSearch && matchesStatus && matchesTab;
    });
  }

  getSelectedJobTitle(): string {
    if (this.selectedLeaderboardJobId === 0) return 'All Job Positions';
    const found = this.availableJobs.find(j => j.id === this.selectedLeaderboardJobId);
    return found ? found.title : 'Selected Job';
  }

  onLeaderboardJobChange(jobId: any): void {
    this.selectedLeaderboardJobId = Number(jobId);
    this.cdr.detectChanges();
  }

  setCandidateTab(tab: 'all' | 'topListed'): void {
    this.selectedCandidateTab = tab;
  }

  shortlistCandidate(candidate: CandidateRow): void {
    if (!candidate.applicationId) {
      candidate.status = 'Shortlisted';
      return;
    }

    this.applicationService.shortlistApplication(candidate.applicationId).subscribe({
      next: () => {
        candidate.status = 'Shortlisted';
        this.stats.shortlisted++;
        this.showToast(`⭐ ${candidate.name} has been successfully shortlisted in MySQL!`);
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.warn('Backend shortlist notification:', err);
        candidate.status = 'Shortlisted';
        this.showToast(`⭐ ${candidate.name} marked as shortlisted.`);
        this.cdr.detectChanges();
      }
    });
  }

  scheduleInterview(candidate: CandidateRow): void {
    candidate.status = 'Interview Scheduled';
    this.router.navigate(['/interview-scheduler'], {
      queryParams: {
        candidateId: candidate.id,
        applicationId: candidate.applicationId,
        candidateName: candidate.name,
        jobId: candidate.jobId
      }
    });
  }

  viewProfile(candidate: CandidateRow): void {
    this.selectedProfileCandidate = candidate;
    this.cdr.detectChanges();
  }

  closeProfileModal(): void {
    this.selectedProfileCandidate = null;
    this.cdr.detectChanges();
  }

  viewResume(candidate: CandidateRow): void {
    if (candidate.applicationId) {
      this.applicationService.getApplicationResumeFile(candidate.applicationId).subscribe({
        next: (blob) => {
          if (!blob || blob.size === 0) {
            alert('Resume file is empty or unavailable.');
            return;
          }
          const fileUrl = window.URL.createObjectURL(blob);
          window.open(fileUrl, '_blank');
        },
        error: () => {
          this.fallbackResumeDownload(candidate);
        }
      });
    } else {
      this.fallbackResumeDownload(candidate);
    }
  }

  private fallbackResumeDownload(candidate: CandidateRow): void {
    const resumeId = candidate.resumeId || candidate.id || 1;
    this.resumeService.downloadResumeBlob(resumeId, candidate.resumeFileName).subscribe({
      next: (blob) => {
        if (!blob || blob.size === 0) {
          alert('Resume file is empty or unavailable.');
          return;
        }
        const fileUrl = window.URL.createObjectURL(blob);
        window.open(fileUrl, '_blank');
      },
      error: () => {
        const fallbackBlob = this.resumeService.createFallbackPdfBlob(resumeId, candidate.resumeFileName || 'Resume.pdf');
        const fileUrl = window.URL.createObjectURL(fallbackBlob);
        window.open(fileUrl, '_blank');
      }
    });
  }

  showToast(msg: string): void {
    this.toastMessage = msg;
    if (this.toastTimeout) clearTimeout(this.toastTimeout);
    this.toastTimeout = setTimeout(() => {
      this.toastMessage = '';
      this.cdr.detectChanges();
    }, 4000);
    this.cdr.detectChanges();
  }

  createJob(): void {
    this.router.navigate(['/job-postings'], { queryParams: { create: 'true' } });
  }

  openSection(section: string): void {
    switch (section) {
      case 'jobs':
        this.router.navigate(['/job-postings']);
        break;
      case 'applicants':
        this.router.navigate(['/job-applicants']);
        break;
      case 'resume':
        this.router.navigate(['/resume-screening']);
        break;
      case 'criteria':
        this.router.navigate(['/evaluation-criteria']);
        break;
      case 'ranking':
        this.router.navigate(['/candidate-ranking']);
        break;
      case 'shortlisted':
        this.router.navigate(['/shortlisted-candidates']);
        break;
      case 'messages':
        this.router.navigate(['/messages']);
        break;
      case 'interview':
        this.router.navigate(['/interview-scheduler']);
        break;
      case 'analytics':
        this.router.navigate(['/reports']);
        break;
      case 'settings':
        this.router.navigate(['/settings']);
        break;
      case 'ai-assistant':
        this.openAiAssistant();
        break;
      default:
        this.router.navigate(['/dashboard']);
    }
  }

  logout(): void {
    this.authService.logout('/');
  }
}