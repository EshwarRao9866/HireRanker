import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-evaluation-criteria',
  standalone: true,
  templateUrl: './evaluation-criteria.html',
  styleUrl: './evaluation-criteria.css'
})
export class EvaluationCriteria {

  constructor(private router: Router) {}

  criteria = [
    { name: 'Technical Skills', weight: 35 },
    { name: 'Experience', weight: 25 },
    { name: 'Education', weight: 15 },
    { name: 'Projects', weight: 15 },
    { name: 'Certifications', weight: 10 }
  ];

  goBack() {
    this.router.navigate(['/dashboard']);
  }
}