import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

@Component({
  selector: 'app-evaluation-criteria',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './evaluation-criteria.html',
  styleUrl: './evaluation-criteria.css'
})
export class EvaluationCriteria {
  skillsWeight = 40;
  experienceWeight = 30;
  educationWeight = 20;
  certificationsWeight = 10;
  minScoreThreshold = 75;

  keywords: string[] = ['Java', 'Spring Boot', 'Angular', 'TypeScript', 'Microservices', 'SQL', 'Docker'];
  newKeyword = '';

  saveMessage = '';

  constructor(private readonly router: Router) {}

  get totalWeight(): number {
    return this.skillsWeight + this.experienceWeight + this.educationWeight + this.certificationsWeight;
  }

  addKeyword(): void {
    if (this.newKeyword.trim() && !this.keywords.includes(this.newKeyword.trim())) {
      this.keywords.push(this.newKeyword.trim());
      this.newKeyword = '';
    }
  }

  removeKeyword(kw: string): void {
    this.keywords = this.keywords.filter(k => k !== kw);
  }

  saveCriteria(): void {
    if (this.totalWeight !== 100) {
      alert(`Warning: Total weight must equal 100%. Current sum: ${this.totalWeight}%`);
      return;
    }

    this.saveMessage = 'Evaluation criteria & weights updated successfully!';
    setTimeout(() => {
      this.saveMessage = '';
    }, 4000);
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }
}