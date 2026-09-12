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
  candidates: ShortlistedCandidate[] = [
    {
      id: 1,
      name: 'Eshwar Rao',
      email: 'eshwar.rao@email.com',
      appliedRole: 'Java Full Stack Developer',
      matchScore: 95,
      experience: '4.5 Yrs Experience',
      skills: ['Java 21', 'Spring Boot', 'Angular', 'Microservices', 'PostgreSQL'],
      resumeFileName: 'Eshwar_Rao_Resume.pdf',
      interviewStatus: 'Not Scheduled'
    },
    {
      id: 2,
      name: 'Krupa Jyothi',
      email: 'krupa.jyothi@email.com',
      appliedRole: 'Senior Angular Developer',
      matchScore: 89,
      experience: '3.8 Yrs Experience',
      skills: ['Angular 18', 'TypeScript', 'RxJS', 'NgRx', 'Tailwind'],
      resumeFileName: 'Krupa_Jyothi_Resume.pdf',
      interviewStatus: 'Scheduled'
    },
    {
      id: 3,
      name: 'Durga Rohith',
      email: 'durga.rohith@email.com',
      appliedRole: 'Java Developer',
      matchScore: 84,
      experience: '3.2 Yrs Experience',
      skills: ['Java 17', 'Spring Boot', 'REST APIs', 'MySQL'],
      resumeFileName: 'Durga_Rohith_Resume.pdf',
      interviewStatus: 'Not Scheduled'
    }
  ];

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