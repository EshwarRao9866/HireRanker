// import { Component, signal } from '@angular/core';
// // import { HomePage } from './home-page/home-page';
// import { FormsModule } from '@angular/forms';
// import { RouterOutlet } from '@angular/router';
// import { Register } from './register/register';

// @Component({
//   selector: 'app-root',
//   imports: [ FormsModule, RouterOutlet, Register],
//   templateUrl: './app.html',
//   styleUrl: './app.css'
// })
// export class App {

// }
import { Component, OnInit, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { RouterOutlet, Router } from '@angular/router';
import { AiChatbot } from './ai-chatbot/ai-chatbot';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, AiChatbot],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly router = inject(Router);

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      try {
        // Validate localStorage integrity for session keys
        const session = localStorage.getItem('hireRankerSession');
        if (session) {
          JSON.parse(session);
        }
      } catch (err) {
        console.warn('[App] Corrupt localStorage session detected during initialization. Resetting credentials.', err);
        try {
          localStorage.removeItem('hireRankerSession');
          localStorage.removeItem('hireRankerToken');
          localStorage.removeItem('isLoggedIn');
          localStorage.removeItem('userRole');
        } catch {}
        alert('Your saved session data was corrupted and has been reset. Please log in again.');
        this.router.navigate(['/login']);
      }
    }
  }
}