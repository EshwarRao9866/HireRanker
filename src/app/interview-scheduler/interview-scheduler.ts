import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { InterviewService, ScheduledInterview } from '../services/interview.service';
import { ApplicationService, ApplicationResponse } from '../services/application.service';

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

  readonly currentDate = new Date();
  readonly todayKey = this.formatDateKey(this.currentDate);
  readonly todayFormatted = this.formatDisplayDate(this.currentDate);
  readonly currentMonthYear = this.currentDate.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });

  selectedDate: Date = new Date();
  selectedDateKey: string = this.formatDateKey(this.selectedDate);

  interviews: ScheduledInterview[] = [];
  filteredInterviews: ScheduledInterview[] = [];
  calendarDays: CalendarDay[] = [];

  private interviewsSub?: Subscription;

  newInterview: Partial<ScheduledInterview> = {
    candidate: '',
    job: 'Java Full Stack Developer',
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

  constructor(
    private readonly router: Router,
    readonly interviewService: InterviewService,
    private readonly applicationService: ApplicationService
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
      }
    });

    // 2. Fetch applications for candidate selector in modal
    this.applicationService.getAllApplications().subscribe({
      next: (apps) => {
        if (apps && apps.length > 0) {
          this.applications = apps;
        }
      },
      error: () => {}
    });
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
    this.showScheduleModal = true;
  }

  closeScheduleModal(): void {
    this.showScheduleModal = false;
  }

  scheduleNewInterview(): void {
    if (!this.newInterview.candidate) {
      alert('Please enter Candidate Name');
      return;
    }

    const inputDate = this.newInterview.date ? new Date(this.newInterview.date) : this.selectedDate;
    const dateKey = !isNaN(inputDate.getTime()) ? this.formatDateKey(inputDate) : this.selectedDateKey;
    const isToday = dateKey === this.todayKey;
    const displayDate = isToday ? `Today, ${this.todayFormatted}` : this.formatDisplayDate(inputDate);

    const interview: ScheduledInterview = {
      id: Date.now(),
      candidate: this.newInterview.candidate,
      job: this.newInterview.job || 'Java Full Stack Developer',
      date: displayDate,
      dateKey: dateKey,
      time: this.newInterview.time || '11:00 AM',
      interviewer: this.newInterview.interviewer || 'Tech Lead Panel',
      type: (this.newInterview.type as any) || 'Live Technical',
      status: 'Scheduled'
    };

    // 1. Add to shared reactive interview state (Updates Timeline, Roster, and Calendar immediately)
    this.interviewService.addScheduledInterview(interview);
    this.showScheduleModal = false;

    // 2. Persist to backend if application is linked
    const matchedApp = this.applications.find(a =>
      (a.candidateName && a.candidateName.toLowerCase().includes(this.newInterview.candidate!.toLowerCase())) ||
      (this.newInterview.job && a.jobTitle && a.jobTitle.toLowerCase().includes(this.newInterview.job.toLowerCase()))
    ) || (this.applications.length > 0 ? this.applications[0] : null);

    if (matchedApp) {
      const isoDateTime = `${dateKey}T10:00:00`;
      const interviewType = this.newInterview.type === 'HR Round' ? 'PHONE' : 'ONLINE';
      const meetingLink = interviewType === 'ONLINE' ? `https://meet.hireranker.com/session-${Date.now()}` : undefined;

      this.interviewService.scheduleInterview({
        applicationId: matchedApp.id,
        scheduledDateTime: isoDateTime,
        type: interviewType,
        interviewType: interviewType,
        meetingLink: meetingLink,
        notes: `${this.newInterview.type || 'Technical Round'} with ${this.newInterview.interviewer || 'Panel'}`
      }).subscribe({
        next: (created) => {
          if (created && created.id) {
            this.interviewService.updateScheduledInterview(interview.id, { id: created.id });
          }
        },
        error: (err) => console.warn('Backend schedule note:', err?.status)
      });
    }

    this.newInterview.candidate = '';
  }

  cancelInterview(item: ScheduledInterview): void {
    this.interviewService.cancelScheduledInterview(item.id);
  }

  configureInterview(item: ScheduledInterview): void {
    this.newInterview = {
      candidate: item.candidate,
      job: item.job,
      date: item.dateKey || this.todayKey,
      time: item.time,
      interviewer: item.interviewer,
      type: item.type,
      status: item.status
    };
    this.showScheduleModal = true;
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }
}