import { Injectable, inject, PLATFORM_ID } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { isPlatformBrowser } from '@angular/common';
import { environment } from '../../environments/environment';

export interface MessageResponse {
  id: number;
  senderId: number;
  senderName: string;
  receiverId: number;
  receiverName: string;
  message: string;
  sentAt: string;
  readStatus: 'UNREAD' | 'READ';
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
  sendMessage(receiverId: number, message: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(this.apiUrl, { receiverId, message });
  }

  /**
   * Get messages for current authenticated user (GET /api/messages)
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
