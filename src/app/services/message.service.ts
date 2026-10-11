import { Injectable, inject, PLATFORM_ID } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { isPlatformBrowser } from '@angular/common';
import { environment } from '../../environments/environment';

export interface MessageResponse {
  id: number;
  senderId: number;
  senderName: string;
  senderEmail: string;
  senderRole: string;
  receiverId: number;
  receiverName: string;
  receiverEmail: string;
  receiverRole: string;
  message: string;
  sentAt: string;
  readStatus: 'UNREAD' | 'READ';
  jobId?: number | null;
  jobTitle?: string | null;
  company?: string | null;
  applicationId?: number | null;
}

export interface ConversationSummary {
  partnerId: number;
  partnerName: string;
  partnerEmail: string;
  partnerRole: string;
  candidateId?: number | null;
  appliedRole?: string | null;
  company?: string | null;
  jobId?: number | null;
  applicationId?: number | null;
  lastMessage: string;
  lastMessageTime: string;
  unreadCount: number;
  messages: MessageResponse[];
}

export interface Recipient {
  userId: number;
  name: string;
  email: string;
  role: string;
  candidateId?: number | null;
  appliedRole?: string | null;
  company?: string | null;
  jobId?: number | null;
  applicationId?: number | null;
  displayLabel?: string;
}

export interface CandidateAppliedJob {
  applicationId: number;
  jobId: number;
  jobTitle: string;
  company: string;
  recruiterId: number | null;
  recruiterName: string | null;
  recruiterEmail: string | null;
  hasRecruiter: boolean;
  displayLabel: string;
  status: string;
  appliedAt: string;
}

export interface UnreadCountResponse {
  unreadCount: number;
}

@Injectable({
  providedIn: 'root'
})
export class MessageService {
  private readonly apiUrl = `${environment.apiUrl}/messages`;
  private readonly platformId = inject(PLATFORM_ID);

  constructor(private readonly http: HttpClient) {}

  /**
   * Send a direct message (POST /api/messages)
   */
  sendMessage(
    receiverId: number,
    message: string,
    jobId?: number | null,
    applicationId?: number | null
  ): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(this.apiUrl, {
      receiverId,
      message,
      jobId: jobId || null,
      applicationId: applicationId || null
    });
  }

  /**
   * Get all conversations grouped by chat partner (GET /api/messages/conversations)
   */
  getConversations(): Observable<ConversationSummary[]> {
    if (!isPlatformBrowser(this.platformId)) {
      return of([]);
    }
    return this.http.get<ConversationSummary[]>(`${this.apiUrl}/conversations`);
  }

  /**
   * Get conversation history with a specific partner (GET /api/messages/conversation/{partnerId})
   */
  getConversationWith(partnerId: number): Observable<MessageResponse[]> {
    if (!isPlatformBrowser(this.platformId)) {
      return of([]);
    }
    return this.http.get<MessageResponse[]>(`${this.apiUrl}/conversation/${partnerId}`);
  }

  /**
   * Mark all messages in a conversation as read (PUT /api/messages/conversation/{partnerId}/read)
   */
  markConversationAsRead(partnerId: number): Observable<{ updatedCount: number }> {
    return this.http.put<{ updatedCount: number }>(`${this.apiUrl}/conversation/${partnerId}/read`, {});
  }

  /**
   * Get total unread messages count for current user (GET /api/messages/unread-count)
   */
  getUnreadCount(): Observable<UnreadCountResponse> {
    if (!isPlatformBrowser(this.platformId)) {
      return of({ unreadCount: 0 });
    }
    return this.http.get<UnreadCountResponse>(`${this.apiUrl}/unread-count`);
  }

  /**
   * Get eligible recipients to start a conversation with (GET /api/messages/recipients)
   */
  getEligibleRecipients(): Observable<Recipient[]> {
    if (!isPlatformBrowser(this.platformId)) {
      return of([]);
    }
    return this.http.get<Recipient[]>(`${this.apiUrl}/recipients`);
  }

  /**
   * Get candidate's applied jobs with resolved recruiter metadata (GET /api/messages/candidate/applied-jobs)
   */
  getCandidateAppliedJobs(): Observable<CandidateAppliedJob[]> {
    if (!isPlatformBrowser(this.platformId)) {
      return of([]);
    }
    return this.http.get<CandidateAppliedJob[]>(`${this.apiUrl}/candidate/applied-jobs`);
  }

  /**
   * Get raw messages for current authenticated user (GET /api/messages)
   */
  getMessages(): Observable<MessageResponse[]> {
    if (!isPlatformBrowser(this.platformId)) {
      return of([]);
    }
    return this.http.get<MessageResponse[]>(this.apiUrl);
  }

  /**
   * Get specific message by ID (GET /api/messages/{id})
   */
  getMessageById(id: number): Observable<MessageResponse> {
    return this.http.get<MessageResponse>(`${this.apiUrl}/${id}`);
  }

  /**
   * Mark message as read (PUT /api/messages/{id}/read)
   */
  markAsRead(id: number): Observable<MessageResponse> {
    return this.http.put<MessageResponse>(`${this.apiUrl}/${id}/read`, {});
  }
}
