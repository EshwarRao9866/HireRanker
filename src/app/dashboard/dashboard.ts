import { Component, OnInit, HostListener, inject } from '@angular/core';
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

interface CandidateRow {
  id: number;
  name: string;
  email: string;
  appliedRole: string;
  matchScore: number;
  skills: number;
  experience: string;
  education: number;
  resumeFileName: string;
  status: 'Shortlisted' | 'Under Review' | 'Interview Scheduled' | 'Rejected';
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
    avgMatchRate: 0
  };

  topSkills: SkillStat[] = [];
  statusBreakdown: StatusStat[] = [];
  candidates: CandidateRow[] = [];

  velocityLabels: string[] = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
  appliedPolygonPoints: string = '0,220 0,220 700,220';
  appliedPolylinePoints: string = '0,220 700,220';
  screenedPolygonPoints: string = '0,220 0,220 700,220';
  screenedPolylinePoints: string = '0,220 700,220';

  skillsDonutGradient: string = 'conic-gradient(#e2e8f0 0% 100%)';
  statusDonutGradient: string = 'conic-gradient(#e2e8f0 0% 100%)';

  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly notificationService = inject(NotificationService);
  private readonly jobService = inject(JobService);
  readonly aiChatService = inject(AiChatService);
  private readonly assistantModeService = inject(AssistantModeService);
  private readonly dashboardService = inject(DashboardService);
  private readonly resumeService = inject(ResumeService);

  showNotificationsDropdown = false;
  notifications: AppNotification[] = [];
  unreadNotificationsCount = 0;

  get adminInitial(): string {
    return (this.adminName || 'A').trim().charAt(0).toUpperCase() || 'A';
  }

  constructor() {}

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
    this.loadDashboardMetrics();
  }

  onPeriodChange(): void {
    this.loadDashboardMetrics();
  }

  loadDashboardMetrics(): void {
    // Fetch live dashboard statistics from Spring Boot backend
    this.dashboardService.getAdminDashboard(this.selectedPeriod).subscribe({
      next: (data) => {
        if (data) {
          this.stats = {
            totalApplicants: data.totalApplications || 0,
            screenedResumes: data.screenedResumes || 0,
            shortlisted: data.shortlistedCandidates || 0,
            interviewsScheduled: data.totalInterviews || 0,
            avgMatchRate: Math.round(data.averageMatchScore || 0)
          };

          if (data.topSkills && data.topSkills.length > 0) {
            this.topSkills = data.topSkills.map(s => ({
              name: s.name,
              percent: Math.round(s.percent),
              class: s.cssClass || 'other'
            }));
            this.updateSkillsDonutGradient(this.topSkills);
          } else {
            this.topSkills = [];
            this.skillsDonutGradient = 'conic-gradient(#e2e8f0 0% 100%)';
          }

          if (data.applicationStatus && data.applicationStatus.length > 0) {
            this.statusBreakdown = data.applicationStatus.map(s => ({
              label: s.label,
              count: s.count,
              percent: Math.round(s.percent),
              class: s.cssClass || 'screened'
            }));
            this.updateStatusDonutGradient(this.statusBreakdown);
          } else {
            this.statusBreakdown = [];
            this.statusDonutGradient = 'conic-gradient(#e2e8f0 0% 100%)';
          }

          if (data.topRankedCandidates && data.topRankedCandidates.length > 0) {
            this.candidates = data.topRankedCandidates.map(c => ({
              id: c.candidateId,
              name: c.candidateName,
              email: c.email,
              appliedRole: c.appliedRole,
              matchScore: Math.round(c.matchScore),
              skills: Math.round(c.skillsScore),
              experience: `${c.experienceScore || 3} yrs`,
              education: Math.round(c.educationScore),
              resumeFileName: c.resumeFileName || 'Resume.pdf',
              status: c.applicationStatus === 'SHORTLISTED' ? 'Shortlisted' :
                      c.applicationStatus === 'INTERVIEW' ? 'Interview Scheduled' :
                      c.applicationStatus === 'REJECTED' ? 'Rejected' : 'Under Review'
            }));
          } else {
            // Fall back to candidates in jobService if topRanked is empty
            this.syncDynamicApplicants();
          }

          // Build SVG line chart from backend timeline series
          if (data.applicantsOverview) {
            this.renderVelocityChart(data.applicantsOverview);
          }
        }
      },
      error: () => {
        this.syncDynamicApplicants();
      }
    });
  }

  private renderVelocityChart(overview: { labels: string[]; applicationsSeries: number[]; screenedSeries: number[] }): void {
    if (!overview || !overview.labels || overview.labels.length === 0) {
      this.velocityLabels = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
      this.appliedPolygonPoints = '0,220 0,220 700,220';
      this.appliedPolylinePoints = '0,220 700,220';
      this.screenedPolygonPoints = '0,220 0,220 700,220';
      this.screenedPolylinePoints = '0,220 700,220';
      return;
    }

    this.velocityLabels = overview.labels;
    const appVals = overview.applicationsSeries || [];
    const scrVals = overview.screenedSeries || [];

    const maxVal = Math.max(
      ...appVals,
      ...scrVals,
      1 // ensure divisor > 0
    );

    const count = overview.labels.length;
    const width = 700;
    const height = 220;
    const padTop = 20;
    const padBottom = 20;
    const usableHeight = height - padTop - padBottom;

    const stepX = count > 1 ? width / (count - 1) : width;

    const appPoints: Array<[number, number]> = [];
    const scrPoints: Array<[number, number]> = [];

    for (let i = 0; i < count; i++) {
      const x = Math.round(i * stepX);
      const appV = appVals[i] || 0;
      const scrV = scrVals[i] || 0;

      // Scale Y inversely: maxVal -> padTop, 0 -> height - padBottom
      const appY = Math.round(height - padBottom - (appV / maxVal) * usableHeight);
      const scrY = Math.round(height - padBottom - (scrV / maxVal) * usableHeight);

      appPoints.push([x, appY]);
      scrPoints.push([x, scrY]);
    }

    // Polyline: "x1,y1 x2,y2 ..."
    this.appliedPolylinePoints = appPoints.map(p => `${p[0]},${p[1]}`).join(' ');
    this.screenedPolylinePoints = scrPoints.map(p => `${p[0]},${p[1]}`).join(' ');

    // Polygon: "0,220 x1,y1 ... xN,yN 700,220"
    this.appliedPolygonPoints = `0,${height} ${this.appliedPolylinePoints} ${width},${height}`;
    this.screenedPolygonPoints = `0,${height} ${this.screenedPolylinePoints} ${width},${height}`;
  }

  private updateSkillsDonutGradient(skills: SkillStat[]): void {
    if (!skills || skills.length === 0) {
      this.skillsDonutGradient = 'conic-gradient(#e2e8f0 0% 100%)';
      return;
    }
    const colorPalette = ['#3b82f6', '#10b981', '#f59e0b', '#6366f1', '#06b6d4', '#ec4899', '#8b5cf6'];
    let currentPct = 0;
    const segments: string[] = [];

    for (let i = 0; i < skills.length; i++) {
      const color = colorPalette[i % colorPalette.length];
      const startPct = currentPct;
      const endPct = Math.min(100, currentPct + skills[i].percent);
      segments.push(`${color} ${startPct}% ${endPct}%`);
      currentPct = endPct;
    }

    if (currentPct < 100) {
      segments.push(`#94a3b8 ${currentPct}% 100%`);
    }

    this.skillsDonutGradient = `conic-gradient(${segments.join(', ')})`;
  }

  private updateStatusDonutGradient(breakdown: StatusStat[]): void {
    const total = breakdown.reduce((sum, item) => sum + item.count, 0);
    if (!breakdown || breakdown.length === 0 || total === 0) {
      this.statusDonutGradient = 'conic-gradient(#e2e8f0 0% 100%)';
      return;
    }

    const colorMap: Record<string, string> = {
      'New / Applied': '#3b82f6',
      'Screened': '#10b981',
      'Shortlisted': '#f59e0b',
      'Interview Scheduled': '#8b5cf6',
      'Rejected': '#f43f5e',
      'Hired': '#06b6d4'
    };

    let currentPct = 0;
    const segments: string[] = [];

    for (const item of breakdown) {
      if (item.percent > 0) {
        const color = colorMap[item.label] || '#94a3b8';
        const startPct = currentPct;
        const endPct = Math.min(100, Math.round((currentPct + item.percent) * 10) / 10);
        segments.push(`${color} ${startPct}% ${endPct}%`);
        currentPct = endPct;
      }
    }

    if (currentPct < 100) {
      segments.push(`#e2e8f0 ${currentPct}% 100%`);
    }

    this.statusDonutGradient = `conic-gradient(${segments.join(', ')})`;
  }

  loadNotifications(): void {
    this.notifications = this.notificationService.getAdminNotifications();
    this.unreadNotificationsCount = this.notificationService.adminUnreadCount();
  }

  syncDynamicApplicants(): void {
    const records: ApplicantRecord[] = this.jobService.getApplicants();
    if (records && records.length > 0) {
      this.candidates = records.map((r: ApplicantRecord) => ({
        id: r.id,
        name: r.name,
        email: r.email,
        appliedRole: r.job,
        matchScore: r.matchScore,
        skills: r.skillsMatch,
        experience: r.experience,
        education: r.educationScore,
        resumeFileName: r.resumeFileName,
        status: r.status as any
      }));
    }

    const appsCount = this.jobService.getCandidateApplications().length;
    this.stats.totalApplicants = records ? records.length : appsCount;
    this.stats.screenedResumes = records ? records.filter(r => r.status !== 'Under Review').length : 0;
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
    return [...this.candidates].sort((a, b) => b.matchScore - a.matchScore);
  }

  get topThreeCandidates(): CandidateRow[] {
    return this.sortedCandidates.slice(0, 3);
  }

  get topCandidates(): CandidateRow[] {
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

  setCandidateTab(tab: 'all' | 'topListed'): void {
    this.selectedCandidateTab = tab;
  }

  shortlistCandidate(candidate: CandidateRow): void {
    candidate.status = 'Shortlisted';
  }

  scheduleInterview(candidate: CandidateRow): void {
    candidate.status = 'Interview Scheduled';
    this.router.navigate(['/interview-scheduler']);
  }

  viewResume(candidate: CandidateRow): void {
    const resumeId = (candidate as any).resumeId || candidate.id || 1;
    this.resumeService.downloadResumeBlob(resumeId).subscribe({
      next: (blob) => {
        if (!blob || blob.size === 0) {
          alert('Resume file is empty or unavailable.');
          return;
        }
        const fileUrl = window.URL.createObjectURL(blob);
        window.open(fileUrl, '_blank');
      },
      error: () => {
        alert('Unable to load resume PDF. Please check server connection or verify that the resume exists.');
      }
    });
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