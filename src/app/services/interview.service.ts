import { Injectable, inject, PLATFORM_ID } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, of, map, catchError } from 'rxjs';
import { isPlatformBrowser } from '@angular/common';
import { environment } from '../../environments/environment';

export interface InterviewRequest {
  applicationId: number;
  scheduledDateTime: string;
  type?: 'ONLINE' | 'OFFLINE' | 'PHONE';
  interviewType?: 'ONLINE' | 'OFFLINE' | 'PHONE';
  meetingLink?: string;
  notes?: string;
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
}

export interface ScheduledInterview {
  id: number;
  candidate: string;
  candidateEmail?: string;
  job: string;
  date: string;
  dateKey: string;
  time: string;
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
   * Refreshes interviews from Spring Boot backend MySQL database.
   * Dynamically calls /api/interviews/my-interviews for candidate users and /api/interviews for admin users.
   */
  refreshInterviewsFromBackend(): Observable<ScheduledInterview[]> {
    if (!isPlatformBrowser(this.platformId)) {
      return of([]);
    }

    let userRole = 'CANDIDATE';
    try {
      const stored = localStorage.getItem('hireRankerUser');
      if (stored) {
        const u = JSON.parse(stored);
        if (u?.role) userRole = u.role;
      }
    } catch {}

    const endpoint = userRole === 'ADMIN' ? this.apiUrl : `${this.apiUrl}/my-interviews`;
    return this.http.get<InterviewResponse[]>(endpoint).pipe(
      map(list => {
        if (!list || !Array.isArray(list)) {
          this.interviewsSubject.next([]);
          return [];
        }
        const mapped: ScheduledInterview[] = list.map(item => {
          const d = item.scheduledDateTime ? new Date(item.scheduledDateTime) : new Date();
          const dateKey = this.formatDateKey(d);
          const dateStr = d.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
          const timeStr = d.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: true });

          return {
            id: item.id,
            candidate: item.candidateName || `Candidate #${item.candidateId}`,
            candidateEmail: item.candidateEmail || '',
            job: item.jobTitle || 'Position',
            date: dateStr,
            dateKey: dateKey,
            time: timeStr,
            interviewer: 'Recruitment Team',
            type: item.type === 'ONLINE' ? 'AI Assessment' : 'Live Technical',
            status: item.status === 'SCHEDULED' ? 'Scheduled' : item.status === 'COMPLETED' ? 'Completed' : 'Cancelled',
            applicationId: item.applicationId,
            meetingLink: item.meetingLink,
            notes: item.notes
          };
        });
        this.interviewsSubject.next(mapped);
        return mapped;
      }),
      catchError(() => {
        this.interviewsSubject.next([]);
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
    return this.http.post<InterviewResponse>(this.apiUrl, payload);
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
  rescheduleInterview(id: number, scheduledDateTime: string): Observable<InterviewResponse> {
    const parsedDate = new Date(scheduledDateTime);
    if (!isNaN(parsedDate.getTime())) {
      const dateKey = this.formatDateKey(parsedDate);
      const dateStr = parsedDate.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
      const timeStr = parsedDate.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: true });
      this.rescheduleScheduledInterview(id, dateStr, timeStr, dateKey);
    }
    return this.http.put<InterviewResponse>(`${this.apiUrl}/${id}/reschedule`, { scheduledDateTime }).pipe(
      map(res => {
        this.refreshInterviewsFromBackend().subscribe();
        return res;
      })
    );
  }

  /**
   * Admin: Cancel interview (PUT /api/interviews/{id}/cancel)
   */
  cancelInterview(id: number): Observable<InterviewResponse> {
    this.cancelScheduledInterview(id);
    return this.http.put<InterviewResponse>(`${this.apiUrl}/${id}/cancel`, {}).pipe(
      map(res => {
        this.refreshInterviewsFromBackend().subscribe();
        return res;
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
    return this.http.post<LiveInterviewResult>(`${this.apiUrl}/${interviewId}/complete`, {});
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
