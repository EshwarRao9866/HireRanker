import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-reports',
  standalone: true,
  templateUrl: './reports.html',
  styleUrl: './reports.css'
})
export class Reports {

  constructor(private router: Router) {}

  goBack() {
    this.router.navigate(['/dashboard']);
  }
}