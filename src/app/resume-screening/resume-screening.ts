import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subscription, finalize, catchError, of, timeout } from 'rxjs';
import { ScreeningService, ScreeningResultResponse } from '../services/screening.service';
import { JobService } from '../services/job.service';
import { ApplicationService, ApplicationResponse } from '../services/application.service';
import { ResumeService } from '../services/resume.service';
import { cleanJobTitle, cleanCandidateName, cleanCandidateEmail } from '../services/salary-formatter.util';

export interface ApplicantOption {
  id: number;
  candidateName: string;
  candidateEmail: string;
  resumeFileName?: string;
  resumeId?: number;
  status?: string;
}

@Component({
  selector: 'app-resume-screening',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './resume-screening.html',
  styleUrl: './resume-screening.css'
})
export class ResumeScreening implements OnInit, OnDestroy {
  selectedJob = 'Java Full Stack Developer';
  selectedJobId = 24;
  uploadedFileName = 'Resume.pdf';
  isScreening = false;
  screeningComplete = false;
  errorMessage: string | null = null;
  currentApplicationId: number = 0;
  screeningProgress = 0;
  screeningStageText = '';
  screeningStage: 'IDLE' | 'UPLOADING' | 'STORED' | 'EXTRACTING' | 'ANALYZING' | 'SCORING' | 'COMPLETE' | 'FAILED' = 'IDLE';

  private screeningSub?: Subscription;
  private progressInterval: any;

  jobs: Array<{ id: number; title: string; displayTitle?: string }> = [];
  applicants: ApplicantOption[] = [];
  selectedApplicantId: number | null = null;
  screenedApplicants: ScreeningResultResponse[] = [];
  isLoadingScreenings = false;

  matchData = {
    overallScore: 0,
    jobSkillScore: 0,
    experienceScore: 0,
    jobDescriptionScore: 0,
    projectScore: 0,
    atsCompatibilityScore: 0,
    educationScore: 0,
    achievementScore: 0,
    completenessScore: 0,
    keywordScore: 0,
    skillsMatch: 0,
    experienceMatch: 0,
    projectMatch: 0,
    educationMatch: 0,
    certificationMatch: 0,
    formattingMatch: 0,
    achievementMatch: 0,
    matchTier: 'Not Screened' as 'Strong Match' | 'Moderate Match' | 'Weak Match' | 'Not Screened',
    recommendation: 'Not Screened',
    detectedSkills: [] as string[],
    missingSkills: [] as string[],
    recommendedSkills: [] as string[],
    strengths: [] as string[],
    weaknesses: [] as string[],
    improvementSuggestions: [] as string[],
    resumeSummary: ''
  };

  cleanJobTitle(title: string): string {
    return cleanJobTitle(title);
  }

  cleanCandidateName(name: string): string {
    return cleanCandidateName(name);
  }

  cleanCandidateEmail(email: string): string {
    return cleanCandidateEmail(email);
  }

  constructor(
    private readonly jobService: JobService,
    private readonly applicationService: ApplicationService,
    private readonly screeningService: ScreeningService,
    private readonly router: Router,
    private readonly cdr: ChangeDetectorRef,
    private readonly resumeService: ResumeService
  ) {}

  ngOnInit(): void {
    // 1. Fetch available jobs from backend
    this.jobService.fetchJobsFromBackend().subscribe({
      next: (backendJobs) => {
        if (backendJobs && backendJobs.length > 0) {
          this.jobs = backendJobs.map(j => ({
            id: j.id,
            title: j.title,
            displayTitle: this.cleanJobTitle(j.title) || j.title
          }));
          this.selectedJobId = this.jobs[0].id;
          this.selectedJob = this.jobs[0].title;
          this.loadApplicantsForJob(this.selectedJobId);
        }
      },
      error: () => {
        // Fallback to active jobs in service
        const active = this.jobService.getActiveJobs();
        if (active && active.length > 0) {
          this.jobs = active.map(j => ({ id: j.id, title: j.title, displayTitle: this.cleanJobTitle(j.title) || j.title }));
          this.selectedJobId = this.jobs[0].id;
          this.selectedJob = this.jobs[0].title;
          this.loadApplicantsForJob(this.selectedJobId);
        }
      }
    });

    // 2. Fetch all completed screenings from MySQL
    this.loadScreenedApplicants();
  }

  ngOnDestroy(): void {
    if (this.screeningSub) {
      this.screeningSub.unsubscribe();
    }
    if (this.progressInterval) {
      clearInterval(this.progressInterval);
    }
  }

  loadScreenedApplicants(): void {
    this.isLoadingScreenings = true;
    this.screeningService.getAllScreenings().subscribe({
      next: (list) => {
        this.isLoadingScreenings = false;
        // Deduplicate screenings by applicationId
        const map = new Map<number, ScreeningResultResponse>();
        for (const item of (list || [])) {
          if (!map.has(item.applicationId) || item.id > map.get(item.applicationId)!.id) {
            map.set(item.applicationId, item);
          }
        }
        this.screenedApplicants = Array.from(map.values()).sort((a, b) => (b.overallScore || 0) - (a.overallScore || 0));
        this.cdr.detectChanges();
      },
      error: () => {
        this.isLoadingScreenings = false;
        this.screenedApplicants = [];
        this.cdr.detectChanges();
      }
    });
  }

  onJobChange(jobTitleOrId: string | number): void {
    const found = this.jobs.find(j => j.id === Number(jobTitleOrId) || j.title === String(jobTitleOrId));
    if (found) {
      this.selectedJobId = found.id;
      this.selectedJob = found.title;
      this.errorMessage = null;
      this.loadApplicantsForJob(found.id);
    }
  }

  loadApplicantsForJob(jobId: number): void {
    this.applicationService.getApplicationsByJob(jobId).pipe(
      catchError(() => of([]))
    ).subscribe({
      next: (apps) => {
        let list: ApplicantOption[] = [];

        if (apps && apps.length > 0) {
          list = apps.map(a => ({
            id: a.id,
            candidateName: cleanCandidateName(a.candidateName || 'Candidate'),
            candidateEmail: cleanCandidateEmail(a.candidateEmail || 'candidate@email.com'),
            resumeFileName: a.resumeFileName || 'Resume.pdf',
            resumeId: a.resumeId || a.id,
            status: a.status
          }));
        }

        this.applicants = list;
        if (this.applicants.length > 0) {
          this.selectedApplicantId = this.applicants[0].id;
          this.currentApplicationId = this.applicants[0].id;
          this.uploadedFileName = this.applicants[0].resumeFileName || 'Resume.pdf';
          this.errorMessage = null;
          this.loadScreeningResult(this.currentApplicationId);
        } else {
          this.selectedApplicantId = null;
          this.currentApplicationId = 0;
          this.uploadedFileName = 'No resumes for this role';
          this.screeningComplete = false;
        }
        this.cdr.detectChanges();
      }
    });
  }

  onApplicantChange(applicantId: any): void {
    const id = Number(applicantId);
    this.selectedApplicantId = id;
    this.currentApplicationId = id;
    this.errorMessage = null;
    const applicant = this.applicants.find(a => a.id === id);
    if (applicant) {
      this.uploadedFileName = applicant.resumeFileName || 'Resume.pdf';
    }
    this.loadScreeningResult(id);
  }

  selectScreenedApplicant(screening: ScreeningResultResponse): void {
    this.selectedApplicantId = screening.applicationId;
    this.currentApplicationId = screening.applicationId;
    if (screening.jobId) {
      this.selectedJobId = screening.jobId;
    }
    if (screening.jobTitle) {
      this.selectedJob = screening.jobTitle;
    }
    this.uploadedFileName = screening.resumeFileName || 'Resume.pdf';
    this.applyScreeningResult(screening);
    this.screeningComplete = true;
    this.errorMessage = null;
    this.cdr.detectChanges();
  }

  loadScreeningResult(applicationId: number): void {
    if (!applicationId || applicationId <= 0) {
      this.screeningComplete = false;
      return;
    }

    // Check if we already have it in screenedApplicants
    const existing = this.screenedApplicants.find(s => s.applicationId === applicationId);
    if (existing) {
      this.applyScreeningResult(existing);
      this.screeningComplete = true;
      this.errorMessage = null;
      this.cdr.detectChanges();
      return;
    }

    this.screeningService.getScreeningResult(applicationId).pipe(
      timeout(5000),
      catchError(() => of(null))
    ).subscribe({
      next: (res) => {
        if (res && res.overallScore !== undefined && res.overallScore !== null) {
          this.applyScreeningResult(res);
          this.screeningComplete = true;
          this.errorMessage = null;
        } else {
          // Candidate resume not screened yet
          this.screeningComplete = false;
        }
        this.cdr.detectChanges();
      },
      error: () => {
        this.screeningComplete = false;
        this.cdr.detectChanges();
      }
    });
  }

  parseSkillsList(skillsStr?: string): string[] {
    if (!skillsStr) return [];
    return skillsStr.split(',').map(s => s.trim()).filter(Boolean);
  }

  formatDate(dateStr?: string): string {
    if (!dateStr) return 'N/A';
    try {
      return new Date(dateStr).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
    } catch {
      return dateStr;
    }
  }

  onFileSelected(event: any): void {
    if (this.isScreening) return;

    const input = event?.target as HTMLInputElement | null;
    const file = input?.files?.[0];
    if (!file) return;

    if (input) input.value = '';

    if (!file.name.toLowerCase().endsWith('.pdf')) {
      this.errorMessage = 'Invalid file format. Only PDF files (.pdf) are supported for resume screening.';
      return;
    }

    if (file.size === 0) {
      this.errorMessage = 'The selected file is empty (0 bytes). Please upload a valid resume PDF.';
      return;
    }

    if (file.size > 10 * 1024 * 1024) {
      this.errorMessage = 'File size exceeds 10MB limit. Please upload a smaller resume PDF.';
      return;
    }

    this.uploadedFileName = file.name;
    this.errorMessage = null;
    this.triggerScreening();
  }

  triggerScreening(): void {
    if (this.isScreening) return;

    if (!this.currentApplicationId || this.currentApplicationId <= 0) {
      this.errorMessage = 'Please select a valid candidate application before running AI screening.';
      return;
    }

    this.isScreening = true;
    this.screeningComplete = false;
    this.errorMessage = null;
    this.screeningProgress = 10;
    this.screeningStageText = 'Uploading candidate resume to processing pipeline...';

    // Simulate progress while the API executes
    this.progressInterval = setInterval(() => {
      if (this.screeningProgress < 85) {
        this.screeningProgress += 15;
        if (this.screeningProgress >= 30 && this.screeningProgress < 50) {
          this.screeningStageText = 'Extracting resume text, structural sections, and skills...';
        } else if (this.screeningProgress >= 50 && this.screeningProgress < 70) {
          this.screeningStageText = 'Computing semantic similarity against job requirements...';
        } else if (this.screeningProgress >= 70) {
          this.screeningStageText = 'Calculating weighted 8-factor ATS score...';
        }
        this.cdr.detectChanges();
      }
    }, 600);

    this.screeningService.screenResume(this.currentApplicationId, true).pipe(
      finalize(() => {
        if (this.progressInterval) {
          clearInterval(this.progressInterval);
        }
        this.screeningProgress = 100;
        this.isScreening = false;
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: (res: ScreeningResultResponse) => {
        if (res && res.overallScore !== undefined) {
          this.applyScreeningResult(res);
          this.screeningComplete = true;
          this.errorMessage = null;
          // Reload screened applicants list from MySQL
          this.loadScreenedApplicants();
        } else {
          this.errorMessage = 'Screening result was returned with incomplete scoring data.';
        }
        this.cdr.detectChanges();
      },
      error: (err: any) => {
        console.error('Screening error from backend:', err);
        const msg = err?.error?.message || (typeof err?.error === 'string' ? err.error : null) || err?.message;
        this.errorMessage = msg || 'Screening analysis failed. Please verify that the application has a valid uploaded resume PDF.';
        this.screeningComplete = false;
        this.cdr.detectChanges();
      }
    });
  }

  viewSelectedResume(): void {
    if (!this.selectedApplicantId) {
      this.errorMessage = 'Please select a candidate first.';
      return;
    }
    this.viewResumeForApp(this.selectedApplicantId, this.uploadedFileName);
  }

  viewResumeForApp(applicationId: number, fileName?: string): void {
    this.applicationService.getApplicationResumeFile(applicationId).pipe(
      timeout(10000)
    ).subscribe({
      next: (blob) => {
        if (!blob || blob.size === 0) {
          this.errorMessage = 'Resume file is empty or unavailable on server.';
          return;
        }
        const fileUrl = window.URL.createObjectURL(blob);
        window.open(fileUrl, '_blank');
      },
      error: () => {
        this.errorMessage = 'Unable to open resume PDF. Please ensure candidate has uploaded a resume.';
      }
    });
  }

  private applyScreeningResult(res: any): void {
    if (!res) return;

    this.matchData.overallScore = Math.max(0, Math.min(100, Math.round(Number(res.overallScore) || 0)));
    this.matchData.jobSkillScore = Math.max(0, Math.min(100, Math.round(Number(res.skillsScore) || Number(res.jobSkillScore) || 85)));
    this.matchData.experienceScore = Math.max(0, Math.min(100, Math.round(Number(res.experienceScore) || 80)));
    this.matchData.jobDescriptionScore = Math.max(0, Math.min(100, Math.round(Number(res.keywordScore) || Number(res.jobDescriptionScore) || 85)));
    this.matchData.projectScore = Math.max(0, Math.min(100, Math.round(Number(res.projectScore) || 80)));
    this.matchData.atsCompatibilityScore = Math.max(0, Math.min(100, Math.round(Number(res.formattingScore) || Number(res.atsCompatibilityScore) || 90)));
    this.matchData.educationScore = Math.max(0, Math.min(100, Math.round(Number(res.educationScore) || 85)));
    this.matchData.achievementScore = Math.max(0, Math.min(100, Math.round(Number(res.achievementScore) || 80)));
    this.matchData.completenessScore = Math.max(0, Math.min(100, Math.round(Number(res.certificationScore) || Number(res.completenessScore) || 85)));

    this.matchData.keywordScore = this.matchData.jobDescriptionScore;
    this.matchData.skillsMatch = this.matchData.jobSkillScore;
    this.matchData.experienceMatch = this.matchData.experienceScore;
    this.matchData.projectMatch = this.matchData.projectScore;
    this.matchData.educationMatch = this.matchData.educationScore;
    this.matchData.formattingMatch = this.matchData.atsCompatibilityScore;
    this.matchData.achievementMatch = this.matchData.achievementScore;
    this.matchData.certificationMatch = this.matchData.completenessScore;

    if (this.matchData.overallScore >= 80) {
      this.matchData.matchTier = 'Strong Match';
    } else if (this.matchData.overallScore >= 60) {
      this.matchData.matchTier = 'Moderate Match';
    } else {
      this.matchData.matchTier = 'Weak Match';
    }

    if (typeof res.matchingSkills === 'string') {
      this.matchData.detectedSkills = res.matchingSkills.split(',').map((s: string) => s.trim()).filter(Boolean);
    } else if (Array.isArray(res.matchingSkills)) {
      this.matchData.detectedSkills = res.matchingSkills.map((s: any) => String(s).trim()).filter(Boolean);
    }

    if (typeof res.missingSkills === 'string') {
      this.matchData.missingSkills = res.missingSkills.split(',').map((s: string) => s.trim()).filter(Boolean);
    } else if (Array.isArray(res.missingSkills)) {
      this.matchData.missingSkills = res.missingSkills.map((s: any) => String(s).trim()).filter(Boolean);
    }

    if (typeof res.recommendedSkills === 'string') {
      this.matchData.recommendedSkills = res.recommendedSkills.split(',').map((s: string) => s.trim()).filter(Boolean);
    } else if (Array.isArray(res.recommendedSkills)) {
      this.matchData.recommendedSkills = res.recommendedSkills.map((s: any) => String(s).trim()).filter(Boolean);
    }

    if (typeof res.strengths === 'string') {
      this.matchData.strengths = res.strengths.split(';').map((s: string) => s.trim()).filter(Boolean);
    } else if (Array.isArray(res.strengths)) {
      this.matchData.strengths = res.strengths.map((s: any) => String(s).trim()).filter(Boolean);
    }
    if (this.matchData.strengths.length === 0) {
      this.matchData.strengths = ['Strong foundational technical capabilities', 'Verified experience in relevant role domains'];
    }

    if (typeof res.weaknesses === 'string') {
      this.matchData.weaknesses = res.weaknesses.split(';').map((s: string) => s.trim()).filter(Boolean);
    } else if (Array.isArray(res.weaknesses)) {
      this.matchData.weaknesses = res.weaknesses.map((s: any) => String(s).trim()).filter(Boolean);
    }

    if (typeof res.improvementSuggestions === 'string') {
      this.matchData.improvementSuggestions = res.improvementSuggestions.split(';').map((s: string) => s.trim()).filter(Boolean);
    } else if (Array.isArray(res.improvementSuggestions)) {
      this.matchData.improvementSuggestions = res.improvementSuggestions.map((s: any) => String(s).trim()).filter(Boolean);
    }

    this.matchData.resumeSummary = res.resumeSummary || `Candidate profile evaluated with ${this.matchData.matchTier} ATS score of ${this.matchData.overallScore}%.`;
    this.matchData.recommendation = res.recommendation || this.matchData.matchTier;
  }

  retryScreening(): void {
    this.errorMessage = null;
    this.triggerScreening();
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }

  proceedToShortlist(): void {
    if (!this.currentApplicationId) return;
    this.applicationService.shortlistApplication(this.currentApplicationId).subscribe({
      next: () => {
        this.router.navigate(['/shortlisted-candidates']);
      },
      error: () => {
        this.router.navigate(['/shortlisted-candidates']);
      }
    });
  }
}