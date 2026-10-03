import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subscription, of } from 'rxjs';
import { finalize, switchMap, catchError } from 'rxjs/operators';
import { InterviewService, ScheduledInterview } from '../services/interview.service';
import { ApplicationService, ApplicationResponse, JobApplicantOption } from '../services/application.service';
import { JobService, JobItem } from '../services/job.service';

export interface CalendarDay {
  day: string;
  date: number;
  dateKey: string;
  fullDate: Date;
  formatted: string;
  hasEvent: boolean;
  active: boolean;
}

@Component({
  selector: 'app-interview-scheduler',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './interview-scheduler.html',
  styleUrl: './interview-scheduler.css'
})
export class InterviewScheduler implements OnInit, OnDestroy {
  showScheduleModal = false;
  applications: ApplicationResponse[] = [];

  // Dynamic Job and Candidate Dropdown state
  availableJobs: JobItem[] = [];
  isJobsLoading = false;
  jobsError: string | null = null;
  selectedJobId: number | null = null;

  jobApplicants: JobApplicantOption[] = [];
  isApplicantsLoading = false;
  applicantsError: string | null = null;
  selectedCandidateId: number | null = null;
  selectedApplicationId: number | null = null;
  selectedApplicant: JobApplicantOption | null = null;

  isScheduling = false;
  scheduleError: string | null = null;
  scheduleSuccess: string | null = null;

  readonly currentDate = new Date();
  readonly todayKey = this.formatDateKey(this.currentDate);
  readonly todayFormatted = this.formatDisplayDate(this.currentDate);
  readonly currentMonthYear = this.currentDate.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });

  selectedDate: Date = new Date();
  selectedDateKey: string = this.formatDateKey(this.selectedDate);

  interviews: ScheduledInterview[] = [];
  filteredInterviews: ScheduledInterview[] = [];
  calendarDays: CalendarDay[] = [];

  reschedulingInterviewId: number | null = null;
  private interviewsSub?: Subscription;

  newInterview: Partial<ScheduledInterview> = {
    candidate: '',
    job: '',
    date: this.formatDateKey(new Date(Date.now() + 86400000)),
    time: '11:00 AM',
    interviewer: 'Tech Lead Panel',
    type: 'Live Technical',
    status: 'Scheduled'
  };

  get selectedTimelineTitle(): string {
    if (this.selectedDateKey === this.todayKey) {
      return `Today's Timeline (${this.todayFormatted})`;
    }
    const day = this.calendarDays.find(d => d.dateKey === this.selectedDateKey);
    const dateLabel = day ? day.formatted : this.formatDisplayDate(this.selectedDate);
    return `Timeline (${dateLabel})`;
  }

  get totalScheduledCount(): number {
    return this.interviews.length;
  }

  get aiAssessmentCount(): number {
    return this.interviews.filter(i => (i.type || '').toLowerCase().includes('ai')).length;
  }

  get liveTechnicalCount(): number {
    return this.interviews.filter(i => (i.type || '').toLowerCase().includes('technical') || (i.type || '').toLowerCase().includes('live')).length;
  }

  get activeScheduledCount(): number {
    return this.interviews.filter(i => i.status === 'Scheduled' || i.status === 'In Progress').length;
  }

  get upcomingInterviews(): ScheduledInterview[] {
    return this.interviews
      .filter(i => i.status === 'Scheduled' || i.status === 'In Progress')
      .sort((a, b) => new Date(a.rawDateTime || a.date).getTime() - new Date(b.rawDateTime || b.date).getTime());
  }

  get pastInterviews(): ScheduledInterview[] {
    return this.interviews
      .filter(i => i.status === 'Completed' || i.status === 'Cancelled')
      .sort((a, b) => new Date(b.rawDateTime || b.date).getTime() - new Date(a.rawDateTime || a.date).getTime());
  }

  constructor(
    private readonly router: Router,
    readonly interviewService: InterviewService,
    private readonly applicationService: ApplicationService,
    private readonly jobService: JobService,
    private readonly cdr: ChangeDetectorRef
  ) {
    this.calendarDays = this.generateCurrentWeekDays();
  }

  ngOnInit(): void {
    // 1. Subscribe to shared reactive interview state (Single Source of Truth)
    this.interviewsSub = this.interviewService.interviews$.subscribe({
      next: (list) => {
        this.interviews = list || [];
        this.updateCalendarEventIndicators();
        this.updateFilteredInterviews();
        this.cdr.detectChanges();
      }
    });

    // 2. Refresh database interviews from backend immediately for Admin view
    this.interviewService.refreshInterviewsFromBackend(true).subscribe({
      next: (list) => {
        if (list && list.length > 0) {
          this.interviews = list;
          this.updateCalendarEventIndicators();
          this.updateFilteredInterviews();
          this.cdr.detectChanges();
        }
      },
      error: () => {}
    });

    // 3. Fetch applications for candidate selector in modal
    this.applicationService.getAllApplications().subscribe({
      next: (apps) => {
        if (apps && apps.length > 0) {
          this.applications = apps;
        }
      },
      error: () => {}
    });

    // 4. Preload jobs for scheduler modal
    this.loadJobs();
  }

  ngOnDestroy(): void {
    if (this.interviewsSub) {
      this.interviewsSub.unsubscribe();
    }
  }

  formatDateKey(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  formatDisplayDate(date: Date): string {
    return date.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  private generateCurrentWeekDays(): CalendarDay[] {
    const dayNames = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
    const now = new Date();
    const currentDayOfWeek = now.getDay();
    const days: CalendarDay[] = [];

    for (let i = 0; i < 7; i++) {
      const d = new Date(now.getFullYear(), now.getMonth(), now.getDate() - currentDayOfWeek + i);
      const key = this.formatDateKey(d);
      const isToday = key === this.todayKey;

      days.push({
        day: dayNames[d.getDay()],
        date: d.getDate(),
        dateKey: key,
        fullDate: d,
        formatted: this.formatDisplayDate(d),
        hasEvent: false,
        active: isToday
      });
    }

    return days;
  }

  selectDate(day: CalendarDay): void {
    if (!day) return;
    this.selectedDate = new Date(day.fullDate);
    this.selectedDateKey = day.dateKey;

    // Update active highlight in week view
    this.calendarDays.forEach(d => {
      d.active = (d.dateKey === day.dateKey);
    });

    // Update timeline slots for the newly selected date
    this.updateFilteredInterviews();
  }

  updateFilteredInterviews(): void {
    this.filteredInterviews = this.interviews.filter(inv => {
      return inv.dateKey === this.selectedDateKey && inv.status !== 'Cancelled';
    });
  }

  updateCalendarEventIndicators(): void {
    this.calendarDays.forEach(day => {
      day.hasEvent = this.interviews.some(inv => inv.dateKey === day.dateKey && inv.status !== 'Cancelled');
    });
  }

  openScheduleModal(): void {
    this.selectedJobId = null;
    this.selectedCandidateId = null;
    this.selectedApplicationId = null;
    this.selectedApplicant = null;
    this.jobApplicants = [];
    this.isApplicantsLoading = false;
    this.applicantsError = null;
    this.scheduleError = null;
    this.scheduleSuccess = null;
    this.isScheduling = false;

    this.newInterview = {
      candidate: '',
      job: '',
      date: this.formatDateKey(new Date(Date.now() + 86400000)),
      time: '11:00 AM',
      interviewer: 'Tech Lead Panel',
      type: 'Live Technical',
      status: 'Scheduled'
    };
    this.showScheduleModal = true;
    this.loadJobs();
  }

  closeScheduleModal(): void {
    this.showScheduleModal = false;
    this.selectedJobId = null;
    this.selectedCandidateId = null;
    this.selectedApplicationId = null;
    this.selectedApplicant = null;
    this.jobApplicants = [];
    this.applicantsError = null;
    this.scheduleError = null;
    this.scheduleSuccess = null;
    this.isScheduling = false;
    this.reschedulingInterviewId = null;
    this.cdr.detectChanges();
  }

  loadJobs(): void {
    this.isJobsLoading = true;
    this.jobsError = null;

    this.jobService.fetchJobsFromBackend().subscribe({
      next: (jobs) => {
        // Exclude deleted or inactive jobs
        this.availableJobs = (jobs || []).filter(j => j.status === 'Active' || !j.status);
        this.isJobsLoading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Failed to load jobs for scheduler modal:', err);
        this.jobsError = 'Unable to load job postings. Please retry.';
        this.isJobsLoading = false;
        this.cdr.markForCheck();
      }
    });
  }

  onJobSelected(jobId: any): void {
    const parsedId = jobId ? Number(jobId) : null;
    this.selectedJobId = parsedId;

    // Rule: If the job selection changes, clear the previously selected candidate immediately
    this.selectedCandidateId = null;
    this.selectedApplicationId = null;
    this.selectedApplicant = null;
    this.jobApplicants = [];
    this.applicantsError = null;
    this.scheduleError = null;
    this.newInterview.candidate = '';

    const matchedJob = this.availableJobs.find(j => j.id === parsedId);
    this.newInterview.job = matchedJob ? matchedJob.title : '';

    if (parsedId) {
      this.loadApplicantsForJob(parsedId);
    }
    this.cdr.detectChanges();
  }

  loadApplicantsForJob(jobId: number): void {
    this.isApplicantsLoading = true;
    this.applicantsError = null;
    this.jobApplicants = [];

    this.applicationService.getApplicantsByJob(jobId).subscribe({
      next: (applicants) => {
        // Avoid duplicate candidates if repository queries return duplicate rows
        const unique = new Map<number, JobApplicantOption>();
        for (const app of applicants || []) {
          if (app.candidateId && !unique.has(app.candidateId)) {
            unique.set(app.candidateId, app);
          }
        }
        this.jobApplicants = Array.from(unique.values());
        this.isApplicantsLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error(`Failed to load applicants for job ${jobId}:`, err);
        this.applicantsError = 'Failed to load applicants for this job.';
        this.isApplicantsLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  onCandidateSelected(candidateId: any): void {
    const parsedId = candidateId ? Number(candidateId) : null;
    this.selectedCandidateId = parsedId;
    this.scheduleError = null;

    const applicant = this.jobApplicants.find(a => a.candidateId === parsedId);
    if (applicant) {
      this.selectedApplicant = applicant;
      this.selectedApplicationId = applicant.applicationId;
      this.newInterview.candidate = applicant.candidateName;
    } else {
      this.selectedApplicant = null;
      this.selectedApplicationId = null;
      this.newInterview.candidate = '';
    }
    this.cdr.detectChanges();
  }

  scheduleNewInterview(): void {
    if (this.isScheduling) {
      return;
    }

    this.scheduleError = null;
    this.scheduleSuccess = null;

    if (!this.newInterview.date) {
      this.scheduleError = 'Please select an interview Date.';
      return;
    }

    const timeStr = this.parseTimeTo24h(this.newInterview.time);
    const dateKey = this.newInterview.date;
    const isoDateTime = `${dateKey}T${timeStr}`;

    const scheduledDateObj = new Date(isoDateTime);
    if (isNaN(scheduledDateObj.getTime()) || scheduledDateObj.getTime() < Date.now() - 60000) {
      this.scheduleError = 'Interview cannot be scheduled in the past. Please select a future date and time.';
      return;
    }

    // Handle RESCHEDULE flow for existing interview
    if (this.reschedulingInterviewId) {
      this.isScheduling = true;
      this.cdr.detectChanges();
      const notes = `${this.newInterview.type || 'Technical Round'} with ${this.newInterview.interviewer || 'Panel'}`;

      this.interviewService.rescheduleInterview(this.reschedulingInterviewId, isoDateTime, notes).pipe(
        finalize(() => {
          this.isScheduling = false;
          this.cdr.detectChanges();
        })
      ).subscribe({
        next: () => {
          this.scheduleSuccess = `Interview successfully rescheduled for ${this.newInterview.candidate}!`;
          this.interviewService.refreshInterviewsFromBackend(true).subscribe();
          setTimeout(() => {
            this.closeScheduleModal();
          }, 1000);
        },
        error: (err) => {
          console.error('Backend reschedule error:', err);
          const errMsg = err?.error?.message || (typeof err?.error === 'string' ? err.error : null);
          this.scheduleError = errMsg || 'Failed to reschedule interview. Please verify details and try again.';
          this.cdr.detectChanges();
        }
      });
      return;
    }

    // Handle NEW SCHEDULE flow
    if (!this.selectedJobId) {
      this.scheduleError = 'Please select a Job Position.';
      return;
    }

    if (!this.selectedCandidateId || !this.selectedApplicationId) {
      this.scheduleError = 'Please select a Candidate.';
      return;
    }

    const inputDate = new Date(this.newInterview.date);
    const isToday = dateKey === this.todayKey;
    const displayDate = isToday ? `Today, ${this.todayFormatted}` : this.formatDisplayDate(inputDate);

    const interviewType: 'ONLINE' | 'OFFLINE' | 'PHONE' = this.newInterview.type === 'HR Round' ? 'PHONE' : 'ONLINE';
    const meetingLink = interviewType === 'ONLINE' ? `https://meet.hireranker.com/session-${Date.now()}` : undefined;

    this.isScheduling = true;
    this.cdr.detectChanges();

    this.interviewService.scheduleInterview({
      applicationId: this.selectedApplicationId,
      scheduledDateTime: isoDateTime,
      type: interviewType,
      interviewType: interviewType,
      meetingLink: meetingLink,
      notes: `${this.newInterview.type || 'Technical Round'} with ${this.newInterview.interviewer || 'Panel'}`
    }).pipe(
      finalize(() => {
        this.isScheduling = false;
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: (created) => {
        this.scheduleSuccess = `Interview successfully scheduled for ${this.newInterview.candidate}!`;

        // Refresh interviews from backend to update Roster, Timeline, Calendar
        this.interviewService.refreshInterviewsFromBackend(true).subscribe();

        setTimeout(() => {
          this.closeScheduleModal();
        }, 1000);
      },
      error: (err) => {
        console.error('Backend schedule error:', err);
        const errMsg = err?.error?.message || (typeof err?.error === 'string' ? err.error : null);
        this.scheduleError = errMsg || 'Failed to schedule interview. Please verify details and try again.';
        this.cdr.detectChanges();
      }
    });
  }

  private parseTimeTo24h(timeStr?: string): string {
    if (!timeStr || !timeStr.trim()) return '11:00:00';
    const clean = timeStr.trim();
    const match = clean.match(/(\d{1,2}):(\d{2})(?:\s*(AM|PM))?/i);
    if (!match) return '11:00:00';
    let hours = parseInt(match[1], 10);
    const minutes = match[2];
    const ampm = match[3] ? match[3].toUpperCase() : null;
    if (ampm === 'PM' && hours < 12) hours += 12;
    if (ampm === 'AM' && hours === 12) hours = 0;
    return `${String(hours).padStart(2, '0')}:${minutes}:00`;
  }

  cancelInterview(item: ScheduledInterview): void {
    if (confirm(`Are you sure you want to cancel the interview for ${item.candidate}?`)) {
      this.interviewService.cancelInterview(item.id).subscribe({
        next: () => {
          this.interviewService.refreshInterviewsFromBackend(true).subscribe();
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Failed to cancel interview on backend:', err);
        }
      });
    }
  }

  completeInterview(item: ScheduledInterview): void {
    if (confirm(`Mark interview for ${item.candidate} as Completed?`)) {
      this.interviewService.markInterviewCompleted(item.id).subscribe({
        next: () => {
          this.interviewService.refreshInterviewsFromBackend(true).subscribe();
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Failed to complete interview on backend:', err);
        }
      });
    }
  }

  configureInterview(item: ScheduledInterview): void {
    this.openScheduleModal();
    this.reschedulingInterviewId = item.id;
    this.selectedApplicationId = item.applicationId || null;

    if (item.job) {
      this.jobService.fetchJobsFromBackend().subscribe(jobs => {
        const found = (jobs || []).find(j => j.title.toLowerCase() === item.job.toLowerCase());
        if (found) {
          this.selectedJobId = found.id;
          this.loadApplicantsForJob(found.id);
        }
      });
    }
    this.newInterview = {
      candidate: item.candidate,
      job: item.job,
      date: item.dateKey || this.todayKey,
      time: item.time,
      interviewer: item.interviewer,
      type: item.type,
      status: item.status
    };
    this.cdr.detectChanges();
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }
}