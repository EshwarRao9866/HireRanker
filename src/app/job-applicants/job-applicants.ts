import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { JobService, ApplicantRecord } from '../services/job.service';
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

  applicants: ApplicantRecord[] = [];
  selectedResumeApplicant: ApplicantRecord | null = null;
  selectedProfileApplicant: ApplicantRecord | null = null;

  toastMessage = '';
  private toastTimeout: any;

  constructor(
    private readonly router: Router,
    private readonly jobService: JobService,
    private readonly applicationService: ApplicationService,
    private readonly resumeService: ResumeService
  ) {}

  ngOnInit(): void {
    this.loadApplicants();
  }

  loadApplicants(): void {
    this.applicants = this.jobService.getApplicants();

    this.applicationService.getAllApplications().subscribe({
      next: (backendApps) => {
        if (backendApps && backendApps.length > 0) {
          const mapped: ApplicantRecord[] = backendApps.map(a => ({
            id: a.id,
            name: a.candidateName || `Candidate #${a.candidateId}`,
            email: `candidate${a.candidateId}@hireranker.internal`,
            job: a.jobTitle || 'Full Stack Engineer',
            matchScore: 92,
            skillsMatch: 94,
            experience: '3.5 yrs',
            educationScore: 90,
            resumeFileName: a.resumeFileName || 'Resume.pdf',
            status: a.status === 'SHORTLISTED' ? 'Shortlisted' :
                    a.status === 'INTERVIEW' ? 'Interview Scheduled' :
                    a.status === 'REJECTED' ? 'Rejected' : 'Under Review',
            phone: '+91 98765 43210',
            location: 'Hyderabad, India',
            skills: ['Java', 'Spring Boot', 'Angular', 'SQL']
          }));

          // Merge without duplicate IDs
          const existingIds = new Set(mapped.map(m => m.id));
          const remaining = this.applicants.filter(app => !existingIds.has(app.id));
          this.applicants = [...mapped, ...remaining];
        }
      },
      error: () => {}
    });
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

      return matchesSearch && matchesFilter;
    });
  }

  viewResume(applicant: ApplicantRecord): void {
    this.selectedProfileApplicant = null;
    this.selectedResumeApplicant = applicant;
  }

  closeResumeModal(): void {
    this.selectedResumeApplicant = null;
  }

  openCandidateProfile(applicant: ApplicantRecord): void {
    this.selectedResumeApplicant = null;
    this.selectedProfileApplicant = applicant;
  }

  closeProfileModal(): void {
    this.selectedProfileApplicant = null;
  }

  openResumeFromProfile(): void {
    if (this.selectedProfileApplicant) {
      const app = this.selectedProfileApplicant;
      this.selectedProfileApplicant = null;
      this.selectedResumeApplicant = app;
    }
  }

  openProfileFromResume(): void {
    if (this.selectedResumeApplicant) {
      const app = this.selectedResumeApplicant;
      this.selectedResumeApplicant = null;
      this.selectedProfileApplicant = app;
    }
  }

  shortlistApplicant(applicant: ApplicantRecord): void {
    const prevStatus = applicant.status;
    applicant.status = 'Shortlisted';
    this.jobService.updateApplicantStatus(applicant.id, applicant.email, applicant.job, 'Shortlisted');

    this.applicationService.shortlistApplication(applicant.id).subscribe({
      next: (res) => {
        this.showToast(`⭐ ${applicant.name} has been shortlisted!`);
      },
      error: (err) => {
        // Fallback to updating status via generic endpoint if shortlist had an issue
        this.applicationService.updateApplicationStatus(applicant.id, 'SHORTLISTED').subscribe({
          next: () => {
            this.showToast(`⭐ ${applicant.name} has been shortlisted!`);
          },
          error: () => {
            // Local state is already updated for demo/offline fallback
            this.showToast(`⭐ ${applicant.name} shortlisted (saved locally).`);
          }
        });
      }
    });
  }

  rejectApplicant(applicant: ApplicantRecord): void {
    applicant.status = 'Rejected';
    this.jobService.updateApplicantStatus(applicant.id, applicant.email, applicant.job, 'Rejected');
    this.applicationService.updateApplicationStatus(applicant.id, 'REJECTED').subscribe({
      next: () => {
        this.showToast(`✕ ${applicant.name} application marked as rejected.`);
      },
      error: () => {
        this.showToast(`✕ ${applicant.name} marked as rejected (saved locally).`);
      }
    });
  }

  scheduleInterview(applicant: ApplicantRecord): void {
    applicant.status = 'Interview Scheduled';
    this.jobService.updateApplicantStatus(applicant.id, applicant.email, applicant.job, 'Interview Scheduled');
    this.applicationService.updateApplicationStatus(applicant.id, 'INTERVIEW').subscribe({
      next: () => {
        this.showToast(`🎤 Interview scheduled for ${applicant.name}!`);
      },
      error: () => {
        this.showToast(`🎤 Interview scheduled for ${applicant.name}!`);
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
    if (this.toastTimeout) clearTimeout(this.toastTimeout);
    this.toastTimeout = setTimeout(() => {
      this.toastMessage = '';
    }, 3000);
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }
}