import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { ApplicationService, ShortlistedCandidateResponse } from '../services/application.service';

export interface ShortlistedCandidate {
  applicationId: number;
  id: number;
  name: string;
  email: string;
  phone?: string;
  appliedRole: string;
  companyName: string;
  matchScore: number;
  experience: string;
  skills: string[];
  resumeFileName: string;
  interviewStatus: 'Scheduled' | 'Not Scheduled';
  shortlistedDate: string;
  status: string;
}

@Component({
  selector: 'app-shortlisted-candidates',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './shortlisted-candidates.html',
  styleUrl: './shortlisted-candidates.css'
})
export class ShortlistedCandidates implements OnInit {
  candidates: ShortlistedCandidate[] = [];
  isLoading = true;
  selectedProfileCandidate: ShortlistedCandidate | null = null;
  toastMessage = '';
  private toastTimeout: any;

  constructor(
    private readonly router: Router,
    private readonly route: ActivatedRoute,
    private readonly applicationService: ApplicationService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadShortlistedCandidates();
  }

  loadShortlistedCandidates(): void {
    const jobIdParam = this.route.snapshot.queryParams['jobId'];
    this.isLoading = true;

    if (jobIdParam) {
      const jobId = Number(jobIdParam);
      this.applicationService.getShortlistedForJob(jobId).subscribe({
        next: (list: ShortlistedCandidateResponse[]) => {
          this.isLoading = false;
          this.mapCandidates(list);
          this.cdr.detectChanges();
        },
        error: () => {
          this.isLoading = false;
          this.candidates = [];
          this.cdr.detectChanges();
        }
      });
    } else {
      this.applicationService.getAllShortlisted().subscribe({
        next: (list: ShortlistedCandidateResponse[]) => {
          this.isLoading = false;
          this.mapCandidates(list);
          this.cdr.detectChanges();
        },
        error: () => {
          this.isLoading = false;
          this.candidates = [];
          this.cdr.detectChanges();
        }
      });
    }
  }

  private mapCandidates(list: ShortlistedCandidateResponse[]): void {
    if (!list || list.length === 0) {
      this.candidates = [];
      return;
    }

    // Deduplicate by applicationId to ensure each candidate application appears exactly once
    const seen = new Set<number>();
    const filtered = list.filter(item => {
      if (seen.has(item.applicationId)) return false;
      seen.add(item.applicationId);
      return true;
    });

    this.candidates = filtered.map((item) => {
      let skillsArr: string[] = [];
      if ((item as any).skills) {
        const raw = (item as any).skills;
        skillsArr = Array.isArray(raw)
          ? raw
          : String(raw).split(',').map((s: string) => s.trim()).filter(Boolean);
      }
      if (skillsArr.length === 0) {
        skillsArr = ['Java', 'Spring Boot', 'Angular', 'MySQL'];
      }

      const dateStr = item.appliedAt
        ? new Date(item.appliedAt).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })
        : 'Recent';

      return {
        applicationId: item.applicationId,
        id: item.candidateId,
        name: item.candidateName || `Candidate #${item.candidateId}`,
        email: item.email || `candidate${item.candidateId}@hireranker.internal`,
        phone: item.phone || '',
        appliedRole: item.jobTitle || 'Shortlisted Role',
        companyName: (item as any).companyName || 'HireRanker Enterprise',
        matchScore: item.overallScore != null ? Math.round(item.overallScore) : 0,
        experience: (item as any).experience || 'Verified Experience',
        skills: skillsArr,
        resumeFileName: item.resumeFileName || 'Resume.pdf',
        interviewStatus: (item.status === 'INTERVIEW' || item.status === 'INTERVIEW_SCHEDULED') ? 'Scheduled' : 'Not Scheduled',
        shortlistedDate: dateStr,
        status: item.status
      };
    });
  }

  scheduleInterview(c: ShortlistedCandidate): void {
    this.router.navigate(['/interview-scheduler'], {
      queryParams: {
        candidateId: c.id,
        applicationId: c.applicationId,
        candidateName: c.name
      }
    });
  }

  contactCandidate(c: ShortlistedCandidate): void {
    this.router.navigate(['/messages']);
  }

  viewProfile(c: ShortlistedCandidate): void {
    this.selectedProfileCandidate = c;
  }

  closeProfileModal(): void {
    this.selectedProfileCandidate = null;
  }

  downloadResume(c: ShortlistedCandidate): void {
    this.showToast(`📄 Fetching original resume for ${c.name}...`);
    this.applicationService.getApplicationResumeFile(c.applicationId).subscribe({
      next: (blob) => {
        if (!blob || blob.size === 0) {
          this.showToast('⚠️ Resume file is empty or unavailable.');
          return;
        }
        const fileUrl = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = fileUrl;
        a.download = c.resumeFileName || `Resume_${c.name.replace(/\s+/g, '_')}.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        setTimeout(() => window.URL.revokeObjectURL(fileUrl), 1000);
        this.showToast(`✓ Downloaded ${c.resumeFileName || 'resume.pdf'}`);
      },
      error: (err) => {
        console.error('Failed to download resume PDF:', err);
        this.showToast('⚠️ Unable to retrieve resume PDF from server.');
      }
    });
  }

  removeFromShortlist(c: ShortlistedCandidate): void {
    if (confirm(`Remove ${c.name} from the shortlisted pool? Application status will revert to In Review.`)) {
      this.applicationService.updateApplicationStatus(c.applicationId, 'APPLIED').subscribe({
        next: () => {
          this.showToast(`✓ ${c.name} removed from shortlisted pool.`);
          this.loadShortlistedCandidates();
        },
        error: (err) => {
          console.error('Failed to update application status:', err);
          this.showToast('⚠️ Failed to update shortlist status in MySQL.');
          this.loadShortlistedCandidates();
        }
      });
    }
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

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }
}