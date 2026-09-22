import { Injectable, NgZone } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { InterviewMediaService } from './interview-media.service';
import { ClientIntegrityEvent } from './interview.service';

export type IntegrityEventType =
  | 'MULTIPLE_PERSON'
  | 'ATTENTION_AWAY'
  | 'TAB_SWITCH'
  | 'WINDOW_BLUR'
  | 'FULLSCREEN_EXIT'
  | 'AUDIO_ANOMALY';

export type IntegritySeverity = 'LOW' | 'MEDIUM' | 'HIGH';

export interface IntegrityAlert {
  title: string;
  message: string;
  severity: IntegritySeverity;
  timestamp: number;
}

export interface IntegritySummary {
  totalEvents: number;
  multiplePersonEvents: number;
  tabSwitchEvents: number;
  attentionAwayEvents: number;
  audioAnomalyEvents: number;
  monitoredDurationSeconds: number;
  integrityStatus: 'NORMAL' | 'REVIEW_REQUIRED';
}

interface ActiveConditionTracker {
  conditionType: IntegrityEventType;
  startTime: number;
  consecutiveFrames: number;
  hasTriggeredEvent: boolean;
}

/**
 * AI Interview Integrity Monitoring System
 *
 * Provides non-invasive, parallel browser/camera/audio integrity monitoring:
 * 1. Multiple people detection in webcam
 * 2. Attention monitoring (prolonged looking away / face missing)
 * 3. Browser tab & window blur detection
 * 4. Audio anomalies / unexpected speech
 *
 * BROWSER LIMITATIONS & ACCURACY RULES:
 * - Cannot see outside webcam boundary (e.g. secondary monitors, external phones).
 * - Probabilistic face/attention cues (natural blinks & pauses are ignored).
 * - Audio analysis flags energy/spectral anomalies, not certified multi-speaker attribution.
 * - Non-punitive: technical scores remain untouched; flags are marked REVIEW_REQUIRED for HR.
 */
@Injectable({
  providedIn: 'root'
})
export class IntegrityMonitoringService {
  // Configuration thresholds
  private readonly MULTIPLE_FACE_THRESHOLD_MS = 3000; // >= 3s sustained 2+ faces
  private readonly ATTENTION_AWAY_THRESHOLD_MS = 3500; // >= 3.5s sustained gaze away
  private readonly VISION_TICK_INTERVAL_MS = 250;     // 4 checks per second (lightweight, non-laggy)
  private readonly ALERT_COOLDOWN_MS = 8000;           // Do not spam alerts; 8s cooldown between popups

  // State subjects
  private readonly activeAlertSubject = new BehaviorSubject<IntegrityAlert | null>(null);
  readonly activeAlert$: Observable<IntegrityAlert | null> = this.activeAlertSubject.asObservable();

  private readonly summarySubject = new BehaviorSubject<IntegritySummary>({
    totalEvents: 0,
    multiplePersonEvents: 0,
    tabSwitchEvents: 0,
    attentionAwayEvents: 0,
    audioAnomalyEvents: 0,
    monitoredDurationSeconds: 0,
    integrityStatus: 'NORMAL'
  });
  readonly summary$: Observable<IntegritySummary> = this.summarySubject.asObservable();

  // Internal event store
  private events: ClientIntegrityEvent[] = [];
  private isMonitoringActive: boolean = false;
  private monitoringStartTime: number = 0;

  // Active continuous condition trackers for deduplication
  private multipleFaceTracker: ActiveConditionTracker | null = null;
  private attentionAwayTracker: ActiveConditionTracker | null = null;
  private tabSwitchTracker: ActiveConditionTracker | null = null;
  private audioAnomalyTracker: ActiveConditionTracker | null = null;

  // Intervals and listeners
  private visionIntervalId: any = null;
  private audioIntervalId: any = null;
  private durationIntervalId: any = null;
  private lastAlertTime: number = 0;
  private alertDismissTimer: any = null;

  // Browser listeners
  private visibilityListener: (() => void) | null = null;
  private blurListener: (() => void) | null = null;
  private focusListener: (() => void) | null = null;

  // Video element reference
  private videoElement: HTMLVideoElement | null = null;
  private offscreenCanvas: HTMLCanvasElement | null = null;
  private offscreenCtx: CanvasRenderingContext2D | null = null;

  // Native Shape Detection API if available
  private faceDetector: any = null;

  constructor(
    private readonly ngZone: NgZone,
    private readonly mediaService: InterviewMediaService
  ) {
    this.initNativeFaceDetectorIfAvailable();
  }

  private initNativeFaceDetectorIfAvailable(): void {
    if (typeof window !== 'undefined' && 'FaceDetector' in window) {
      try {
        const FaceDetectorClass = (window as any).FaceDetector;
        this.faceDetector = new FaceDetectorClass({ fastMode: true, maxDetectedFaces: 5 });
        console.log('[IntegrityService] Native browser FaceDetector API initialized.');
      } catch (e) {
        console.warn('[IntegrityService] FaceDetector API unavailable, using canvas heuristics.', e);
      }
    }
  }

  /**
   * Start integrity monitoring on the given video stream
   */
  startMonitoring(videoEl?: HTMLVideoElement | null): void {
    if (this.isMonitoringActive) {
      return;
    }
    this.isMonitoringActive = true;
    this.monitoringStartTime = Date.now();
    this.videoElement = videoEl || null;

    if (typeof document !== 'undefined') {
      this.offscreenCanvas = document.createElement('canvas');
      this.offscreenCanvas.width = 160; // Lightweight resolution for fast heuristic scanning
      this.offscreenCanvas.height = 120;
      this.offscreenCtx = this.offscreenCanvas.getContext('2d', { willReadFrequently: true });
    }

    console.log('[IntegrityService] Monitoring started.');

    // 1. Setup Tab / Window listeners
    this.setupBrowserEventListeners();

    // 2. Setup Vision Interval (4 checks per sec)
    this.ngZone.runOutsideAngular(() => {
      this.visionIntervalId = setInterval(() => {
        this.analyzeVisionFrame();
      }, this.VISION_TICK_INTERVAL_MS);

      // 3. Setup Audio Anomaly Interval (every 1s)
      this.audioIntervalId = setInterval(() => {
        this.analyzeAudioSignal();
      }, 1000);

      // 4. Update monitored duration
      this.durationIntervalId = setInterval(() => {
        this.updateMonitoredDuration();
      }, 1000);
    });
  }

  /**
   * Stop all integrity monitoring and finalize duration
   */
  stopMonitoring(): void {
    if (!this.isMonitoringActive) {
      return;
    }
    this.isMonitoringActive = false;

    // Resolve any pending active continuous conditions
    const now = Date.now();
    if (this.multipleFaceTracker) {
      this.closeActiveCondition(this.multipleFaceTracker, now);
      this.multipleFaceTracker = null;
    }
    if (this.attentionAwayTracker) {
      this.closeActiveCondition(this.attentionAwayTracker, now);
      this.attentionAwayTracker = null;
    }
    if (this.tabSwitchTracker) {
      this.closeActiveCondition(this.tabSwitchTracker, now);
      this.tabSwitchTracker = null;
    }
    if (this.audioAnomalyTracker) {
      this.closeActiveCondition(this.audioAnomalyTracker, now);
      this.audioAnomalyTracker = null;
    }

    // Clean up timers
    if (this.visionIntervalId) clearInterval(this.visionIntervalId);
    if (this.audioIntervalId) clearInterval(this.audioIntervalId);
    if (this.durationIntervalId) clearInterval(this.durationIntervalId);
    if (this.alertDismissTimer) clearTimeout(this.alertDismissTimer);

    // Clean up listeners
    this.removeBrowserEventListeners();

    console.log('[IntegrityService] Monitoring stopped. Total events recorded:', this.events.length);
    this.emitSummary();
  }

  /**
   * Return recorded integrity events for submission to backend
   */
  getRecordedEvents(): ClientIntegrityEvent[] {
    return [...this.events];
  }

  /**
   * Reset monitoring state
   */
  reset(): void {
    this.stopMonitoring();
    this.events = [];
    this.activeAlertSubject.next(null);
    this.emitSummary();
  }

  // =========================================================================
  // 1. BROWSER TAB / FOCUS LISTENERS
  // =========================================================================
  private setupBrowserEventListeners(): void {
    if (typeof window === 'undefined' || typeof document === 'undefined') return;

    this.visibilityListener = () => {
      if (document.hidden) {
        this.handleTabSwitchStart('TAB_SWITCH', 'Candidate navigated away from the interview tab.');
      } else {
        this.handleTabSwitchEnd();
      }
    };

    this.blurListener = () => {
      // Window lost focus (candidate might have clicked another app/window)
      if (!this.tabSwitchTracker) {
        this.handleTabSwitchStart('WINDOW_BLUR', 'Interview window lost focus.');
      }
    };

    this.focusListener = () => {
      this.handleTabSwitchEnd();
    };

    document.addEventListener('visibilitychange', this.visibilityListener);
    window.addEventListener('blur', this.blurListener);
    window.addEventListener('focus', this.focusListener);
  }

  private removeBrowserEventListeners(): void {
    if (typeof window === 'undefined' || typeof document === 'undefined') return;
    if (this.visibilityListener) {
      document.removeEventListener('visibilitychange', this.visibilityListener);
      this.visibilityListener = null;
    }
    if (this.blurListener) {
      window.removeEventListener('blur', this.blurListener);
      this.blurListener = null;
    }
    if (this.focusListener) {
      window.removeEventListener('focus', this.focusListener);
      this.focusListener = null;
    }
  }

  private handleTabSwitchStart(type: IntegrityEventType, message: string): void {
    if (!this.isMonitoringActive) return;
    if (this.tabSwitchTracker) return; // Deduplication: Already tracking open tab switch

    const now = Date.now();
    this.tabSwitchTracker = {
      conditionType: type,
      startTime: now,
      consecutiveFrames: 1,
      hasTriggeredEvent: true
    };

    // Calculate count of previous tab switches to determine severity
    const prevSwitches = this.events.filter(e => e.eventType === 'TAB_SWITCH' || e.eventType === 'WINDOW_BLUR').length;
    const severity: IntegritySeverity = prevSwitches >= 2 ? 'HIGH' : prevSwitches >= 1 ? 'MEDIUM' : 'LOW';

    this.triggerAlert({
      title: 'Interview Screen Notice',
      message: 'Switching away from the interview screen has been recorded. Please remain on this page.',
      severity,
      timestamp: now
    });
  }

  private handleTabSwitchEnd(): void {
    if (!this.tabSwitchTracker) return;
    const now = Date.now();
    const duration = Math.max(1, (now - this.tabSwitchTracker.startTime) / 1000);

    const prevSwitches = this.events.filter(e => e.eventType === 'TAB_SWITCH' || e.eventType === 'WINDOW_BLUR').length;
    const severity: IntegritySeverity = prevSwitches >= 2 ? 'HIGH' : prevSwitches >= 1 ? 'MEDIUM' : 'LOW';

    this.events.push({
      eventType: this.tabSwitchTracker.conditionType,
      severity,
      startTime: this.tabSwitchTracker.startTime,
      endTime: now,
      durationSeconds: Math.round(duration * 10) / 10,
      message: `Candidate left interview view for ${Math.round(duration)} seconds.`
    });

    this.tabSwitchTracker = null;
    this.emitSummary();
  }

  // =========================================================================
  // 2. WEBCAM VISION ANALYSIS (Faces & Attention)
  // =========================================================================
  private async analyzeVisionFrame(): Promise<void> {
    if (!this.videoElement || !this.isMonitoringActive) return;
    if (this.videoElement.readyState < 2) return; // Not ready yet

    const now = Date.now();

    try {
      if (this.faceDetector) {
        // Native FaceDetector API (e.g. Chrome/Chromium with shape detection)
        const faces = await this.faceDetector.detect(this.videoElement);
        this.processFaceDetectionResults(faces.length, faces, now);
      } else {
        // High-performance canvas heuristic fallback
        this.processCanvasVisionHeuristics(now);
      }
    } catch (e) {
      // Fallback to canvas heuristics if native detector errors
      this.processCanvasVisionHeuristics(now);
    }
  }

  private processFaceDetectionResults(faceCount: number, faces: any[], now: number): void {
    // 1. Multiple people detection
    if (faceCount >= 2) {
      if (!this.multipleFaceTracker) {
        this.multipleFaceTracker = {
          conditionType: 'MULTIPLE_PERSON',
          startTime: now,
          consecutiveFrames: 1,
          hasTriggeredEvent: false
        };
      } else {
        this.multipleFaceTracker.consecutiveFrames++;
        const sustainedDuration = now - this.multipleFaceTracker.startTime;
        if (sustainedDuration >= this.MULTIPLE_FACE_THRESHOLD_MS && !this.multipleFaceTracker.hasTriggeredEvent) {
          this.multipleFaceTracker.hasTriggeredEvent = true;
          this.triggerAlert({
            title: 'Multiple People Detected',
            message: 'Multiple people were detected in the camera view. Please make sure you are the only person visible during the interview.',
            severity: 'HIGH',
            timestamp: now
          });
        }
      }
    } else {
      // Resolved
      if (this.multipleFaceTracker) {
        if (this.multipleFaceTracker.hasTriggeredEvent) {
          this.closeActiveCondition(this.multipleFaceTracker, now);
        }
        this.multipleFaceTracker = null;
      }
    }

    // 2. Attention / Eye direction monitoring
    if (faceCount === 1 && faces.length > 0) {
      const face = faces[0].boundingBox;
      const vidWidth = this.videoElement?.videoWidth || 640;
      const vidHeight = this.videoElement?.videoHeight || 480;

      // Check if face center is significantly offset from center (indicating head turned away)
      const faceCenterX = face.x + face.width / 2;
      const faceCenterY = face.y + face.height / 2;
      const normX = faceCenterX / vidWidth;
      const normY = faceCenterY / vidHeight;

      const isTurnedAway = normX < 0.20 || normX > 0.80 || normY < 0.15 || normY > 0.85;

      if (isTurnedAway) {
        this.handleAttentionAwayTick(now);
      } else {
        this.handleAttentionReturned(now);
      }
    } else if (faceCount === 0) {
      // Candidate not in frame
      this.handleAttentionAwayTick(now);
    } else {
      this.handleAttentionReturned(now);
    }
  }

  /**
   * Lightweight Canvas heuristic fallback for browsers without window.FaceDetector
   */
  private processCanvasVisionHeuristics(now: number): void {
    if (!this.offscreenCanvas || !this.offscreenCtx || !this.videoElement) return;

    try {
      this.offscreenCtx.drawImage(this.videoElement, 0, 0, 160, 120);
      const imgData = this.offscreenCtx.getImageData(0, 0, 160, 120);
      const data = imgData.data;

      let skinPixels = 0;
      let leftHalfSkin = 0;
      let rightHalfSkin = 0;
      const totalPixels = 160 * 120;

      for (let i = 0; i < data.length; i += 16) { // Sample every 4th pixel
        const r = data[i];
        const g = data[i + 1];
        const b = data[i + 2];

        // Standard skin-tone chrominance heuristic
        if (r > 60 && g > 40 && b > 20 && r > g && r > b && (r - Math.min(g, b)) > 15) {
          skinPixels++;
          const pxIndex = i / 4;
          const x = pxIndex % 160;
          if (x < 70) leftHalfSkin++;
          if (x > 90) rightHalfSkin++;
        }
      }

      const sampledCount = totalPixels / 4;
      const skinRatio = skinPixels / sampledCount;

      // Heuristic face presence
      if (skinRatio < 0.03) {
        // No face / candidate absent from frame
        this.handleAttentionAwayTick(now);
      } else {
        // Check balance (if looking straight vs head heavily angled out of frame)
        const diff = Math.abs(leftHalfSkin - rightHalfSkin);
        const total = leftHalfSkin + rightHalfSkin + 1;
        const imbalance = diff / total;

        if (imbalance > 0.85 && skinRatio > 0.05) {
          this.handleAttentionAwayTick(now);
        } else {
          this.handleAttentionReturned(now);
        }
      }
    } catch {
      // Ignore canvas access glitches
    }
  }

  private handleAttentionAwayTick(now: number): void {
    if (!this.attentionAwayTracker) {
      this.attentionAwayTracker = {
        conditionType: 'ATTENTION_AWAY',
        startTime: now,
        consecutiveFrames: 1,
        hasTriggeredEvent: false
      };
    } else {
      this.attentionAwayTracker.consecutiveFrames++;
      const sustained = now - this.attentionAwayTracker.startTime;

      // Only trigger if sustained for >= 3.5s to avoid natural blinking and normal thinking movements
      if (sustained >= this.ATTENTION_AWAY_THRESHOLD_MS && !this.attentionAwayTracker.hasTriggeredEvent) {
        this.attentionAwayTracker.hasTriggeredEvent = true;
        const prevAway = this.events.filter(e => e.eventType === 'ATTENTION_AWAY').length;
        const severity: IntegritySeverity = prevAway >= 3 ? 'HIGH' : prevAway >= 1 ? 'MEDIUM' : 'LOW';

        this.triggerAlert({
          title: 'Attention Notice',
          message: 'Please keep your attention on the interview screen.',
          severity,
          timestamp: now
        });
      }
    }
  }

  private handleAttentionReturned(now: number): void {
    if (!this.attentionAwayTracker) return;
    if (this.attentionAwayTracker.hasTriggeredEvent) {
      this.closeActiveCondition(this.attentionAwayTracker, now);
    }
    this.attentionAwayTracker = null;
  }

  // =========================================================================
  // 3. AUDIO MONITORING (Unusual background speech / overlapping audio)
  // =========================================================================
  private analyzeAudioSignal(): void {
    if (!this.isMonitoringActive) return;

    // Use current acoustic level from InterviewMediaService
    const audioLevel = this.mediaService.audioLevel$.getValue();
    const isCandidateSpeaking = this.mediaService.isSpeaking$.getValue();
    const now = Date.now();

    // If candidate is NOT actively speaking according to STT but high acoustic vocal energy continues
    if (!isCandidateSpeaking && audioLevel > 35) {
      if (!this.audioAnomalyTracker) {
        this.audioAnomalyTracker = {
          conditionType: 'AUDIO_ANOMALY',
          startTime: now,
          consecutiveFrames: 1,
          hasTriggeredEvent: false
        };
      } else {
        this.audioAnomalyTracker.consecutiveFrames++;
        const sustained = now - this.audioAnomalyTracker.startTime;
        if (sustained >= 4000 && !this.audioAnomalyTracker.hasTriggeredEvent) {
          this.audioAnomalyTracker.hasTriggeredEvent = true;
          this.triggerAlert({
            title: 'Audio Notice',
            message: 'Unusual background speech or audio was detected. Please make sure you are completing the interview without assistance.',
            severity: 'MEDIUM',
            timestamp: now
          });
        }
      }
    } else {
      if (this.audioAnomalyTracker) {
        if (this.audioAnomalyTracker.hasTriggeredEvent) {
          this.closeActiveCondition(this.audioAnomalyTracker, now);
        }
        this.audioAnomalyTracker = null;
      }
    }
  }

  // =========================================================================
  // 4. EVENT DEDUPLICATION, CLOSE & EMIT
  // =========================================================================
  private closeActiveCondition(tracker: ActiveConditionTracker, now: number): void {
    const duration = Math.max(1, (now - tracker.startTime) / 1000);
    const count = this.events.filter(e => e.eventType === tracker.conditionType).length;
    const severity: IntegritySeverity = count >= 2 ? 'HIGH' : count >= 1 ? 'MEDIUM' : 'LOW';

    let msg = '';
    switch (tracker.conditionType) {
      case 'MULTIPLE_PERSON':
        msg = `Multiple people visible in camera frame for ${Math.round(duration)} seconds.`;
        break;
      case 'ATTENTION_AWAY':
        msg = `Prolonged attention diverted from interview screen for ${Math.round(duration)} seconds.`;
        break;
      case 'AUDIO_ANOMALY':
        msg = `Unusual background vocal audio detected for ${Math.round(duration)} seconds.`;
        break;
      default:
        msg = `Integrity event ${tracker.conditionType} lasted ${Math.round(duration)} seconds.`;
    }

    this.events.push({
      eventType: tracker.conditionType,
      severity,
      startTime: tracker.startTime,
      endTime: now,
      durationSeconds: Math.round(duration * 10) / 10,
      message: msg
    });

    this.emitSummary();
  }

  // =========================================================================
  // 5. ALERT ESCALATION & NON-BLOCKING UI
  // =========================================================================
  private triggerAlert(alert: IntegrityAlert): void {
    const now = Date.now();
    // Prevent alert spamming: respect cooldown
    if (now - this.lastAlertTime < this.ALERT_COOLDOWN_MS) {
      return;
    }
    this.lastAlertTime = now;

    this.ngZone.run(() => {
      this.activeAlertSubject.next(alert);

      if (this.alertDismissTimer) {
        clearTimeout(this.alertDismissTimer);
      }
      // Auto-dismiss alert banner after 6 seconds
      this.alertDismissTimer = setTimeout(() => {
        this.dismissAlert();
      }, 6000);
    });
  }

  dismissAlert(): void {
    this.ngZone.run(() => {
      this.activeAlertSubject.next(null);
    });
  }

  private updateMonitoredDuration(): void {
    if (!this.isMonitoringActive) return;
    this.emitSummary();
  }

  private emitSummary(): void {
    const totalEvents = this.events.length;
    const multiplePerson = this.events.filter(e => e.eventType === 'MULTIPLE_PERSON').length;
    const tabSwitches = this.events.filter(e => e.eventType === 'TAB_SWITCH' || e.eventType === 'WINDOW_BLUR').length;
    const attentionAway = this.events.filter(e => e.eventType === 'ATTENTION_AWAY').length;
    const audioAnomalies = this.events.filter(e => e.eventType === 'AUDIO_ANOMALY').length;

    const monitoredDuration = this.monitoringStartTime > 0
      ? Math.round((Date.now() - this.monitoringStartTime) / 1000)
      : 0;

    const requiresReview = multiplePerson > 0 || tabSwitches >= 3 || attentionAway >= 4 || totalEvents >= 5;

    this.summarySubject.next({
      totalEvents,
      multiplePersonEvents: multiplePerson,
      tabSwitchEvents: tabSwitches,
      attentionAwayEvents: attentionAway,
      audioAnomalyEvents: audioAnomalies,
      monitoredDurationSeconds: monitoredDuration,
      integrityStatus: requiresReview ? 'REVIEW_REQUIRED' : 'NORMAL'
    });
  }
}
