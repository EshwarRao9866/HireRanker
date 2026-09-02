import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-resume-screening',
  standalone: true,
  templateUrl: './resume-screening.html',
  styleUrl: './resume-screening.css'
})
export class ResumeScreening {

  constructor(private router: Router) {}

  startScreening() {
    alert('AI Resume Screening will be connected to the backend.');
  }

  goBack() {
    this.router.navigate(['/dashboard']);
  }
}