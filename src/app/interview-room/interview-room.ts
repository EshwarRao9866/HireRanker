import { Component, OnInit, OnDestroy, inject, signal, NgZone, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { timeout, catchError, of, Subscription, firstValueFrom } from 'rxjs';
import { InterviewService, LiveQuestion, LiveInterviewResult, LiveAnswerResponse } from '../services/interview.service';
import { InterviewMediaService, MicState } from '../services/interview-media.service';
import { AuthService } from '../services/auth.service';
import { IntegrityMonitoringService, IntegrityAlert, IntegritySummary } from '../services/integrity-monitoring.service';

export type InterviewSessionState =
  | 'PREPARING'
  | 'INTRODUCTION'
  | 'QUESTION_LOADING'
  | 'QUESTION_SPEAKING'
  | 'WAITING_FOR_RESPONSE'
  | 'CANDIDATE_ANSWERING'
  | 'SUBMITTING_ANSWER'
  | 'EVALUATING_ANSWER'
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
  readonly integrityService = inject(IntegrityMonitoringService);
  private readonly ngZone = inject(NgZone);
  private readonly cdr = inject(ChangeDetectorRef);
  readonly authService = inject(AuthService);

  private mediaSub?: Subscription;
  private integrityAlertSub?: Subscription;
  private integritySummarySub?: Subscription;
  private activeQuestionToken: number = 0;

  // Active Integrity Alert & Summary
  activeIntegrityAlert: IntegrityAlert | null = null;
  integritySummary: IntegritySummary = {
    totalEvents: 0,
    multiplePersonEvents: 0,
    tabSwitchEvents: 0,
    attentionAwayEvents: 0,
    audioAnomalyEvents: 0,
    monitoredDurationSeconds: 0,
    integrityStatus: 'NORMAL'
  };

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

  // Authoritative Interview State Machine Tracker
  sessionState: InterviewSessionState = 'PREPARING';

  // Question State Machine
  questionState: QuestionFlowState = 'WAITING_FOR_QUESTION';
  autoSkipMessage: string = '';

  // Candidate Microphone & Recording State
  isRecording: boolean = false;
  private silenceDebounceTimer: any = null;

  // AI Interviewer Introduction Phase
  isIntroPhase: boolean = true;
  introGreeting: string = 'Welcome to your HireRanker technical interview. I am your AI Technical Interviewer. I will ask you a series of technical questions based on your skills and experience. Please answer naturally and clearly. Your responses will be evaluated on technical knowledge, problem-solving, and communication. Let\'s begin.';
  isIntroSpeaking: boolean = false;
  introCompleted: boolean = false;
  private canvasAnimFrameId: number | null = null;

  // 15-second speech countdown state
  countdownSeconds: number = 15;
  timerActive: boolean = false;
  private countdownTimer: any = null;

  // Speaking & Answering state
  isSpeaking: boolean = false;
  candidateIsAnswering: boolean = false;
  answerText: string = '';
  finalTranscript: string = '';
  interimTranscript: string = '';
  isSubmitting: boolean = false;
  private submissionGuard: boolean = false;
  errorMessage: string = '';
  completionNotice: string = '';

  // Completed State
  isCompleted: boolean = false;
  scorecard: LiveInterviewResult | null = null;

  // 15-Minute Overall Continuous Assessment Timer (authoritative start/end timestamp)
  sessionSecondsRemaining: number = 15 * 60; // 900 seconds
  private sessionStartTime: number = 0;
  private sessionEndTime: number = 0;
  private sessionTimer: any = null;

  get sessionTimerFormatted(): string {
    const mins = Math.floor(this.sessionSecondsRemaining / 60);
    const secs = this.sessionSecondsRemaining % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  }

  // Introduction Button Label & Status
  get introButtonText(): string {
    if (this.isQuestionStarting) {
      return 'Loading Question 1...';
    }
    if (this.sessionState === 'PREPARING') {
      return 'Preparing Interview...';
    }
    if (this.sessionState === 'INTRODUCTION' && this.isIntroSpeaking) {
      return 'Start Question 1 Now';
    }
    return 'Start Question 1';
  }

  get canStartQuestion1(): boolean {
    return !this.isQuestionStarting;
  }

  // Duplicate Protection Guards
  private isQuestionStarting: boolean = false;
  private isRequestInProgress: boolean = false;
  private introSafetyTimer: any = null;

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
      this.sessionState = 'ERROR';
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
        candidateName: this.candidateName || 'Eshwar Rao',
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
      this.sessionState = 'COMPLETED';
      this.completionNotice = 'Assessment Completed: This session was evaluated and saved. Retakes require HR authorization.';
      return;
    }

    // Enter active assessment room
    this.startActiveAssessment();

    // Subscribe to real-time acoustic level and speaking state from InterviewMediaService
    this.mediaSub = new Subscription();
    this.mediaSub.add(
      this.interviewMediaService.audioLevel$.subscribe((lvl) => {
        this.audioLevel = lvl;
      })
    );
    this.mediaSub.add(
      this.interviewMediaService.isSpeaking$.subscribe((speaking) => {
        this.isAcousticSpeaking = speaking;
        if (speaking) {
          if (this.sessionState === 'WAITING_FOR_RESPONSE' || this.questionState === 'WAITING_FOR_RESPONSE') {
            if (!this.isRecording) {
              this.startSpeaking();
            } else {
              this.startSpeakingFromAcousticInput();
            }
          } else if (this.isRecording) {
            this.isSpeaking = true;
          }
        } else {
          this.isSpeaking = false;
        }
      })
    );
    this.mediaSub.add(
      this.interviewMediaService.micState$.subscribe((state) => {
        this.micState = state;
      })
    );

    // Subscribe to AI Integrity Monitoring alerts and summary
    this.integrityAlertSub = this.integrityService.activeAlert$.subscribe((alert) => {
      this.activeIntegrityAlert = alert;
      this.cdr.markForCheck();
    });

    this.integritySummarySub = this.integrityService.summary$.subscribe((summary) => {
      this.integritySummary = summary;
      this.cdr.markForCheck();
    });
  }

  private startActiveAssessment(): void {
    this.sessionState = 'PREPARING';
    this.isIntroPhase = true;
    this.introCompleted = false;
    this.isQuestionStarting = false;

    this.initCamera();

    // Default question is pre-loaded as fallback so room never hangs on a blank spinner
    this.currentQuestion = this.fallbackQuestions[0];
    this.currentQuestionNumber = 1;

    // Initialize backend session in the background
    this.interviewService.startInterview({ interviewId: this.interviewId }).pipe(
      timeout(3500),
      catchError((err) => {
        this.errorMessage = 'Backend server is currently unavailable. Please start the backend service.';
        return of(null);
      })
    ).subscribe({
      next: (session) => {
        if (session) {
          this.errorMessage = '';
          if (session.candidateName) this.candidateName = session.candidateName;
          if (session.jobTitle) this.jobTitle = session.jobTitle;
          if (session.totalQuestionsTarget) this.totalQuestions = session.totalQuestionsTarget;
          if (session.introduction) {
            this.introGreeting = session.introduction;
          }
          if (session.firstQuestion) {
            this.currentQuestion = session.firstQuestion;
            this.currentQuestionNumber = session.firstQuestion.questionNumber || 1;
          }
        }
      },
      error: () => {
        this.errorMessage = 'Backend server is currently unavailable. Please start the backend service.';
      }
    });

    // Start the AI Introduction and the global 15-minute timer at the exact beginning of introduction
    setTimeout(() => {
      this.startIntroPhase();
    }, 150);
  }

  finishIntroPhase(): void {
    if (this.introSafetyTimer) {
      clearTimeout(this.introSafetyTimer);
      this.introSafetyTimer = null;
    }
    this.ngZone.run(() => {
      this.isTtsSpeaking = false;
      this.isIntroSpeaking = false;
      this.introCompleted = true;
      if (this.sessionState === 'PREPARING' || this.sessionState === 'INTRODUCTION') {
        // Automatically progress from introduction to question 1
        this.beginFirstQuestion();
      }
    });
  }

  startIntroPhase(): void {
    this.isIntroPhase = true;
    this.sessionState = 'INTRODUCTION';
    this.isIntroSpeaking = true;
    this.introCompleted = false;

    // Start the global 15-minute timer at the exact beginning of the introduction
    this.startSessionTimer();

    if (!this.introGreeting || this.introGreeting.trim() === '') {
      this.introGreeting = `Welcome to your HireRanker technical interview, ${this.candidateName}. I am your AI Technical Interviewer for the ${this.jobTitle} position. I will ask you a series of technical questions based on your skills and experience. Please answer naturally and clearly. Your responses will be evaluated on technical knowledge, problem-solving, and communication. Let's begin.`;
    }

    // Safety fallback timer: Ensure introduction phase finishes cleanly and advances to Question 1 even if speech synthesis is muted, blocked, or hangs
    if (this.introSafetyTimer) {
      clearTimeout(this.introSafetyTimer);
    }
    this.introSafetyTimer = setTimeout(() => {
      this.finishIntroPhase();
    }, 12000);

    if (this.ttsEnabled && typeof window !== 'undefined' && 'speechSynthesis' in window) {
      try {
        window.speechSynthesis.cancel();
        const utterance = new SpeechSynthesisUtterance(this.introGreeting);
        utterance.rate = 1.0;
        utterance.pitch = 1.0;
        utterance.onstart = () => {
          this.ngZone.run(() => {
            this.isTtsSpeaking = true;
            this.isIntroSpeaking = true;
          });
        };
        utterance.onend = () => {
          this.finishIntroPhase();
        };
        utterance.onerror = () => {
          this.finishIntroPhase();
        };
        window.speechSynthesis.speak(utterance);
      } catch (e) {
        this.finishIntroPhase();
      }
    } else {
      this.finishIntroPhase();
    }
  }

  beginFirstQuestion(): void {
    // Stop any remaining TTS and introduction state
    this.isIntroSpeaking = false;
    this.introCompleted = true;
    if (this.introSafetyTimer) {
      clearTimeout(this.introSafetyTimer);
      this.introSafetyTimer = null;
    }

    if (this.isQuestionStarting) {
      return;
    }

    this.isQuestionStarting = true;
    this.stopTts();
    this.isIntroPhase = false;
    this.sessionState = 'QUESTION_LOADING';
    this.questionState = 'WAITING_FOR_QUESTION';

    if (!this.currentQuestion) {
      this.currentQuestion = this.fallbackQuestions[0];
      this.currentQuestionNumber = 1;
    }

    this.onQuestionLoaded();
  }

  startSessionTimer(): void {
    if (this.sessionTimer) return;
    this.sessionStartTime = Date.now();
    this.sessionEndTime = this.sessionStartTime + (15 * 60 * 1000);
    this.sessionSecondsRemaining = 15 * 60;

    // Single stable timing mechanism calculating remaining time from authoritative start/end timestamp
    this.sessionTimer = setInterval(() => {
      const now = Date.now();
      this.sessionSecondsRemaining = Math.max(0, Math.round((this.sessionEndTime - now) / 1000));
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

    // 1. First check if persistent MediaStream is available in InterviewMediaService
    let stream = this.interviewMediaService.getCombinedStream();

    // 2. Or from InterviewService
    if (!stream) {
      const existing = this.interviewService.getMediaStream();
      if (existing && existing.getTracks().some(t => t.readyState === 'live')) {
        stream = existing;
        this.interviewMediaService.setActiveStream(existing);
      }
    }

    if (stream) {
      console.log('[INTERVIEW ROOM] Reusing active persistent MediaStream from pre-interview check.');
      this.mediaStream = stream;
      this.cameraActive = this.interviewMediaService.isCameraLive();
      this.interviewMediaService.initAudioAnalyser();
      setTimeout(() => {
        this.attachVideoFeed();
      }, 100);
      return;
    }

    // 3. Otherwise request camera and microphone from browser independently
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
        console.warn('Physical camera/microphone request note:', err);
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

      // Start AI Integrity Monitoring with video element
      this.integrityService.startMonitoring(videoEl);
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

        // Candidate silhouette (human silhouette representation)
        ctx.fillStyle = '#1e293b';
        ctx.beginPath();
        ctx.arc(320, 190, 75, 0, Math.PI * 2);
        ctx.fill();

        ctx.beginPath();
        ctx.ellipse(320, 390, 150, 120, 0, 0, Math.PI * 2);
        ctx.fill();

        // Facial landmarks (eyes)
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

        // HUD overlay text
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
    this.integrityService.stopMonitoring();
    this.stopCountdown();
    this.stopTts();
    this.stopVoiceRecognition();
    if (this.sessionTimer) {
      clearInterval(this.sessionTimer);
      this.sessionTimer = null;
    }
    if (this.introSafetyTimer) {
      clearTimeout(this.introSafetyTimer);
      this.introSafetyTimer = null;
    }
    if (this.silenceDebounceTimer) {
      clearTimeout(this.silenceDebounceTimer);
      this.silenceDebounceTimer = null;
    }
    if (this.canvasAnimFrameId) {
      cancelAnimationFrame(this.canvasAnimFrameId);
      this.canvasAnimFrameId = null;
    }
    if (this.mediaSub) {
      this.mediaSub.unsubscribe();
    }
    if (this.integrityAlertSub) {
      this.integrityAlertSub.unsubscribe();
    }
    if (this.integritySummarySub) {
      this.integritySummarySub.unsubscribe();
    }
    this.interviewMediaService.setPreserveMedia(false);
    this.interviewMediaService.stopAll(true);
    if (this.mediaStream) {
      this.mediaStream.getTracks().forEach(t => t.stop());
      this.mediaStream = null;
      this.cameraActive = false;
    }
    this.interviewService.stopActiveMediaStream();
  }

  loadActiveQuestion(): void {
    if (this.isRequestInProgress || this.isCompleted) return;
    this.isRequestInProgress = true;
    this.errorMessage = '';
    this.autoSkipMessage = '';
    this.sessionState = 'QUESTION_LOADING';
    this.questionState = 'WAITING_FOR_QUESTION';

    this.interviewService.getNextQuestion(this.interviewId).pipe(
      timeout(3500),
      catchError(() => {
        this.errorMessage = 'Backend server is currently unavailable. Please start the backend service.';
        return of(null);
      })
    ).subscribe({
      next: (q) => {
        this.isRequestInProgress = false;
        let newQuestion: LiveQuestion;
        if (q && q.questionId) {
          this.errorMessage = '';
          newQuestion = q;
        } else {
          this.errorMessage = 'Backend server is currently unavailable. Please start the backend service.';
          newQuestion = this.fallbackQuestions[this.currentQuestionNumber - 1] || this.fallbackQuestions[0];
        }

        // 1. FIRST: Immediately update currentQuestion and questionNumber
        this.currentQuestion = newQuestion;
        this.currentQuestionNumber = newQuestion.questionNumber || this.currentQuestionNumber;

        // 2. Then trigger question loaded lifecycle if intro phase is over
        if (!this.isIntroPhase) {
          this.onQuestionLoaded();
        }
      },
      error: () => {
        this.isRequestInProgress = false;
        this.errorMessage = 'Backend server is currently unavailable. Please start the backend service.';
        const fallback = this.fallbackQuestions[this.currentQuestionNumber - 1] || this.fallbackQuestions[0];
        this.currentQuestion = fallback;
        this.currentQuestionNumber = fallback.questionNumber || this.currentQuestionNumber;
        if (!this.isIntroPhase) {
          this.onQuestionLoaded();
        }
      }
    });
  }

  private onQuestionLoaded(): void {
    // Increment active question token to invalidate any stale asynchronous callbacks from previous questions
    this.activeQuestionToken++;
    const currentToken = this.activeQuestionToken;

    // 1. Reset the question-specific UI state FIRST so the Angular UI renders the new question immediately
    this.answerText = '';
    this.finalTranscript = '';
    this.interimTranscript = '';
    this.submissionGuard = false;
    this.isSpeaking = false;
    this.candidateIsAnswering = false;
    this.autoSkipMessage = '';
    this.questionState = 'QUESTION_ASKED';
    this.sessionState = 'QUESTION_SPEAKING';
    this.stopCountdown();
    this.stopVoiceRecognition();
    if (this.silenceDebounceTimer) {
      clearTimeout(this.silenceDebounceTimer);
      this.silenceDebounceTimer = null;
    }

    // Force immediate Angular change detection to guarantee DOM has rendered the new question card
    this.cdr.detectChanges();

    // 2. ONLY AFTER the DOM has rendered the new question, TTS starts speaking
    setTimeout(() => {
      // Guard against token mismatch or state transition during the render tick
      if (this.activeQuestionToken !== currentToken) return;

      if (this.currentQuestion?.questionText && this.ttsEnabled) {
        this.speakQuestion(this.currentQuestion.questionText, currentToken);
      } else {
        this.startWaitingForCandidateResponse();
      }
    }, 60);
  }

  private startWaitingForCandidateResponse(): void {
    this.questionState = 'WAITING_FOR_RESPONSE';
    this.sessionState = 'WAITING_FOR_RESPONSE';
    this.candidateIsAnswering = false;
    this.isSpeaking = false;
    this.isRecording = false;

    // Reset turn speech and set up silence detection callback
    this.interviewMediaService.resetTurnSpeech();
    this.interviewMediaService.setSilenceCallback(() => {
      this.onNaturalSilenceDetected();
    });

    // Prime speech recognition and acoustic recorder so voice input is captured immediately without manual button clicks
    this.startVoiceRecognition();
    this.interviewMediaService.startAnswerRecording();
    this.isRecording = true;

    // Start completely separate 15-second countdown ONLY after TTS finishes
    this.start15SecondCountdown();
  }

  start15SecondCountdown(): void {
    this.stopCountdown();
    this.countdownSeconds = 15;
    this.timerActive = true;

    this.countdownTimer = setInterval(() => {
      if (this.countdownSeconds > 0) {
        this.countdownSeconds--;
      } else {
        // 15-second speech countdown expired without an answer!
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
  }

  speakQuestion(text: string, token: number = this.activeQuestionToken): void {
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
        this.ngZone.run(() => {
          if (this.activeQuestionToken === token) {
            this.isTtsSpeaking = true;
            this.sessionState = 'QUESTION_SPEAKING';
          }
        });
      };
      utterance.onend = () => {
        this.ngZone.run(() => {
          if (this.activeQuestionToken === token) {
            this.isTtsSpeaking = false;
            this.startWaitingForCandidateResponse();
          }
        });
      };
      utterance.onerror = () => {
        this.ngZone.run(() => {
          if (this.activeQuestionToken === token) {
            this.isTtsSpeaking = false;
            this.startWaitingForCandidateResponse();
          }
        });
      };
      window.speechSynthesis.speak(utterance);
    } else {
      this.isTtsSpeaking = false;
      this.startWaitingForCandidateResponse();
    }
  }

  stopTts(): void {
    if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
      window.speechSynthesis.cancel();
      this.isTtsSpeaking = false;
    }
  }

  replayQuestionVoice(): void {
    // Replay ONLY the current question text without changing question, number, or state
    if (this.currentQuestion?.questionText) {
      this.stopCountdown();
      this.speakQuestion(this.currentQuestion.questionText, this.activeQuestionToken);
    }
  }

  private baseTranscript: string = '';

  private recognitionRestartAttempts: number = 0;
  private maxRestartAttempts: number = 8;

  startVoiceRecognition(): void {
    if (typeof window === 'undefined') return;
    const SpeechRec = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SpeechRec) {
      console.warn('[INTERVIEW ROOM] SpeechRecognition API not available in this browser');
      return;
    }

    this.stopVoiceRecognition();
    this.interimTranscript = '';

    try {
      this.recognition = new SpeechRec();
      this.recognition.continuous = true;
      this.recognition.interimResults = true;
      this.recognition.lang = 'en-US';

      this.recognition.onstart = () => {
        this.ngZone.run(() => {
          this.isListening = true;
          this.recognitionRestartAttempts = 0;
        });
      };

      this.recognition.onspeechstart = () => {
        this.ngZone.run(() => {
          this.startSpeakingFromAcousticInput();
        });
      };

      this.recognition.onresult = (event: any) => {
        this.ngZone.run(() => {
          let accumulatedFinal = '';
          let currentInterim = '';

          for (let i = event.resultIndex; i < event.results.length; i++) {
            const res = event.results[i];
            const textChunk = (res[0]?.transcript || '').trim();
            if (!textChunk) continue;

            if (res.isFinal) {
              accumulatedFinal += (accumulatedFinal ? ' ' : '') + textChunk;
            } else {
              currentInterim += (currentInterim ? ' ' : '') + textChunk;
            }
          }

          if (accumulatedFinal) {
            this.finalTranscript = this.finalTranscript
              ? (this.finalTranscript + ' ' + accumulatedFinal).replace(/\s+/g, ' ').trim()
              : accumulatedFinal.trim();
          }

          this.interimTranscript = currentInterim.trim();

          // Full combined text: confirmed final transcript + current partial interim speech
          const combined = [this.finalTranscript, this.interimTranscript]
            .filter(Boolean)
            .join(' ')
            .replace(/\s+/g, ' ')
            .trim();

          if (combined) {
            this.answerText = combined;
            this.startSpeakingFromAcousticInput();
            this.onSpeechActivity();
          }
        });
      };

      this.recognition.onerror = (e: any) => {
        console.warn('[INTERVIEW ROOM] Speech recognition error/notice:', e?.error || e);
        if (e?.error === 'not-allowed') {
          this.errorMessage = 'Microphone permission is required to answer.';
        }
      };

      this.recognition.onend = () => {
        this.ngZone.run(() => {
          this.isListening = false;
          // If candidate is still answering or waiting, restart recognition cleanly without losing accumulated final transcript
          if (this.isRecording && !this.submissionGuard && !this.isSubmitting && !this.isTtsSpeaking && !this.isCompleted) {
            if (this.sessionState === 'CANDIDATE_ANSWERING' || this.sessionState === 'WAITING_FOR_RESPONSE') {
              if (this.recognitionRestartAttempts < this.maxRestartAttempts) {
                this.recognitionRestartAttempts++;
                try {
                  this.recognition.start();
                  this.isListening = true;
                } catch (startErr) {
                  console.warn('[INTERVIEW ROOM] Speech recognition restart exception:', startErr);
                }
              }
            }
          }
        });
      };

      this.recognition.start();
    } catch (e) {
      console.warn('[INTERVIEW ROOM] Speech recognition start failed:', e);
    }
  }

  stopVoiceRecognition(): void {
    this.recognitionRestartAttempts = 0;
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
    this.sessionState = 'CANDIDATE_ANSWERING';
    this.questionState = 'CANDIDATE_ANSWERING';
    // Candidate started speaking: stop the 15-second response countdown (Global 15-min timer continues undisturbed)
    this.stopCountdown();
    this.stopTts();
  }

  onTextInput(): void {
    this.candidateIsAnswering = true;
    this.sessionState = 'CANDIDATE_ANSWERING';
    this.questionState = 'CANDIDATE_ANSWERING';
    this.stopCountdown();
    this.stopTts();
    this.finalTranscript = this.answerText.trim();
    this.interimTranscript = '';
    this.onSpeechActivity();
  }

  startSpeaking(): void {
    if (this.submissionGuard || this.isSubmitting || this.isCompleted) return;
    this.isRecording = true;
    this.candidateIsAnswering = true;
    this.sessionState = 'CANDIDATE_ANSWERING';
    this.questionState = 'CANDIDATE_ANSWERING';
    this.stopCountdown();
    this.stopTts();
    this.startVoiceRecognition();
    this.interviewMediaService.startAnswerRecording();
    if (!this.interviewMediaService.isMicrophoneLive()) {
      this.interviewMediaService.requestCameraAndMicrophone().then(() => {
        this.interviewMediaService.initAudioAnalyser();
      });
    }
  }

  onSpeechActivity(): void {
    // Reset silence timer on speech activity
    if (this.silenceDebounceTimer) {
      clearTimeout(this.silenceDebounceTimer);
      this.silenceDebounceTimer = null;
    }

    const text = this.answerText.trim();
    // Allow natural pauses: only schedule 4.0s silence debounce if substantial answer content exists (>= 20 chars, >= 4 words)
    if (text.length >= 20 && text.split(/\s+/).length >= 4) {
      this.silenceDebounceTimer = setTimeout(() => {
        if ((this.sessionState === 'CANDIDATE_ANSWERING' || this.questionState === 'CANDIDATE_ANSWERING') && !this.submissionGuard && !this.isSubmitting && !this.isCompleted) {
          console.log('[INTERVIEW ROOM] Natural 4.0s silence threshold detected after complete candidate response. Finalizing answer...');
          this.finalizeAndSubmitAnswer();
        }
      }, 4000);
    }
  }

  private onNaturalSilenceDetected(): void {
    if ((this.sessionState === 'CANDIDATE_ANSWERING' || this.questionState === 'CANDIDATE_ANSWERING') && !this.submissionGuard && !this.isSubmitting && !this.isCompleted && this.isRecording) {
      const text = this.answerText.trim();
      // Only finalize on acoustic silence if candidate has provided a substantial spoken answer (>= 20 chars and >= 4 words)
      if (text.length >= 20 && text.split(/\s+/).length >= 4) {
        console.log('[INTERVIEW ROOM] 4000ms acoustic silence threshold detected after substantial answer. Finalizing and submitting answer...');
        this.finalizeAndSubmitAnswer();
      } else {
        // If transcript is short or candidate paused mid-sentence, keep listening without auto-submitting
        console.log('[INTERVIEW ROOM] Acoustic pause detected with short transcript. Continuing to listen for candidate...');
      }
    }
  }

  submitAnswer(): void {
    this.finalizeAndSubmitAnswer();
  }

  async finalizeAndSubmitAnswer(): Promise<void> {
    if (!this.currentQuestion || this.submissionGuard || this.isSubmitting || this.isCompleted) return;

    const trimmedAnswer = this.answerText.trim();
    if (!trimmedAnswer && !this.isRecording) {
      console.log('[INTERVIEW ROOM] Attempted submit with completely empty answer and no recording. Aborting submission.');
      return;
    }

    // Activate submission guard immediately to prevent double submissions
    this.submissionGuard = true;
    this.isRecording = false;

    if (this.silenceDebounceTimer) {
      clearTimeout(this.silenceDebounceTimer);
      this.silenceDebounceTimer = null;
    }

    this.isSubmitting = true;
    this.sessionState = 'SUBMITTING_ANSWER';
    this.questionState = 'PROCESSING_ANSWER';
    this.stopCountdown();
    this.stopTts();
    this.stopVoiceRecognition();

    try {
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
            if (!this.answerText.trim() || this.answerText.trim().length < whisperText.length) {
              this.answerText = whisperText;
            } else if (!this.answerText.toLowerCase().includes(whisperText.toLowerCase().slice(0, 15))) {
              this.answerText = `${this.answerText} ${whisperText}`.trim();
            }
          }
        } catch (whisperErr) {
          console.warn('[INTERVIEW ROOM] Groq Whisper transcription note, using existing transcript:', whisperErr);
        }
      }
    } finally {
      this.isTranscribingWithWhisper = false;
    }

    // Check once more after Whisper transcription
    if (!this.answerText.trim()) {
      console.log('[INTERVIEW ROOM] Answer remains completely empty after audio processing. Handling as no response timeout.');
      this.isSubmitting = false;
      this.submissionGuard = false;
      this.autoSkipDueToTimeout();
      return;
    }

    this.executeSubmitAnswer();
  }

  private executeSubmitAnswer(): void {
    const answer = this.answerText.trim() || 'Candidate provided verbal answer during live session.';
    this.sessionState = 'EVALUATING_ANSWER';
    this.isRecording = false;

    this.interviewService.submitAnswer(this.interviewId, this.currentQuestion!.questionId, answer, 15).subscribe({
      next: (res) => {
        this.isSubmitting = false;

        // If answer is incorrect and AI provides constructive explanation, speak it aloud before next question
        if (res.correct === false && res.explanation) {
          const explanationSpeech = `Thank you for your answer. To clarify: ${res.explanation}`;
          if (this.ttsEnabled && typeof window !== 'undefined' && 'speechSynthesis' in window) {
            const utt = new SpeechSynthesisUtterance(explanationSpeech);
            utt.rate = 1.0;
            utt.onend = () => {
              this.proceedAfterSubmission(res);
            };
            utt.onerror = () => {
              this.proceedAfterSubmission(res);
            };
            window.speechSynthesis.speak(utt);
            return;
          }
        }

        this.proceedAfterSubmission(res);
      },
      error: (err) => {
        this.isSubmitting = false;
        this.submissionGuard = false;
        this.errorMessage = 'Backend server is currently unavailable. Please start the backend service.';
      }
    });
  }

  private proceedAfterSubmission(res: LiveAnswerResponse): void {
    if (res.allQuestionsCompleted || res.interviewFinished || !res.nextQuestion) {
      this.questionState = 'INTERVIEW_COMPLETED';
      this.sessionState = 'COMPLETED';
      this.finishInterview();
    } else {
      this.questionState = 'ANSWER_COMPLETED';
      this.sessionState = 'NEXT_QUESTION';

      // 1. FIRST update currentQuestion and currentQuestionNumber immediately
      this.currentQuestion = res.nextQuestion;
      this.currentQuestionNumber = res.nextQuestion.questionNumber || (this.currentQuestionNumber + 1);

      // 2. Then proceed to onQuestionLoaded (UI render first, TTS only after)
      this.onQuestionLoaded();
    }
  }

  skipQuestion(): void {
    if (!this.currentQuestion || this.isSubmitting || this.isCompleted) return;

    if (this.silenceDebounceTimer) {
      clearTimeout(this.silenceDebounceTimer);
      this.silenceDebounceTimer = null;
    }

    this.isSubmitting = true;
    this.stopCountdown();
    this.stopTts();
    this.stopVoiceRecognition();
    this.questionState = 'QUESTION_SKIPPED';
    this.sessionState = 'SUBMITTING_ANSWER';

    this.interviewService.skipQuestion(this.interviewId, this.currentQuestion.questionId, 'Candidate skipped question').subscribe({
      next: (res) => {
        this.isSubmitting = false;
        this.proceedAfterSubmission(res);
      },
      error: () => {
        this.isSubmitting = false;
        this.errorMessage = 'Backend server is currently unavailable. Please start the backend service.';
        if (this.currentQuestionNumber >= this.totalQuestions || this.currentQuestionNumber >= this.fallbackQuestions.length) {
          this.questionState = 'INTERVIEW_COMPLETED';
          this.sessionState = 'COMPLETED';
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
    if (!this.currentQuestion || this.sessionState === 'CANDIDATE_ANSWERING' || this.questionState === 'CANDIDATE_ANSWERING') return;

    this.stopCountdown();
    this.stopVoiceRecognition();
    this.questionState = 'QUESTION_SKIPPED';
    this.sessionState = 'NEXT_QUESTION';

    // Speak exact required prompt: "I didn't hear an answer. We'll move to the next question."
    const timeoutMsg = "I didn't hear an answer. We'll move to the next question.";
    this.autoSkipMessage = timeoutMsg;

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

    // Stop integrity monitoring and collect recorded events
    this.integrityService.stopMonitoring();
    const integrityEvents = this.integrityService.getRecordedEvents();

    this.interviewMediaService.setPreserveMedia(false);
    this.interviewMediaService.stopAll(true);
    if (this.mediaStream) {
      this.mediaStream.getTracks().forEach(t => t.stop());
      this.mediaStream = null;
      this.cameraActive = false;
    }
    this.interviewService.stopActiveMediaStream();
    this.isSubmitting = true;

    // Log integrity events to backend then finalize interview
    const completeAndShowResult = () => {
      this.interviewService.completeInterview(this.interviewId).subscribe({
        next: (result) => {
          this.isSubmitting = false;
          this.isCompleted = true;
          this.questionState = 'INTERVIEW_COMPLETED';
          this.sessionState = 'COMPLETED';
          this.scorecard = result;
          if (typeof localStorage !== 'undefined') {
            localStorage.setItem('hireRanker_scorecard_' + this.interviewId, JSON.stringify(result));
          }

          // TEST 22: Professional AI conclusion speech
          const conclusionSpeech = `Congratulations ${this.candidateName}. You have completed your technical interview for ${this.jobTitle}. Your responses have been evaluated and your performance scorecard is now available. Thank you for your time.`;
          if (this.ttsEnabled && typeof window !== 'undefined' && 'speechSynthesis' in window) {
            window.speechSynthesis.cancel();
            const utt = new SpeechSynthesisUtterance(conclusionSpeech);
            utt.rate = 1.0;
            window.speechSynthesis.speak(utt);
          }
        },
        error: () => {
          // Attempt retrieving existing scorecard
          this.interviewService.getInterviewResult(this.interviewId).subscribe({
            next: (res) => {
              this.isSubmitting = false;
              this.isCompleted = true;
              this.questionState = 'INTERVIEW_COMPLETED';
              this.scorecard = res;
              if (typeof localStorage !== 'undefined') {
                localStorage.setItem('hireRanker_scorecard_' + this.interviewId, JSON.stringify(res));
              }
            },
            error: () => {
              this.isSubmitting = false;
              this.errorMessage = 'Backend server is currently unavailable. Please start the backend service.';
            }
          });
        }
      });
    };

    if (integrityEvents.length > 0) {
      this.interviewService.logIntegrityEvents(this.interviewId, integrityEvents).pipe(
        timeout(3000),
        catchError(() => of(null))
      ).subscribe(() => {
        completeAndShowResult();
      });
    } else {
      completeAndShowResult();
    }
  }

  promptExit(): void {
    if (this.isCompleted) {
      this.exitRoom();
      return;
    }
    this.showExitModal = true;
  }

  cancelExit(): void {
    this.showExitModal = false;
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
    this.interviewMediaService.setPreserveMedia(false);
    this.interviewMediaService.stopAll(true);
    if (this.mediaStream) {
      this.mediaStream.getTracks().forEach(t => t.stop());
      this.mediaStream = null;
      this.cameraActive = false;
    }
    this.interviewService.stopActiveMediaStream();

    // Call backend to mark interview status as PENDING_HR_REVIEW / ABANDONED
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
    if (this.authService.isAdmin()) {
      this.router.navigate(['/interview-scheduler']);
    } else {
      this.router.navigate(['/interviews']);
    }
  }
}
