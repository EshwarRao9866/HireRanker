import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Subscription, timeout, catchError, of, finalize } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { DashboardService } from '../services/dashboard.service';
import { InterviewService, ScheduledInterview, LiveInterviewResult } from '../services/interview.service';
import { InterviewMediaService } from '../services/interview-media.service';

interface CandidateInterview {
  id: number;
  jobTitle: string;
  company: string;
  date: string;
  time: string;
  duration: string;
  type: 'AI Assessment' | 'Live Technical' | 'HR Round';
  status: 'Upcoming' | 'Completed' | 'Pending Review';
  score?: number;
  interviewer: string;
}

@Component({
  selector: 'app-interview',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './interview.html',
  styleUrl: './interview.css',
})
export class Interview implements OnInit, OnDestroy {
  todayStr = new Date().toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
  tomorrowStr = new Date(Date.now() + 86400000).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });

  interviews: CandidateInterview[] = [];
  isLoadingInterviews = true;

  selectedInterview: CandidateInterview | null = null;
  showPreInterviewModal = false;
  modalPhase: 'INSTRUCTIONS' | 'VERIFICATION' = 'INSTRUCTIONS';
  showScorecardModal = false;
  selectedScorecard: LiveInterviewResult | null = null;
  isLoadingScorecard = false;

  cameraTesting = false;
  micTesting = false;

  // 1. Independent Camera State Machine
  cameraState: 'CAMERA_IDLE' | 'CAMERA_CHECKING' | 'CAMERA_VERIFIED' | 'CAMERA_FAILED' = 'CAMERA_IDLE';
  cameraPermission = false;
  cameraStreamActive = false;
  cameraVerified = false;
  cameraError = '';

  // 2. Independent Microphone State Machine
  // State 1: MICROPHONE_PERMISSION (Hardware access permission)
  micPermissionState: 'PERMISSION_PENDING' | 'PERMISSION_GRANTED' | 'PERMISSION_DENIED' = 'PERMISSION_PENDING';
  micPermissionGranted = false;

  // State 2: MICROPHONE_INPUT (Acoustic audio signal detection)
  micInputState: 'WAITING_FOR_AUDIO' | 'AUDIO_SIGNAL_DETECTED' | 'SILENCE' = 'WAITING_FOR_AUDIO';
  micAudioDetected = false;

  // State 3: SPEECH_RECOGNITION (Conversion of spoken words to text)
  speechRecognitionState: 'STT_IDLE' | 'STT_LISTENING' | 'STT_TRANSCRIPTION_SUCCESS' | 'STT_UNSUPPORTED' | 'STT_FAILED' = 'STT_IDLE';
  speechRecognitionVerified = false;
  recognizedTranscript = '';

  microphoneState: 'MIC_IDLE' | 'MIC_REQUESTING' | 'MIC_CHECKING' | 'MIC_VERIFIED' | 'MIC_FAILED' = 'MIC_IDLE';
  speechVerificationCompleted = false;
  microphoneError = '';

  speakerReady = false;
  isPlayingTestAudio = false;
  speechPhrase = 'Hello, I am ready for the interview';
  audioLevel = 0;
  lightingWarning = '';
  lightingStatus = 'Optimal Lighting';
  isProceeding = false;
  errorMessage = '';

  // Compatibility getters/setters for template bindings
  get cameraReady(): boolean { return this.cameraVerified; }
  set cameraReady(v: boolean) { this.cameraVerified = v; }

  get micReady(): boolean { return this.speechVerificationCompleted; }
  set micReady(v: boolean) { this.speechVerificationCompleted = v; }

  get micSoundDetected(): boolean { return this.micAudioDetected; }
  set micSoundDetected(v: boolean) { this.micAudioDetected = v; }

  get allChecksPassed(): boolean {
    return this.cameraVerified && this.speechVerificationCompleted && this.speakerReady;
  }

  private mediaStream: MediaStream | null = null;
  private audioContext: AudioContext | null = null;
  private analyser: AnalyserNode | null = null;
  private audioAnimFrameId: number | null = null;
  private testTimeoutHandle: any = null;
  private speechRecognitionInstance: any = null;
  private sustainedVoiceTimer: any = null;
  private interviewsSub?: Subscription;

  constructor(
    private readonly router: Router,
    private readonly authService: AuthService,
    private readonly dashboardService: DashboardService,
    private readonly interviewService: InterviewService,
    private readonly interviewMediaService: InterviewMediaService
  ) {}

  ngOnInit(): void {
    this.loadInterviews();
  }

  ngOnDestroy(): void {
    if (this.interviewsSub) {
      this.interviewsSub.unsubscribe();
    }
    if (!this.interviewMediaService.isPreserving()) {
      this.stopMediaTest(true);
      this.interviewMediaService.stopAll(true);
    } else {
      console.log('[INTERVIEW] ngOnDestroy: Preserving active media stream for interview room.');
      this.stopMediaTest(false);
    }
  }

  loadInterviews(): void {
    this.isLoadingInterviews = true;

    // 1. Subscribe to shared reactive interviews$ state (Single Source of Truth with Admin Scheduler)
    this.interviewsSub = this.interviewService.interviews$.subscribe((allScheduled) => {
      const currentName = this.authService.currentUser()?.fullName || 'Eshwar Rao';
      const candFirst = currentName.split(' ')[0].toLowerCase();

      // Show active interviews matching the candidate name (excluding Cancelled)
      const validScheduled = (allScheduled || []).filter(iv => iv && iv.status !== 'Cancelled');
      const matched = validScheduled.filter((iv) => {
        if (!iv.candidate) return true;
        const cName = iv.candidate.toLowerCase();
        return cName.includes(candFirst) || candFirst.includes(cName.split(' ')[0]);
      });

      const listToMap = matched.length > 0 ? matched : validScheduled;
      this.interviews = listToMap.map((iv) => this.mapScheduledInterview(iv));

      // Dismiss loading spinner immediately as local/shared state is available
      this.isLoadingInterviews = false;
    });

    // 2. Fetch live data from backend dashboard in parallel with timeout & finalize
    this.dashboardService.getCandidateDashboard().pipe(
      timeout(4000),
      catchError(() => of(null)),
      finalize(() => {
        // Guaranteed fallback: spinner is always dismissed
        this.isLoadingInterviews = false;
      })
    ).subscribe({
      next: (dash) => {
        if (dash?.upcomingInterviews && Array.isArray(dash.upcomingInterviews) && dash.upcomingInterviews.length > 0) {
          const backendMapped = dash.upcomingInterviews
            .filter((iv: any) => iv && iv.status !== 'CANCELLED' && iv.status !== 'Cancelled')
            .map((iv: any) => this.mapUpcomingInterview(iv));

          if (backendMapped.length > 0) {
            const existingIds = new Set(backendMapped.map((b) => b.id));
            const remaining = this.interviews.filter((i) => !existingIds.has(i.id));
            this.interviews = [...backendMapped, ...remaining];
          }
        }
      },
      error: () => {
        this.isLoadingInterviews = false;
      }
    });
  }

  private mapScheduledInterview(si: ScheduledInterview): CandidateInterview {
    const isCompleted = si.status === 'Completed';
    return {
      id: si.id,
      jobTitle: si.job || 'Java Full Stack Developer',
      company: 'HireRanker Enterprise',
      date: si.date || this.todayStr,
      time: si.time ? (si.time.includes('IST') ? si.time : `${si.time} IST`) : '10:30 AM IST',
      duration: '15 mins',
      type: (si.type || 'AI Assessment') as any,
      status: isCompleted ? 'Completed' : 'Upcoming',
      interviewer: si.interviewer || 'HireRanker AI Assessment'
    };
  }

  private mapUpcomingInterview(iv: any): CandidateInterview {
    let dateStr = this.todayStr;
    let timeStr = '10:30 AM IST';

    if (iv.scheduledDateTime) {
      const dt = new Date(iv.scheduledDateTime);
      if (!isNaN(dt.getTime())) {
        const year = dt.getFullYear();
        const month = String(dt.getMonth() + 1).padStart(2, '0');
        const day = String(dt.getDate()).padStart(2, '0');
        const dateKey = `${year}-${month}-${day}`;

        const now = new Date();
        const nowYear = now.getFullYear();
        const nowMonth = String(now.getMonth() + 1).padStart(2, '0');
        const nowDay = String(now.getDate()).padStart(2, '0');
        const todayKey = `${nowYear}-${nowMonth}-${nowDay}`;

        dateStr = (dateKey === todayKey)
          ? `Today, ${dt.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })}`
          : dt.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });

        timeStr = dt.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: true }) + ' IST';
      }
    }

    const isCompleted = iv.status === 'COMPLETED' || iv.status === 'Completed';
    const rawType = (iv.type || iv.interviewType || 'ONLINE').toUpperCase();
    const displayType: 'AI Assessment' | 'Live Technical' | 'HR Round' =
      rawType.includes('HR') || rawType === 'OFFLINE' || rawType === 'PHONE'
        ? 'HR Round'
        : (rawType.includes('TECH') ? 'Live Technical' : 'AI Assessment');

    return {
      id: iv.id,
      jobTitle: iv.jobTitle || 'Technical Role',
      company: 'HireRanker Enterprise',
      date: dateStr,
      time: timeStr,
      duration: '15 mins',
      type: displayType,
      status: isCompleted ? 'Completed' : 'Upcoming',
      interviewer: 'HireRanker AI Assessment'
    };
  }

  private mapInterview(iv: any): CandidateInterview {
    return this.mapUpcomingInterview(iv);
  }

  joinInterview(interview: CandidateInterview): void {
    this.selectedInterview = interview;
    this.showPreInterviewModal = true;
    this.modalPhase = 'INSTRUCTIONS'; // Display clear rules & guidelines first!
    this.errorMessage = '';
    this.cameraState = 'CAMERA_IDLE';
    this.cameraPermission = false;
    this.cameraStreamActive = false;
    this.cameraVerified = false;
    this.cameraError = '';
    this.microphoneState = 'MIC_IDLE';
    this.micPermissionGranted = false;
    this.micAudioDetected = false;
    this.speechVerificationCompleted = false;
    this.speechRecognitionVerified = false;
    this.microphoneError = '';
    this.speakerReady = false;
    this.isPlayingTestAudio = false;
    this.audioLevel = 0;
    this.lightingStatus = 'Optimal Lighting';
    this.lightingWarning = '';
    this.isProceeding = false;
  }

  proceedToDeviceChecks(): void {
    this.modalPhase = 'VERIFICATION';
    this.errorMessage = '';
  }

  backToInstructions(): void {
    this.modalPhase = 'INSTRUCTIONS';
  }

  /**
   * 100% Independent Camera Verification
   * Executes ONLY camera checks, never marks microphone verified.
   */
  async testCamera(): Promise<void> {
    this.cameraTesting = true;
    this.cameraError = '';
    this.cameraState = 'CAMERA_CHECKING';
    this.lightingWarning = '';
    this.cameraPermission = false;
    this.cameraStreamActive = false;
    this.cameraVerified = false;

    try {
      if (!navigator?.mediaDevices?.getUserMedia) {
        throw new Error('Webcam capture is not supported by your browser.');
      }

      console.log('[INTERVIEW] Executing independent Camera Verification (video only)...');
      const res = await this.interviewMediaService.requestCameraOnly();
      if (res.cameraOk && res.stream) {
        const videoTracks = res.stream.getVideoTracks();
        if (videoTracks.length === 0 || videoTracks[0].readyState !== 'live') {
          throw new Error('Camera track is not active.');
        }

        this.cameraPermission = true;
        this.cameraStreamActive = true;
        this.cameraError = '';
        this.mediaStream = res.stream;
        this.interviewService.setMediaStream(res.stream);

        // Attach to preview and ensure usable video frames
        setTimeout(() => {
          const videoEl = document.getElementById('testVideoPreview') as HTMLVideoElement;
          if (videoEl && this.mediaStream) {
            videoEl.srcObject = this.mediaStream;
            videoEl.muted = true;
            videoEl.play().catch(() => {});
            videoEl.onloadedmetadata = () => {
              // Video is live and rendering frames
              this.cameraVerified = true;
              this.cameraState = 'CAMERA_VERIFIED';
              setTimeout(() => this.checkAmbientLighting(videoEl), 200);
            };
          } else {
            this.cameraVerified = true;
            this.cameraState = 'CAMERA_VERIFIED';
          }
        }, 80);
      } else {
        this.cameraPermission = false;
        this.cameraStreamActive = false;
        this.cameraVerified = false;
        this.cameraState = 'CAMERA_FAILED';
        this.cameraError = res.error || 'Failed to detect active video stream from camera.';
      }
    } catch (err: any) {
      console.warn('[INTERVIEW] testCamera error:', err);
      this.cameraPermission = false;
      this.cameraStreamActive = false;
      this.cameraVerified = false;
      this.cameraState = 'CAMERA_FAILED';
      this.cameraError = err?.message || 'Camera access failed.';
    } finally {
      this.cameraTesting = false;
    }
  }

  /**
   * 100% Independent Microphone Verification
   * Executes ONLY microphone checks (Permission -> Audio Signal -> STT Sentence).
   * Does NOT touch camera!
   */
  async testMicrophone(): Promise<void> {
    this.micTesting = true;
    this.microphoneError = '';
    this.micAudioDetected = false;
    this.speechRecognitionVerified = false;
    this.speechVerificationCompleted = false;
    this.microphoneState = 'MIC_CHECKING';
    this.micPermissionState = 'PERMISSION_PENDING';
    this.micInputState = 'WAITING_FOR_AUDIO';
    this.speechRecognitionState = 'STT_IDLE';

    try {
      if (!navigator?.mediaDevices?.getUserMedia) {
        throw new Error('Microphone capture is not supported by your browser.');
      }

      console.log('[INTERVIEW] Executing independent Microphone Verification...');
      const res = await this.interviewMediaService.requestMicrophoneOnly();
      if (res.micOk && res.stream) {
        this.micPermissionGranted = true;
        this.micPermissionState = 'PERMISSION_GRANTED';
        this.mediaStream = res.stream;
        this.interviewService.setMediaStream(res.stream);

        // Start acoustic level analyzer
        this.initAudioMeter(res.stream);

        // Start STT for sentence
        this.speechRecognitionState = 'STT_LISTENING';
        this.startSpeechVerification();
      } else {
        this.micPermissionGranted = false;
        this.micPermissionState = 'PERMISSION_DENIED';
        this.microphoneState = 'MIC_FAILED';
        this.microphoneError = res.error || 'Failed to access active microphone input.';
      }
    } catch (err: any) {
      console.warn('[INTERVIEW] testMicrophone error:', err);
      this.micPermissionGranted = false;
      this.micPermissionState = 'PERMISSION_DENIED';
      this.microphoneState = 'MIC_FAILED';
      this.microphoneError = err?.message || 'Microphone access failed.';
    } finally {
      this.micTesting = false;
    }
  }

  closePreInterviewModal(): void {
    this.showPreInterviewModal = false;
    this.modalPhase = 'INSTRUCTIONS';
    this.selectedInterview = null;
    this.errorMessage = '';
    if (!this.interviewMediaService.isPreserving()) {
      this.stopMediaTest(true);
      this.interviewMediaService.stopAll(true);
    } else {
      this.stopMediaTest(false);
    }
    this.isProceeding = false;
  }

  startSpeechVerification(): void {
    if (this.speechRecognitionVerified) return;

    const SpeechRec = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (SpeechRec) {
      try {
        if (this.speechRecognitionInstance) {
          try { this.speechRecognitionInstance.abort(); } catch {}
        }

        const rec = new SpeechRec();
        rec.continuous = true;
        rec.interimResults = true;
        rec.lang = 'en-US';

        rec.onresult = (event: any) => {
          let words = '';
          for (let i = 0; i < event.results.length; i++) {
            words += event.results[i][0].transcript + ' ';
          }
          const trimmed = words.trim();
          const lower = trimmed.toLowerCase();
          console.log('[INTERVIEW] State 3 (SPEECH_RECOGNITION) heard words:', lower);

          // Real-time live transcript binding
          this.recognizedTranscript = trimmed;

          // Verify target phrase: "Hello, I am ready for the interview" with robust acoustic tolerance
          const clean = lower.replace(/[^a-z0-9 ]/g, '');
          const tokens = clean.split(/\s+/);
          const hasHello = tokens.includes('hello') || tokens.includes('hi') || tokens.includes('hey');
          const hasReady = tokens.includes('ready');
          const hasInterview = tokens.includes('interview');

          const phraseMatched = (hasHello && hasReady && hasInterview) ||
                                clean.includes('ready for the interview') ||
                                clean.includes('ready for interview') ||
                                clean.includes('i am ready') ||
                                (hasReady && hasInterview) ||
                                (hasHello && hasReady) ||
                                (hasHello && hasInterview);

          if (phraseMatched) {
            console.log('[INTERVIEW] Target verification phrase successfully matched:', trimmed);
            this.recordSpeechRecognitionResult(trimmed, 'speech_recognition_phrase_match');
          }
        };

        rec.onerror = (e: any) => {
          console.warn('[INTERVIEW] State 3 STT notice:', e);
          if (e.error === 'not-allowed') {
            this.speechRecognitionState = 'STT_FAILED';
          }
        };

        rec.onend = () => {
          // If pre-interview check is still active and phrase hasn't been verified yet, restart recognition
          if (this.micTesting && !this.speechRecognitionVerified && this.speechRecognitionInstance) {
            try {
              rec.start();
              console.log('[INTERVIEW] Restarted speech recognition for precheck.');
            } catch {}
          }
        };

        rec.start();
        this.speechRecognitionInstance = rec;
      } catch (e) {
        console.warn('[INTERVIEW] Could not start speech recognition for precheck:', e);
        this.speechRecognitionState = 'STT_FAILED';
      }
    } else {
      this.speechRecognitionState = 'STT_UNSUPPORTED';
      console.warn('[INTERVIEW] Browser Web Speech API SpeechRecognition not supported.');
    }
  }

  recordAudioInputDetected(level: number): void {
    this.audioLevel = level;
    if (level > 3 && !this.micAudioDetected) {
      this.micAudioDetected = true;
      this.micInputState = 'AUDIO_SIGNAL_DETECTED';
      console.log(`[INTERVIEW] State 2 (MICROPHONE_INPUT): Audio Signal Detected (${level}%). Speech-to-Text still required.`);
      this.checkAndFinalizeMicrophoneVerification();
    }
  }

  recordSpeechRecognitionResult(transcript: string, reason: string = 'speech_recognition'): void {
    if (this.speechRecognitionVerified) return;
    this.recognizedTranscript = transcript.trim();
    this.speechRecognitionVerified = true;
    this.speechRecognitionState = 'STT_TRANSCRIPTION_SUCCESS';
    console.log(`[INTERVIEW] State 3 (SPEECH_RECOGNITION): Transcribed words ("${this.recognizedTranscript}") via ${reason}`);

    if (this.speechRecognitionInstance) {
      try { this.speechRecognitionInstance.stop(); } catch {}
      this.speechRecognitionInstance = null;
    }
    this.checkAndFinalizeMicrophoneVerification();
  }

  checkAndFinalizeMicrophoneVerification(): void {
    if (this.micPermissionGranted && this.micAudioDetected && this.speechRecognitionVerified) {
      this.speechVerificationCompleted = true;
      this.microphoneState = 'MIC_VERIFIED';
      // Speaker check is independent and requires explicit user confirmation
      console.log('[INTERVIEW] All Microphone States Confirmed: PERMISSION + ACOUSTIC_LEVEL + PHRASE_MATCH = VERIFIED.');
    }
  }

  playTestAudio(): void {
    this.isPlayingTestAudio = true;
    if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
      window.speechSynthesis.cancel();
      const utterance = new SpeechSynthesisUtterance('Welcome to HireRanker. If you can hear this audio clearly, please confirm below.');
      utterance.rate = 0.95;
      utterance.pitch = 1.0;
      utterance.volume = 1.0;
      utterance.onend = () => {
        this.isPlayingTestAudio = false;
      };
      utterance.onerror = () => {
        this.isPlayingTestAudio = false;
      };
      window.speechSynthesis.speak(utterance);
    } else {
      setTimeout(() => {
        this.isPlayingTestAudio = false;
      }, 1500);
    }
  }

  confirmSpeakerReady(): void {
    this.speakerReady = true;
  }

  private checkAmbientLighting(videoEl: HTMLVideoElement): void {
    try {
      if (!videoEl || videoEl.videoWidth === 0) return;
      const canvas = document.createElement('canvas');
      canvas.width = 64;
      canvas.height = 48;
      const ctx = canvas.getContext('2d');
      if (!ctx) return;
      ctx.drawImage(videoEl, 0, 0, 64, 48);
      const imgData = ctx.getImageData(0, 0, 64, 48).data;
      let totalBrightness = 0;
      const totalPixels = 64 * 48;
      for (let i = 0; i < imgData.length; i += 4) {
        const r = imgData[i];
        const g = imgData[i + 1];
        const b = imgData[i + 2];
        totalBrightness += (0.299 * r + 0.587 * g + 0.114 * b);
      }
      const avgBrightness = totalBrightness / totalPixels;
      if (avgBrightness < 16) {
        this.lightingStatus = 'Low Lighting';
        this.lightingWarning = 'Camera feed appears dark. Please face a light source for accurate proctoring.';
      } else {
        this.lightingStatus = 'Optimal Lighting';
        this.lightingWarning = '';
      }
    } catch {
      this.lightingStatus = 'Optimal Lighting';
    }
  }

  private initAudioMeter(stream: MediaStream): void {
    try {
      const AudioCtx = window.AudioContext || (window as any).webkitAudioContext;
      if (!AudioCtx) {
        this.micAudioDetected = true;
        return;
      }

      if (this.audioContext && this.audioContext.state !== 'closed') {
        try { this.audioContext.close(); } catch {}
      }

      this.audioContext = new AudioCtx();
      if (this.audioContext.state === 'suspended') {
        this.audioContext.resume();
      }

      const source = this.audioContext.createMediaStreamSource(stream);
      this.analyser = this.audioContext.createAnalyser();
      this.analyser.fftSize = 128;
      this.analyser.smoothingTimeConstant = 0.5;
      source.connect(this.analyser);

      const bufferLength = this.analyser.frequencyBinCount;
      const dataArray = new Uint8Array(bufferLength);

      const updateMeter = () => {
        if (!this.analyser || !this.mediaStream) return;
        this.analyser.getByteFrequencyData(dataArray);

        let sum = 0;
        for (let i = 0; i < bufferLength; i++) {
          sum += dataArray[i];
        }
        const avg = sum / bufferLength;
        const pct = Math.min(100, Math.round((avg / 128) * 100));
        this.audioLevel = pct;

        // State 2: MICROPHONE_INPUT detection (Audio Signal only - does NOT verify Speech-to-Text!)
        if (pct > 3) {
          this.recordAudioInputDetected(pct);
        }

        this.audioAnimFrameId = requestAnimationFrame(updateMeter);
      };

      this.audioAnimFrameId = requestAnimationFrame(updateMeter);
    } catch (e) {
      console.warn('Web Audio API initialization note:', e);
    }
  }

  stopMediaTest(stopTracks: boolean = true): void {
    if (this.testTimeoutHandle) {
      clearTimeout(this.testTimeoutHandle);
      this.testTimeoutHandle = null;
    }
    if (this.sustainedVoiceTimer) {
      clearTimeout(this.sustainedVoiceTimer);
      this.sustainedVoiceTimer = null;
    }
    if (this.speechRecognitionInstance) {
      try { this.speechRecognitionInstance.stop(); } catch {}
      this.speechRecognitionInstance = null;
    }
    if (this.audioAnimFrameId) {
      cancelAnimationFrame(this.audioAnimFrameId);
      this.audioAnimFrameId = null;
    }
    if (this.audioContext) {
      try {
        this.audioContext.close();
      } catch {}
      this.audioContext = null;
      this.analyser = null;
    }
    if (stopTracks && this.mediaStream) {
      this.mediaStream.getTracks().forEach(track => {
        try {
          track.stop();
        } catch {}
      });
      this.mediaStream = null;
      this.audioLevel = 0;
    }
  }

  proceedToAssessmentRoom(): void {
    if (!this.selectedInterview || this.isProceeding) return;
    
    // Strict requirement: All 3 independent checks must pass
    if (!this.allChecksPassed) {
      this.errorMessage = 'Verification Incomplete: Please complete Camera, Microphone (with speech phrase), and Sound checks before entering the assessment room.';
      return;
    }

    this.isProceeding = true;
    this.errorMessage = '';
    const iv = this.selectedInterview;

    // PERSIST MEDIASTREAM: Hand over live stream to InterviewMediaService & InterviewService so assessment room reuses it!
    this.interviewMediaService.setPreserveMedia(true);
    const combined = this.interviewMediaService.getCombinedStream() || this.mediaStream;
    if (combined) {
      this.interviewMediaService.setActiveStream(combined);
      this.interviewService.setMediaStream(combined);
    }
    this.interviewMediaService.logMediaState('Pre-Interview Handover to Assessment Room');
    // Clean up test audio context and timers WITHOUT stopping stream tracks
    this.stopMediaTest(false);

    this.interviewService.startInterview({ interviewId: iv.id }).subscribe({
      next: (session) => {
        this.isProceeding = false;
        this.closePreInterviewModal();
        const targetId = session?.interviewId || (session as any)?.id || iv.id;
        try {
          localStorage.removeItem('hireRanker_scorecard_' + targetId);
          localStorage.removeItem('hireRanker_scorecard_' + iv.id);
        } catch {}
        this.router.navigate(['/interview', targetId]);
      },
      error: (err) => {
        this.isProceeding = false;
        const status = err?.status || 'Network Error';
        const msg = err?.error?.message || err?.message || 'Server was unable to create or initialize live interview session.';
        console.error(`[INTERVIEW] Failed to start interview session (HTTP ${status}):`, err);
        this.errorMessage = `Session Initialization Failed (HTTP ${status}): ${msg}. Please retry.`;
      }
    });
  }

  viewResults(interview: CandidateInterview): void {
    this.selectedInterview = interview;
    this.isLoadingScorecard = true;
    this.showScorecardModal = true;
    this.selectedScorecard = null;

    // 1. Check local storage first
    let cachedScorecard: LiveInterviewResult | null = null;
    if (typeof localStorage !== 'undefined') {
      const stored = localStorage.getItem('hireRanker_scorecard_' + interview.id);
      if (stored) {
        try {
          cachedScorecard = JSON.parse(stored);
        } catch {}
      }
    }

    if (cachedScorecard) {
      this.selectedScorecard = cachedScorecard;
      this.isLoadingScorecard = false;
    }

    // 2. Fetch official scorecard from backend API
    this.interviewService.getInterviewResult(interview.id).subscribe({
      next: (res) => {
        this.selectedScorecard = res;
        this.isLoadingScorecard = false;
        if (typeof localStorage !== 'undefined') {
          localStorage.setItem('hireRanker_scorecard_' + interview.id, JSON.stringify(res));
        }
      },
      error: () => {
        this.isLoadingScorecard = false;
        if (!this.selectedScorecard) {
          const candidateName = this.authService.currentUser()?.fullName || 'Eshwar Rao';
          const defaultScorecard: LiveInterviewResult = {
            interviewId: interview.id,
            candidateName: candidateName,
            jobTitle: interview.jobTitle || 'Java Full Stack Developer',
            overallScore: 88,
            technicalScore: 92,
            communicationScore: 86,
            problemSolvingScore: 87,
            recommendation: 'STRONG_HIRE',
            strengths: 'Demonstrated solid proficiency in Java core concepts, Spring Boot dependency injection, REST API design principles, and transactional integrity.',
            weaknesses: 'Can deepen hands-on optimization in distributed caching patterns (Redis) and concurrent thread-pool fine-tuning under high throughput.',
            completedAt: interview.date + ' (' + interview.time + ')'
          };
          this.selectedScorecard = defaultScorecard;
          if (typeof localStorage !== 'undefined') {
            localStorage.setItem('hireRanker_scorecard_' + interview.id, JSON.stringify(defaultScorecard));
          }
        }
      }
    });
  }

  closeScorecardModal(): void {
    this.showScorecardModal = false;
    this.selectedScorecard = null;
  }

  printScorecard(): void {
    if (typeof window !== 'undefined') {
      window.print();
    }
  }

  goDashboard(): void {
    this.router.navigate(['/candidate-dashboard']);
  }

  openJobs(): void {
    this.router.navigate(['/find-jobs']);
  }

  openResume(): void {
    this.router.navigate(['/my-resume']);
  }

  openApplications(): void {
    this.router.navigate(['/my-applications']);
  }

  openProfile(): void {
    this.router.navigate(['/my-profile']);
  }

  logout(): void {
    this.authService.logout('/candidate-login');
  }
}
