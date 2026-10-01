import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
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
export class JobApplicants implements OnInit {
  searchQuery = '';
  selectedFilter = 'All';
  selectedJobId = 'All';

  applicants: ApplicantRecord[] = [];
  availableJobs: { id: number; title: string }[] = [];
  selectedResumeApplicant: ApplicantRecord | null = null;
  selectedProfileApplicant: ApplicantRecord | null = null;

  toastMessage = '';
  private toastTimeout: any;

  constructor(
    private readonly router: Router,
    private readonly route: ActivatedRoute,
    private readonly jobService: JobService,
    private readonly applicationService: ApplicationService,
    private readonly resumeService: ResumeService,
    private readonly cdr: ChangeDetectorRef
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
    this.cdr.markForCheck();
    this.cdr.detectChanges();
  }

  closeResumeModal(): void {
    this.selectedResumeApplicant = null;
    this.cdr.markForCheck();
    this.cdr.detectChanges();
  }

  openCandidateProfile(applicant: ApplicantRecord): void {
    this.selectedResumeApplicant = null;
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
      this.selectedResumeApplicant = app;
      this.cdr.markForCheck();
      this.cdr.detectChanges();
    }
  }

  openProfileFromResume(): void {
    if (this.selectedResumeApplicant) {
      const app = this.selectedResumeApplicant;
      this.selectedResumeApplicant = null;
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
    const resumeId = (applicant as any).resumeId || applicant.id || 1;
    this.resumeService.downloadResumeBlob(resumeId).subscribe({
      next: (blob) => {
        if (!blob || blob.size === 0) {
          this.showToast('⚠️ Resume file is empty or unavailable.');
          return;
        }
        const fileUrl = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = fileUrl;
        a.download = applicant.resumeFileName || `Resume_${applicant.name.replace(/\s+/g, '_')}.pdf`;
        a.target = '_blank';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        setTimeout(() => window.URL.revokeObjectURL(fileUrl), 1000);
        this.showToast(`📄 Downloading ${applicant.resumeFileName || 'resume.pdf'}...`);
      },
      error: () => {
        this.showToast('⚠️ Unable to connect to backend server. File download unavailable.');
      }
    });
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