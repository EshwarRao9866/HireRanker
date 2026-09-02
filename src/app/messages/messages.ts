import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-messages',
  standalone: true,
  templateUrl: './messages.html',
  styleUrl: './messages.css'
})
export class Messages {

  constructor(private router: Router) {}

  goBack() {
    this.router.navigate(['/dashboard']);
  }
}