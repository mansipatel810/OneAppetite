import { Component, ElementRef, ViewChild, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatService, ChatMessage, ChatTurn } from '../services/chat.service';

@Component({
  selector: 'app-chatbot',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chatbot.component.html',
  styleUrl: './chatbot.component.css',
})
export class ChatbotComponent {
  @ViewChild('messagesEnd') messagesEnd!: ElementRef;

  open = signal(false);
  input = '';
  messages = signal<ChatMessage[]>([
    { from: 'bot', text: "Hi! I'm the campus food guide. Ask me what's available right now across the cafes and dining halls, or about calorie estimates for any item." },
  ]);

  private history: ChatTurn[] = [];
  private chatService: ChatService;

  constructor(chatService: ChatService) {
    this.chatService = chatService;
  }

  toggle() {
    this.open.update(v => !v);
    if (this.open()) {
      setTimeout(() => this.scrollToBottom(), 50);
    }
  }

  send() {
    const text = this.input.trim();
    if (!text) return;

    this.input = '';
    this.messages.update(msgs => [...msgs, { from: 'user', text }]);
    this.messages.update(msgs => [...msgs, { from: 'bot', text: '', loading: true }]);
    setTimeout(() => this.scrollToBottom(), 50);

    this.chatService.send(text, this.history).subscribe({
      next: (res) => {
        this.history.push({ role: 'user', text });
        this.history.push({ role: 'model', text: res.reply });

        // Keep history to last 10 turns to avoid token overflow
        if (this.history.length > 20) {
          this.history = this.history.slice(this.history.length - 20);
        }

        this.messages.update(msgs => {
          const updated = [...msgs];
          updated[updated.length - 1] = { from: 'bot', text: res.reply, loading: false };
          return updated;
        });
        setTimeout(() => this.scrollToBottom(), 50);
      },
      error: () => {
        this.messages.update(msgs => {
          const updated = [...msgs];
          updated[updated.length - 1] = { from: 'bot', text: 'Sorry, something went wrong. Please try again.', loading: false };
          return updated;
        });
      },
    });
  }

  onEnter(event: KeyboardEvent) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.send();
    }
  }

  private scrollToBottom() {
    this.messagesEnd?.nativeElement?.scrollIntoView({ behavior: 'smooth' });
  }
}
