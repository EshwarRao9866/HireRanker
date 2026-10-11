import { Component, OnInit, OnDestroy, ChangeDetectorRef, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subscription, interval } from 'rxjs';
import { MessageService, ConversationSummary, MessageResponse, Recipient } from '../services/message.service';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-messages',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './messages.html',
  styleUrl: './messages.css'
})
export class Messages implements OnInit, OnDestroy {
  @ViewChild('messagesContainer') private messagesContainer?: ElementRef;

  searchQuery = '';
  newMessageText = '';
  isLoading = true;
  isSending = false;
  errorFeedback: string | null = null;
  successFeedback: string | null = null;

  conversations: ConversationSummary[] = [];
  selectedConversation: ConversationSummary | null = null;
  activeMessages: MessageResponse[] = [];

  // Applicant Selection & Company Filtering
  eligibleRecipients: Recipient[] = [];
  selectedApplicant: Recipient | null = null;
  selectedCompanyFilter: string = 'ALL';
  companyFilterList: string[] = [];

  // New Message Modal
  showNewMessageModal = false;
  recipientSearchQuery = '';
  selectedRecipient: Recipient | null = null;
  newConversationText = '';
  isLoadingRecipients = false;

  private pollingSub?: Subscription;

  constructor(
    private readonly router: Router,
    private readonly messageService: MessageService,
    private readonly authService: AuthService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadConversations(true);
    this.loadApplicantOptions();

    // Poll every 5 seconds for incoming messages without full-screen reloads
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

  get filteredApplicants(): Recipient[] {
    let list = this.eligibleRecipients;
    if (this.selectedCompanyFilter !== 'ALL') {
      list = list.filter(r => r.company === this.selectedCompanyFilter);
    }
    if (this.searchQuery.trim()) {
      const q = this.searchQuery.toLowerCase();
      list = list.filter(r =>
        r.name?.toLowerCase().includes(q) ||
        r.email?.toLowerCase().includes(q) ||
        r.appliedRole?.toLowerCase().includes(q) ||
        r.company?.toLowerCase().includes(q)
      );
    }
    return list;
  }

  get filteredRecipients(): Recipient[] {
    if (!this.recipientSearchQuery.trim()) {
      return this.eligibleRecipients;
    }
    const q = this.recipientSearchQuery.toLowerCase();
    return this.eligibleRecipients.filter(r =>
      r.name?.toLowerCase().includes(q) ||
      r.email?.toLowerCase().includes(q) ||
      r.appliedRole?.toLowerCase().includes(q) ||
      r.company?.toLowerCase().includes(q)
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

        if (this.selectedConversation) {
          // Re-bind currently selected conversation with new data
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
        console.error('Error loading conversations:', err);
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  loadApplicantOptions(): void {
    this.messageService.getEligibleRecipients().subscribe({
      next: (recipients) => {
        this.eligibleRecipients = recipients || [];
        const companies = new Set<string>();
        for (const r of this.eligibleRecipients) {
          if (r.company && r.company !== 'Company' && r.company !== 'General Applicant') {
            companies.add(r.company);
          }
        }
        this.companyFilterList = Array.from(companies).sort();
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error loading applicant options:', err);
      }
    });
  }

  /**
   * Action triggered when recruiter selects an applicant from the Select Applicant dropdown
   */
  onApplicantSelect(applicant: Recipient | null): void {
    if (!applicant) return;
    this.selectedApplicant = applicant;
    this.errorFeedback = null;

    // Check if conversation already exists with this candidate
    const existing = this.conversations.find(c => c.partnerId === applicant.userId);
    if (existing) {
      this.selectConversation(existing);
    } else {
      // Create pending active conversation session with applicant metadata
      const newConvo: ConversationSummary = {
        partnerId: applicant.userId,
        partnerName: applicant.name,
        partnerEmail: applicant.email,
        partnerRole: 'CANDIDATE',
        candidateId: applicant.candidateId,
        appliedRole: applicant.appliedRole,
        company: applicant.company,
        jobId: applicant.jobId,
        applicationId: applicant.applicationId,
        lastMessage: '',
        lastMessageTime: '',
        unreadCount: 0,
        messages: []
      };

      // Add to front of conversation list
      this.conversations.unshift(newConvo);
      this.selectConversation(newConvo);
    }
  }

  refreshConversationsSilently(): void {
    this.messageService.getConversations().subscribe({
      next: (convos) => {
        this.conversations = convos || [];
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

  selectConversation(convo: ConversationSummary): void {
    this.selectedConversation = convo;
    this.activeMessages = convo.messages || [];
    this.errorFeedback = null;

    // Mark as read immediately on backend
    if (convo.unreadCount > 0) {
      convo.unreadCount = 0;
      this.messageService.markConversationAsRead(convo.partnerId).subscribe({
        next: () => {
          this.cdr.detectChanges();
        },
        error: (err) => console.warn('Failed to mark read:', err)
      });
    }

    this.cdr.detectChanges();
    this.scrollToBottom();
  }

  /**
   * Keyboard handler for message textarea:
   * Enter: send message
   * Shift + Enter: new line without sending
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
        // Clears typed message text ONLY after backend confirms success
        this.newMessageText = '';

        // Add to active message stream immediately
        this.activeMessages.push(savedMsg);
        if (this.selectedConversation) {
          this.selectedConversation.lastMessage = savedMsg.message;
          this.selectedConversation.lastMessageTime = savedMsg.sentAt;
        }

        this.cdr.detectChanges();
        this.scrollToBottom();

        // Refresh conversation summary in background
        this.refreshConversationsSilently();
      },
      error: (err) => {
        this.isSending = false;
        // Preserves typed message in textarea on failure
        console.error('Failed to send message:', err);
        this.errorFeedback = err?.error?.message || 'Unable to send message. Please verify network connectivity and try again.';
        this.cdr.detectChanges();
      }
    });
  }

  openNewMessageModal(): void {
    this.showNewMessageModal = true;
    this.recipientSearchQuery = '';
    this.selectedRecipient = null;
    this.newConversationText = '';
    this.isLoadingRecipients = true;

    this.messageService.getEligibleRecipients().subscribe({
      next: (recipients) => {
        this.eligibleRecipients = recipients || [];
        this.isLoadingRecipients = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load eligible recipients:', err);
        this.isLoadingRecipients = false;
        this.cdr.detectChanges();
      }
    });
  }

  closeNewMessageModal(): void {
    this.showNewMessageModal = false;
    this.selectedRecipient = null;
    this.newConversationText = '';
  }

  selectRecipient(r: Recipient): void {
    this.selectedRecipient = r;
  }

  /**
   * Keyboard handler for modal message textarea:
   * Enter: send message
   * Shift + Enter: new line without sending
   */
  onNewConversationKeydown(event: KeyboardEvent): void {
    if (event.key !== 'Enter') {
      return;
    }
    if (event.shiftKey) {
      return;
    }
    event.preventDefault();
    this.sendNewConversationMessage();
  }

  sendNewConversationMessage(): void {
    if (!this.selectedRecipient || this.isSending) return;
    const text = this.newConversationText.trim();
    if (!text) return;

    this.isSending = true;
    const recipient = this.selectedRecipient;

    this.messageService.sendMessage(
      recipient.userId,
      text,
      recipient.jobId,
      recipient.applicationId
    ).subscribe({
      next: () => {
        this.isSending = false;
        // Clears input only on success
        this.newConversationText = '';
        this.closeNewMessageModal();

        // Reload conversations and automatically select this recipient's thread
        this.messageService.getConversations().subscribe({
          next: (convos) => {
            this.conversations = convos || [];
            const target = this.conversations.find(c => c.partnerId === recipient.userId);
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
        // Preserves typed message on failure
        console.error('Failed to start conversation:', err);
        this.errorFeedback = err?.error?.message || 'Failed to start conversation. Please try again.';
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
    if (!name) return 'U';
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

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }
}