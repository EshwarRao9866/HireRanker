import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-shortlisted-candidates',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './shortlisted-candidates.html',
  styleUrl: './shortlisted-candidates.css'
})
export class ShortlistedCandidates {

  constructor(private router: Router) {}

  candidates = [
    {
      name: 'Eshwar Rao',
      score: 95,
      interview: 'Not Scheduled'
    },
    {
      name: 'Krupa Jyothi',
      score: 89,
      interview: 'Scheduled'
    },
    {
      name: 'Amit Verma',
      score: 84,
      interview: 'Not Scheduled'
    }
  ];

  goBack() {
    this.router.navigate(['/dashboard']);
  }
}