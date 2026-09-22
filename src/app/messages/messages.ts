import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MessageService, MessageResponse } from '../services/message.service';
import { AuthService } from '../services/auth.service';

interface ChatMessage {
  sender: 'admin' | 'candidate';
  text: string;
  time: string;
}

interface Conversation {
  id: number;
  candidateName: string;
  candidateRole: string;
  avatar: string;
  lastMessage: string;
  time: string;
  unreadCount: number;
  online: boolean;
  messages: ChatMessage[];
}

@Component({
  selector: 'app-messages',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './messages.html',
  styleUrl: './messages.css'
})
export class Messages implements OnInit {
  searchQuery = '';
  newMessageText = '';

  conversations: Conversation[] = [];
  selectedConversation: Conversation | null = null;

  constructor(
    private readonly router: Router,
    private readonly messageService: MessageService,
    private readonly authService: AuthService
  ) {}

  ngOnInit(): void {
    this.messageService.getMessages().subscribe({
      next: (msgs: MessageResponse[]) => {
        if (msgs && msgs.length > 0) {
          const currentUserId = this.authService.currentUser()?.id;
          // Group messages by counterparty
          const convoMap = new Map<number, ChatMessage[]>();
          for (const m of msgs) {
            const partnerId = m.senderId === currentUserId ? m.receiverId : m.senderId;
            if (!convoMap.has(partnerId)) {
              convoMap.set(partnerId, []);
            }
            convoMap.get(partnerId)!.push({
              sender: m.senderId === currentUserId ? 'admin' : 'candidate',
              text: m.message,
              time: m.sentAt ? new Date(m.sentAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Recent'
            });
          }
        }
      },
      error: () => {}
    });
  }

  get filteredConversations(): Conversation[] {
    return this.conversations.filter(c =>
      !this.searchQuery || c.candidateName.toLowerCase().includes(this.searchQuery.toLowerCase())
    );
  }

  selectConversation(convo: Conversation): void {
    this.selectedConversation = convo;
    convo.unreadCount = 0;
  }

  sendMessage(): void {
    const text = this.newMessageText.trim();
    if (!text || !this.selectedConversation) return;

    this.selectedConversation.messages.push({
      sender: 'admin',
      text: text,
      time: 'Just now'
    });

    this.selectedConversation.lastMessage = text;
    this.selectedConversation.time = 'Just now';
    this.newMessageText = '';

    // Send to backend POST /api/messages
    this.messageService.sendMessage(this.selectedConversation.id, text).subscribe({
      next: () => {},
      error: (err) => console.warn('Backend message save not available:', err?.status)
    });
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }
}