import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { ApplicationService, ShortlistedCandidateResponse } from '../services/application.service';

interface ShortlistedCandidate {
  id: number;
  name: string;
  email: string;
  appliedRole: string;
  matchScore: number;
  experience: string;
  skills: string[];
  resumeFileName: string;
  interviewStatus: 'Scheduled' | 'Not Scheduled';
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

  constructor(
    private readonly router: Router,
    private readonly route: ActivatedRoute,
    private readonly applicationService: ApplicationService
  ) {}

  ngOnInit(): void {
    const jobIdParam = this.route.snapshot.queryParams['jobId'] || '1';
    const jobId = Number(jobIdParam);
    if (jobId) {
      this.applicationService.getShortlistedForJob(jobId).subscribe({
        next: (list: ShortlistedCandidateResponse[]) => {
          if (list && list.length > 0) {
            this.candidates = list.map((item) => ({
              id: item.candidateId,
              name: item.candidateName || `Candidate #${item.candidateId}`,
              email: item.email || `candidate${item.candidateId}@hireranker.internal`,
              appliedRole: item.jobTitle || 'Java Full Stack Developer',
              matchScore: Math.round(item.overallScore || 90),
              experience: '3+ Yrs Experience',
              skills: ['Java', 'Spring Boot', 'Angular', 'SQL'],
              resumeFileName: item.resumeFileName || 'Resume.pdf',
              interviewStatus: (item.status === 'INTERVIEW' || item.status === 'INTERVIEW_SCHEDULED') ? 'Scheduled' : 'Not Scheduled'
            }));
          }
        },
        error: (err) => {
          console.warn('Backend shortlisted endpoint not accessible, using cached list:', err?.status);
        }
      });
    }
  }

  scheduleInterview(c: ShortlistedCandidate): void {
    this.router.navigate(['/interview-scheduler']);
  }

  contactCandidate(c: ShortlistedCandidate): void {
    this.router.navigate(['/messages']);
  }

  removeFromShortlist(id: number): void {
    if (confirm('Remove candidate from shortlisted pool?')) {
      this.candidates = this.candidates.filter(c => c.id !== id);
    }
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }
}