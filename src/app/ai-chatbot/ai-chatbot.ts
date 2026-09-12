import { Component, ElementRef, ViewChild, AfterViewChecked, inject, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavigationEnd, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { AiChatService, ChatMessage } from '../services/ai-chat.service';

@Component({
  selector: 'app-ai-chatbot',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ai-chatbot.html',
  styleUrl: './ai-chatbot.css'
})
export class AiChatbot implements OnInit, AfterViewChecked, OnDestroy {
  readonly chatService = inject(AiChatService);
  private readonly router = inject(Router);

  @ViewChild('messagesContainer') private messagesContainer!: ElementRef<HTMLDivElement>;

  inputText = '';
  isMinimized = false;
  speechAvailable = false;
  private recognition: any = null;
  private routerSub?: Subscription;

  readonly currentUrl = signal<string>(this.router.url);

  get isDashboardPage(): boolean {
    const url = (this.currentUrl() || this.router.url || '').split('?')[0].split('#')[0];
    return url === '/dashboard' || url === '/candidate-dashboard';
  }

  get isOpen(): boolean {
    return this.chatService.isOpen();
  }

  get isThinking(): boolean {
    return this.chatService.isThinking();
  }

  get isListening(): boolean {
    return this.chatService.isListening();
  }

  get messages(): ChatMessage[] {
    return this.chatService.messages();
  }

  get currentRole(): 'CANDIDATE' | 'ADMIN' {
    return this.chatService.currentRole();
  }

  get currentMode(): 'RECRUITER' | 'CANDIDATE' {
    return this.chatService.currentMode();
  }

  get isRecruiter(): boolean {
    return this.chatService.isRecruiter();
  }

  get suggestedPrompts(): string[] {
    return this.chatService.getSuggestedPrompts();
  }

  ngOnInit(): void {
    this.initSpeechRecognition();
    this.currentUrl.set(this.router.url);
    this.chatService.checkAndInitializeGreeting();
    this.routerSub = this.router.events.subscribe((evt) => {
      if (evt instanceof NavigationEnd) {
        this.currentUrl.set(evt.urlAfterRedirects || evt.url);
        this.chatService.checkAndInitializeGreeting();
      }
    });
  }

  ngAfterViewChecked(): void {
    this.scrollToBottom();
  }

  ngOnDestroy(): void {
    if (this.routerSub) {
      this.routerSub.unsubscribe();
    }
    if (this.recognition) {
      try { this.recognition.abort(); } catch {}
    }
  }

  toggleOpen(): void {
    this.chatService.toggleChatbot();
    if (this.isMinimized) {
      this.isMinimized = false;
    }
  }

  close(): void {
    this.chatService.closeChatbot();
  }

  toggleMinimize(): void {
    this.isMinimized = !this.isMinimized;
  }

  clearChat(event?: Event): void {
    if (event) {
      event.preventDefault();
      event.stopPropagation();
    }
    this.inputText = '';
    this.chatService.clearHistory();
  }

  send(): void {
    if (!this.inputText.trim() || this.isThinking) return;
    const msg = this.inputText;
    this.inputText = '';
    this.chatService.sendMessage(msg);
  }

  selectPrompt(prompt: string): void {
    this.chatService.sendMessage(prompt);
  }

  formatMessage(text: string): string {
    if (!text) return '';
    let formatted = text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');

    // Code blocks ```java ... ```
    formatted = formatted.replace(/```([a-z]*)\n([\s\S]*?)```/gm, '<pre class="code-block"><code>$2</code></pre>');

    // Inline code `code`
    formatted = formatted.replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>');

    // Bold **text**
    formatted = formatted.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');

    // Bullet points
    formatted = formatted.replace(/^[•\-\*]\s+(.*)$/gm, '<li>$1</li>');
    if (formatted.includes('<li>')) {
      formatted = formatted.replace(/(<li>.*<\/li>)/s, '<ul>$1</ul>');
    }

    // Newlines to <br>
    formatted = formatted.replace(/\n/g, '<br>');

    return formatted;
  }

  toggleVoice(): void {
    if (!this.speechAvailable) {
      alert('Speech recognition is not supported in your current browser.');
      return;
    }

    if (this.isListening) {
      this.stopVoice();
    } else {
      this.startVoice();
    }
  }

  private initSpeechRecognition(): void {
    if (typeof window !== 'undefined') {
      const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
      if (SpeechRecognition) {
        this.speechAvailable = true;
        this.recognition = new SpeechRecognition();
        this.recognition.continuous = false;
        this.recognition.interimResults = false;
        this.recognition.lang = 'en-US';

        this.recognition.onresult = (event: any) => {
          const transcript = event.results[0][0].transcript;
          if (transcript) {
            this.inputText = transcript;
            this.send();
          }
          this.chatService.isListening.set(false);
        };

        this.recognition.onerror = () => {
          this.chatService.isListening.set(false);
        };

        this.recognition.onend = () => {
          this.chatService.isListening.set(false);
        };
      }
    }
  }

  private startVoice(): void {
    if (this.recognition) {
      try {
        this.chatService.isListening.set(true);
        this.recognition.start();
      } catch (e) {
        this.chatService.isListening.set(false);
      }
    }
  }

  private stopVoice(): void {
    if (this.recognition) {
      try {
        this.recognition.stop();
      } catch (e) {}
      this.chatService.isListening.set(false);
    }
  }

  private scrollToBottom(): void {
    if (this.messagesContainer) {
      try {
        const el = this.messagesContainer.nativeElement;
        el.scrollTop = el.scrollHeight;
      } catch (err) {}
    }
  }
}
