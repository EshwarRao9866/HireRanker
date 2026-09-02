import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-interview-scheduler',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './interview-scheduler.html',
  styleUrl: './interview-scheduler.css'
})
export class InterviewScheduler {

  constructor(private router: Router) {}

  interviews = [
    {
      candidate: 'Krupa Jyothi',
      job: 'Java Full Stack Developer',
      date: '28 Aug 2026',
      time: '10:00 AM',
      status: 'Scheduled'
    },
    {
      candidate: 'Eshwar Rao',
      job: 'Angular Developer',
      date: '29 Aug 2026',
      time: '11:30 AM',
      status: 'Scheduled'
    }
  ];

  startInterview() {
    alert('AI Live Interview module will be connected here.');
  }

  goBack() {
    this.router.navigate(['/dashboard']);
  }
}