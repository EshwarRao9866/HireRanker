import { Component, OnInit, OnDestroy, ChangeDetectorRef, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { Subscription, interval } from 'rxjs';
import { MessageService, ConversationSummary, MessageResponse, CandidateAppliedJob } from '../services/message.service';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-candidate-messages',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './candidate-messages.html',
  styleUrl: './candidate-messages.css'
})
export class CandidateMessages implements OnInit, OnDestroy {
  @ViewChild('messagesContainer') private messagesContainer?: ElementRef;

  candidateName = '';
  candidateEmail = '';
  candidateAvatar = '';

  searchQuery = '';
  newMessageText = '';
  isLoading = true;
  isSending = false;
  errorFeedback: string | null = null;
  unreadTotal = 0;

  conversations: ConversationSummary[] = [];
  selectedConversation: ConversationSummary | null = null;
  activeMessages: MessageResponse[] = [];

  // New Inquiry Modal with Company/Applied Job Dropdown
  showNewInquiryModal = false;
  appliedJobs: CandidateAppliedJob[] = [];
  selectedAppliedJob: CandidateAppliedJob | null = null;
  newInquiryText = '';
  inquiryJobId: number | null = null;
  inquiryAppId: number | null = null;
  inquiryJobTitle: string = '';
  inquiryCompany: string = '';
  isLoadingAppliedJobs = false;

  private pollingSub?: Subscription;

  constructor(
    private readonly router: Router,
    private readonly route: ActivatedRoute,
    private readonly messageService: MessageService,
    private readonly authService: AuthService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const user = this.authService.currentUser();
    this.candidateName = user?.fullName || 'Candidate';
    this.candidateEmail = user?.email || '';

    // Check query params if navigated via "Contact Recruiter" from applications or job cards
    this.route.queryParams.subscribe((params) => {
      if (params['jobId']) {
        this.inquiryJobId = Number(params['jobId']);
      }
      if (params['applicationId']) {
        this.inquiryAppId = Number(params['applicationId']);
      }
      if (params['jobTitle']) {
        this.inquiryJobTitle = params['jobTitle'];
      }
      if (params['company']) {
        this.inquiryCompany = params['company'];
      }
      if (params['contactRecruiter'] === 'true') {
        this.openNewInquiryModal();
      }
    });

    this.loadConversations(true);

    // Polling every 5 seconds for incoming messages from recruiters
    this.pollingSub = interval(5000).subscribe(() => {
      this.refreshConversationsSilently();
    });
  }

  ngOnDestroy(): void {
    this.pollingSub?.unsubscribe();
  }

  get currentUserId(): number | undefined {
    return this.authService.currentUser()?.id;
  }

  get filteredConversations(): ConversationSummary[] {
    if (!this.searchQuery.trim()) {
      return this.conversations;
    }
    const q = this.searchQuery.toLowerCase();
    return this.conversations.filter(c =>
      c.partnerName?.toLowerCase().includes(q) ||
      c.appliedRole?.toLowerCase().includes(q) ||
      c.company?.toLowerCase().includes(q) ||
      c.lastMessage?.toLowerCase().includes(q)
    );
  }

  loadConversations(initial: boolean = false): void {
    if (initial) {
      this.isLoading = true;
    }
    this.messageService.getConversations().subscribe({
      next: (convos) => {
        this.isLoading = false;
        this.conversations = convos || [];
        this.updateTotalUnread();

        if (this.selectedConversation) {
          const current = this.conversations.find(c => c.partnerId === this.selectedConversation?.partnerId);
          if (current) {
            this.selectedConversation = current;
            this.activeMessages = current.messages || [];
          }
        } else if (this.conversations.length > 0) {
          this.selectConversation(this.conversations[0]);
        }

        this.cdr.detectChanges();
        if (initial) {
          this.scrollToBottom();
        }
      },
      error: (err) => {
        console.error('Candidate messages failed to load:', err);
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  refreshConversationsSilently(): void {
    this.messageService.getConversations().subscribe({
      next: (convos) => {
        this.conversations = convos || [];
        this.updateTotalUnread();

        if (this.selectedConversation) {
          const matched = this.conversations.find(c => c.partnerId === this.selectedConversation?.partnerId);
          if (matched) {
            const previousLen = this.activeMessages.length;
            this.selectedConversation = matched;
            this.activeMessages = matched.messages || [];
            if (matched.messages.length > previousLen) {
              this.scrollToBottom();
            }
          }
        }
        this.cdr.detectChanges();
      },
      error: () => {}
    });
  }

  updateTotalUnread(): void {
    this.unreadTotal = this.conversations.reduce((sum, c) => sum + (c.unreadCount || 0), 0);
  }

  selectConversation(convo: ConversationSummary): void {
    this.selectedConversation = convo;
    this.activeMessages = convo.messages || [];
    this.errorFeedback = null;

    if (convo.unreadCount > 0) {
      convo.unreadCount = 0;
      this.updateTotalUnread();
      this.messageService.markConversationAsRead(convo.partnerId).subscribe({
        next: () => this.cdr.detectChanges(),
        error: (err) => console.warn('Mark read failed:', err)
      });
    }

    this.cdr.detectChanges();
    this.scrollToBottom();
  }

  /**
   * Keyboard handler for chat window textarea
   * Enter: send message
   * Shift + Enter: new line
   */
  onMessageKeydown(event: KeyboardEvent): void {
    if (event.key !== 'Enter' || event.isComposing) {
      return;
    }
    if (event.shiftKey) {
      // Allow textarea to insert a new line
      return;
    }
    // Prevent default newline / form submit
    event.preventDefault();
    this.sendMessage();
  }

  /**
   * Alias for template binding compatibility
   */
  onKeyDown(event: KeyboardEvent): void {
    this.onMessageKeydown(event);
  }

  sendMessage(): void {
    if (!this.selectedConversation || this.isSending) return;
    const text = this.newMessageText.trim();
    if (!text) return;

    this.isSending = true;
    this.errorFeedback = null;
    const partnerId = this.selectedConversation.partnerId;
    const jobId = this.selectedConversation.jobId;
    const applicationId = this.selectedConversation.applicationId;

    this.messageService.sendMessage(partnerId, text, jobId, applicationId).subscribe({
      next: (savedMsg) => {
        this.isSending = false;
        // Clear message text ONLY after backend has successfully accepted the message
        this.newMessageText = '';

        this.activeMessages.push(savedMsg);
        if (this.selectedConversation) {
          this.selectedConversation.lastMessage = savedMsg.message;
          this.selectedConversation.lastMessageTime = savedMsg.sentAt;
        }

        this.cdr.detectChanges();
        this.scrollToBottom();
        this.refreshConversationsSilently();
      },
      error: (err) => {
        this.isSending = false;
        // Preserve typed message in textarea on failure
        console.error('Failed to send reply:', err);
        this.errorFeedback = err?.error?.message || 'Unable to send message. Please verify network connectivity and try again.';
        this.cdr.detectChanges();
      }
    });
  }

  openNewInquiryModal(): void {
    this.showNewInquiryModal = true;
    this.isLoadingAppliedJobs = true;
    this.newInquiryText = '';
    this.errorFeedback = null;

    this.messageService.getCandidateAppliedJobs().subscribe({
      next: (jobs) => {
        this.appliedJobs = jobs || [];
        this.isLoadingAppliedJobs = false;

        if (this.appliedJobs.length > 0) {
          // If pre-filled by query params, match it
          if (this.inquiryJobId) {
            const match = this.appliedJobs.find(j => j.jobId === this.inquiryJobId);
            this.selectedAppliedJob = match || this.appliedJobs[0];
          } else {
            this.selectedAppliedJob = this.appliedJobs[0];
          }
          this.onAppliedJobChange(this.selectedAppliedJob);
        } else {
          this.selectedAppliedJob = null;
        }

        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load candidate applied jobs:', err);
        this.isLoadingAppliedJobs = false;
        this.cdr.detectChanges();
      }
    });
  }

  onAppliedJobChange(job: CandidateAppliedJob | null): void {
    if (!job) {
      this.selectedAppliedJob = null;
      return;
    }
    this.selectedAppliedJob = job;
    this.inquiryJobId = job.jobId;
    this.inquiryAppId = job.applicationId;
    this.inquiryJobTitle = job.jobTitle;
    this.inquiryCompany = job.company;
    this.cdr.detectChanges();
  }

  closeNewInquiryModal(): void {
    this.showNewInquiryModal = false;
    this.selectedAppliedJob = null;
    this.newInquiryText = '';
  }

  /**
   * Keyboard handler for modal inquiry textarea
   * Enter: send inquiry
   * Shift + Enter: new line
   */
  onInquiryKeydown(event: KeyboardEvent): void {
    if (event.key !== 'Enter') {
      return;
    }
    if (event.shiftKey) {
      return;
    }
    event.preventDefault();
    this.sendNewInquiry();
  }

  sendNewInquiry(): void {
    if (!this.selectedAppliedJob || !this.selectedAppliedJob.hasRecruiter || !this.selectedAppliedJob.recruiterId || this.isSending) {
      return;
    }
    const text = this.newInquiryText.trim();
    if (!text) return;

    this.isSending = true;
    const job = this.selectedAppliedJob;
    const recipientId = job.recruiterId;
    if (!recipientId) {
      this.isSending = false;
      return;
    }

    this.messageService.sendMessage(
      recipientId,
      text,
      job.jobId,
      job.applicationId
    ).subscribe({
      next: () => {
        this.isSending = false;
        // Clears typed inquiry only on success
        this.newInquiryText = '';
        this.closeNewInquiryModal();

        this.messageService.getConversations().subscribe({
          next: (convos) => {
            this.conversations = convos || [];
            const target = this.conversations.find(c => c.partnerId === recipientId);
            if (target) {
              this.selectConversation(target);
            } else if (this.conversations.length > 0) {
              this.selectConversation(this.conversations[0]);
            }
            this.cdr.detectChanges();
          }
        });
      },
      error: (err) => {
        this.isSending = false;
        // Preserves typed message on error
        console.error('Inquiry send error:', err);
        this.errorFeedback = err?.error?.message || 'Failed to submit inquiry. Please try again.';
        this.cdr.detectChanges();
      }
    });
  }

  formatTime(isoString?: string): string {
    if (!isoString) return '';
    try {
      const date = new Date(isoString);
      const now = new Date();
      const isToday = date.toDateString() === now.toDateString();
      if (isToday) {
        return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
      }
      return date.toLocaleDateString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
    } catch {
      return isoString;
    }
  }

  getInitials(name?: string): string {
    if (!name) return 'R';
    const parts = name.trim().split(' ');
    if (parts.length >= 2) {
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return name.slice(0, 2).toUpperCase();
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      if (this.messagesContainer) {
        this.messagesContainer.nativeElement.scrollTop = this.messagesContainer.nativeElement.scrollHeight;
      }
    }, 50);
  }

  // Navigation
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

  openInterviews(): void {
    this.router.navigate(['/interviews']);
  }

  openProfile(): void {
    this.router.navigate(['/my-profile']);
  }

  logout(): void {
    this.authService.logout('/candidate-login');
  }
}
