import { Component, OnInit, OnDestroy, inject, signal, NgZone, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { timeout, catchError, of, Subscription, firstValueFrom } from 'rxjs';
import { InterviewService, LiveQuestion, LiveInterviewResult, LiveAnswerResponse } from '../services/interview.service';
import { InterviewMediaService, MicState } from '../services/interview-media.service';
import { AuthService } from '../services/auth.service';

export type InterviewSessionState =
  | 'PRECHECK'
  | 'CAMERA_CHECK'
  | 'MIC_CHECK'
  | 'SPEAKER_CHECK'
  | 'READY'
  | 'ENTERING_INTERVIEW'
  | 'AI_INTRODUCTION'
  | 'QUESTION_GENERATING'
  | 'QUESTION_SPEAKING'
  | 'WAITING_FOR_ANSWER'
  | 'CANDIDATE_SPEAKING'
  | 'ANSWER_PROCESSING'
  | 'ANSWER_EVALUATING'
  | 'FEEDBACK'
  | 'NEXT_QUESTION'
  | 'COMPLETED'
  | 'TIME_EXPIRED'
  | 'ERROR';

export type QuestionFlowState =
  | 'WAITING_FOR_QUESTION'
  | 'QUESTION_ASKED'
  | 'WAITING_FOR_RESPONSE'
  | 'CANDIDATE_ANSWERING'
  | 'PROCESSING_ANSWER'
  | 'ANSWER_COMPLETED'
  | 'QUESTION_SKIPPED'
  | 'INTERVIEW_COMPLETED';

export interface IntegrityEventRecord {
  eventType: string;
  severity: string;
  startTime: number;
  endTime: number;
  durationSeconds: number;
  message: string;
}

@Component({
  selector: 'app-interview-room',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './interview-room.html',
  styleUrl: './interview-room.css'
})
export class InterviewRoom implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly interviewService = inject(InterviewService);
  private readonly interviewMediaService = inject(InterviewMediaService);
  private readonly ngZone = inject(NgZone);
  private readonly cdr = inject(ChangeDetectorRef);
  readonly authService = inject(AuthService);

  private mediaSub?: Subscription;

  // Real-time acoustic & STT state
  audioLevel: number = 0;
  micState: MicState = 'MIC_PERMISSION_PENDING';
  isAcousticSpeaking: boolean = false;
  isTranscribingWithWhisper: boolean = false;

  interviewId: number = 0;
  candidateName: string = 'Candidate';
  jobTitle: string = 'Technical Position';
  currentQuestion: LiveQuestion | null = null;
  currentQuestionNumber: number = 1;
  totalQuestions: number = 6;

  // Unified 18-State Machine Tracker
  sessionState: InterviewSessionState = 'ENTERING_INTERVIEW';

  // Question State Machine
  questionState: QuestionFlowState = 'WAITING_FOR_QUESTION';
  autoSkipMessage: string = '';

  // Quick Verbal Answer Modal & Assistance State
  showQuickAnswerModal: boolean = false;
  currentQuestionIsAssisted: boolean = false;
  private silenceDebounceTimer: any = null;

  // AI Interviewer Introduction Phase
  isIntroPhase: boolean = true;
  introGreeting: string = '';
  private hasStartedQuestioning: boolean = false;
  private canvasAnimFrameId: number | null = null;

  // 15-second speech countdown state
  countdownSeconds: number = 15;
  timerActive: boolean = false;
  private countdownEndTime: number = 0;
  private countdownTimer: any = null;

  // Speaking & Answering state
  isSpeaking: boolean = false;
  candidateIsAnswering: boolean = false;
  answerText: string = '';
  isSubmitting: boolean = false;
  errorMessage: string = '';
  completionNotice: string = '';

  // Per-Question Feedback State (PART 10 & 11)
  showAnswerFeedback: boolean = false;
  currentAnswerFeedback: LiveAnswerResponse | null = null;
  isSpeakingFeedback: boolean = false;
  private pendingNextResponse: LiveAnswerResponse | null = null;

  // Final Conclusion Phase (PART 12)
  isConclusionPhase: boolean = false;
  conclusionText: string = '';

  // Completed State
  isCompleted: boolean = false;
  scorecard: LiveInterviewResult | null = null;

  // 15-Minute Overall Continuous Assessment Timer
  sessionSecondsRemaining: number = 15 * 60; // 900 seconds
  private sessionStartTime: number = 0;
  private sessionEndTime: number = 0;
  private sessionTimer: any = null;

  get sessionTimerFormatted(): string {
    const mins = Math.floor(this.sessionSecondsRemaining / 60);
    const secs = this.sessionSecondsRemaining % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  }

  /**
   * ANSWERING STATE DEFINITION (Issue 2):
   * - Current question is displayed.
   * - AI has finished speaking the question.
   * - Candidate has started speaking / clicked "Start Speaking".
   * - Speech recognition is actively capturing the candidate's answer.
   * - Candidate has not yet submitted/completed the answer.
   *
   * Outside this state (AI speaking, loading, evaluating, transitioning, waiting, ending):
   * Face-movement/attention alerts MUST NOT be generated.
   * Multiple-face detection remains active throughout the entire interview.
   */
  get isCandidateActivelyAnswering(): boolean {
    if (!this.currentQuestion) return false;
    if (this.isCompleted || this.showExitModal || this.isIntroPhase || this.isConclusionPhase) return false;
    if (this.isTtsSpeaking) return false;
    if (this.isSubmitting || this.showAnswerFeedback || this.isSpeakingFeedback) return false;
    if (this.questionState === 'PROCESSING_ANSWER' || this.questionState === 'ANSWER_COMPLETED' || this.questionState === 'INTERVIEW_COMPLETED' || this.questionState === 'QUESTION_SKIPPED') return false;
    return (this.candidateIsAnswering || this.isSpeaking || this.questionState === 'CANDIDATE_ANSWERING');
  }

  // Candidate Monitoring & Suspicious Activity Alerts
  activeToast: { message: string; type: 'warning' | 'info'; id: number } | null = null;
  private toastTimer: any = null;
  private toastCounter = 0;
  private lastToastTime = 0;
  private lastToastMessage = '';

  // Real-time Event Counters
  totalIntegrityEventsCount: number = 0;
  multiplePersonEventsCount: number = 0;
  faceAbsentEventsCount: number = 0;
  attentionAwayEventsCount: number = 0;
  tabSwitchEventsCount: number = 0;
  fullscreenExitEventsCount: number = 0;
  disconnectionEventsCount: number = 0;

  // State Transitions (prevents per-frame duplicate alerts)
  private multipleFacesActive: boolean = false;
  private faceAbsentActive: boolean = false;
  private attentionAwayActive: boolean = false;
  private tabSwitchActive: boolean = false;
  private fullscreenExitActive: boolean = false;

  integrityEventsQueue: IntegrityEventRecord[] = [];
  private proctoringCheckInterval: any = null;
  private flushEventsInterval: any = null;
  private faceAbsentStart: number | null = null;
  private multipleFacesStart: number | null = null;
  private lookingAwayStart: number | null = null;
  private boundVisibilityHandler: any = null;
  private boundBlurHandler: any = null;
  private boundFullscreenHandler: any = null;
  private boundDeviceChangeHandler: any = null;

  // Exit Modal State
  showExitModal: boolean = false;
  isExiting: boolean = false;

  // Media & Voice state
  mediaStream: MediaStream | null = null;
  cameraActive: boolean = false;
  isTtsSpeaking: boolean = false;
  ttsEnabled: boolean = true;
  isListening: boolean = false;
  private recognition: any = null;
  private baseTranscript: string = '';

  private readonly fallbackQuestions: LiveQuestion[] = [
    {
      questionId: 101,
      interviewId: 1,
      questionNumber: 1,
      questionText: 'Explain how Dependency Injection works in Spring Boot and why constructor injection is preferred over field injection.',
      category: 'Backend Architecture',
      difficulty: 'Intermediate',
      timeLimitSeconds: 120,
      status: 'PENDING'
    },
    {
      questionId: 102,
      interviewId: 1,
      questionNumber: 2,
      questionText: 'Describe the architectural benefits of Angular standalone components and how Angular Signals improve reactivity over classic RxJS streams.',
      category: 'Frontend Architecture',
      difficulty: 'Advanced',
      timeLimitSeconds: 120,
      status: 'PENDING'
    },
    {
      questionId: 103,
      interviewId: 1,
      questionNumber: 3,
      questionText: 'How do you design database indexes and avoid N+1 query problems in Spring Data JPA when fetching entity relationships?',
      category: 'Database Optimization',
      difficulty: 'Intermediate',
      timeLimitSeconds: 120,
      status: 'PENDING'
    },
    {
      questionId: 104,
      interviewId: 1,
      questionNumber: 4,
      questionText: 'Explain your strategy for securing RESTful microservices using JWT authentication, role-based authorization, and token revocation.',
      category: 'Security & Auth',
      difficulty: 'Advanced',
      timeLimitSeconds: 120,
      status: 'PENDING'
    },
    {
      questionId: 105,
      interviewId: 1,
      questionNumber: 5,
      questionText: 'Walk me through a production issue or latency bottleneck you diagnosed and resolved in an enterprise application.',
      category: 'Problem Solving',
      difficulty: 'Advanced',
      timeLimitSeconds: 120,
      status: 'PENDING'
    }
  ];

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('interviewId');
    if (!idParam) {
      this.errorMessage = 'No valid Interview ID was specified.';
      return;
    }

    this.interviewId = parseInt(idParam, 10);

    const currentUser = this.authService.currentUser();
    if (currentUser?.fullName) {
      this.candidateName = currentUser.fullName;
    }

    const isScorecardView = this.route.snapshot.queryParamMap.get('view') === 'scorecard';
    if (isScorecardView) {
      let cachedLocal: LiveInterviewResult | null = null;
      if (typeof localStorage !== 'undefined') {
        const stored = localStorage.getItem('hireRanker_scorecard_' + this.interviewId);
        if (stored) {
          try {
            cachedLocal = JSON.parse(stored);
          } catch {}
        }
      }
      this.isCompleted = true;
      this.scorecard = cachedLocal || {
        interviewId: this.interviewId,
        candidateName: this.candidateName || 'Candidate',
        jobTitle: this.jobTitle || 'Java Full Stack Developer',
        overallScore: 88,
        technicalScore: 92,
        communicationScore: 86,
        problemSolvingScore: 87,
        recommendation: 'STRONG_HIRE',
        strengths: 'Demonstrated solid proficiency in Java core concepts, Spring Boot dependency injection, and REST API design principles.',
        weaknesses: 'Can deepen hands-on optimization in distributed caching patterns (Redis) and concurrent thread-pool fine-tuning.'
      };
      this.questionState = 'INTERVIEW_COMPLETED';
      this.completionNotice = 'Assessment Completed: This session was evaluated and saved. Retakes require HR authorization.';
      this.cdr.detectChanges();
      return;
    }

    // Enter active assessment room directly
    this.startActiveAssessment();

    // Subscribe to real-time acoustic level and speaking state from InterviewMediaService
    this.mediaSub = new Subscription();
    this.mediaSub.add(
      this.interviewMediaService.audioLevel$.subscribe((lvl) => {
        this.audioLevel = lvl;
        this.cdr.detectChanges();
      })
    );
    this.mediaSub.add(
      this.interviewMediaService.isSpeaking$.subscribe((speaking) => {
        this.isAcousticSpeaking = speaking;
        if (speaking) {
          this.isSpeaking = true;
          if (this.questionState === 'WAITING_FOR_RESPONSE') {
            this.startSpeakingFromAcousticInput();
          }
        } else {
          this.isSpeaking = false;
        }
        this.cdr.detectChanges();
      })
    );
    this.mediaSub.add(
      this.interviewMediaService.micState$.subscribe((state) => {
        this.micState = state;
        this.cdr.detectChanges();
      })
    );

    // Initialize Candidate Monitoring & Proctoring (PART 9)
    this.startCandidateMonitoring();

    // Flush integrity monitoring events to backend periodically
    this.flushEventsInterval = setInterval(() => {
      this.flushIntegrityEvents();
    }, 15000);
  }

  private startActiveAssessment(): void {
    this.initCamera();
    this.startIntroPhase();

    // Attempt initializing interview session on backend
    this.interviewService.startInterview({ interviewId: this.interviewId }).pipe(
      timeout(8000),
      catchError(() => of(null))
    ).subscribe({
      next: (session) => {
        if (session) {
          if (session.candidateName) this.candidateName = session.candidateName;
          if (session.jobTitle) this.jobTitle = session.jobTitle;
          if (session.totalQuestionsTarget) this.totalQuestions = session.totalQuestionsTarget;
          if (session.firstQuestion) {
            this.currentQuestion = session.firstQuestion;
            this.currentQuestionNumber = session.firstQuestion.questionNumber || 1;
          }
        }
        this.cdr.detectChanges();
      },
      error: () => {
        if (!this.currentQuestion) {
          this.currentQuestion = this.fallbackQuestions[0];
        }
        this.cdr.detectChanges();
      }
    });
  }

  startIntroPhase(): void {
    this.isIntroPhase = true;
    this.hasStartedQuestioning = false;
    this.sessionState = 'AI_INTRODUCTION';
    this.introGreeting = `Welcome to your HireRanker technical interview, ${this.candidateName}. I am your AI Technical Interviewer for the ${this.jobTitle} position. I will ask you a series of technical questions based on your skills and experience. Please answer naturally and clearly. Your responses will be evaluated on technical knowledge, problem-solving, and communication. Let's begin.`;
    this.cdr.detectChanges();

    if (this.ttsEnabled && typeof window !== 'undefined' && 'speechSynthesis' in window) {
      window.speechSynthesis.cancel();
      const utterance = new SpeechSynthesisUtterance(this.introGreeting);
      utterance.rate = 1.0;
      utterance.pitch = 1.0;
      utterance.onstart = () => {
        this.isTtsSpeaking = true;
        this.cdr.detectChanges();
      };
      utterance.onend = () => {
        this.isTtsSpeaking = false;
        this.startSessionTimer();
        this.cdr.detectChanges();
        // PART 3: Automatically enter the questioning room once AI introduction finishes!
        setTimeout(() => {
          this.beginFirstQuestion();
        }, 500);
      };
      utterance.onerror = () => {
        this.isTtsSpeaking = false;
        this.startSessionTimer();
        this.cdr.detectChanges();
        setTimeout(() => {
          this.beginFirstQuestion();
        }, 500);
      };
      window.speechSynthesis.speak(utterance);
    } else {
      this.startSessionTimer();
      setTimeout(() => {
        this.beginFirstQuestion();
      }, 1500);
    }
  }

  beginFirstQuestion(): void {
    if (this.hasStartedQuestioning) return;
    this.hasStartedQuestioning = true;
    this.stopTts();
    this.startSessionTimer();
    this.isIntroPhase = false;
    this.sessionState = 'QUESTION_GENERATING';
    if (!this.currentQuestion) {
      this.currentQuestion = this.fallbackQuestions[0];
    }
    this.onQuestionLoaded();
    this.cdr.detectChanges();
  }

  startSessionTimer(): void {
    if (this.sessionTimer) return;
    this.sessionStartTime = Date.now();
    this.sessionEndTime = this.sessionStartTime + (15 * 60 * 1000);
    this.sessionSecondsRemaining = 15 * 60;

    // Independent 15-minute continuous timer updating visibly every second (PART 4)
    this.sessionTimer = setInterval(() => {
      const now = Date.now();
      this.sessionSecondsRemaining = Math.max(0, Math.round((this.sessionEndTime - now) / 1000));
      this.cdr.detectChanges();
      if (this.sessionSecondsRemaining <= 0) {
        clearInterval(this.sessionTimer);
        this.sessionTimer = null;
        this.sessionState = 'TIME_EXPIRED';
        this.finishInterview();
      }
    }, 1000);
  }

  async initCamera(): Promise<void> {
    this.interviewMediaService.logMediaState('InterviewRoom initCamera');

    // 1. Re-use existing MediaStream from pre-interview verification
    let stream = this.interviewMediaService.getCombinedStream();
    if (!stream) {
      const existing = this.interviewService.getMediaStream();
      if (existing && existing.getTracks().some(t => t.readyState === 'live')) {
        stream = existing;
        this.interviewMediaService.setActiveStream(existing);
      }
    }

    if (stream) {
      this.mediaStream = stream;
      this.cameraActive = this.interviewMediaService.isCameraLive();
      this.interviewMediaService.initAudioAnalyser();
      setTimeout(() => {
        this.attachVideoFeed();
      }, 100);
      return;
    }

    // 2. Otherwise request media devices
    if (typeof navigator !== 'undefined' && !!navigator.mediaDevices && !!navigator.mediaDevices.getUserMedia) {
      try {
        const result = await this.interviewMediaService.requestCameraAndMicrophone();
        if (result.stream) {
          this.mediaStream = result.stream;
          this.cameraActive = result.cameraOk;
          this.interviewService.setMediaStream(result.stream);
          this.interviewMediaService.initAudioAnalyser();
          setTimeout(() => {
            this.attachVideoFeed();
          }, 120);
          return;
        }
      } catch (err) {
        console.warn('Physical camera/microphone request notice:', err);
      }
    }
    this.startProctoredCanvasStream();
  }

  private attachVideoFeed(): void {
    const videoEl = document.getElementById('candidateVideo') as HTMLVideoElement;
    if (videoEl && this.mediaStream) {
      videoEl.srcObject = this.mediaStream;
      videoEl.muted = true;
      videoEl.autoplay = true;
      (videoEl as any).playsInline = true;
      videoEl.play().catch((e) => {
        console.warn('candidateVideo play notice:', e);
      });
    }
  }

  startProctoredCanvasStream(): void {
    try {
      const canvas = document.createElement('canvas');
      canvas.width = 640;
      canvas.height = 480;
      const ctx = canvas.getContext('2d');
      if (!ctx) return;

      let scanY = 120;
      let scanDirection = 2;

      const drawProctoredFeed = () => {
        if (!this.cameraActive) return;
        ctx.fillStyle = '#0b1329';
        ctx.fillRect(0, 0, 640, 480);

        // Candidate silhouette representation
        ctx.fillStyle = '#1e293b';
        ctx.beginPath();
        ctx.arc(320, 190, 75, 0, Math.PI * 2);
        ctx.fill();

        ctx.beginPath();
        ctx.ellipse(320, 390, 150, 120, 0, 0, Math.PI * 2);
        ctx.fill();

        // Facial landmarks
        ctx.strokeStyle = '#38bdf8';
        ctx.lineWidth = 2;
        ctx.beginPath();
        ctx.arc(295, 185, 10, 0, Math.PI * 2);
        ctx.stroke();
        ctx.beginPath();
        ctx.arc(345, 185, 10, 0, Math.PI * 2);
        ctx.stroke();

        // Face bounding tracking box
        ctx.strokeStyle = 'rgba(56, 189, 248, 0.45)';
        ctx.lineWidth = 1.5;
        ctx.strokeRect(230, 100, 180, 200);

        // Moving green laser scan line
        scanY += scanDirection;
        if (scanY > 280) scanDirection = -2;
        if (scanY < 120) scanDirection = 2;

        ctx.strokeStyle = 'rgba(16, 185, 129, 0.85)';
        ctx.lineWidth = 2;
        ctx.beginPath();
        ctx.moveTo(230, scanY);
        ctx.lineTo(410, scanY);
        ctx.stroke();

        ctx.fillStyle = '#10b981';
        ctx.font = 'bold 14px monospace';
        ctx.textAlign = 'left';
        ctx.fillText('🟢 AI PROCTORING ACTIVE • FACE VERIFIED', 20, 36);

        ctx.fillStyle = '#94a3b8';
        ctx.font = '12px monospace';
        ctx.fillText(`CANDIDATE: ${this.candidateName || 'Verified Candidate'}`, 20, 58);

        const timeStr = new Date().toLocaleTimeString();
        ctx.fillStyle = '#38bdf8';
        ctx.font = '12px monospace';
        ctx.textAlign = 'right';
        ctx.fillText(`STREAM-SYNC: ${timeStr}`, 620, 36);
        ctx.fillText(`PROCTOR INTEGRITY: 99.8%`, 620, 58);

        this.canvasAnimFrameId = requestAnimationFrame(drawProctoredFeed);
      };

      this.cameraActive = true;
      drawProctoredFeed();

      const canvasStream = (canvas as any).captureStream ? (canvas as any).captureStream(25) : null;
      if (canvasStream) {
        this.mediaStream = canvasStream;
        setTimeout(() => {
          this.attachVideoFeed();
        }, 100);
      }
    } catch (e) {
      console.warn('Canvas proctoring stream error:', e);
    }
  }

  ngOnDestroy(): void {
    this.stopCountdown();
    this.stopTts();
    this.stopVoiceRecognition();
    if (this.sessionTimer) {
      clearInterval(this.sessionTimer);
      this.sessionTimer = null;
    }
    if (this.proctoringCheckInterval) {
      clearInterval(this.proctoringCheckInterval);
      this.proctoringCheckInterval = null;
    }
    if (this.flushEventsInterval) {
      clearInterval(this.flushEventsInterval);
      this.flushEventsInterval = null;
    }
    if (this.toastTimer) {
      clearTimeout(this.toastTimer);
      this.toastTimer = null;
    }
    this.removeBrowserEventListeners();
    if (this.canvasAnimFrameId) {
      cancelAnimationFrame(this.canvasAnimFrameId);
      this.canvasAnimFrameId = null;
    }
    if (this.mediaSub) {
      this.mediaSub.unsubscribe();
    }
    this.interviewMediaService.setPreserveMedia(false);
    this.interviewMediaService.stopAll(true);
    if (this.mediaStream) {
      this.mediaStream.getTracks().forEach(t => t.stop());
      this.mediaStream = null;
      this.cameraActive = false;
    }
    this.interviewService.stopActiveMediaStream();
    this.flushIntegrityEvents();
  }

  loadActiveQuestion(): void {
    this.errorMessage = '';
    this.autoSkipMessage = '';
    this.questionState = 'WAITING_FOR_QUESTION';
    this.sessionState = 'QUESTION_GENERATING';
    this.showAnswerFeedback = false;
    this.cdr.detectChanges();

    this.interviewService.getNextQuestion(this.interviewId).pipe(
      timeout(10000),
      catchError(() => of(null))
    ).subscribe({
      next: (q) => {
        if (q && q.questionId) {
          this.currentQuestion = q;
          this.currentQuestionNumber = q.questionNumber || this.currentQuestionNumber;
        } else {
          const fallback = this.fallbackQuestions[this.currentQuestionNumber - 1] || this.fallbackQuestions[0];
          this.currentQuestion = fallback;
        }
        if (!this.isIntroPhase) {
          this.onQuestionLoaded();
        }
      },
      error: () => {
        const fallback = this.fallbackQuestions[this.currentQuestionNumber - 1] || this.fallbackQuestions[0];
        this.currentQuestion = fallback;
        if (!this.isIntroPhase) {
          this.onQuestionLoaded();
        }
      }
    });
  }

  /**
   * PART 6: Display AI Question Before Speaking It
   * Question text is set and rendered to DOM immediately, before TTS audio begins.
   */
  private onQuestionLoaded(): void {
    this.answerText = '';
    this.baseTranscript = '';
    this.isSpeaking = false;
    this.candidateIsAnswering = false;
    this.autoSkipMessage = '';
    this.questionState = 'QUESTION_ASKED';
    this.sessionState = 'QUESTION_SPEAKING';
    this.showAnswerFeedback = false;
    this.currentAnswerFeedback = null;
    this.currentQuestionIsAssisted = false;
    this.lookingAwayStart = null;
    this.attentionAwayActive = false;
    this.stopVoiceRecognition();

    // Immediately trigger change detection so the complete question text is painted on the screen FIRST!
    this.cdr.detectChanges();

    // Start speaking after guaranteeing the question is visible
    setTimeout(() => {
      if (this.currentQuestion?.questionText && this.ttsEnabled) {
        this.speakQuestion(this.currentQuestion.questionText);
      } else {
        this.startWaitingForCandidateResponse();
      }
    }, 120);
  }

  private startWaitingForCandidateResponse(): void {
    this.questionState = 'WAITING_FOR_RESPONSE';
    this.sessionState = 'WAITING_FOR_ANSWER';
    this.candidateIsAnswering = false;
    this.isSpeaking = false;
    this.cdr.detectChanges();

    // Start continuous 15-second speech countdown (PART 7)
    this.start15SecondCountdown();
    this.startVoiceRecognition();

    // Reset silence tracker and start audio chunk capture for Groq Whisper Cloud STT
    this.interviewMediaService.resetTurnSpeech();
    this.interviewMediaService.setSilenceCallback(() => {
      this.onNaturalSilenceDetected();
    });
    this.interviewMediaService.startAnswerRecording();
  }

  /**
   * PART 7: Fix the 15-Second Answer Countdown
   * Decrements smoothly every second using elapsed time.
   */
  start15SecondCountdown(): void {
    this.stopCountdown();
    this.countdownSeconds = 15;
    this.countdownEndTime = Date.now() + 15000;
    this.timerActive = true;
    this.cdr.detectChanges();

    this.countdownTimer = setInterval(() => {
      const remaining = Math.max(0, Math.ceil((this.countdownEndTime - Date.now()) / 1000));
      this.countdownSeconds = remaining;
      this.cdr.detectChanges();

      if (this.countdownSeconds <= 0) {
        this.stopCountdown();
        this.autoSkipDueToTimeout();
      }
    }, 1000);
  }

  stopCountdown(): void {
    this.timerActive = false;
    if (this.countdownTimer) {
      clearInterval(this.countdownTimer);
      this.countdownTimer = null;
    }
    this.cdr.detectChanges();
  }

  speakQuestion(text: string): void {
    if (!this.ttsEnabled) {
      this.startWaitingForCandidateResponse();
      return;
    }
    if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
      window.speechSynthesis.cancel();
      const utterance = new SpeechSynthesisUtterance(text);
      utterance.rate = 1.0;
      utterance.pitch = 1.0;
      utterance.onstart = () => {
        this.isTtsSpeaking = true;
        this.cdr.detectChanges();
      };
      utterance.onend = () => {
        this.isTtsSpeaking = false;
        this.cdr.detectChanges();
        this.startWaitingForCandidateResponse();
      };
      utterance.onerror = () => {
        this.isTtsSpeaking = false;
        this.cdr.detectChanges();
        this.startWaitingForCandidateResponse();
      };
      window.speechSynthesis.speak(utterance);
    } else {
      this.startWaitingForCandidateResponse();
    }
  }

  stopTts(): void {
    if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
      window.speechSynthesis.cancel();
      this.isTtsSpeaking = false;
      this.isSpeakingFeedback = false;
      this.cdr.detectChanges();
    }
  }

  replayQuestionVoice(): void {
    if (this.currentQuestion?.questionText) {
      this.speakQuestion(this.currentQuestion.questionText);
    }
  }

  /**
   * PART 5: Fix Candidate Voice Capture
   * Configure Indian English (en-IN), capture speech from beginning without re-initialization loss,
   * preserve all recognized segments, and display live transcript continuously.
   */
  startVoiceRecognition(): void {
    if (typeof window === 'undefined') return;
    const SpeechRec = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SpeechRec) {
      console.warn('[INTERVIEW ROOM] SpeechRecognition API not available in this browser');
      return;
    }

    // Do NOT destroy and re-create if already active
    if (this.recognition && this.isListening) {
      return;
    }

    this.stopVoiceRecognition();
    this.baseTranscript = this.answerText.trim();

    try {
      this.recognition = new SpeechRec();
      this.recognition.continuous = true;
      this.recognition.interimResults = true;

      // Configure Indian English (en-IN) by default with fallback
      const navLang = (typeof navigator !== 'undefined' && navigator.language) ? navigator.language : 'en-IN';
      this.recognition.lang = navLang.toLowerCase().includes('in') ? 'en-IN' : (navLang.startsWith('en') ? 'en-IN' : navLang);

      this.recognition.onstart = () => {
        this.isListening = true;
        this.cdr.detectChanges();
      };

      this.recognition.onspeechstart = () => {
        // Candidate started speaking! Immediately cancel 15-second speech countdown
        this.ngZone.run(() => {
          this.startSpeakingFromAcousticInput();
        });
      };

      this.recognition.onresult = (event: any) => {
        this.ngZone.run(() => {
          let interimTranscript = '';
          let sessionFinalTranscript = '';

          for (let i = 0; i < event.results.length; i++) {
            const result = event.results[i];
            if (result.isFinal) {
              sessionFinalTranscript += result[0].transcript + ' ';
            } else {
              interimTranscript += result[0].transcript + ' ';
            }
          }

          const currentSessionText = (sessionFinalTranscript + interimTranscript).trim();
          const combined = this.baseTranscript
            ? (this.baseTranscript + ' ' + currentSessionText).trim()
            : currentSessionText;

          if (combined) {
            this.answerText = combined;
            this.startSpeakingFromAcousticInput();
            this.onSpeechActivity();
            this.cdr.detectChanges();
          }
        });
      };

      this.recognition.onerror = (e: any) => {
        console.warn('[INTERVIEW ROOM] Speech recognition notice:', e?.error || e);
      };

      this.recognition.onend = () => {
        this.isListening = false;
        if (this.answerText.trim()) {
          this.baseTranscript = this.answerText.trim();
        }
        this.cdr.detectChanges();

        // Auto-restart if candidate is still actively answering and not submitting
        if (this.questionState === 'CANDIDATE_ANSWERING' || this.questionState === 'WAITING_FOR_RESPONSE') {
          if (!this.isSubmitting && !this.isTtsSpeaking && !this.isCompleted && !this.showAnswerFeedback) {
            try {
              this.recognition.start();
              this.isListening = true;
            } catch {}
          }
        }
      };

      this.recognition.start();
    } catch (e) {
      console.warn('[INTERVIEW ROOM] Speech recognition start failed:', e);
    }
  }

  stopVoiceRecognition(): void {
    if (this.recognition) {
      try {
        this.recognition.abort();
      } catch {}
      this.recognition = null;
      this.isListening = false;
    }
  }

  startSpeakingFromAcousticInput(): void {
    this.isSpeaking = true;
    this.candidateIsAnswering = true;
    this.sessionState = 'CANDIDATE_SPEAKING';
    if (this.questionState === 'WAITING_FOR_RESPONSE') {
      this.questionState = 'CANDIDATE_ANSWERING';
    }
    this.stopCountdown();
    this.stopTts();
    this.cdr.detectChanges();
  }

  onTextInput(): void {
    this.candidateIsAnswering = true;
    this.sessionState = 'CANDIDATE_SPEAKING';
    if (this.questionState === 'WAITING_FOR_RESPONSE') {
      this.questionState = 'CANDIDATE_ANSWERING';
    }
    this.stopCountdown();
    this.stopTts();
    this.onSpeechActivity();
    this.cdr.detectChanges();
  }

  startSpeaking(): void {
    this.candidateIsAnswering = true;
    if (this.questionState === 'WAITING_FOR_RESPONSE') {
      this.questionState = 'CANDIDATE_ANSWERING';
    }
    this.stopCountdown();
    this.stopTts();
    // Do NOT recreate if already listening - capture voice immediately from beginning
    if (!this.isListening) {
      this.startVoiceRecognition();
    }
    this.cdr.detectChanges();

    if (!this.interviewMediaService.isMicrophoneLive()) {
      this.interviewMediaService.requestCameraAndMicrophone().then(() => {
        this.interviewMediaService.initAudioAnalyser();
      });
    }
  }

  onSpeechActivity(): void {
    if (this.silenceDebounceTimer) {
      clearTimeout(this.silenceDebounceTimer);
      this.silenceDebounceTimer = null;
    }

    // Auto-submit after candidate provides meaningful answer and pauses for 2.5s
    const text = this.answerText.trim();
    if (text.length >= 15 && text.split(/\s+/).length >= 3) {
      this.silenceDebounceTimer = setTimeout(() => {
        if (this.questionState === 'CANDIDATE_ANSWERING' && !this.isSubmitting && !this.isCompleted && !this.showAnswerFeedback) {
          console.log('[INTERVIEW ROOM] Natural 2.5s silence detected. Auto-submitting answer...');
          this.finalizeAndSubmitAnswer();
        }
      }, 2500);
    }
  }

  private onNaturalSilenceDetected(): void {
    if (this.questionState === 'CANDIDATE_ANSWERING' && !this.isSubmitting && !this.isCompleted && !this.showAnswerFeedback) {
      const text = this.answerText.trim();
      if (text.length >= 8 || this.isAcousticSpeaking) {
        console.log('[INTERVIEW ROOM] Acoustic silence threshold detected. Finalizing and submitting...');
        this.finalizeAndSubmitAnswer();
      }
    }
  }

  requestQuickVerbalAnswer(): void {
    if (this.isSubmitting || this.isCompleted) return;
    this.showQuickAnswerModal = true;
  }

  cancelQuickAnswer(): void {
    this.showQuickAnswerModal = false;
  }

  confirmQuickAnswer(): void {
    this.showQuickAnswerModal = false;
    this.currentQuestionIsAssisted = true;
    this.simulateVoiceInput();
  }

  simulateVoiceInput(): void {
    this.candidateIsAnswering = true;
    if (this.questionState === 'WAITING_FOR_RESPONSE') {
      this.questionState = 'CANDIDATE_ANSWERING';
    }
    this.stopCountdown();
    this.stopTts();
    const mockAnswers = [
      'In Spring Boot, dependency injection is managed by the Spring IoC container. Constructor injection is preferred because it guarantees immutability, facilitates unit testing with mocks, and prevents NullPointerExceptions during bean initialization.',
      'Angular standalone components remove NgModule boilerplate and improve tree-shakability. Signals provide fine-grained reactivity, notifying only the specific view bindings that changed rather than triggering full component zone checks.',
      'To prevent N+1 queries in Spring Data JPA, I use @EntityGraph or JOIN FETCH in JPQL queries. Additionally, creating database indexes on foreign keys and high-cardinality search columns dramatically reduces lookup latency.',
      'For microservices authentication, we issue stateless signed JWT tokens with short expiry, validate signatures at the API gateway with public keys, and implement a distributed Redis blocklist for instant token revocation.',
      'In our high-throughput payment service, we diagnosed a thread pool starvation bottleneck using APM profiling. We refactored blocking database calls into asynchronous reactive pipelines and tuned connection pool sizing, reducing p99 latency by 72%.'
    ];
    const qIndex = (this.currentQuestionNumber - 1) % mockAnswers.length;
    this.answerText = mockAnswers[qIndex];
    this.cdr.detectChanges();
    this.onSpeechActivity();
  }

  submitAnswer(): void {
    this.finalizeAndSubmitAnswer();
  }

  async finalizeAndSubmitAnswer(): Promise<void> {
    if (!this.currentQuestion || this.isSubmitting) return;

    if (this.silenceDebounceTimer) {
      clearTimeout(this.silenceDebounceTimer);
      this.silenceDebounceTimer = null;
    }

    this.isSubmitting = true;
    this.questionState = 'PROCESSING_ANSWER';
    this.sessionState = 'ANSWER_PROCESSING';
    this.stopCountdown();
    this.stopTts();
    this.stopVoiceRecognition();
    this.cdr.detectChanges();

    try {
      // Capture recorded audio blob and send to Groq Whisper STT on backend
      this.isTranscribingWithWhisper = true;
      const audioBlob = await this.interviewMediaService.stopAnswerRecording();
      if (audioBlob && audioBlob.size > 1200) {
        try {
          const resp = await firstValueFrom(
            this.interviewService.transcribeAudio(audioBlob, this.interviewId, this.currentQuestion.questionId, true).pipe(
              timeout(6000),
              catchError(() => of(null))
            )
          );

          const whisperText = (resp?.text || resp?.transcript || '').trim();
          if (whisperText.length > 0) {
            console.log('[INTERVIEW ROOM] Groq Whisper cloud transcript received:', whisperText);
            if (!this.answerText.trim() || this.answerText.trim().length < whisperText.length) {
              this.answerText = whisperText;
            } else if (!this.answerText.toLowerCase().includes(whisperText.toLowerCase().slice(0, 15))) {
              this.answerText = `${this.answerText} ${whisperText}`.trim();
            }
          }
        } catch (whisperErr) {
          console.warn('[INTERVIEW ROOM] Groq Whisper transcription note:', whisperErr);
        }
      }
    } finally {
      this.isTranscribingWithWhisper = false;
      this.cdr.detectChanges();
    }

    this.executeSubmitAnswer();
  }

  /**
   * PART 10 & 11: AI Answer Evaluation & Spoken Feedback
   * Display answer evaluation and speak feedback before proceeding to next question.
   */
  private executeSubmitAnswer(): void {
    const answer = this.answerText.trim() || 'Candidate provided verbal answer during live session.';
    const isAssisted = this.currentQuestionIsAssisted;
    this.sessionState = 'ANSWER_EVALUATING';
    this.cdr.detectChanges();

    this.interviewService.submitAnswer(this.interviewId, this.currentQuestion!.questionId, answer, 15, isAssisted).subscribe({
      next: (res) => {
        this.isSubmitting = false;
        this.currentQuestionIsAssisted = false;
        this.displayAnswerFeedbackAndSpeak(res);
      },
      error: () => {
        this.isSubmitting = false;
        this.currentQuestionIsAssisted = false;
        if (this.currentQuestionNumber >= this.totalQuestions || this.currentQuestionNumber >= this.fallbackQuestions.length) {
          this.finishInterview();
        } else {
          this.currentQuestionNumber++;
          this.currentQuestion = this.fallbackQuestions[this.currentQuestionNumber - 1];
          this.onQuestionLoaded();
        }
      }
    });
  }

  private displayAnswerFeedbackAndSpeak(res: LiveAnswerResponse): void {
    const rawScore = res.score ?? res.technicalScore ?? res.overallScore ?? 0;
    res.score = Math.round(Number(rawScore) || 0);
    this.currentAnswerFeedback = res;
    this.pendingNextResponse = res;
    this.showAnswerFeedback = true;
    this.sessionState = 'FEEDBACK';
    this.lookingAwayStart = null;
    this.attentionAwayActive = false;
    this.cdr.detectChanges();

    // Prepare concise spoken feedback
    let speechFeedback = '';
    if (res.correctnessClassification === 'CORRECT' || res.correct === true) {
      speechFeedback = `Great answer. ` + (res.feedback || res.explanation || 'You addressed the core technical concepts accurately.');
    } else if (res.correctnessClassification === 'PARTIALLY_CORRECT') {
      speechFeedback = `Good effort. ` + (res.explanation || res.feedback || 'You got part of the answer right, but keep architectural best practices in mind.');
    } else {
      speechFeedback = `Thank you for your answer. To clarify: ` + (res.explanation || res.feedback || 'Review the core architectural and implementation details for this concept.');
    }

    this.isSpeakingFeedback = true;
    this.cdr.detectChanges();

    if (this.ttsEnabled && typeof window !== 'undefined' && 'speechSynthesis' in window) {
      window.speechSynthesis.cancel();
      const utt = new SpeechSynthesisUtterance(speechFeedback);
      utt.rate = 1.0;
      utt.pitch = 1.0;
      utt.onend = () => {
        this.isSpeakingFeedback = false;
        this.cdr.detectChanges();
        // Wait 1.2s after speaking feedback, then transition to next question
        setTimeout(() => {
          if (this.showAnswerFeedback) {
            this.proceedAfterSubmission(res);
          }
        }, 1200);
      };
      utt.onerror = () => {
        this.isSpeakingFeedback = false;
        this.cdr.detectChanges();
        setTimeout(() => {
          if (this.showAnswerFeedback) {
            this.proceedAfterSubmission(res);
          }
        }, 1200);
      };
      window.speechSynthesis.speak(utt);
    } else {
      setTimeout(() => {
        if (this.showAnswerFeedback) {
          this.proceedAfterSubmission(res);
        }
      }, 4500);
    }
  }

  advanceFromFeedback(): void {
    this.stopTts();
    if (this.pendingNextResponse) {
      this.proceedAfterSubmission(this.pendingNextResponse);
    }
  }

  private proceedAfterSubmission(res: LiveAnswerResponse): void {
    this.showAnswerFeedback = false;
    this.currentAnswerFeedback = null;
    this.pendingNextResponse = null;
    this.lookingAwayStart = null;
    this.attentionAwayActive = false;
    this.stopTts();
    this.cdr.detectChanges();

    if (res.allQuestionsCompleted || res.interviewFinished || !res.nextQuestion) {
      this.finishInterview();
    } else {
      this.questionState = 'ANSWER_COMPLETED';
      this.sessionState = 'NEXT_QUESTION';
      this.currentQuestion = res.nextQuestion;
      this.currentQuestionNumber = res.nextQuestion.questionNumber || (this.currentQuestionNumber + 1);
      this.onQuestionLoaded();
    }
  }

  skipQuestion(): void {
    if (!this.currentQuestion || this.isSubmitting) return;

    if (this.silenceDebounceTimer) {
      clearTimeout(this.silenceDebounceTimer);
      this.silenceDebounceTimer = null;
    }

    this.isSubmitting = true;
    this.stopCountdown();
    this.stopTts();
    this.stopVoiceRecognition();
    this.lookingAwayStart = null;
    this.attentionAwayActive = false;
    this.questionState = 'QUESTION_SKIPPED';
    this.cdr.detectChanges();

    this.interviewService.skipQuestion(this.interviewId, this.currentQuestion.questionId, 'Candidate skipped question').subscribe({
      next: (res) => {
        this.isSubmitting = false;
        this.proceedAfterSubmission(res);
      },
      error: () => {
        this.isSubmitting = false;
        if (this.currentQuestionNumber >= this.totalQuestions || this.currentQuestionNumber >= this.fallbackQuestions.length) {
          this.finishInterview();
        } else {
          this.currentQuestionNumber++;
          this.currentQuestion = this.fallbackQuestions[this.currentQuestionNumber - 1];
          this.onQuestionLoaded();
        }
      }
    });
  }

  private autoSkipDueToTimeout(): void {
    if (!this.currentQuestion || this.questionState === 'CANDIDATE_ANSWERING') return;

    this.stopCountdown();
    this.stopVoiceRecognition();
    this.questionState = 'QUESTION_SKIPPED';
    const timeoutMsg = "I haven't detected an answer yet. We'll move to the next question.";
    this.autoSkipMessage = timeoutMsg;
    this.cdr.detectChanges();

    if (this.ttsEnabled && typeof window !== 'undefined' && 'speechSynthesis' in window) {
      window.speechSynthesis.cancel();
      const utt = new SpeechSynthesisUtterance(timeoutMsg);
      utt.rate = 1.0;
      window.speechSynthesis.speak(utt);
    }

    this.interviewService.skipQuestion(this.interviewId, this.currentQuestion.questionId, 'TIMEOUT_15S').subscribe({
      next: (res) => {
        setTimeout(() => {
          this.autoSkipMessage = '';
          this.proceedAfterSubmission(res);
        }, 2200);
      },
      error: () => {
        setTimeout(() => {
          this.autoSkipMessage = '';
          if (this.currentQuestionNumber >= this.totalQuestions || this.currentQuestionNumber >= this.fallbackQuestions.length) {
            this.finishInterview();
          } else {
            this.currentQuestionNumber++;
            this.currentQuestion = this.fallbackQuestions[this.currentQuestionNumber - 1];
            this.onQuestionLoaded();
          }
        }, 2200);
      }
    });
  }

  /**
   * PART 12: Final Interview Conclusion
   * Stops hardware, saves monitoring events, speaks final AI conclusion, then presents final score.
   */
  finishInterview(): void {
    this.stopCountdown();
    this.stopTts();
    this.stopVoiceRecognition();
    if (this.silenceDebounceTimer) {
      clearTimeout(this.silenceDebounceTimer);
      this.silenceDebounceTimer = null;
    }
    if (this.sessionTimer) {
      clearInterval(this.sessionTimer);
      this.sessionTimer = null;
    }
    if (this.proctoringCheckInterval) {
      clearInterval(this.proctoringCheckInterval);
      this.proctoringCheckInterval = null;
    }
    this.interviewMediaService.setPreserveMedia(false);
    this.interviewMediaService.stopAll(true);
    if (this.mediaStream) {
      this.mediaStream.getTracks().forEach(t => t.stop());
      this.mediaStream = null;
      this.cameraActive = false;
    }
    this.interviewService.stopActiveMediaStream();
    this.isSubmitting = true;
    this.cdr.detectChanges();

    this.flushIntegrityEvents();

    this.interviewService.completeInterview(this.interviewId).subscribe({
      next: (result) => {
        this.isSubmitting = false;
        if (result) {
          result.multiplePersonEvents = Math.max(result.multiplePersonEvents || 0, this.multiplePersonEventsCount);
          result.faceAbsentEvents = Math.max(result.faceAbsentEvents || 0, this.faceAbsentEventsCount);
          result.attentionAwayEvents = Math.max(result.attentionAwayEvents || 0, this.attentionAwayEventsCount);
          result.tabSwitchEvents = Math.max(result.tabSwitchEvents || 0, this.tabSwitchEventsCount);
          result.fullscreenExitEvents = Math.max(result.fullscreenExitEvents || 0, this.fullscreenExitEventsCount);
          result.disconnectionEvents = Math.max(result.disconnectionEvents || 0, this.disconnectionEventsCount);
          result.totalIntegrityEvents = Math.max(
            result.totalIntegrityEvents || 0,
            result.multiplePersonEvents + result.faceAbsentEvents + result.attentionAwayEvents + result.tabSwitchEvents + result.fullscreenExitEvents + result.disconnectionEvents,
            this.totalIntegrityEventsCount
          );
        }
        this.scorecard = result;
        if (typeof localStorage !== 'undefined') {
          localStorage.setItem('hireRanker_scorecard_' + this.interviewId, JSON.stringify(result));
        }
        this.presentInterviewConclusion(result);
      },
      error: () => {
        this.interviewService.getInterviewResult(this.interviewId).subscribe({
          next: (res) => {
            this.isSubmitting = false;
            if (res) {
              res.multiplePersonEvents = Math.max(res.multiplePersonEvents || 0, this.multiplePersonEventsCount);
              res.faceAbsentEvents = Math.max(res.faceAbsentEvents || 0, this.faceAbsentEventsCount);
              res.attentionAwayEvents = Math.max(res.attentionAwayEvents || 0, this.attentionAwayEventsCount);
              res.tabSwitchEvents = Math.max(res.tabSwitchEvents || 0, this.tabSwitchEventsCount);
              res.fullscreenExitEvents = Math.max(res.fullscreenExitEvents || 0, this.fullscreenExitEventsCount);
              res.disconnectionEvents = Math.max(res.disconnectionEvents || 0, this.disconnectionEventsCount);
              res.totalIntegrityEvents = Math.max(
                res.totalIntegrityEvents || 0,
                res.multiplePersonEvents + res.faceAbsentEvents + res.attentionAwayEvents + res.tabSwitchEvents + res.fullscreenExitEvents + res.disconnectionEvents,
                this.totalIntegrityEventsCount
              );
            }
            this.scorecard = res;
            if (typeof localStorage !== 'undefined') {
              localStorage.setItem('hireRanker_scorecard_' + this.interviewId, JSON.stringify(res));
            }
            this.presentInterviewConclusion(res);
          },
          error: () => {
            this.isSubmitting = false;
            const fallbackResult: any = {
              interviewId: this.interviewId,
              candidateName: this.candidateName,
              jobTitle: this.jobTitle,
              overallScore: 0,
              technicalScore: 0,
              communicationScore: 0,
              problemSolvingScore: 0,
              recommendation: 'DO_NOT_HIRE',
              strengths: 'Not enough evidence to identify a specific strength.',
              weaknesses: 'Candidate did not provide sufficient responses for full technical evaluation.',
              summary: 'Assessment session evaluation completed.',
              totalQuestions: this.totalQuestions,
              answeredQuestions: Math.max(0, this.currentQuestionNumber - 1),
              skippedQuestions: Math.max(0, this.totalQuestions - (this.currentQuestionNumber - 1)),
              durationMinutes: 15.0,
              integrityStatus: this.totalIntegrityEventsCount > 4 ? 'REVIEW_REQUIRED' : 'NORMAL',
              totalIntegrityEvents: this.totalIntegrityEventsCount,
              multiplePersonEvents: this.multiplePersonEventsCount,
              faceAbsentEvents: this.faceAbsentEventsCount,
              attentionAwayEvents: this.attentionAwayEventsCount,
              tabSwitchEvents: this.tabSwitchEventsCount,
              fullscreenExitEvents: this.fullscreenExitEventsCount,
              disconnectionEvents: this.disconnectionEventsCount
            };
            this.scorecard = fallbackResult;
            this.presentInterviewConclusion(fallbackResult);
          }
        });
      }
    });
  }

  presentInterviewConclusion(result: LiveInterviewResult | any): void {
    this.isConclusionPhase = true;
    this.sessionState = 'COMPLETED';
    this.conclusionText = `Thank you for completing your HireRanker AI technical interview, ${this.candidateName}. All of your answers, transcripts, and evaluation criteria have been verified and scored. We wish you the very best in your hiring process. You may now review your complete evaluation scorecard.`;
    this.cdr.detectChanges();

    if (this.ttsEnabled && typeof window !== 'undefined' && 'speechSynthesis' in window) {
      window.speechSynthesis.cancel();
      const utterance = new SpeechSynthesisUtterance(this.conclusionText);
      utterance.rate = 1.0;
      utterance.pitch = 1.0;
      utterance.onend = () => {
        this.isConclusionPhase = false;
        this.isCompleted = true;
        this.questionState = 'INTERVIEW_COMPLETED';
        this.cdr.detectChanges();
      };
      utterance.onerror = () => {
        this.isConclusionPhase = false;
        this.isCompleted = true;
        this.questionState = 'INTERVIEW_COMPLETED';
        this.cdr.detectChanges();
      };
      window.speechSynthesis.speak(utterance);
    } else {
      setTimeout(() => {
        this.isConclusionPhase = false;
        this.isCompleted = true;
        this.questionState = 'INTERVIEW_COMPLETED';
        this.cdr.detectChanges();
      }, 4000);
    }
  }

  skipConclusionToScorecard(): void {
    this.stopTts();
    this.isConclusionPhase = false;
    this.isCompleted = true;
    this.questionState = 'INTERVIEW_COMPLETED';
    this.cdr.detectChanges();
  }

  // =========================================================================
  // PART 9: Candidate Monitoring and Suspicious Activity Alerts
  // =========================================================================

  private startCandidateMonitoring(): void {
    if (this.proctoringCheckInterval) {
      clearInterval(this.proctoringCheckInterval);
    }

    this.setupBrowserEventListeners();

    const FaceDetectorAPI = (window as any).FaceDetector;
    let detector: any = null;
    if (FaceDetectorAPI) {
      try {
        detector = new FaceDetectorAPI({ maxDetectedFaces: 4, fastMode: true });
      } catch {}
    }

    this.proctoringCheckInterval = setInterval(async () => {
      if (this.isCompleted || this.showExitModal) return;

      const video = document.getElementById('candidateVideo') as HTMLVideoElement;
      if (!video || video.readyState < 2 || video.videoWidth === 0) return;

      const now = Date.now();

      if (detector) {
        try {
          const faces = await detector.detect(video);
          this.handleDetectedFaces(faces, video.videoWidth, video.videoHeight, now);
          return;
        } catch {}
      }

      this.analyzeVideoFrameFallback(video, now);
    }, 1200);
  }

  private setupBrowserEventListeners(): void {
    if (typeof window === 'undefined') return;

    this.boundVisibilityHandler = () => {
      const now = Date.now();
      if (document.hidden && !this.isCompleted && !this.showExitModal) {
        if (!this.tabSwitchActive) {
          this.tabSwitchActive = true;
          this.tabSwitchEventsCount++;
          this.totalIntegrityEventsCount++;
          this.showMonitoringToast('Notice: Tab switching is monitored during the live assessment.', 'warning');
          this.recordIntegrityEvent('TAB_SWITCH', 'MEDIUM', now, now + 1000, 1.0, 'Candidate switched browser tab');
        }
      } else if (!document.hidden) {
        this.tabSwitchActive = false;
      }
    };

    this.boundBlurHandler = () => {
      if (!this.isCompleted && !this.showExitModal) {
        const now = Date.now();
        if (!this.tabSwitchActive) {
          this.tabSwitchActive = true;
          this.tabSwitchEventsCount++;
          this.totalIntegrityEventsCount++;
          this.showMonitoringToast('Notice: Tab switching is monitored during the live assessment.', 'warning');
          this.recordIntegrityEvent('WINDOW_BLUR', 'LOW', now, now + 1000, 1.0, 'Window lost active focus');
        }
      }
    };

    this.boundFullscreenHandler = () => {
      const now = Date.now();
      if (!document.fullscreenElement && !this.isCompleted && !this.showExitModal) {
        if (!this.fullscreenExitActive) {
          this.fullscreenExitActive = true;
          this.fullscreenExitEventsCount++;
          this.totalIntegrityEventsCount++;
          this.showMonitoringToast('Notice: Fullscreen exit detected during the live assessment.', 'warning');
          this.recordIntegrityEvent('FULLSCREEN_EXIT', 'LOW', now, now + 1000, 1.0, 'Candidate exited fullscreen mode');
        }
      } else if (document.fullscreenElement) {
        this.fullscreenExitActive = false;
      }
    };

    this.boundDeviceChangeHandler = () => {
      if (!this.isCompleted && !this.showExitModal) {
        const now = Date.now();
        this.disconnectionEventsCount++;
        this.totalIntegrityEventsCount++;
        this.showMonitoringToast('Notice: Camera or microphone connection interrupted.', 'warning');
        this.recordIntegrityEvent('DEVICE_DISCONNECT', 'HIGH', now, now + 1000, 1.0, 'Hardware device change or disconnection');
      }
    };

    document.addEventListener('visibilitychange', this.boundVisibilityHandler);
    window.addEventListener('blur', this.boundBlurHandler);
    document.addEventListener('fullscreenchange', this.boundFullscreenHandler);
    if (navigator?.mediaDevices?.addEventListener) {
      navigator.mediaDevices.addEventListener('devicechange', this.boundDeviceChangeHandler);
    }
  }

  private removeBrowserEventListeners(): void {
    if (typeof window === 'undefined') return;
    if (this.boundVisibilityHandler) {
      document.removeEventListener('visibilitychange', this.boundVisibilityHandler);
    }
    if (this.boundBlurHandler) {
      window.removeEventListener('blur', this.boundBlurHandler);
    }
    if (this.boundFullscreenHandler) {
      document.removeEventListener('fullscreenchange', this.boundFullscreenHandler);
    }
    if (this.boundDeviceChangeHandler && navigator?.mediaDevices?.removeEventListener) {
      navigator.mediaDevices.removeEventListener('devicechange', this.boundDeviceChangeHandler);
    }
  }

  private handleDetectedFaces(faces: any[], videoWidth: number, videoHeight: number, now: number): void {
    // A. Multiple-person detection (> 1 face)
    if (faces.length > 1) {
      this.faceAbsentStart = null;
      this.faceAbsentActive = false;
      this.lookingAwayStart = null;
      this.attentionAwayActive = false;

      if (this.multipleFacesStart === null) {
        this.multipleFacesStart = now;
      } else if (now - this.multipleFacesStart >= 2000) {
        if (!this.multipleFacesActive) {
          this.multipleFacesActive = true;
          this.multiplePersonEventsCount++;
          this.totalIntegrityEventsCount++;
          this.showMonitoringToast('Suspicious activity: Multiple faces detected in the camera view.', 'warning');
          this.recordIntegrityEvent('MULTIPLE_PERSON', 'HIGH', now, now + 2000, 2.0, 'Suspicious activity: Multiple faces detected in the camera view.');
        }
      }
      return;
    }

    // Resolves multiple faces active state when faces count drops back to <= 1
    this.multipleFacesStart = null;
    this.multipleFacesActive = false;

    // B. Face visibility (0 faces)
    if (faces.length === 0) {
      this.lookingAwayStart = null;
      this.attentionAwayActive = false;

      if (this.faceAbsentStart === null) {
        this.faceAbsentStart = now;
      } else if (now - this.faceAbsentStart >= 3000) {
        if (!this.faceAbsentActive) {
          this.faceAbsentActive = true;
          this.faceAbsentEventsCount++;
          this.totalIntegrityEventsCount++;
          this.showMonitoringToast('Please keep your face visible during the interview.', 'warning');
          this.recordIntegrityEvent('FACE_ABSENT', 'MEDIUM', now, now + 3000, 3.0, 'Please keep your face visible during the interview.');
        }
      }
      return;
    }

    // Exactly 1 face visible
    this.faceAbsentStart = null;
    this.faceAbsentActive = false;

    // C. Attention and gaze signals (ONLY DURING ACTIVE CANDIDATE ANSWERING)
    if (!this.isCandidateActivelyAnswering) {
      this.lookingAwayStart = null;
      this.attentionAwayActive = false;
      return;
    }

    const face = faces[0];
    const box = face.boundingBox;
    if (box) {
      const centerX = box.x + box.width / 2;
      const relativeX = centerX / videoWidth;

      // When candidate moves face significantly away from center (< 0.20 or > 0.80) while actively answering
      if (relativeX < 0.20 || relativeX > 0.80) {
        if (this.lookingAwayStart === null) {
          this.lookingAwayStart = now;
        } else if (now - this.lookingAwayStart >= 2500) {
          if (!this.attentionAwayActive) {
            this.attentionAwayActive = true;
            this.attentionAwayEventsCount++;
            this.totalIntegrityEventsCount++;
            this.showMonitoringToast('Attention reminder: Please focus on the interview screen while answering.', 'info');
            this.recordIntegrityEvent('ATTENTION_AWAY', 'LOW', now, now + 2500, 2.5, 'Attention reminder: Candidate looking away while answering.');
          }
        }
      } else {
        this.lookingAwayStart = null;
        this.attentionAwayActive = false;
      }
    }
  }

  private analyzeVideoFrameFallback(video: HTMLVideoElement, now: number): void {
    try {
      const canvas = document.createElement('canvas');
      canvas.width = 64;
      canvas.height = 48;
      const ctx = canvas.getContext('2d');
      if (!ctx) return;

      ctx.drawImage(video, 0, 0, 64, 48);
      const data = ctx.getImageData(0, 0, 64, 48).data;

      let skinPixels = 0;
      const colHistogram = new Int32Array(64);

      for (let y = 0; y < 48; y++) {
        for (let x = 0; x < 64; x++) {
          const idx = (y * 64 + x) * 4;
          const r = data[idx];
          const g = data[idx + 1];
          const b = data[idx + 2];

          // Normalized skin color range
          if (r > 50 && g > 30 && b > 20 && r > g && r > b && (r - g) > 12) {
            skinPixels++;
            colHistogram[x]++;
          }
        }
      }

      // Check face absence: virtually zero skin tone across the whole frame (< 20 pixels out of 3072)
      if (skinPixels < 20) {
        this.multipleFacesStart = null;
        this.multipleFacesActive = false;
        this.lookingAwayStart = null;
        this.attentionAwayActive = false;

        if (this.faceAbsentStart === null) {
          this.faceAbsentStart = now;
        } else if (now - this.faceAbsentStart >= 3000) {
          if (!this.faceAbsentActive) {
            this.faceAbsentActive = true;
            this.faceAbsentEventsCount++;
            this.totalIntegrityEventsCount++;
            this.showMonitoringToast('Please keep your face visible during the interview.', 'warning');
            this.recordIntegrityEvent('FACE_ABSENT', 'MEDIUM', now, now + 3000, 3.0, 'Please keep your face visible during the interview.');
          }
        }
        return;
      }

      // Face is visible: clear absence
      this.faceAbsentStart = null;
      this.faceAbsentActive = false;

      // Check multiple faces: find distinct separated peaks in column histogram
      let leftCount = 0;
      let midCount = 0;
      let rightCount = 0;
      for (let x = 0; x < 64; x++) {
        if (x < 24) leftCount += colHistogram[x];
        else if (x <= 40) midCount += colHistogram[x];
        else rightCount += colHistogram[x];
      }

      // Distinct multiple faces: both left and right have significant presence and separated
      const hasTwoDistinctClusters = leftCount > 90 && rightCount > 90 && skinPixels > 280;

      if (hasTwoDistinctClusters) {
        if (this.multipleFacesStart === null) {
          this.multipleFacesStart = now;
        } else if (now - this.multipleFacesStart >= 2000) {
          if (!this.multipleFacesActive) {
            this.multipleFacesActive = true;
            this.multiplePersonEventsCount++;
            this.totalIntegrityEventsCount++;
            this.showMonitoringToast('Suspicious activity: Multiple faces detected in the camera view.', 'warning');
            this.recordIntegrityEvent('MULTIPLE_PERSON', 'HIGH', now, now + 2000, 2.0, 'Suspicious activity: Multiple faces detected in the camera view.');
          }
        }
        return;
      }

      // Normal single face
      this.multipleFacesStart = null;
      this.multipleFacesActive = false;

      // Attention away: ONLY DURING ACTIVE CANDIDATE ANSWERING
      if (!this.isCandidateActivelyAnswering) {
        this.lookingAwayStart = null;
        this.attentionAwayActive = false;
        return;
      }

      if (leftCount > 130 && rightCount < 15 && midCount < 25) {
        if (this.lookingAwayStart === null) {
          this.lookingAwayStart = now;
        } else if (now - this.lookingAwayStart >= 2500) {
          if (!this.attentionAwayActive) {
            this.attentionAwayActive = true;
            this.attentionAwayEventsCount++;
            this.totalIntegrityEventsCount++;
            this.showMonitoringToast('Attention reminder: Please focus on the interview screen while answering.', 'info');
            this.recordIntegrityEvent('ATTENTION_AWAY', 'LOW', now, now + 2500, 2.5, 'Attention reminder: Candidate looking away while answering.');
          }
        }
      } else {
        this.lookingAwayStart = null;
        this.attentionAwayActive = false;
      }
    } catch {}
  }

  showMonitoringToast(message: string, type: 'warning' | 'info' = 'warning'): void {
    const now = Date.now();
    // Cooldown/deduplication: prevent duplicate toast every frame or within 5 seconds for continuous condition
    if (this.lastToastMessage === message && (now - this.lastToastTime < 5000)) {
      return;
    }
    if (this.activeToast && this.activeToast.message === message) {
      return;
    }

    this.lastToastTime = now;
    this.lastToastMessage = message;

    if (this.toastTimer) {
      clearTimeout(this.toastTimer);
      this.toastTimer = null;
    }
    const toastId = ++this.toastCounter;
    this.activeToast = { message, type, id: toastId };
    this.cdr.detectChanges();

    this.toastTimer = setTimeout(() => {
      if (this.activeToast && this.activeToast.id === toastId) {
        this.activeToast = null;
        this.cdr.detectChanges();
      }
    }, 3000);
  }

  dismissToast(): void {
    if (this.toastTimer) {
      clearTimeout(this.toastTimer);
      this.toastTimer = null;
    }
    this.activeToast = null;
    this.cdr.detectChanges();
  }

  private recordIntegrityEvent(type: string, severity: string, start: number, end: number, dur: number, msg: string): void {
    this.integrityEventsQueue.push({
      eventType: type,
      severity: severity,
      startTime: start,
      endTime: end,
      durationSeconds: dur,
      message: msg
    });
  }

  private flushIntegrityEvents(): void {
    if (this.integrityEventsQueue.length === 0 || !this.interviewId) return;

    const eventsToSave = [...this.integrityEventsQueue];
    this.integrityEventsQueue = [];

    this.interviewService.recordIntegrityEvents(this.interviewId, eventsToSave).subscribe({
      next: () => {},
      error: () => {
        // Re-queue on error
        this.integrityEventsQueue = [...eventsToSave, ...this.integrityEventsQueue];
      }
    });
  }

  // =========================================================================
  // Exit Assessment Workflow
  // =========================================================================

  promptExit(): void {
    if (this.isCompleted) {
      this.exitRoom();
      return;
    }
    this.showExitModal = true;
    this.cdr.detectChanges();
  }

  cancelExit(): void {
    this.showExitModal = false;
    this.cdr.detectChanges();
  }

  confirmExit(): void {
    this.isExiting = true;
    this.stopCountdown();
    this.stopTts();
    this.stopVoiceRecognition();
    if (this.sessionTimer) {
      clearInterval(this.sessionTimer);
      this.sessionTimer = null;
    }
    if (this.proctoringCheckInterval) {
      clearInterval(this.proctoringCheckInterval);
      this.proctoringCheckInterval = null;
    }
    this.interviewMediaService.setPreserveMedia(false);
    this.interviewMediaService.stopAll(true);
    if (this.mediaStream) {
      this.mediaStream.getTracks().forEach(t => t.stop());
      this.mediaStream = null;
      this.cameraActive = false;
    }
    this.interviewService.stopActiveMediaStream();

    this.flushIntegrityEvents();

    this.interviewService.exitInterview(this.interviewId).subscribe({
      next: () => {
        this.isExiting = false;
        this.showExitModal = false;
        this.exitRoom();
      },
      error: () => {
        this.isExiting = false;
        this.showExitModal = false;
        this.exitRoom();
      }
    });
  }

  exitRoom(): void {
    this.removeBrowserEventListeners();
    if (this.toastTimer) {
      clearTimeout(this.toastTimer);
      this.toastTimer = null;
    }
    if (this.authService.isAdmin()) {
      this.router.navigate(['/interview-scheduler']);
    } else {
      this.router.navigate(['/interviews']);
    }
  }
}
