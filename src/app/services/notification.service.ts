import { Injectable, signal, computed } from '@angular/core';

export interface AppNotification {
  id: string;
  title: string;
  message: string;
  type: 'job' | 'application' | 'info';
  timestamp: string;
  createdAt: number;
  read: boolean;
  link?: string;
  meta?: {
    jobId?: number;
    jobTitle?: string;
    company?: string;
    candidateName?: string;
    candidateEmail?: string;
    matchScore?: number;
    location?: string;
    salary?: string;
  };
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private readonly STORAGE_CANDIDATE_NOTIFS = 'hireRankerCandidateNotifications';
  private readonly STORAGE_ADMIN_NOTIFS = 'hireRankerAdminNotifications';

  private readonly defaultCandidateNotifications: AppNotification[] = [];

  private readonly defaultAdminNotifications: AppNotification[] = [];

  readonly candidateNotificationsSignal = signal<AppNotification[]>([]);
  readonly adminNotificationsSignal = signal<AppNotification[]>([]);

  readonly candidateUnreadCount = computed(() =>
    this.candidateNotificationsSignal().filter(n => !n.read).length
  );

  readonly adminUnreadCount = computed(() =>
    this.adminNotificationsSignal().filter(n => !n.read).length
  );

  constructor() {
    this.loadCandidateNotifications();
    this.loadAdminNotifications();
  }

  private isBrowser(): boolean {
    return typeof window !== 'undefined' && typeof localStorage !== 'undefined';
  }

  // ================= CANDIDATE NOTIFICATIONS =================

  getCandidateNotifications(): AppNotification[] {
    if (!this.isBrowser()) return [];
    try {
      const raw = localStorage.getItem(this.STORAGE_CANDIDATE_NOTIFS);
      if (raw) {
        const parsed = JSON.parse(raw);
        if (Array.isArray(parsed)) return parsed;
      }
      return [];
    } catch {
      return [];
    }
  }

  private loadCandidateNotifications(): void {
    this.candidateNotificationsSignal.set(this.getCandidateNotifications());
  }

  private saveCandidateNotifications(notifications: AppNotification[]): void {
    if (!this.isBrowser()) return;
    try {
      localStorage.setItem(this.STORAGE_CANDIDATE_NOTIFS, JSON.stringify(notifications));
      this.candidateNotificationsSignal.set([...notifications]);
    } catch (err) {
      console.error('Failed to save candidate notifications:', err);
    }
  }

  notifyNewJob(job: {
    id: number;
    title: string;
    company?: string;
    location?: string;
    salary?: string;
    matchScore?: number;
  }): void {
    const notifications = this.getCandidateNotifications();
    const newNotif: AppNotification = {
      id: `job-notif-${Date.now()}-${Math.floor(Math.random() * 1000)}`,
      title: `💼 New Job Opening: ${job.title}`,
      message: `${job.company || 'HireRanker'} posted a new opening for ${job.title} in ${job.location || 'India'} (${job.salary || 'Competitive'}). Check your match score and apply!`,
      type: 'job',
      timestamp: 'Just now',
      createdAt: Date.now(),
      read: false,
      link: '/find-jobs',
      meta: {
        jobId: job.id,
        jobTitle: job.title,
        company: job.company || 'HireRanker Technologies',
        location: job.location,
        salary: job.salary,
        matchScore: job.matchScore || 90
      }
    };

    const updated = [newNotif, ...notifications];
    this.saveCandidateNotifications(updated);
  }

  markCandidateNotificationAsRead(id: string): void {
    const list = this.getCandidateNotifications();
    const target = list.find(n => n.id === id);
    if (target) {
      target.read = true;
      this.saveCandidateNotifications(list);
    }
  }

  markAllCandidateNotificationsAsRead(): void {
    const list = this.getCandidateNotifications().map(n => ({ ...n, read: true }));
    this.saveCandidateNotifications(list);
  }

  clearCandidateNotifications(): void {
    this.saveCandidateNotifications([]);
  }

  // ================= ADMIN NOTIFICATIONS =================

  getAdminNotifications(): AppNotification[] {
    if (!this.isBrowser()) return [];
    try {
      const raw = localStorage.getItem(this.STORAGE_ADMIN_NOTIFS);
      if (raw) {
        const parsed = JSON.parse(raw);
        if (Array.isArray(parsed)) return parsed;
      }
      return [];
    } catch {
      return [];
    }
  }

  private loadAdminNotifications(): void {
    this.adminNotificationsSignal.set(this.getAdminNotifications());
  }

  private saveAdminNotifications(notifications: AppNotification[]): void {
    if (!this.isBrowser()) return;
    try {
      localStorage.setItem(this.STORAGE_ADMIN_NOTIFS, JSON.stringify(notifications));
      this.adminNotificationsSignal.set([...notifications]);
    } catch (err) {
      console.error('Failed to save admin notifications:', err);
    }
  }

  notifyNewApplication(details: {
    candidateName: string;
    candidateEmail?: string;
    jobTitle: string;
    jobId: number;
    matchScore: number;
  }): void {
    const notifications = this.getAdminNotifications();
    const newNotif: AppNotification = {
      id: `app-notif-${Date.now()}-${Math.floor(Math.random() * 1000)}`,
      title: `📄 New Application: ${details.candidateName}`,
      message: `${details.candidateName} applied for "${details.jobTitle}" with an AI match score of ${details.matchScore}%.`,
      type: 'application',
      timestamp: 'Just now',
      createdAt: Date.now(),
      read: false,
      link: '/job-applicants',
      meta: {
        jobId: details.jobId,
        jobTitle: details.jobTitle,
        candidateName: details.candidateName,
        candidateEmail: details.candidateEmail,
        matchScore: details.matchScore
      }
    };

    const updated = [newNotif, ...notifications];
    this.saveAdminNotifications(updated);
  }

  markAdminNotificationAsRead(id: string): void {
    const list = this.getAdminNotifications();
    const target = list.find(n => n.id === id);
    if (target) {
      target.read = true;
      this.saveAdminNotifications(list);
    }
  }

  markAllAdminNotificationsAsRead(): void {
    const list = this.getAdminNotifications().map(n => ({ ...n, read: true }));
    this.saveAdminNotifications(list);
  }

  clearAdminNotifications(): void {
    this.saveAdminNotifications([]);
  }
}
