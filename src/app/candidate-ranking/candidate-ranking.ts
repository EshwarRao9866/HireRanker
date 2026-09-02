import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-candidate-ranking',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './candidate-ranking.html',
  styleUrl: './candidate-ranking.css'
})
export class CandidateRanking {

  constructor(private router: Router) {}

  candidates = [
    { rank: 1, name: 'Eshwar Rao', score: 95 },
    { rank: 2, name: 'Krupa Jyothi', score: 89 },
    { rank: 3, name: 'Amit Verma', score: 84 },
    { rank: 4, name: 'Sneha Patel', score: 82 },
    { rank: 5, name: 'Vikram Singh', score: 78 }
  ];

  goBack() {
    this.router.navigate(['/dashboard']);
  }
}