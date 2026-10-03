import { Injectable, inject, PLATFORM_ID } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, of, map, catchError, tap } from 'rxjs';
import { isPlatformBrowser } from '@angular/common';
import { environment } from '../../environments/environment';

export interface InterviewRequest {
  applicationId: number;
  scheduledDateTime: string;
  type?: 'ONLINE' | 'OFFLINE' | 'PHONE';
  interviewType?: 'ONLINE' | 'OFFLINE' | 'PHONE';
  meetingLink?: string;
  notes?: string;
  status?: 'SCHEDULED' | 'COMPLETED' | 'CANCELLED' | 'RESCHEDULED';
}

export interface InterviewResponse {
  id: number;
  applicationId: number;
  candidateId: number;
  candidateName: string;
  candidateEmail?: string;
  jobId?: number;
  jobTitle: string;
  companyName?: string;
  scheduledDateTime: string;
  type: string;
  interviewType?: string;
  status: 'SCHEDULED' | 'COMPLETED' | 'CANCELLED' | 'RESCHEDULED';
  meetingLink?: string;
  notes?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface LiveQuestion {
  questionId: number;
  interviewId: number;
  questionNumber: number;
  questionText: string;
  category: string;
  difficulty: string;
  timeLimitSeconds: number;
  status: string;
}

export interface InterviewTranscriptionResponse {
  success: boolean;
  text: string;
  transcript?: string;
  isFinal: boolean;
  provider: string;
  model: string;
  latencyMs: number;
  error?: string;
}

export interface StartInterviewSessionResponse {
  interviewId: number;
  applicationId: number;
  candidateName: string;
  jobTitle: string;
  status: string;
  totalQuestionsTarget: number;
  firstQuestion?: LiveQuestion;
}

export interface LiveAnswerResponse {
  answerId?: number;
  questionId?: number;
  score?: number;
  technicalScore?: number;
  clarityScore?: number;
  feedback?: string;
  explanation?: string;
  missingConcepts?: string[];
  improvementAreas?: string[];
  correct?: boolean;
  correctnessClassification?: 'CORRECT' | 'PARTIALLY_CORRECT' | 'INCORRECT' | string;
  status: string;
  allQuestionsCompleted?: boolean;
  interviewFinished?: boolean;
  nextQuestion?: LiveQuestion;
}

export interface LiveInterviewResult {
  interviewId: number;
  candidateName: string;
  jobTitle: string;
  overallScore: number;
  technicalScore: number;
  communicationScore: number;
  problemSolvingScore: number;
  recommendation: string;
  strengths: string;
  weaknesses: string;
  completedAt?: string;
  totalQuestions?: number;
  answeredQuestions?: number;
  skippedQuestions?: number;
  durationMinutes?: number;
  integrityStatus?: string;
  totalIntegrityEvents?: number;
  multiplePersonEvents?: number;
  tabSwitchEvents?: number;
  attentionAwayEvents?: number;
  audioAnomalyEvents?: number;
}

export interface ScheduledInterview {
  id: number;
  candidate: string;
  candidateEmail?: string;
  job: string;
  company?: string;
  date: string;
  dateKey: string;
  time: string;
  rawDateTime: string;
  interviewer: string;
  type: 'AI Assessment' | 'Live Technical' | 'HR Round' | string;
  status: 'Scheduled' | 'Completed' | 'In Progress' | 'Cancelled' | string;
  applicationId?: number;
  meetingLink?: string;
  notes?: string;
}

@Injectable({
  providedIn: 'root'
})
export class InterviewService {
  private readonly apiUrl = `${environment.apiUrl}/interviews`;
  private readonly platformId = inject(PLATFORM_ID);

  // Shared reactive state - Single Source of Truth for Scheduler, Timeline, and Roster
  private readonly interviewsSubject = new BehaviorSubject<ScheduledInterview[]>([]);
  readonly interviews$ = this.interviewsSubject.asObservable();

  constructor(private readonly http: HttpClient) {
    if (isPlatformBrowser(this.platformId)) {
      this.refreshInterviewsFromBackend().subscribe({ error: () => {} });
    }
  }

  formatDateKey(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  /**
   * Candidate: Get interviews for currently authenticated candidate (GET /api/interviews/my-interviews)
   */
  getMyInterviews(): Observable<InterviewResponse[]> {
    return this.http.get<InterviewResponse[]>(`${this.apiUrl}/my-interviews`).pipe(
      catchError((err) => {
        console.warn('Failed to load candidate interviews from backend:', err);
        return of([]);
      })
    );
  }

  /**
   * Transforms raw backend InterviewResponse into the UI ScheduledInterview model.
   * Accurately determines type (AI Assessment vs Live Technical vs HR Round)
   * and panel interviewer from notes and database fields.
   */
  mapInterviewResponseToScheduled(item: InterviewResponse | any): ScheduledInterview {
    const d = item.scheduledDateTime ? new Date(item.scheduledDateTime) : new Date();
    const dateKey = this.formatDateKey(d);
    const dateStr = d.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
    const timeStr = d.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: true });

    const notesLower = (item.notes || '').toLowerCase();
    const rawType = (item.interviewType || item.type || '').toUpperCase();

    // Determine Type: AI Assessment vs Live Technical vs HR Round
    let resolvedType = 'Live Technical';
    if (notesLower.includes('ai') || rawType === 'LIVE_AI') {
      resolvedType = 'AI Assessment';
    } else if (notesLower.includes('hr') || rawType === 'PHONE') {
      resolvedType = 'HR Round';
    } else if (notesLower.includes('technical') || notesLower.includes('live') || rawType === 'ONLINE' || rawType === 'OFFLINE') {
      resolvedType = 'Live Technical';
    }

    // Determine Interviewer Panel from notes if available (e.g. "with Senior Engineering Panel")
    let interviewer = 'Tech Lead Panel';
    if (item.notes) {
      const withMatch = item.notes.match(/with\s+(.+)$/i);
      if (withMatch && withMatch[1]) {
        interviewer = withMatch[1].trim();
      } else if (resolvedType === 'AI Assessment') {
        interviewer = 'HireRanker AI Bot';
      }
    }

    // Status mapping
    let resolvedStatus: 'Scheduled' | 'Completed' | 'In Progress' | 'Cancelled' = 'Scheduled';
    const st = (item.status || '').toUpperCase();
    if (st === 'COMPLETED') {
      resolvedStatus = 'Completed';
    } else if (st === 'CANCELLED') {
      resolvedStatus = 'Cancelled';
    } else if (st === 'IN_PROGRESS') {
      resolvedStatus = 'In Progress';
    } else {
      resolvedStatus = 'Scheduled'; // SCHEDULED or RESCHEDULED
    }

    return {
      id: item.id,
      candidate: item.candidateName || `Candidate #${item.candidateId || item.id}`,
      candidateEmail: item.candidateEmail || '',
      job: item.jobTitle || 'Position',
      company: item.companyName || '',
      date: dateStr,
      dateKey: dateKey,
      time: timeStr,
      rawDateTime: item.scheduledDateTime || d.toISOString(),
      interviewer: interviewer,
      type: resolvedType,
      status: resolvedStatus,
      applicationId: item.applicationId,
      meetingLink: item.meetingLink,
      notes: item.notes
    };
  }

  /**
   * Refreshes interviews from Spring Boot backend MySQL database.
   * Dynamically calls /api/interviews for admin users and /api/interviews/my-interviews for candidate users.
   */
  refreshInterviewsFromBackend(forceAdmin?: boolean): Observable<ScheduledInterview[]> {
    if (!isPlatformBrowser(this.platformId)) {
      return of([]);
    }

    let userRole = 'CANDIDATE';
    try {
      if (forceAdmin) {
        userRole = 'ADMIN';
      } else {
        const directRole = localStorage.getItem('userRole');
        if (directRole === 'ADMIN' || directRole === 'ROLE_ADMIN') {
          userRole = 'ADMIN';
        } else {
          const session = localStorage.getItem('hireRankerSession');
          if (session) {
            const s = JSON.parse(session);
            if (s?.role === 'ADMIN' || s?.role === 'ROLE_ADMIN') {
              userRole = 'ADMIN';
            }
          }
        }
      }
    } catch {}

    const endpoint = userRole === 'ADMIN' ? this.apiUrl : `${this.apiUrl}/my-interviews`;
    return this.http.get<InterviewResponse[]>(endpoint).pipe(
      map(list => {
        if (!list || !Array.isArray(list)) {
          this.interviewsSubject.next([]);
          return [];
        }
        const mapped: ScheduledInterview[] = list.map(item => this.mapInterviewResponseToScheduled(item));
        this.interviewsSubject.next(mapped);
        return mapped;
      }),
      catchError(() => {
        this.interviewsSubject.next([]);
        return of([]);
      })
    );
  }

  /**
   * Explicitly retrieves all database interviews for Admin view.
   * Ensures the reactive interview state is refreshed.
   */
  getAllInterviews(): Observable<ScheduledInterview[]> {
    return this.http.get<InterviewResponse[]>(this.apiUrl).pipe(
      map(list => {
        if (!list || !Array.isArray(list)) {
          this.interviewsSubject.next([]);
          return [];
        }
        const mapped = list.map(item => this.mapInterviewResponseToScheduled(item));
        this.interviewsSubject.next(mapped);
        return mapped;
      }),
      catchError((err) => {
        console.error('Failed to get all interviews from backend:', err);
        return of([]);
      })
    );
  }

  private persistInterviews(list: ScheduledInterview[]): void {
    if (typeof window !== 'undefined' && window.localStorage) {
      try {
        localStorage.setItem('hireRankerScheduledInterviews', JSON.stringify(list));
      } catch {}
    }
    this.interviewsSubject.next(list);
  }

  getInterviewsSnapshot(): ScheduledInterview[] {
    return this.interviewsSubject.getValue();
  }

  addScheduledInterview(interview: ScheduledInterview): void {
    const current = this.interviewsSubject.getValue();
    const updated = [interview, ...current];
    this.persistInterviews(updated);
  }

  updateScheduledInterview(id: number, updates: Partial<ScheduledInterview>): void {
    const current = this.interviewsSubject.getValue();
    const updated = current.map(item => item.id === id ? { ...item, ...updates } : item);
    this.persistInterviews(updated);
  }

  cancelScheduledInterview(id: number): void {
    this.updateScheduledInterview(id, { status: 'Cancelled' });
  }

  rescheduleScheduledInterview(id: number, newDate: string, newTime: string, newDateKey: string): void {
    this.updateScheduledInterview(id, {
      date: newDate,
      time: newTime,
      dateKey: newDateKey,
      status: 'Scheduled'
    });
  }

  /**
   * Admin: Schedule an interview (POST /api/interviews)
   */
  scheduleInterview(request: InterviewRequest): Observable<InterviewResponse> {
    const payload = {
      ...request,
      interviewType: request.interviewType || request.type || 'ONLINE'
    };
    return this.http.post<InterviewResponse>(this.apiUrl, payload).pipe(
      tap(() => {
        this.refreshInterviewsFromBackend(true).subscribe();
      })
    );
  }

  /**
   * Get interview by ID (GET /api/interviews/{id})
   */
  getInterviewById(id: number): Observable<InterviewResponse> {
    return this.http.get<InterviewResponse>(`${this.apiUrl}/${id}`);
  }

  /**
   * Get interviews for candidate (GET /api/interviews/candidate/{candidateId})
   */
  getInterviewsForCandidate(candidateId: number): Observable<InterviewResponse[]> {
    return this.http.get<InterviewResponse[]>(`${this.apiUrl}/candidate/${candidateId}`);
  }

  /**
   * Admin: Reschedule interview (PUT /api/interviews/{id}/reschedule)
   */
  rescheduleInterview(id: number, scheduledDateTime: string, notes?: string): Observable<InterviewResponse> {
    const payload: any = { scheduledDateTime };
    if (notes) payload.notes = notes;

    return this.http.put<InterviewResponse>(`${this.apiUrl}/${id}/reschedule`, payload).pipe(
      tap(() => {
        this.refreshInterviewsFromBackend(true).subscribe();
      })
    );
  }

  /**
   * Admin: Complete interview (PUT /api/interviews/{id}/complete)
   * Transitions status to COMPLETED and refreshes scheduler & dashboard.
   */
  markInterviewCompleted(id: number): Observable<InterviewResponse> {
    return this.http.put<InterviewResponse>(`${this.apiUrl}/${id}/complete`, {}).pipe(
      catchError(() => {
        // Fallback to general update if /complete endpoint is not directly available
        return this.http.put<InterviewResponse>(`${this.apiUrl}/${id}`, { status: 'COMPLETED' });
      }),
      tap(() => {
        this.refreshInterviewsFromBackend(true).subscribe();
      })
    );
  }

  /**
   * Admin: Cancel interview (PUT /api/interviews/{id}/cancel)
   */
  cancelInterview(id: number, reason?: string): Observable<InterviewResponse> {
    const payload = reason ? { reason } : {};
    return this.http.put<InterviewResponse>(`${this.apiUrl}/${id}/cancel`, payload).pipe(
      tap(() => {
        this.refreshInterviewsFromBackend(true).subscribe();
      })
    );
  }

  /**
   * Live Interview: Start or resume an interview session (POST /api/interviews/start)
   */
  startInterview(request: { applicationId?: number; interviewId?: number }): Observable<StartInterviewSessionResponse> {
    return this.http.post<StartInterviewSessionResponse>(`${this.apiUrl}/start`, request);
  }

  /**
   * Live Interview: Fetch current or next question (GET /api/interviews/{id}/next-question)
   */
  getNextQuestion(interviewId: number): Observable<LiveQuestion> {
    return this.http.get<LiveQuestion>(`${this.apiUrl}/${interviewId}/next-question`);
  }

  // Persistent media stream shared between pre-interview check and assessment room
  private persistentMediaStream: MediaStream | null = null;

  setMediaStream(stream: MediaStream | null): void {
    this.persistentMediaStream = stream;
  }

  getMediaStream(): MediaStream | null {
    if (this.persistentMediaStream) {
      const tracks = this.persistentMediaStream.getTracks();
      const hasLiveTrack = tracks.some(t => t.readyState === 'live');
      if (hasLiveTrack) {
        return this.persistentMediaStream;
      }
    }
    return null;
  }

  stopActiveMediaStream(): void {
    if (this.persistentMediaStream) {
      try {
        this.persistentMediaStream.getTracks().forEach(track => {
          try { track.stop(); } catch {}
        });
      } catch {}
      this.persistentMediaStream = null;
    }
  }

  /**
   * Live Interview STT: Dispatches audio chunk or answer blob to Groq Whisper endpoint.
   */
  transcribeAudio(
    audioBlob: Blob,
    interviewId?: number,
    questionId?: number,
    isFinal: boolean = false
  ): Observable<InterviewTranscriptionResponse> {
    const formData = new FormData();
    const extension = audioBlob.type.includes('wav') ? 'wav' : 'webm';
    const filename = `answer_${Date.now()}.${extension}`;
    formData.append('file', audioBlob, filename);
    if (questionId) {
      formData.append('questionId', questionId.toString());
    }
    formData.append('isFinal', isFinal ? 'true' : 'false');

    const endpoint = interviewId
      ? `${this.apiUrl}/${interviewId}/transcribe`
      : `${this.apiUrl}/transcribe`;

    return this.http.post<InterviewTranscriptionResponse>(endpoint, formData);
  }

  /**
   * Live Interview: Submit spoken/written answer (POST /api/interviews/{id}/questions/{qId}/answer)
   */
  submitAnswer(
    interviewId: number,
    questionId: number,
    answerText: string,
    timeSpentSeconds: number = 15,
    assisted: boolean = false
  ): Observable<LiveAnswerResponse> {
    return this.http.post<LiveAnswerResponse>(`${this.apiUrl}/${interviewId}/questions/${questionId}/answer`, {
      answerText,
      responseTimeSeconds: 2.0,
      answerDurationSeconds: timeSpentSeconds,
      assisted
    });
  }

  /**
   * Live Interview: Skip question when 15s timer expires (POST /api/interviews/{id}/questions/{qId}/skip)
   */
  skipQuestion(interviewId: number, questionId: number, reason: string = '15-second speech countdown expired'): Observable<LiveAnswerResponse> {
    return this.http.post<LiveAnswerResponse>(`${this.apiUrl}/${interviewId}/questions/${questionId}/skip`, {
      reason
    });
  }

  /**
   * Live Interview: Complete interview and generate scorecard (POST /api/interviews/{id}/complete)
   */
  completeInterview(interviewId: number): Observable<LiveInterviewResult> {
    return this.http.post<LiveInterviewResult>(`${this.apiUrl}/${interviewId}/complete`, {}).pipe(
      tap(() => {
        this.refreshInterviewsFromBackend().subscribe({ error: () => {} });
      })
    );
  }

  /**
   * Live Interview: Record proctoring/integrity events (POST /api/interviews/{id}/integrity-events)
   */
  recordIntegrityEvents(interviewId: number, events: any[]): Observable<any> {
    if (!events || events.length === 0) {
      return of({ count: 0 });
    }
    return this.http.post<any>(`${this.apiUrl}/${interviewId}/integrity-events`, { events }).pipe(
      catchError(err => {
        console.warn('[INTERVIEW SERVICE] Failed to record integrity events:', err);
        return of({ count: 0 });
      })
    );
  }

  /**
   * Live Interview: Retrieve final result / scorecard (GET /api/interviews/{id}/result)
   */
  getInterviewResult(interviewId: number): Observable<LiveInterviewResult> {
    return this.http.get<LiveInterviewResult>(`${this.apiUrl}/${interviewId}/result`);
  }

  /**
   * Live Interview: Exit assessment early (POST /api/interviews/{id}/exit)
   */
  exitInterview(interviewId: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/${interviewId}/exit`, {});
  }

  /**
   * Live Interview: Admin approves retake (POST /api/interviews/{id}/approve-retake)
   */
  approveRetake(interviewId: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/${interviewId}/approve-retake`, {});
  }
}
