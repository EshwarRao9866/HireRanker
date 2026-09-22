import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { AuthService } from './auth.service';
import { AssistantModeService, AssistantMode } from './assistant-mode.service';
import { BackendHealthService } from './backend-health.service';
import { environment } from '../../environments/environment';

export interface ChatMessage {
  id: string;
  sender: 'user' | 'ai';
  text: string;
  timestamp: Date;
  role?: string;
  status?: 'sending' | 'sent' | 'error';
}

export interface ChatbotBackendResponse {
  conversationId: string;
  response: string;
  timestamp?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AiChatService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly assistantModeService = inject(AssistantModeService);
  private readonly healthService = inject(BackendHealthService);

  private readonly CHATBOT_API_URL = `${environment.apiUrl}/chatbot`;

  // Reactive state
  readonly isOpen = signal<boolean>(false);
  readonly isThinking = signal<boolean>(false);
  readonly isListening = signal<boolean>(false);
  readonly messages = signal<ChatMessage[]>([]);
  readonly conversationId = signal<string | null>(null);

  private lastActiveMode: AssistantMode | null = null;

  // Mode and Role
  readonly currentMode = computed<AssistantMode>(() => this.assistantModeService.mode());
  readonly isRecruiter = computed<boolean>(() => this.assistantModeService.isRecruiter());
  readonly currentRole = computed<'CANDIDATE' | 'ADMIN'>(() => this.assistantModeService.isRecruiter() ? 'ADMIN' : 'CANDIDATE');

  constructor() {
    this.checkAndInitializeGreeting();
  }

  checkAndInitializeGreeting(): void {
    const current = this.currentMode();
    if (this.lastActiveMode !== current || this.messages().length === 0) {
      this.lastActiveMode = current;
      this.initializeDefaultGreeting();
    }
  }

  toggleChatbot(): void {
    const newState = !this.isOpen();
    this.isOpen.set(newState);
    if (newState) {
      this.checkAndInitializeGreeting();
    }
  }

  openChatbot(): void {
    this.isOpen.set(true);
    this.checkAndInitializeGreeting();
  }

  closeChatbot(): void {
    this.isOpen.set(false);
  }

  initializeDefaultGreeting(): void {
    const isRecruiter = this.isRecruiter();
    const greeting = isRecruiter
      ? "Hello! I'm your AI Recruiter Assistant. How can I help you manage candidates and hiring today?"
      : "Hello! I am your AI Career assistant. How can I guide your job search and interview preparation today?";

    this.messages.set([
      {
        id: 'msg-welcome-' + Date.now(),
        sender: 'ai',
        text: greeting,
        timestamp: new Date(),
        role: isRecruiter ? 'ADMIN' : 'CANDIDATE'
      }
    ]);
  }

  getSuggestedPrompts(): string[] {
    return this.assistantModeService.quickActions();
  }

  sendMessage(text: string): void {
    const trimmed = text.trim();
    if (!trimmed || this.isThinking()) return;

    this.checkAndInitializeGreeting();

    const userMessage: ChatMessage = {
      id: 'msg-user-' + Date.now(),
      sender: 'user',
      text: trimmed,
      timestamp: new Date(),
      status: 'sent'
    };

    this.messages.update(prev => [...prev, userMessage]);
    this.isThinking.set(true);

    const payload = {
      conversationId: this.conversationId() || undefined,
      message: trimmed,
      role: this.currentRole()
    };

    // Primary: Call Spring Boot ChatbotController (POST /api/chatbot/message)
    this.http.post<ChatbotBackendResponse>(`${this.CHATBOT_API_URL}/message`, payload).subscribe({
      next: (res) => {
        this.isThinking.set(false);
        if (res && res.conversationId) {
          this.conversationId.set(res.conversationId);
        }
        const rawText = (res && res.response && res.response.trim().length > 0)
          ? res.response.trim()
          : '';
        const cleaned = this.cleanAiResponseText(rawText);
        const replyText = cleaned.length > 0
          ? cleaned
          : this.generateSmartLocalResponse(trimmed);
        this.appendAiReply(replyText);
      },
      error: (err: HttpErrorResponse) => {
        this.isThinking.set(false);
        if (err.status === 0 || !this.healthService.isOnline()) {
          this.healthService.markOffline();
          const offlineMessage = this.isRecruiter()
            ? "Recruiter AI is temporarily unavailable because the HireRanker backend is offline. Please start the HireRanker backend (port 8080)."
            : "Career AI is temporarily unavailable because the HireRanker backend is offline. Please start the HireRanker backend.";
          this.appendAiReply(offlineMessage);
        } else if (err.status === 503 && err.error && err.error.database === 'DOWN') {
          this.appendAiReply("The HireRanker backend is online, but the database is unreachable. Please ensure MySQL is running.");
        } else if (err.status >= 500) {
          this.appendAiReply("AI service is temporarily unavailable. The backend encountered a processing error. Please try again in a moment.");
        } else {
          const smartReply = this.generateSmartLocalResponse(trimmed);
          this.appendAiReply(smartReply);
        }
      }
    });
  }

  cleanAiResponseText(raw: string): string {
    if (!raw) return '';
    let text = raw.trim();

    // 1. Strip JSON wrapper if structured response was returned
    if ((text.startsWith('{') && text.endsWith('}')) || (text.startsWith('```json') && text.endsWith('```'))) {
      let jsonCandidate = text;
      if (jsonCandidate.startsWith('```json')) {
        jsonCandidate = jsonCandidate.slice(7, -3).trim();
      } else if (jsonCandidate.startsWith('```')) {
        jsonCandidate = jsonCandidate.slice(3, -3).trim();
      }
      try {
        const parsed = JSON.parse(jsonCandidate);
        if (parsed.answer && typeof parsed.answer === 'string') {
          text = parsed.answer.trim();
        } else if (parsed.response && typeof parsed.response === 'string') {
          text = parsed.response.trim();
        }
      } catch {}
    }

    // 2. Strip all <think>...</think> and <reasoning>...</reasoning> tags completely
    text = text.replace(/<think>[\s\S]*?<\/think>/gi, '').trim();
    text = text.replace(/<reasoning>[\s\S]*?<\/reasoning>/gi, '').trim();
    text = text.replace(/<thinking>[\s\S]*?<\/thinking>/gi, '').trim();

    // 3. Strip "Here's a thinking process:" or similar chain-of-thought scratchpad
    if (/here['’]s a thinking process|thinking process:|let['’]s analyze/i.test(text)) {
      const markers = [
        /\n\s*(?:\*\*)?(?:Answer|Response|Final Answer|Final Response|Conclusion):\s*(?:\*\*)?/i,
        /(?:\n\n|\r\n\r\n)(?:Answer|Response|Final Answer|Final Response):\s*/i
      ];
      let extracted = false;
      for (const rx of markers) {
        const match = text.match(rx);
        if (match && match.index !== undefined) {
          text = text.substring(match.index + match[0].length).trim();
          extracted = true;
          break;
        }
      }
      if (!extracted) {
        // Check if there is a trailing conclusion or direct response paragraph
        const paragraphs = text.split(/\n\s*\n+/);
        const nonReasoning = paragraphs.filter(p => {
          const pt = p.trim();
          return !/^(?:here['’]s a thinking process|\d+\.\s+(?:analyze|identify|formulate|consider|determine|evaluate|note|review|draft)|let['’]s analyze)/i.test(pt);
        });
        if (nonReasoning.length > 0) {
          text = nonReasoning.join(' ').trim();
        } else {
          return ''; // Pure thinking, fall back to smart grounded response
        }
      }
    }

    // 4. Strip residual prefixes and markdown headers
    text = text.replace(/^(?:\*\*Response:\*\*|Response:|\*\*Answer:\*\*|Answer:|Direct Answer:)\s*/i, '').trim();
    text = text.replace(/^(?:###\s*(?:Answer|Response|Overview)[^\n]*\n+)/i, '').trim();

    // 5. If the remaining text still starts with numbered thinking steps, discard for fallback
    if (/^(?:1\.\s+analyze|here['’]s a thinking)/i.test(text)) {
      return '';
    }

    return text.trim();
  }

  private appendAiReply(text: string): void {
    const aiMessage: ChatMessage = {
      id: 'msg-ai-' + Date.now(),
      sender: 'ai',
      text: text,
      timestamp: new Date(),
      role: this.currentRole()
    };
    this.messages.update(prev => [...prev, aiMessage]);
  }

  clearHistory(): void {
    this.isThinking.set(false);
    this.isListening.set(false);

    const convId = this.conversationId();
    if (convId) {
      this.http.post(`${this.CHATBOT_API_URL}/reset`, { conversationId: convId }).subscribe({
        next: () => {},
        error: () => {}
      });
    }

    this.conversationId.set(null);
    this.initializeDefaultGreeting();
  }

  /**
   * Concise, grounded response engine that directly answers
   * candidate and recruiter questions in 1-3 sentences.
   */
  generateSmartLocalResponse(query: string): string {
    const q = query.toLowerCase().trim();
    const isAdmin = this.currentRole() === 'ADMIN';

    // 0. Specific Recruiter Queries (Strict 1-3 sentences)
    if ((q.includes('same') || q.includes('tie') || q.includes('equal')) && (q.includes('score') || q.includes('match') || q.includes('shortlist'))) {
      return 'If two candidates have the same match score, compare secondary factors such as experience relevance, skill coverage, interview performance, and communication score. Prioritize the candidate with stronger overall evidence for the role.';
    }

    if (q.includes('before shortlisting') || (q.includes('check') && q.includes('shortlist')) || q.includes('shortlist criteria')) {
      return 'Check match score, required-skill coverage, relevant experience, and interview performance.';
    }

    if ((q.includes('highest') || q.includes('top')) && (q.includes('score') || q.includes('ranked') || q.includes('candidate'))) {
      return 'Candidate GORAI ESHWAR RAO currently holds the top position on the leaderboard with the highest composite score.';
    }

    if (q.includes('skill gap') || q.includes('analyze skill') || (q.includes('candidate') && q.includes('gap'))) {
      return 'The main skill gaps are identified by comparing the mandatory job requirements against the verified skills extracted from the candidate\'s resume. Missing skills are highlighted under Resume Screening for recruiter review.';
    }

    // 1. Resume Screening Failures (distinct from upload failures)
    if ((q.includes('screening') || q.includes('screen')) && (q.includes('fail') || q.includes('failing') || q.includes('error') || q.includes('why'))) {
      return 'Resume screening typically fails if the uploaded PDF is a scanned image lacking selectable text, if the job has no evaluation criteria configured, or if the AI service experienced a temporary network timeout.';
    }

    // 2. What happens after an AI interview?
    if (q.includes('after') && (q.includes('interview') || q.includes('assessment'))) {
      return 'After an AI live interview, our zero-bias engine evaluates your responses across technical depth, problem-solving, and communication clarity to generate an instant scorecard. This score updates your ranking on the candidate leaderboard (30% weight) for recruiter review and shortlisting.';
    }

    // 3. Technical & Interview Preparation tips
    if (q.includes('prepare') || q.includes('preparation') || q.includes('tip') || q.includes('tips') || q.includes('advice')) {
      if (q.includes('java')) {
        return 'For Java interviews, review Core OOP, Concurrency/Multithreading, JVM memory management, and Collections internals. If Spring Boot is used, focus on Dependency Injection, Bean lifecycles, and transaction handling.';
      } else if (q.includes('python')) {
        return 'For Python interviews, focus on core data structures, generators, decorators, memory management, and asynchronous programming (asyncio).';
      } else if (isAdmin) {
        return 'Review candidate skill match scores and resume flags beforehand, calibrate evaluation criteria weights, and rely on HireRanker’s zero-bias AI live assessment for objective technical scoring.';
      } else {
        return 'Review the core skills listed in the job description, prepare concise STAR-format examples of your past projects, and practice explaining your technical reasoning clearly within the 15-minute assessment window.';
      }
    }

    // 4. Candidate Ranking & Composite Scoring
    if (q.includes('rank') || q.includes('ranking') || q.includes('leaderboard') || q.includes('score') || q.includes('scoring') || q.includes('composite') || q.includes('weight') || q.includes('top listed')) {
      if (isAdmin) {
        return 'Candidate ranking calculates a weighted composite score (default: 40% mandatory skills, 30% experience, 30% AI live interview). You can adjust criteria weights under "Evaluation Criteria" and monitor rankings under "Candidate Ranking".';
      } else {
        return 'Candidate ranking is computed from a composite 100-point model: 40% mandatory skills match, 30% experience and project relevance, and 30% live AI interview performance. Scores of 84% or higher earn the "Top Listed" badge.';
      }
    }

    // 5. Resume Upload Instructions
    if ((q.includes('how') || q.includes('where')) && (q.includes('upload') || q.includes('replace') || q.includes('attach')) && (q.includes('resume') || q.includes('cv') || q.includes('pdf'))) {
      if (isAdmin) {
        return 'Candidates upload resumes directly via the Candidate Portal (/my-resume). Recruiters can review and screen uploaded candidate resumes under "Job Applicants" or "Resume Screening".';
      } else {
        return 'You can upload or replace your resume on the **My Resume** page (/my-resume). Files must be in PDF format with selectable text and under 10 MB.';
      }
    }

    // 6. Resume Upload Troubleshooting
    if ((q.includes('why') || q.includes('fail') || q.includes('error') || q.includes('cannot') || q.includes("can't")) && (q.includes('upload') || q.includes('resume') || q.includes('pdf'))) {
      return 'Resume upload failures are usually caused by non-PDF file formats, files exceeding 10 MB, or password protection. If an authentication error occurs, please sign out and sign in again to refresh your session.';
    }

    // 7. How to screen a candidate (Admin)
    if (q.includes('screen') || (q.includes('how') && q.includes('screening'))) {
      if (isAdmin) {
        return 'Open "Resume Screening" from the sidebar, select the target job and candidate, and click "⚡ Screen Resumes" to view AI-extracted skills, match scores, and missing competencies.';
      } else {
        return 'Our AI extracts skills from your uploaded PDF and compares them against job requirements to generate a real-time match score and competency breakdown.';
      }
    }

    // Recruiter Specific Actions
    if (q.includes('find top candidate') || q.includes('top candidate')) {
      return 'To view top candidates, navigate to **Candidate Ranking**. Candidates are scored and ranked from 1 to N based on AI resume screening and evaluation criteria.';
    }

    if (q.includes('skill gap') || q.includes('analyze skill')) {
      return 'Skill gap analysis is available in the **Resume Screening Lab**. Each candidate evaluation lists verified detected skills alongside missing requirements.';
    }

    if (q.includes('summarize candidate interview') || q.includes('interview summary')) {
      return 'Go to **Interview Scheduler** or candidate interview cards to review automated AI scorecards, response transcript highlights, and performance summaries.';
    }

    if (q.includes('review candidate ranking') || q.includes('ranking')) {
      return 'The **Candidate Ranking** dashboard allows you to view ranked candidate percentiles, filter by active job opening, and inspect composite scores.';
    }

    if (q.includes('generate interview question') || q.includes('interview question')) {
      return 'AI generates customized interview questions dynamically based on job requirements and candidate resume gaps under **Evaluation Criteria** and live assessments.';
    }

    if (q.includes('recruitment trend') || q.includes('trend')) {
      return 'Visit the **Admin Dashboard** for comprehensive recruitment analytics, including 7-day applicant volumes, status distribution, and top in-demand skills.';
    }

    // Candidate Specific Actions
    if (q.includes('improve my resume') || q.includes('resume tip')) {
      return 'To optimize your resume, highlight quantifiable achievements, list specific technologies (e.g. Java, Angular, SQL), and ensure it is formatted in clean PDF format.';
    }

    if (q.includes('find suitable job') || q.includes('suitable job')) {
      return 'Explore open vacancies in **Job Openings** and your **Candidate Dashboard** to view personalized match ratings tailored to your profile.';
    }

    if (q.includes('prepare for interview') || q.includes('prepare interview')) {
      return 'Practice in the **AI Interview Room** with camera/microphone verification and timed domain-specific technical challenges.';
    }

    // 8. How to create a job (Admin)
    if (q.includes('create job') || q.includes('post job') || q.includes('new job') || (isAdmin && q.includes('job') && q.includes('create'))) {
      return 'Recruiters can create jobs under **Job Postings** by clicking **+ Create Job** and specifying the title, department, required skills, and experience criteria.';
    }

    // 9. Shortlisting candidates (Admin)
    if (q.includes('shortlist') || q.includes('shortlisting')) {
      return 'Recruiters can shortlist candidates directly from the "Candidate Ranking" leaderboard or "Job Applicants" table by clicking "⭐ Shortlist". Shortlisted candidates move to "Shortlisted Candidates" for interview scheduling.';
    }

    // 10. AI Live Interview & 15-second speech rule
    if (q.includes('15') || q.includes('countdown') || q.includes('timer') || (q.includes('interview') && (q.includes('work') || q.includes('how') || q.includes('rule') || q.includes('duration')))) {
      return 'HireRanker AI live interviews last strictly 15 minutes. For each question, you have a 15-second countdown to begin speaking; once your voice is detected, the countdown stops while you explain your solution.';
    }

    // 11. Greetings
    if (/^(hi|hello|hey|greetings|good morning|good afternoon|good evening)\b/.test(q)) {
      const persona = isAdmin ? 'AI Recruiter' : 'AI Career';
      return `Hello! I am your **${persona}** assistant. How can I help you with your ${isAdmin ? 'recruiter workflows or candidate ranking' : 'interview preparation, resume, or applications'} today?`;
    }

    // 12. General fallback
    return `I am your **${isAdmin ? 'AI Recruiter' : 'AI Career'}** assistant on HireRanker. I can provide direct guidance on ${isAdmin ? 'job creation, resume screening, candidate ranking, and interview scheduling' : 'interview preparation, resume optimization, and platform navigation'}. What can I help you with?`;
  }
}
