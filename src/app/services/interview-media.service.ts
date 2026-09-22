import { Injectable, signal, NgZone } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export type MicState =
  | 'MIC_PERMISSION_PENDING'
  | 'MIC_PERMISSION_GRANTED'
  | 'MIC_INPUT_WAITING'
  | 'MIC_INPUT_DETECTED'
  | 'MIC_STT_STARTING'
  | 'MIC_STT_ACTIVE'
  | 'MIC_SPEAKING'
  | 'MIC_SILENT'
  | 'MIC_ERROR'
  | 'MIC_STOPPED';

export type CameraState =
  | 'CAMERA_IDLE'
  | 'CAMERA_CHECKING'
  | 'CAMERA_VERIFIED'
  | 'CAMERA_FAILED';

@Injectable({
  providedIn: 'root'
})
export class InterviewMediaService {
  // Configurable thresholds
  readonly MIC_AUDIO_THRESHOLD = 4; // Acoustic level % (0-100) calibrated for quiet-to-normal speech
  readonly ANSWER_SILENCE_TIMEOUT_MS = 4000; // 4.0-second silence duration before auto-submitting answer to allow natural pauses

  // Centralized media state
  private activeStream: MediaStream | null = null;
  private audioContext: AudioContext | null = null;
  private analyser: AnalyserNode | null = null;
  private audioAnimFrameId: number | null = null;

  // MediaRecorder for Groq Whisper Cloud STT
  private mediaRecorder: MediaRecorder | null = null;
  private recordedChunks: Blob[] = [];

  // Reactive state
  readonly micState$ = new BehaviorSubject<MicState>('MIC_PERMISSION_PENDING');
  readonly cameraState$ = new BehaviorSubject<CameraState>('CAMERA_IDLE');
  readonly audioLevel$ = new BehaviorSubject<number>(0);
  readonly isSpeaking$ = new BehaviorSubject<boolean>(false);

  // Stream preservation flag across route transitions
  private isPreservingMedia: boolean = false;

  // Silence callback
  private silenceTimer: any = null;
  private hasSpokenInCurrentTurn: boolean = false;
  private onSilenceCallback: (() => void) | null = null;

  constructor(private readonly ngZone: NgZone) {}

  setPreserveMedia(preserve: boolean): void {
    this.isPreservingMedia = preserve;
    console.log(`[InterviewMic] Media preservation set to: ${preserve}`);
  }

  isPreserving(): boolean {
    return this.isPreservingMedia;
  }

  setActiveStream(stream: MediaStream): void {
    this.activeStream = stream;
    if (this.isMicrophoneLive()) {
      this.micState$.next('MIC_PERMISSION_GRANTED');
      this.initAudioAnalyser();
    }
    if (this.isCameraLive()) {
      this.cameraState$.next('CAMERA_VERIFIED');
    }
    this.logMediaState('setActiveStream called');
  }

  getCombinedStream(): MediaStream | null {
    if (this.activeStream && this.isStreamAlive(this.activeStream)) {
      return this.activeStream;
    }
    return null;
  }

  getMicrophoneTrack(): MediaStreamTrack | null {
    if (!this.activeStream) return null;
    const tracks = this.activeStream.getAudioTracks();
    return tracks.length > 0 && tracks[0].readyState === 'live' ? tracks[0] : null;
  }

  getCameraTrack(): MediaStreamTrack | null {
    if (!this.activeStream) return null;
    const tracks = this.activeStream.getVideoTracks();
    return tracks.length > 0 && tracks[0].readyState === 'live' ? tracks[0] : null;
  }

  isMicrophoneLive(): boolean {
    const track = this.getMicrophoneTrack();
    return track !== null && track.readyState === 'live' && track.enabled;
  }

  isCameraLive(): boolean {
    const track = this.getCameraTrack();
    return track !== null && track.readyState === 'live' && track.enabled;
  }

  /**
   * Layer 1: Request Camera & Microphone independently or together.
   * Camera and Microphone permissions & errors are strictly segregated.
   */
  async requestCameraAndMicrophone(): Promise<{ cameraOk: boolean; micOk: boolean; stream: MediaStream | null; error?: string }> {
    let cameraOk = false;
    let micOk = false;
    let errorMsg = '';

    // If existing stream is fully live, reuse it
    if (this.activeStream && this.isMicrophoneLive() && this.isCameraLive()) {
      this.logMediaState('Reusing live existing camera and mic session');
      this.initAudioAnalyser();
      return { cameraOk: true, micOk: true, stream: this.activeStream };
    }

    // 1. Request microphone independently with crystal-clear constraints
    try {
      this.micState$.next('MIC_PERMISSION_PENDING');
      const micStream = await navigator.mediaDevices.getUserMedia({
        audio: {
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true
        },
        video: false
      });

      const audioTrack = micStream.getAudioTracks()[0];
      if (audioTrack && audioTrack.readyState === 'live') {
        micOk = true;
        this.micState$.next('MIC_PERMISSION_GRANTED');
        this.addTrackToSession(audioTrack);
        this.initAudioAnalyser();
      }
    } catch (err: any) {
      console.warn('[InterviewMic] Microphone access error:', err?.name, err?.message);
      this.micState$.next('MIC_ERROR');
      errorMsg = this.formatMediaError('microphone', err);
    }

    // 2. Request camera independently
    try {
      this.cameraState$.next('CAMERA_CHECKING');
      const camStream = await navigator.mediaDevices.getUserMedia({
        video: {
          width: { ideal: 640 },
          height: { ideal: 480 },
          facingMode: 'user'
        },
        audio: false
      });

      const videoTrack = camStream.getVideoTracks()[0];
      if (videoTrack && videoTrack.readyState === 'live') {
        cameraOk = true;
        this.cameraState$.next('CAMERA_VERIFIED');
        this.addTrackToSession(videoTrack);
      }
    } catch (err: any) {
      console.warn('[InterviewCam] Camera access error:', err?.name, err?.message);
      this.cameraState$.next('CAMERA_FAILED');
      if (!errorMsg) {
        errorMsg = this.formatMediaError('camera', err);
      }
    }

    this.logMediaState('Completed requestCameraAndMicrophone');
    return {
      cameraOk,
      micOk,
      stream: this.activeStream,
      error: errorMsg
    };
  }

  /**
   * Dedicated Camera Request (Decoupled from Microphone)
   */
  async requestCameraOnly(): Promise<{ cameraOk: boolean; stream: MediaStream | null; error?: string }> {
    try {
      this.cameraState$.next('CAMERA_CHECKING');
      const camStream = await navigator.mediaDevices.getUserMedia({
        video: {
          width: { ideal: 640 },
          height: { ideal: 480 },
          facingMode: 'user'
        },
        audio: false
      });

      const videoTrack = camStream.getVideoTracks()[0];
      if (videoTrack && videoTrack.readyState === 'live') {
        this.cameraState$.next('CAMERA_VERIFIED');
        this.addTrackToSession(videoTrack);
        return { cameraOk: true, stream: this.activeStream };
      }
      return { cameraOk: false, stream: null, error: 'No active video track detected' };
    } catch (err: any) {
      console.warn('[InterviewCam] Camera access error:', err?.name, err?.message);
      this.cameraState$.next('CAMERA_FAILED');
      return { cameraOk: false, stream: null, error: this.formatMediaError('camera', err) };
    }
  }

  /**
   * Dedicated Microphone Request (Decoupled from Camera)
   */
  async requestMicrophoneOnly(): Promise<{ micOk: boolean; stream: MediaStream | null; error?: string }> {
    try {
      this.micState$.next('MIC_PERMISSION_PENDING');
      const micStream = await navigator.mediaDevices.getUserMedia({
        audio: {
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true
        },
        video: false
      });

      const audioTrack = micStream.getAudioTracks()[0];
      if (audioTrack && audioTrack.readyState === 'live') {
        this.micState$.next('MIC_PERMISSION_GRANTED');
        this.addTrackToSession(audioTrack);
        this.initAudioAnalyser();
        return { micOk: true, stream: this.activeStream };
      }
      return { micOk: false, stream: null, error: 'No active audio track detected' };
    } catch (err: any) {
      console.warn('[InterviewMic] Microphone access error:', err?.name, err?.message);
      this.micState$.next('MIC_ERROR');
      return { micOk: false, stream: null, error: this.formatMediaError('microphone', err) };
    }
  }

  private addTrackToSession(track: MediaStreamTrack): void {
    if (!this.activeStream) {
      this.activeStream = new MediaStream();
    }
    // Remove old tracks of same kind if closed
    const oldTracks = track.kind === 'video'
      ? this.activeStream.getVideoTracks()
      : this.activeStream.getAudioTracks();

    oldTracks.forEach(old => {
      if (old !== track) {
        this.activeStream?.removeTrack(old);
        try { old.stop(); } catch {}
      }
    });

    this.activeStream.addTrack(track);
  }

  /**
   * Layer 2: Web Audio API AnalyserNode for true acoustic signal detection.
   */
  initAudioAnalyser(): void {
    if (typeof window === 'undefined') return;
    const micTrack = this.getMicrophoneTrack();
    if (!micTrack) {
      console.warn('[InterviewMic] Cannot init analyser: no live audio track');
      return;
    }

    try {
      if (!this.audioContext || this.audioContext.state === 'closed') {
        const AudioCtx = window.AudioContext || (window as any).webkitAudioContext;
        this.audioContext = new AudioCtx();
      }

      if (this.audioContext.state === 'suspended') {
        this.audioContext.resume().catch(() => {});
      }

      const streamOnlyAudio = new MediaStream([micTrack]);
      const source = this.audioContext.createMediaStreamSource(streamOnlyAudio);
      this.analyser = this.audioContext.createAnalyser();
      this.analyser.fftSize = 256;
      this.analyser.smoothingTimeConstant = 0.4;
      source.connect(this.analyser);

      const bufferLength = this.analyser.frequencyBinCount;
      const dataArray = new Uint8Array(bufferLength);

      const checkLevel = () => {
        if (!this.analyser || !this.isMicrophoneLive()) {
          this.audioLevel$.next(0);
          this.isSpeaking$.next(false);
          return;
        }

        this.analyser.getByteFrequencyData(dataArray);
        let sum = 0;
        for (let i = 0; i < bufferLength; i++) {
          sum += dataArray[i];
        }
        const avg = sum / bufferLength;
        const pct = Math.min(100, Math.round((avg / 128) * 100));

        this.ngZone.run(() => {
          this.audioLevel$.next(pct);

          if (pct >= this.MIC_AUDIO_THRESHOLD) {
            // Real acoustic signal detected!
            this.micState$.next('MIC_SPEAKING');
            this.isSpeaking$.next(true);
            this.hasSpokenInCurrentTurn = true;

            // Clear silence timer while user is speaking
            if (this.silenceTimer) {
              clearTimeout(this.silenceTimer);
              this.silenceTimer = null;
            }
          } else {
            // Below threshold -> silence
            if (this.isSpeaking$.value) {
              this.isSpeaking$.next(false);
              this.micState$.next('MIC_SILENT');

              // If candidate was speaking previously, start silence countdown
              if (this.hasSpokenInCurrentTurn && !this.silenceTimer && this.onSilenceCallback) {
                this.silenceTimer = setTimeout(() => {
                  console.log(`[InterviewMic] Silence threshold of ${this.ANSWER_SILENCE_TIMEOUT_MS}ms reached.`);
                  if (this.onSilenceCallback) {
                    this.onSilenceCallback();
                  }
                  this.silenceTimer = null;
                }, this.ANSWER_SILENCE_TIMEOUT_MS);
              }
            }
          }
        });

        this.audioAnimFrameId = requestAnimationFrame(checkLevel);
      };

      if (this.audioAnimFrameId) {
        cancelAnimationFrame(this.audioAnimFrameId);
      }
      this.audioAnimFrameId = requestAnimationFrame(checkLevel);
      console.log('[InterviewMic] Web Audio API analyser initialized successfully.');

    } catch (err) {
      console.warn('[InterviewMic] Web Audio analyser initialization note:', err);
    }
  }

  setSilenceCallback(callback: (() => void) | null): void {
    this.onSilenceCallback = callback;
  }

  resetTurnSpeech(): void {
    this.hasSpokenInCurrentTurn = false;
    if (this.silenceTimer) {
      clearTimeout(this.silenceTimer);
      this.silenceTimer = null;
    }
  }

  /**
   * Layer 3: Audio chunk recording for dedicated Cloud STT (Groq Whisper).
   */
  startAnswerRecording(): boolean {
    const micTrack = this.getMicrophoneTrack();
    if (!micTrack) {
      console.warn('[InterviewSTT] Cannot record answer audio: mic track is not live');
      return false;
    }

    try {
      this.stopAnswerRecording();
      this.recordedChunks = [];

      const stream = new MediaStream([micTrack]);
      const mimeTypes = [
        'audio/webm;codecs=opus',
        'audio/webm',
        'audio/ogg;codecs=opus',
        'audio/mp4'
      ];

      let selectedMime = '';
      for (const m of mimeTypes) {
        if (MediaRecorder.isTypeSupported(m)) {
          selectedMime = m;
          break;
        }
      }

      const options: MediaRecorderOptions = selectedMime ? { mimeType: selectedMime } : {};
      this.mediaRecorder = new MediaRecorder(stream, options);

      this.mediaRecorder.ondataavailable = (event: BlobEvent) => {
        if (event.data && event.data.size > 0) {
          this.recordedChunks.push(event.data);
        }
      };

      // Collect data in small 500ms time slices
      this.mediaRecorder.start(500);
      this.micState$.next('MIC_STT_ACTIVE');
      console.log(`[InterviewSTT] MediaRecorder started with mimeType='${selectedMime || 'default'}'`);
      return true;

    } catch (err) {
      console.warn('[InterviewSTT] MediaRecorder start error:', err);
      return false;
    }
  }

  stopAnswerRecording(): Promise<Blob | null> {
    return new Promise((resolve) => {
      if (!this.mediaRecorder || this.mediaRecorder.state === 'inactive') {
        resolve(this.getCombinedBlob());
        return;
      }

      this.mediaRecorder.onstop = () => {
        const blob = this.getCombinedBlob();
        this.mediaRecorder = null;
        resolve(blob);
      };

      try {
        this.mediaRecorder.stop();
      } catch {
        this.mediaRecorder = null;
        resolve(this.getCombinedBlob());
      }
    });
  }

  private getCombinedBlob(): Blob | null {
    if (this.recordedChunks.length === 0) return null;
    const type = this.recordedChunks[0]?.type || 'audio/webm';
    return new Blob(this.recordedChunks, { type });
  }

  /**
   * Teardown logic: Only stops tracks when interview has truly completed or exited.
   */
  stopAll(force: boolean = false): void {
    if (this.isPreservingMedia && !force) {
      console.log('[InterviewMic] stopAll skipped because media preservation is active.');
      return;
    }

    console.log('[InterviewMic] Stopping all media streams and audio context.');
    if (this.audioAnimFrameId) {
      cancelAnimationFrame(this.audioAnimFrameId);
      this.audioAnimFrameId = null;
    }

    if (this.silenceTimer) {
      clearTimeout(this.silenceTimer);
      this.silenceTimer = null;
    }

    this.stopAnswerRecording();

    if (this.audioContext && this.audioContext.state !== 'closed') {
      try { this.audioContext.close(); } catch {}
      this.audioContext = null;
      this.analyser = null;
    }

    if (this.activeStream) {
      this.activeStream.getTracks().forEach(track => {
        try {
          track.stop();
          console.log(`[InterviewMic] Track ${track.kind} stopped.`);
        } catch {}
      });
      this.activeStream = null;
    }

    this.micState$.next('MIC_STOPPED');
    this.cameraState$.next('CAMERA_IDLE');
    this.audioLevel$.next(0);
    this.isSpeaking$.next(false);
    this.isPreservingMedia = false;
  }

  private isStreamAlive(stream: MediaStream): boolean {
    const tracks = stream.getTracks();
    return tracks.length > 0 && tracks.some(t => t.readyState === 'live');
  }

  logMediaState(context: string): void {
    const stream = this.activeStream;
    const audioTrack = this.getMicrophoneTrack();
    const videoTrack = this.getCameraTrack();

    console.log(`[InterviewMic] [${context}]`);
    console.log(`  Stream exists: ${stream !== null} | isPreserving: ${this.isPreservingMedia}`);
    if (audioTrack) {
      console.log(`  AudioTrack: id=${audioTrack.id} | readyState=${audioTrack.readyState} | enabled=${audioTrack.enabled} | muted=${audioTrack.muted}`);
    } else {
      console.log('  AudioTrack: NONE / ENDED');
    }
    if (videoTrack) {
      console.log(`  VideoTrack: id=${videoTrack.id} | readyState=${videoTrack.readyState} | enabled=${videoTrack.enabled}`);
    } else {
      console.log('  VideoTrack: NONE / ENDED');
    }
  }

  formatMediaError(device: 'microphone' | 'camera', err: any): string {
    const name = err?.name || '';
    if (name === 'NotAllowedError' || name === 'PermissionDeniedError') {
      return `${device === 'microphone' ? 'Microphone' : 'Camera'} permission was denied. Please click the lock icon in your browser address bar to allow ${device} access.`;
    }
    if (name === 'NotFoundError' || name === 'DevicesNotFoundError') {
      return `No ${device} device was detected on your computer. Please connect a working ${device} and retry.`;
    }
    if (name === 'NotReadableError' || name === 'TrackStartError') {
      return `Your ${device} is currently in use by another application (Zoom, Teams, etc.). Please close other programs and try again.`;
    }
    if (name === 'OverconstrainedError') {
      return `The requested ${device} settings are not supported by your hardware.`;
    }
    return `Could not access your ${device}: ${err?.message || 'Unknown device error'}.`;
  }
}
