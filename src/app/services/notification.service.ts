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

  private readonly defaultCandidateNotifications: AppNotification[] = [
    {
      id: 'cand-notif-1',
      title: '💼 New Job Opening: Senior Angular Developer',
      message: 'HireRanker Technologies is hiring for Senior Angular Developer in Bangalore (₹14 - 20 LPA). Your profile is an 89% match!',
      type: 'job',
      timestamp: '10 mins ago',
      createdAt: Date.now() - 10 * 60 * 1000,
      read: false,
      link: '/find-jobs',
      meta: {
        jobId: 2,
        jobTitle: 'Senior Angular Developer',
        company: 'HireRanker Technologies',
        matchScore: 89,
        location: 'Bangalore',
        salary: '₹14 - 20 LPA'
      }
    },
    {
      id: 'cand-notif-2',
      title: '⭐ Application Shortlisted',
      message: 'Great news! Your application for Java Full Stack Developer has been shortlisted by the recruiter.',
      type: 'info',
      timestamp: '2 hours ago',
      createdAt: Date.now() - 2 * 60 * 60 * 1000,
      read: true,
      link: '/my-applications',
      meta: {
        jobId: 1,
        jobTitle: 'Java Full Stack Developer'
      }
    }
  ];

  private readonly defaultAdminNotifications: AppNotification[] = [
    {
      id: 'admin-notif-1',
      title: '📄 New Application: Eshwar Rao',
      message: 'Eshwar Rao just applied for "Java Full Stack Developer" with an AI resume match score of 94%.',
      type: 'application',
      timestamp: '15 mins ago',
      createdAt: Date.now() - 15 * 60 * 1000,
      read: false,
      link: '/job-applicants',
      meta: {
        jobId: 1,
        jobTitle: 'Java Full Stack Developer',
        candidateName: 'Eshwar Rao',
        candidateEmail: 'eshwar@candidate.com',
        matchScore: 94
      }
    },
    {
      id: 'admin-notif-2',
      title: '📄 New Application: Krupa Jyothi',
      message: 'Krupa Jyothi applied for "Senior Angular Developer" with an AI resume match score of 89%.',
      type: 'application',
      timestamp: '1 hour ago',
      createdAt: Date.now() - 60 * 60 * 1000,
      read: true,
      link: '/job-applicants',
      meta: {
        jobId: 2,
        jobTitle: 'Senior Angular Developer',
        candidateName: 'Krupa Jyothi',
        candidateEmail: 'krupa.jyothi@email.com',
        matchScore: 89
      }
    }
  ];

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
    if (!this.isBrowser()) return [...this.defaultCandidateNotifications];
    try {
      const raw = localStorage.getItem(this.STORAGE_CANDIDATE_NOTIFS);
      if (raw) {
        const parsed = JSON.parse(raw);
        if (Array.isArray(parsed)) return parsed;
      }
      this.saveCandidateNotifications(this.defaultCandidateNotifications);
      return [...this.defaultCandidateNotifications];
    } catch {
      return [...this.defaultCandidateNotifications];
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
    if (!this.isBrowser()) return [...this.defaultAdminNotifications];
    try {
      const raw = localStorage.getItem(this.STORAGE_ADMIN_NOTIFS);
      if (raw) {
        const parsed = JSON.parse(raw);
        if (Array.isArray(parsed)) return parsed;
      }
      this.saveAdminNotifications(this.defaultAdminNotifications);
      return [...this.defaultAdminNotifications];
    } catch {
      return [...this.defaultAdminNotifications];
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
