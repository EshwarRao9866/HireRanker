import { Component, OnInit, HostListener } from '@angular/core';
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
    totalApplicants: 248,
    screenedResumes: 186,
    shortlisted: 32,
    interviewsScheduled: 14,
    avgMatchRate: 84
  };

  topSkills: SkillStat[] = [
    { name: 'Java', percent: 32, class: 'java' },
    { name: 'Spring Boot', percent: 24, class: 'spring' },
    { name: 'SQL', percent: 18, class: 'sql' },
    { name: 'JavaScript / TypeScript', percent: 12, class: 'js' },
    { name: 'Angular / React', percent: 8, class: 'react' },
    { name: 'Others', percent: 6, class: 'other' }
  ];

  statusBreakdown: StatusStat[] = [
    { label: 'New', percent: 28, count: 70, class: 'new' },
    { label: 'Screened', percent: 50, count: 124, class: 'screened' },
    { label: 'Shortlisted', percent: 13, count: 32, class: 'shortlisted' },
    { label: 'Rejected', percent: 9, count: 22, class: 'rejected' }
  ];

  candidates: CandidateRow[] = [
    {
      id: 1,
      name: 'Eshwar Rao',
      email: 'eshwar.rao@email.com',
      appliedRole: 'Java Full Stack Developer',
      matchScore: 95,
      skills: 96,
      experience: '4.5 yrs',
      education: 92,
      resumeFileName: 'Eshwar_Rao_Resume.pdf',
      status: 'Shortlisted'
    },
    {
      id: 2,
      name: 'Krupa Jyothi',
      email: 'krupa.jyothi@email.com',
      appliedRole: 'Senior Angular Developer',
      matchScore: 89,
      skills: 92,
      experience: '3.8 yrs',
      education: 88,
      resumeFileName: 'Krupa_Jyothi_Resume.pdf',
      status: 'Interview Scheduled'
    },
    {
      id: 3,
      name: 'Amit Verma',
      email: 'amit.verma@email.com',
      appliedRole: 'Java Developer',
      matchScore: 84,
      skills: 88,
      experience: '3.2 yrs',
      education: 85,
      resumeFileName: 'Amit_Verma_Resume.pdf',
      status: 'Under Review'
    },
    {
      id: 4,
      name: 'Sneha Patel',
      email: 'sneha.patel@email.com',
      appliedRole: 'UI/UX Frontend Engineer',
      matchScore: 82,
      skills: 85,
      experience: '2.9 yrs',
      education: 83,
      resumeFileName: 'Sneha_Patel_Resume.pdf',
      status: 'Under Review'
    },
    {
      id: 5,
      name: 'Vikram Singh',
      email: 'vikram.singh@email.com',
      appliedRole: 'Cloud & AI Engineer',
      matchScore: 78,
      skills: 80,
      experience: '2.5 yrs',
      education: 82,
      resumeFileName: 'Vikram_Singh_Resume.pdf',
      status: 'Rejected'
    }
  ];

  showNotificationsDropdown = false;
  notifications: AppNotification[] = [];
  unreadNotificationsCount = 0;

  constructor(
    private readonly router: Router,
    private readonly authService: AuthService,
    private readonly notificationService: NotificationService,
    private readonly jobService: JobService,
    readonly aiChatService: AiChatService,
    private readonly assistantModeService: AssistantModeService,
    private readonly dashboardService: DashboardService,
    private readonly resumeService: ResumeService
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
    this.syncDynamicApplicants();

    // Fetch live dashboard statistics from Spring Boot backend
    this.dashboardService.getAdminDashboard().subscribe({
      next: (data) => {
        if (data) {
          this.stats = {
            totalApplicants: data.totalApplications || data.applicantsOverview?.totalApplicants || this.stats.totalApplicants,
            screenedResumes: data.screenedResumes || this.stats.screenedResumes,
            shortlisted: data.shortlistedCandidates || this.stats.shortlisted,
            interviewsScheduled: data.totalInterviews || this.stats.interviewsScheduled,
            avgMatchRate: Math.round(data.averageMatchScore || this.stats.avgMatchRate)
          };
          if (data.topSkills && data.topSkills.length > 0) {
            this.topSkills = data.topSkills.map(s => ({
              name: s.name,
              percent: Math.round(s.percent),
              class: s.cssClass || 'other'
            }));
          }
          if (data.applicationStatus && data.applicationStatus.length > 0) {
            this.statusBreakdown = data.applicationStatus.map(s => ({
              label: s.label,
              count: s.count,
              percent: Math.round(s.percent),
              class: s.cssClass || 'screened'
            }));
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
          }
        }
      },
      error: () => {}
    });
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
    this.stats.totalApplicants = 245 + appsCount;
    this.stats.screenedResumes = 184 + appsCount;
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