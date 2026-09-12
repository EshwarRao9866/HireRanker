import { Injectable, signal, computed, inject } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { AuthService } from './auth.service';

export type AssistantMode = 'RECRUITER' | 'CANDIDATE';

@Injectable({
  providedIn: 'root'
})
export class AssistantModeService {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  readonly mode = signal<AssistantMode>('RECRUITER');

  readonly isRecruiter = computed(() => this.mode() === 'RECRUITER');
  readonly isCandidate = computed(() => this.mode() === 'CANDIDATE');

  readonly greeting = computed(() => {
    return this.isRecruiter()
      ? "Hello! I'm your AI Recruiter Assistant. How can I help you manage candidates and hiring today?"
      : "Hello! I am your AI Career assistant. How can I guide your job search and interview preparation today?";
  });

  readonly title = computed(() => this.isRecruiter() ? 'HireRanker AI Recruiter' : 'HireRanker AI Career');
  readonly badgeTag = computed(() => this.isRecruiter() ? 'Recruiter Mode' : 'Candidate Mode');
  readonly subtitle = computed(() => this.isRecruiter()
    ? 'Talent Acquisition & Screening Assistant'
    : 'Career Coach & Interview Preparation');

  readonly quickActions = computed<string[]>(() => {
    if (this.isRecruiter()) {
      return [
        'Find Top Candidates',
        'Analyze Skill Gaps',
        'Summarize Candidate Interviews',
        'Review Candidate Rankings',
        'Shortlist Candidates',
        'Generate Interview Questions',
        'Analyze Recruitment Trends'
      ];
    } else {
      return [
        'Improve My Resume',
        'Find Suitable Jobs',
        'Prepare for Interview',
        'Analyze My Skills'
      ];
    }
  });

  constructor() {
    this.detectModeFromUrl(this.router.url);

    this.router.events.subscribe((event) => {
      if (event instanceof NavigationEnd) {
        this.detectModeFromUrl(event.urlAfterRedirects || event.url);
      }
    });
  }

  detectModeFromUrl(url: string): void {
    const cleanUrl = (url || '').split('?')[0].split('#')[0];

    // Candidate routes
    if (
      cleanUrl.startsWith('/candidate-dashboard') ||
      cleanUrl.startsWith('/candidate-profile') ||
      cleanUrl.startsWith('/my-applications') ||
      cleanUrl.startsWith('/resume-upload') ||
      (cleanUrl.startsWith('/interview') && !cleanUrl.startsWith('/interview-scheduler'))
    ) {
      this.mode.set('CANDIDATE');
      return;
    }

    // Admin routes
    if (
      cleanUrl.startsWith('/dashboard') ||
      cleanUrl.startsWith('/job-postings') ||
      cleanUrl.startsWith('/job-applicants') ||
      cleanUrl.startsWith('/resume-screening') ||
      cleanUrl.startsWith('/evaluation-criteria') ||
      cleanUrl.startsWith('/candidate-ranking') ||
      cleanUrl.startsWith('/shortlisted-candidates') ||
      cleanUrl.startsWith('/interview-scheduler') ||
      cleanUrl.startsWith('/reports') ||
      cleanUrl.startsWith('/settings')
    ) {
      this.mode.set('RECRUITER');
      return;
    }

    // Fallback based on logged-in user role
    const role = this.authService.getUserRole();
    if (role === 'ADMIN') {
      this.mode.set('RECRUITER');
    } else {
      this.mode.set('CANDIDATE');
    }
  }

  setMode(mode: AssistantMode): void {
    this.mode.set(mode);
  }
}
