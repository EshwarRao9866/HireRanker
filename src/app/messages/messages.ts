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
  isLoading = true;

  conversations: Conversation[] = [];
  selectedConversation: Conversation | null = null;

  constructor(
    private readonly router: Router,
    private readonly messageService: MessageService,
    private readonly authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadMessages();
  }

  loadMessages(): void {
    this.isLoading = true;
    this.messageService.getMessages().subscribe({
      next: (msgs: MessageResponse[]) => {
        this.isLoading = false;
        if (!msgs || msgs.length === 0) {
          this.conversations = [];
          this.selectedConversation = null;
          return;
        }

        const currentUserId = this.authService.currentUser()?.id;
        const convoMap = new Map<number, { name: string; msgs: ChatMessage[]; lastMsg: string; lastTime: string; unread: number }>();

        for (const m of msgs) {
          const isSenderMe = m.senderId === currentUserId;
          const partnerId = isSenderMe ? m.receiverId : m.senderId;
          const partnerName = isSenderMe ? (m.receiverName || `User #${partnerId}`) : (m.senderName || `User #${partnerId}`);

          if (!convoMap.has(partnerId)) {
            convoMap.set(partnerId, {
              name: partnerName,
              msgs: [],
              lastMsg: '',
              lastTime: '',
              unread: 0
            });
          }

          const entry = convoMap.get(partnerId)!;
          const formattedTime = m.sentAt
            ? new Date(m.sentAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
            : 'Recent';

          entry.msgs.push({
            sender: isSenderMe ? 'admin' : 'candidate',
            text: m.message,
            time: formattedTime
          });
          entry.lastMsg = m.message;
          entry.lastTime = formattedTime;
          if (!isSenderMe && m.readStatus === 'UNREAD') {
            entry.unread++;
          }
        }

        const builtConvos: Conversation[] = [];
        convoMap.forEach((val, partnerId) => {
          builtConvos.push({
            id: partnerId,
            candidateName: val.name,
            candidateRole: 'Candidate',
            avatar: val.name.charAt(0).toUpperCase() || 'U',
            lastMessage: val.lastMsg,
            time: val.lastTime,
            unreadCount: val.unread,
            online: true,
            messages: val.msgs
          });
        });

        this.conversations = builtConvos;
        if (builtConvos.length > 0) {
          this.selectedConversation = builtConvos[0];
        } else {
          this.selectedConversation = null;
        }
      },
      error: (err) => {
        console.warn('Backend message load failed:', err?.status);
        this.isLoading = false;
        this.conversations = [];
        this.selectedConversation = null;
      }
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
    if (!this.selectedConversation) return;
    const text = this.newMessageText.trim();
    if (!text) return;

    const currentConvo = this.selectedConversation;
    currentConvo.messages.push({
      sender: 'admin',
      text: text,
      time: 'Just now'
    });

    currentConvo.lastMessage = text;
    currentConvo.time = 'Just now';
    this.newMessageText = '';

    // Send to backend POST /api/messages
    this.messageService.sendMessage(currentConvo.id, text).subscribe({
      next: () => {},
      error: (err) => console.warn('Backend message save not available:', err?.status)
    });
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }
}