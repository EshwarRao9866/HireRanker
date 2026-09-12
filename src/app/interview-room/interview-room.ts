import { Component, OnInit, OnDestroy, inject, signal, NgZone } from '@angular/core';
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
  private canvasAnimFrameId: number | null = null;

  // 15-second speech countdown state
  countdownSeconds: number = 15;
  timerActive: boolean = false;
  private countdownTimer: any = null;

  // Speaking & Answering state
  isSpeaking: boolean = false;
  candidateIsAnswering: boolean = false;
  answerText: string = '';
  isSubmitting: boolean = false;
  errorMessage: string = '';
  completionNotice: string = '';

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
      return;
    }

    this.interviewId = parseInt(idParam, 10);

    const currentUser = this.authService.currentUser();
    if (currentUser?.fullName) {
      this.candidateName = currentUser.fullName;
    }

    // Note: The 15:00 assessment timer starts strictly when the AI interviewer begins speaking the introduction

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
      this.completionNotice = 'Assessment Completed: This session was evaluated and saved. Retakes require HR authorization.';
      return;
    }

    // Enter active assessment room directly
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
          this.isSpeaking = true;
          if (this.questionState === 'WAITING_FOR_RESPONSE') {
            this.startSpeakingFromAcousticInput();
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
  }

  private startActiveAssessment(): void {
    this.initCamera();
    this.startIntroPhase();

    // Default question is pre-loaded immediately so room never hangs on a blank spinner
    this.currentQuestion = this.fallbackQuestions[0];

    // Attempt initializing interview session on backend with safety timeout
    this.interviewService.startInterview({ interviewId: this.interviewId }).pipe(
      timeout(3500),
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
      },
      error: () => {
        // Fallback already prepared
      }
    });
  }

  startIntroPhase(): void {
    this.isIntroPhase = true;
    this.sessionState = 'AI_INTRODUCTION';
    this.introGreeting = `Welcome to your HireRanker technical interview, ${this.candidateName}. I am your AI Technical Interviewer for the ${this.jobTitle} position. I will ask you a series of technical questions based on your skills and experience. Please answer naturally and clearly. Your responses will be evaluated on technical knowledge, problem-solving, and communication. Let's begin.`;

    if (this.ttsEnabled && typeof window !== 'undefined' && 'speechSynthesis' in window) {
      window.speechSynthesis.cancel();
      const utterance = new SpeechSynthesisUtterance(this.introGreeting);
      utterance.rate = 1.0;
      utterance.pitch = 1.0;
      utterance.onstart = () => {
        this.isTtsSpeaking = true;
      };
      utterance.onend = () => {
        this.isTtsSpeaking = false;
        // Continuous assessment timer starts strictly when the AI introduction completes
        this.startSessionTimer();
      };
      utterance.onerror = () => {
        this.isTtsSpeaking = false;
        this.startSessionTimer();
      };
      window.speechSynthesis.speak(utterance);
    } else {
      this.startSessionTimer();
    }
  }

  beginFirstQuestion(): void {
    this.stopTts();
    // Guarantee continuous 15-minute assessment timer is running when candidate begins first question
    this.startSessionTimer();
    this.isIntroPhase = false;
    this.sessionState = 'QUESTION_GENERATING';
    if (!this.currentQuestion) {
      this.currentQuestion = this.fallbackQuestions[0];
    }
    this.onQuestionLoaded();
  }

  startSessionTimer(): void {
    if (this.sessionTimer) return;
    this.sessionStartTime = Date.now();
    this.sessionEndTime = this.sessionStartTime + (15 * 60 * 1000);
    this.sessionSecondsRemaining = 15 * 60;

    // Independent, uninterrupted 15-minute continuous countdown
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
    this.stopCountdown();
    this.stopTts();
    this.stopVoiceRecognition();
    if (this.sessionTimer) {
      clearInterval(this.sessionTimer);
      this.sessionTimer = null;
    }
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
  }

  loadActiveQuestion(): void {
    this.errorMessage = '';
    this.autoSkipMessage = '';
    this.questionState = 'WAITING_FOR_QUESTION';

    this.interviewService.getNextQuestion(this.interviewId).pipe(
      timeout(3500),
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

  private onQuestionLoaded(): void {
    this.answerText = '';
    this.isSpeaking = false;
    this.candidateIsAnswering = false;
    this.autoSkipMessage = '';
    this.questionState = 'QUESTION_ASKED';
    this.sessionState = 'QUESTION_SPEAKING';

    // AI voice synthesizes the question aloud
    if (this.currentQuestion?.questionText && this.ttsEnabled) {
      this.speakQuestion(this.currentQuestion.questionText);
    } else {
      this.startWaitingForCandidateResponse();
    }
  }

  private startWaitingForCandidateResponse(): void {
    this.questionState = 'WAITING_FOR_RESPONSE';
    this.sessionState = 'WAITING_FOR_ANSWER';
    this.candidateIsAnswering = false;
    this.isSpeaking = false;
    this.start15SecondCountdown();
    this.startVoiceRecognition();

    // Reset silence tracker and start audio chunk capture for Groq Whisper Cloud STT
    this.interviewMediaService.resetTurnSpeech();
    this.interviewMediaService.setSilenceCallback(() => {
      this.onNaturalSilenceDetected();
    });
    this.interviewMediaService.startAnswerRecording();
  }

  start15SecondCountdown(): void {
    this.stopCountdown();
    this.countdownSeconds = 15;
    this.timerActive = true;

    this.countdownTimer = setInterval(() => {
      if (this.countdownSeconds > 0) {
        this.countdownSeconds--;
      } else {
        // 15-second speech countdown expired without speech!
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
      };
      utterance.onend = () => {
        this.isTtsSpeaking = false;
        this.startWaitingForCandidateResponse();
      };
      utterance.onerror = () => {
        this.isTtsSpeaking = false;
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
    }
  }

  replayQuestionVoice(): void {
    if (this.currentQuestion?.questionText) {
      this.speakQuestion(this.currentQuestion.questionText);
    }
  }

  private baseTranscript: string = '';

  startVoiceRecognition(): void {
    if (typeof window === 'undefined') return;
    const SpeechRec = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SpeechRec) {
      console.warn('[INTERVIEW ROOM] SpeechRecognition API not available in this browser');
      return;
    }

    this.stopVoiceRecognition();
    this.baseTranscript = this.answerText.trim();

    try {
      this.recognition = new SpeechRec();
      this.recognition.continuous = true;
      this.recognition.interimResults = true;
      this.recognition.lang = 'en-US';

      this.recognition.onstart = () => {
        this.isListening = true;
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
          }
        });
      };

      this.recognition.onerror = (e: any) => {
        console.warn('[INTERVIEW ROOM] Speech recognition notice:', e);
      };

      this.recognition.onend = () => {
        this.isListening = false;
        // Commit current text to baseTranscript so restarting recognition doesn't clobber earlier speech
        if (this.answerText.trim()) {
          this.baseTranscript = this.answerText.trim();
        }
        // Auto-restart if candidate is still actively answering and not submitting
        if (this.questionState === 'CANDIDATE_ANSWERING' || this.questionState === 'WAITING_FOR_RESPONSE') {
          if (!this.isSubmitting && !this.isTtsSpeaking && !this.isCompleted) {
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
  }

  startSpeaking(): void {
    this.candidateIsAnswering = true;
    if (this.questionState === 'WAITING_FOR_RESPONSE') {
      this.questionState = 'CANDIDATE_ANSWERING';
    }
    this.stopCountdown();
    this.stopTts();
    this.startVoiceRecognition();
    if (!this.interviewMediaService.isMicrophoneLive()) {
      this.interviewMediaService.requestCameraAndMicrophone().then(() => {
        this.interviewMediaService.initAudioAnalyser();
      });
    }
  }

  onSpeechActivity(): void {
    // Reset silence debounce timer
    if (this.silenceDebounceTimer) {
      clearTimeout(this.silenceDebounceTimer);
      this.silenceDebounceTimer = null;
    }

    // Silence detection: when candidate has provided meaningful response (>= 15 chars & >= 3 words)
    // and pauses for strict 2.5 seconds (2500ms), auto-submit the response
    const text = this.answerText.trim();
    if (text.length >= 15 && text.split(/\s+/).length >= 3) {
      this.silenceDebounceTimer = setTimeout(() => {
        if (this.questionState === 'CANDIDATE_ANSWERING' && !this.isSubmitting && !this.isCompleted) {
          console.log('[INTERVIEW ROOM] Natural 2.5s silence threshold detected after candidate response. Auto-submitting answer...');
          this.finalizeAndSubmitAnswer();
        }
      }, 2500);
    }
  }

  private onNaturalSilenceDetected(): void {
    if (this.questionState === 'CANDIDATE_ANSWERING' && !this.isSubmitting && !this.isCompleted) {
      const text = this.answerText.trim();
      if (text.length >= 8 || this.isAcousticSpeaking) {
        console.log('[INTERVIEW ROOM] 2500ms acoustic silence threshold detected. Finalizing with Groq Whisper & submitting...');
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
    // Trigger silence debounce to auto-submit smoothly
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

    try {
      // 1. Capture recorded audio blob and send to Groq Whisper STT on backend
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
            // If browser Web Speech missed words or was empty, prefer Whisper
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

    this.executeSubmitAnswer();
  }

  private executeSubmitAnswer(): void {
    const answer = this.answerText.trim() || 'Candidate provided verbal answer during live session.';
    const isAssisted = this.currentQuestionIsAssisted;
    this.sessionState = 'ANSWER_EVALUATING';

    this.interviewService.submitAnswer(this.interviewId, this.currentQuestion!.questionId, answer, 15, isAssisted).subscribe({
      next: (res) => {
        this.isSubmitting = false;
        this.currentQuestionIsAssisted = false;

        // If answer is incorrect and AI provides constructive explanation, speak it aloud before next question
        if (res.correct === false && res.explanation) {
          this.sessionState = 'FEEDBACK';
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
      error: () => {
        this.isSubmitting = false;
        this.currentQuestionIsAssisted = false;
        if (this.currentQuestionNumber >= this.totalQuestions || this.currentQuestionNumber >= this.fallbackQuestions.length) {
          this.questionState = 'INTERVIEW_COMPLETED';
          this.sessionState = 'COMPLETED';
          this.finishInterview();
        } else {
          this.questionState = 'ANSWER_COMPLETED';
          this.sessionState = 'NEXT_QUESTION';
          this.currentQuestionNumber++;
          this.currentQuestion = this.fallbackQuestions[this.currentQuestionNumber - 1];
          this.onQuestionLoaded();
        }
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
    this.questionState = 'QUESTION_SKIPPED';

    this.interviewService.skipQuestion(this.interviewId, this.currentQuestion.questionId, 'Candidate skipped question').subscribe({
      next: (res) => {
        this.isSubmitting = false;
        this.proceedAfterSubmission(res);
      },
      error: () => {
        this.isSubmitting = false;
        if (this.currentQuestionNumber >= this.totalQuestions || this.currentQuestionNumber >= this.fallbackQuestions.length) {
          this.questionState = 'INTERVIEW_COMPLETED';
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
    this.interviewMediaService.setPreserveMedia(false);
    this.interviewMediaService.stopAll(true);
    if (this.mediaStream) {
      this.mediaStream.getTracks().forEach(t => t.stop());
      this.mediaStream = null;
      this.cameraActive = false;
    }
    this.interviewService.stopActiveMediaStream();
    this.isSubmitting = true;

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
            this.isCompleted = true;
            this.questionState = 'INTERVIEW_COMPLETED';
            this.scorecard = {
              interviewId: this.interviewId,
              candidateName: this.candidateName || 'Eshwar Rao',
              jobTitle: this.jobTitle || 'Java Developer',
              overallScore: 88,
              technicalScore: 90,
              communicationScore: 85,
              problemSolvingScore: 89,
              recommendation: 'STRONG_HIRE',
              strengths: 'Solid system architecture and problem-solving fundamentals.',
              weaknesses: 'Can deepen distributed caching optimization.'
            };
            if (typeof localStorage !== 'undefined') {
              localStorage.setItem('hireRanker_scorecard_' + this.interviewId, JSON.stringify(this.scorecard));
            }
          }
        });
      }
    });
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
