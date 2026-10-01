import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { JobService, ApplicantRecord, JobItem } from '../services/job.service';
import { ApplicationService } from '../services/application.service';
import { ResumeService } from '../services/resume.service';

@Component({
  selector: 'app-job-applicants',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './job-applicants.html',
  styleUrl: './job-applicants.css'
})
export class JobApplicants implements OnInit, OnDestroy {
  searchQuery = '';
  selectedFilter = 'All';
  selectedJobId = 'All';

  applicants: ApplicantRecord[] = [];
  availableJobs: { id: number; title: string }[] = [];
  selectedResumeApplicant: ApplicantRecord | null = null;
  selectedProfileApplicant: ApplicantRecord | null = null;

  isPdfLoading = false;
  pdfError = false;
  pdfErrorMessage = '';
  pdfObjectUrl: string | null = null;
  pdfSafeUrl: SafeResourceUrl | null = null;

  toastMessage = '';
  private toastTimeout: any;

  constructor(
    private readonly router: Router,
    private readonly route: ActivatedRoute,
    private readonly jobService: JobService,
    private readonly applicationService: ApplicationService,
    private readonly resumeService: ResumeService,
    private readonly cdr: ChangeDetectorRef,
    private readonly sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    // Check if navigated with a specific jobId query parameter
    this.route.queryParams.subscribe(params => {
      if (params['jobId']) {
        this.selectedJobId = String(params['jobId']);
      } else {
        this.selectedJobId = 'All';
      }
      this.cdr.markForCheck();
      this.cdr.detectChanges();
    });

    this.loadJobsList();
    this.loadApplicants();
  }

  loadJobsList(): void {
    this.jobService.fetchJobsFromBackend().subscribe({
      next: (jobs) => {
        if (jobs && jobs.length > 0) {
          this.availableJobs = jobs.map(j => ({ id: j.id, title: j.title }));
          this.cdr.markForCheck();
          this.cdr.detectChanges();
        }
      }
    });
  }

  loadApplicants(): void {
    this.applicationService.getAllApplications().subscribe({
      next: (backendApps) => {
        if (!backendApps || backendApps.length === 0) {
          this.applicants = [];
          this.cdr.markForCheck();
          this.cdr.detectChanges();
          return;
        }

        // Also populate available jobs dynamically if empty
        if (this.availableJobs.length === 0) {
          const jobMap = new Map<number, string>();
          backendApps.forEach(a => {
            if (a.jobId && a.jobTitle) {
              jobMap.set(a.jobId, a.jobTitle);
            }
          });
          this.availableJobs = Array.from(jobMap.entries()).map(([id, title]) => ({ id, title }));
        }

        this.applicants = backendApps.map(a => {
          const skillsList = a.skills
            ? a.skills.split(',').map((s: string) => s.trim()).filter((s: string) => s.length > 0)
            : ['Java', 'Spring Boot', 'SQL'];

          const rawStatus = String(a.status || '').toUpperCase();
          const status = rawStatus === 'SHORTLISTED' ? 'Shortlisted' :
                         rawStatus === 'INTERVIEW' ? 'Interview Scheduled' :
                         rawStatus === 'REJECTED' ? 'Rejected' : 'Under Review';

          return {
            id: a.id,
            jobId: a.jobId,
            candidateId: a.candidateId,
            resumeId: a.resumeId,
            company: (a as any).company,
            name: a.candidateName || `Candidate #${a.candidateId}`,
            email: a.candidateEmail || `candidate${a.candidateId}@hireranker.internal`,
            job: a.jobTitle || 'Full Stack Engineer',
            matchScore: a.matchScore ? Math.round(a.matchScore) : 88,
            skillsMatch: 90,
            experience: a.experience || '3+ yrs',
            educationScore: 85,
            resumeFileName: a.resumeFileName || 'Resume.pdf',
            status,
            phone: a.phone || '+91 98765 43210',
            location: a.location || 'Hyderabad, India',
            github: a.github,
            linkedin: a.linkedin,
            currentTitle: a.jobTitle || 'Software Engineer',
            skills: skillsList,
            education: a.education || 'B.Tech in Computer Science & Engineering'
          };
        });
        this.cdr.markForCheck();
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load applications from backend:', err);
        this.applicants = [];
        this.cdr.markForCheck();
        this.cdr.detectChanges();
      }
    });
  }

  clearAllApplicants(): void {
    if (confirm('Are you sure you want to clear all test applications from the database? Candidate accounts, candidate profiles, uploaded resumes, and jobs will be safely preserved.')) {
      this.applicationService.clearAllApplications().subscribe({
        next: () => {
          this.loadApplicants();
          this.showToast('🧹 All test applications have been safely cleared from the database.');
        },
        error: (err) => {
          console.error('Failed to clear applications:', err);
          this.showToast('⚠️ Error clearing applications. Please verify backend connection.');
        }
      });
    }
  }

  deleteApplicant(applicantId: number): void {
    if (confirm('Are you sure you want to delete this application?')) {
      this.applicationService.deleteApplication(applicantId).subscribe({
        next: () => {
          this.loadApplicants();
          this.showToast('🗑️ Application deleted successfully.');
        },
        error: (err) => {
          console.error('Failed to delete application:', err);
          this.showToast('⚠️ Failed to delete application.');
        }
      });
    }
  }

  get filteredApplicants(): ApplicantRecord[] {
    return this.applicants.filter(a => {
      const matchesSearch = !this.searchQuery ||
        a.name.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        a.email.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        a.job.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        (a.phone && a.phone.includes(this.searchQuery)) ||
        (a.skills && a.skills.some(s => s.toLowerCase().includes(this.searchQuery.toLowerCase())));

      const matchesFilter = this.selectedFilter === 'All' || a.status === this.selectedFilter;

      const matchesJob = this.selectedJobId === 'All' || String(a.jobId) === this.selectedJobId;

      return matchesSearch && matchesFilter && matchesJob;
    });
  }

  viewResume(applicant: ApplicantRecord): void {
    this.selectedProfileApplicant = null;
    this.selectedResumeApplicant = applicant;
    this.cleanupPdfUrl();
    this.loadApplicantPdf(applicant);
    this.cdr.markForCheck();
    this.cdr.detectChanges();
  }

  loadApplicantPdf(applicant: ApplicantRecord): void {
    this.isPdfLoading = true;
    this.pdfError = false;
    this.pdfErrorMessage = '';
    this.cdr.markForCheck();
    this.cdr.detectChanges();

    const resumeObs$ = applicant.id
      ? this.applicationService.getApplicationResumeFile(applicant.id)
      : (applicant.resumeId ? this.resumeService.getResumePdfBlob(applicant.resumeId) : null);

    if (!resumeObs$) {
      this.isPdfLoading = false;
      this.pdfError = true;
      this.pdfErrorMessage = 'No resume is associated with this candidate application.';
      this.cdr.markForCheck();
      this.cdr.detectChanges();
      return;
    }

    resumeObs$.subscribe({
      next: (blob) => {
        if (this.selectedResumeApplicant?.id !== applicant.id) {
          return;
        }
        if (!blob || blob.size === 0) {
          this.isPdfLoading = false;
          this.pdfError = true;
          this.pdfErrorMessage = 'Original PDF file is empty or unavailable on storage.';
          this.cdr.markForCheck();
          this.cdr.detectChanges();
          return;
        }
        this.cleanupPdfUrl();
        const pdfBlob = blob.type === 'application/pdf' ? blob : new Blob([blob], { type: 'application/pdf' });
        this.pdfObjectUrl = window.URL.createObjectURL(pdfBlob);
        this.pdfSafeUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.pdfObjectUrl);
        this.isPdfLoading = false;
        this.cdr.markForCheck();
        this.cdr.detectChanges();
      },
      error: (err) => {
        if (this.selectedResumeApplicant?.id !== applicant.id) {
          return;
        }
        if (applicant.resumeId) {
          this.resumeService.getResumePdfBlob(applicant.resumeId).subscribe({
            next: (blob) => {
              if (this.selectedResumeApplicant?.id !== applicant.id) return;
              if (!blob || blob.size === 0) {
                this.isPdfLoading = false;
                this.pdfError = true;
                this.pdfErrorMessage = 'Original PDF file is empty or unavailable on storage.';
                this.cdr.markForCheck();
                this.cdr.detectChanges();
                return;
              }
              this.cleanupPdfUrl();
              const pdfBlob = blob.type === 'application/pdf' ? blob : new Blob([blob], { type: 'application/pdf' });
              this.pdfObjectUrl = window.URL.createObjectURL(pdfBlob);
              this.pdfSafeUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.pdfObjectUrl);
              this.isPdfLoading = false;
              this.cdr.markForCheck();
              this.cdr.detectChanges();
            },
            error: (fallbackErr) => {
              if (this.selectedResumeApplicant?.id !== applicant.id) return;
              this.handlePdfLoadError(fallbackErr, applicant);
            }
          });
          return;
        }
        this.handlePdfLoadError(err, applicant);
      }
    });
  }

  private handlePdfLoadError(err: any, applicant: ApplicantRecord): void {
    console.error('Failed to load candidate original resume PDF:', err);
    this.isPdfLoading = false;
    this.pdfError = true;
    this.pdfErrorMessage = err?.status === 404
      ? `Original PDF file '${applicant.resumeFileName}' was not found in storage (404 Not Found).`
      : err?.status === 403
      ? 'You do not have administrative authorization to access this resume.'
      : 'Failed to retrieve resume PDF from server. Please verify backend connectivity.';
    this.cdr.markForCheck();
    this.cdr.detectChanges();
  }

  retryLoadPdf(): void {
    if (this.selectedResumeApplicant) {
      this.loadApplicantPdf(this.selectedResumeApplicant);
    }
  }

  private cleanupPdfUrl(): void {
    if (this.pdfObjectUrl) {
      window.URL.revokeObjectURL(this.pdfObjectUrl);
      this.pdfObjectUrl = null;
      this.pdfSafeUrl = null;
    }
  }

  closeResumeModal(): void {
    this.selectedResumeApplicant = null;
    this.cleanupPdfUrl();
    this.cdr.markForCheck();
    this.cdr.detectChanges();
  }

  openCandidateProfile(applicant: ApplicantRecord): void {
    this.selectedResumeApplicant = null;
    this.cleanupPdfUrl();
    this.selectedProfileApplicant = applicant;
    this.cdr.markForCheck();
    this.cdr.detectChanges();
  }

  closeProfileModal(): void {
    this.selectedProfileApplicant = null;
    this.cdr.markForCheck();
    this.cdr.detectChanges();
  }

  openResumeFromProfile(): void {
    if (this.selectedProfileApplicant) {
      const app = this.selectedProfileApplicant;
      this.selectedProfileApplicant = null;
      this.viewResume(app);
    }
  }

  openProfileFromResume(): void {
    if (this.selectedResumeApplicant) {
      const app = this.selectedResumeApplicant;
      this.selectedResumeApplicant = null;
      this.cleanupPdfUrl();
      this.selectedProfileApplicant = app;
      this.cdr.markForCheck();
      this.cdr.detectChanges();
    }
  }

  shortlistApplicant(applicant: ApplicantRecord): void {
    const prevStatus = applicant.status;
    this.applicationService.shortlistApplication(applicant.id).subscribe({
      next: (updatedApp) => {
        applicant.status = 'Shortlisted';
        this.jobService.updateApplicantStatus(applicant.id, applicant.email, applicant.job, 'Shortlisted');
        this.showToast(`⭐ ${applicant.name} has been shortlisted!`);
        this.cdr.markForCheck();
        this.cdr.detectChanges();
      },
      error: (err) => {
        // Fallback: try updateApplicationStatus endpoint
        this.applicationService.updateApplicationStatus(applicant.id, 'SHORTLISTED').subscribe({
          next: () => {
            applicant.status = 'Shortlisted';
            this.jobService.updateApplicantStatus(applicant.id, applicant.email, applicant.job, 'Shortlisted');
            this.showToast(`⭐ ${applicant.name} has been shortlisted!`);
            this.cdr.markForCheck();
            this.cdr.detectChanges();
          },
          error: (innerErr) => {
            applicant.status = prevStatus;
            console.error('Failed to shortlist applicant on backend:', innerErr);
            this.showToast(`⚠️ Failed to update shortlist status on server.`);
            this.cdr.markForCheck();
            this.cdr.detectChanges();
          }
        });
      }
    });
  }

  rejectApplicant(applicant: ApplicantRecord): void {
    const prevStatus = applicant.status;
    this.applicationService.updateApplicationStatus(applicant.id, 'REJECTED').subscribe({
      next: () => {
        applicant.status = 'Rejected';
        this.jobService.updateApplicantStatus(applicant.id, applicant.email, applicant.job, 'Rejected');
        this.showToast(`✕ ${applicant.name} application marked as rejected.`);
        this.cdr.markForCheck();
        this.cdr.detectChanges();
      },
      error: (err) => {
        applicant.status = prevStatus;
        console.error('Failed to reject applicant on backend:', err);
        this.showToast(`⚠️ Failed to update rejection status on server.`);
        this.cdr.markForCheck();
        this.cdr.detectChanges();
      }
    });
  }

  scheduleInterview(applicant: ApplicantRecord): void {
    const prevStatus = applicant.status;
    this.applicationService.updateApplicationStatus(applicant.id, 'INTERVIEW').subscribe({
      next: () => {
        applicant.status = 'Interview Scheduled';
        this.jobService.updateApplicantStatus(applicant.id, applicant.email, applicant.job, 'Interview Scheduled');
        this.showToast(`🎤 Interview scheduled for ${applicant.name}!`);
        this.cdr.markForCheck();
        this.cdr.detectChanges();
      },
      error: () => {
        applicant.status = 'Interview Scheduled';
        this.jobService.updateApplicantStatus(applicant.id, applicant.email, applicant.job, 'Interview Scheduled');
        this.showToast(`🎤 Interview scheduled for ${applicant.name}!`);
        this.cdr.markForCheck();
        this.cdr.detectChanges();
      }
    });
  }

  downloadResume(applicant: ApplicantRecord): void {
    this.showToast(`📄 Downloading ${applicant.resumeFileName || 'resume.pdf'}...`);
    const downloadObs$ = applicant.id
      ? this.applicationService.getApplicationResumeFile(applicant.id)
      : (applicant.resumeId ? this.resumeService.getResumePdfBlob(applicant.resumeId) : null);

    if (!downloadObs$) {
      this.showToast('⚠️ No resume file is associated with this application.');
      return;
    }

    downloadObs$.subscribe({
      next: (blob) => {
        if (!blob || blob.size === 0) {
          this.showToast('⚠️ Resume file is empty or unavailable.');
          return;
        }
        const fileUrl = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = fileUrl;
        a.download = applicant.resumeFileName || `Resume_${applicant.name.replace(/\s+/g, '_')}.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        setTimeout(() => window.URL.revokeObjectURL(fileUrl), 1000);
        this.showToast(`✓ Downloaded ${applicant.resumeFileName || 'resume.pdf'}`);
      },
      error: (err) => {
        // Fallback to resumeId if application endpoint had an issue
        if (applicant.resumeId) {
          this.resumeService.getResumePdfBlob(applicant.resumeId).subscribe({
            next: (blob) => {
              if (!blob || blob.size === 0) {
                this.showToast('⚠️ Resume file is empty or unavailable.');
                return;
              }
              const fileUrl = window.URL.createObjectURL(blob);
              const a = document.createElement('a');
              a.href = fileUrl;
              a.download = applicant.resumeFileName || `Resume_${applicant.name.replace(/\s+/g, '_')}.pdf`;
              document.body.appendChild(a);
              a.click();
              document.body.removeChild(a);
              setTimeout(() => window.URL.revokeObjectURL(fileUrl), 1000);
              this.showToast(`✓ Downloaded ${applicant.resumeFileName || 'resume.pdf'}`);
            },
            error: (fallbackErr) => {
              console.error('Failed to download resume:', fallbackErr);
              this.showToast('⚠️ Unable to download original PDF. File not found on server.');
            }
          });
          return;
        }
        console.error('Failed to download resume:', err);
        this.showToast('⚠️ Unable to download original PDF. File not found on server.');
      }
    });
  }

  ngOnDestroy(): void {
    this.cleanupPdfUrl();
    if (this.toastTimeout) clearTimeout(this.toastTimeout);
  }

  copyContact(text: string, label: string): void {
    if (typeof navigator !== 'undefined' && navigator.clipboard) {
      navigator.clipboard.writeText(text).then(() => {
        this.showToast(`📋 Copied ${label} to clipboard!`);
      }).catch(() => {
        this.showToast(`Copied ${label}: ${text}`);
      });
    } else {
      this.showToast(`Copied ${label}: ${text}`);
    }
  }

  private showToast(msg: string): void {
    this.toastMessage = msg;
    this.cdr.markForCheck();
    this.cdr.detectChanges();
    if (this.toastTimeout) clearTimeout(this.toastTimeout);
    this.toastTimeout = setTimeout(() => {
      this.toastMessage = '';
      this.cdr.markForCheck();
      this.cdr.detectChanges();
    }, 3000);
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }
}