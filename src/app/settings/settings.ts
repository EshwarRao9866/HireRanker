import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './settings.html',
  styleUrl: './settings.css'
})
export class Settings {

  constructor(private router: Router) {}

  companyName = 'HireRanker';
  email = 'admin@hireranker.com';
  notifications = true;

  saveSettings() {
    alert('Settings saved successfully.');
  }

  goBack() {
    this.router.navigate(['/dashboard']);
  }
}