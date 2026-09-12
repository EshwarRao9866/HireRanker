import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './settings.html',
  styleUrl: './settings.css'
})
export class Settings implements OnInit {
  activeTab: 'profile' | 'security' | 'notifications' | 'app' | 'screening' | 'evaluation' = 'profile';
  saveMessage = '';

  // Profile Settings
  adminName = 'Admin Recruiter';
  adminAvatar = '';
  companyName = 'HireRanker Technologies';
  companyWebsite = 'https://hireranker.com';
  adminEmail = 'admin@hireranker.com';
  contactNumber = '+91 98765 43210';

  // Security
  twoFactorAuth = true;
  sessionTimeout = 30;

  // Notifications
  emailNotifications = true;
  candidateAlerts = true;
  dailyDigest = false;

  // Application Settings
  allowAutoReject = false;
  requireCoverLetter = false;

  // Screening Settings
  minMatchScore = 75;
  enableAIScoring = true;
  ocrParsingEngine = 'HireRanker DeepParser v2';

  constructor(
    private readonly router: Router,
    private readonly authService: AuthService
  ) {}

  ngOnInit(): void {
    // 1. Restore saved admin settings from persistent storage
    const saved = this.authService.getAdminSettings();
    if (saved) {
      if (saved.adminName) this.adminName = saved.adminName;
      if (saved.adminAvatar) this.adminAvatar = saved.adminAvatar;
      if (saved.companyName) this.companyName = saved.companyName;
      if (saved.companyWebsite) this.companyWebsite = saved.companyWebsite;
      if (saved.adminEmail) this.adminEmail = saved.adminEmail;
      if (saved.contactNumber) this.contactNumber = saved.contactNumber;
      if (saved.twoFactorAuth !== undefined) this.twoFactorAuth = saved.twoFactorAuth;
      if (saved.sessionTimeout !== undefined) this.sessionTimeout = saved.sessionTimeout;
      if (saved.emailNotifications !== undefined) this.emailNotifications = saved.emailNotifications;
      if (saved.candidateAlerts !== undefined) this.candidateAlerts = saved.candidateAlerts;
      if (saved.dailyDigest !== undefined) this.dailyDigest = saved.dailyDigest;
      if (saved.allowAutoReject !== undefined) this.allowAutoReject = saved.allowAutoReject;
      if (saved.requireCoverLetter !== undefined) this.requireCoverLetter = saved.requireCoverLetter;
      if (saved.minMatchScore !== undefined) this.minMatchScore = saved.minMatchScore;
      if (saved.enableAIScoring !== undefined) this.enableAIScoring = saved.enableAIScoring;
      if (saved.ocrParsingEngine) this.ocrParsingEngine = saved.ocrParsingEngine;
    } else {
      const user = this.authService.currentUser();
      if (user) {
        if (user.fullName) this.adminName = user.fullName;
        if (user.email) this.adminEmail = user.email;
      }
    }
  }

  setTab(tab: 'profile' | 'security' | 'notifications' | 'app' | 'screening' | 'evaluation'): void {
    this.activeTab = tab;
    this.saveMessage = '';
  }

  onAdminAvatarSelected(event: any): void {
    const file = event.target.files?.[0];
    if (file) {
      if (!file.type.startsWith('image/')) {
        alert('Please select a valid image file (PNG, JPG, WEBP, etc.)');
        return;
      }
      if (file.size > 5 * 1024 * 1024) {
        alert('Image size exceeds 5MB limit. Please select a smaller photo.');
        return;
      }
      const reader = new FileReader();
      reader.onload = () => {
        this.adminAvatar = reader.result as string;
        this.saveSettings();
        this.saveMessage = 'Admin profile picture updated and saved!';
        setTimeout(() => (this.saveMessage = ''), 3000);
      };
      reader.readAsDataURL(file);
    }
  }

  removeAdminAvatar(): void {
    this.adminAvatar = '';
    this.saveSettings();
    this.saveMessage = 'Admin profile picture removed.';
    setTimeout(() => (this.saveMessage = ''), 3000);
  }

  saveSettings(): void {
    const settingsPayload = {
      adminName: this.adminName,
      adminAvatar: this.adminAvatar,
      companyName: this.companyName,
      companyWebsite: this.companyWebsite,
      adminEmail: this.adminEmail,
      contactNumber: this.contactNumber,
      twoFactorAuth: this.twoFactorAuth,
      sessionTimeout: this.sessionTimeout,
      emailNotifications: this.emailNotifications,
      candidateAlerts: this.candidateAlerts,
      dailyDigest: this.dailyDigest,
      allowAutoReject: this.allowAutoReject,
      requireCoverLetter: this.requireCoverLetter,
      minMatchScore: this.minMatchScore,
      enableAIScoring: this.enableAIScoring,
      ocrParsingEngine: this.ocrParsingEngine
    };

    // Save to persistent storage and update user session
    this.authService.saveAdminSettings(settingsPayload);

    this.saveMessage = 'Settings & profile details saved successfully!';
    setTimeout(() => {
      this.saveMessage = '';
    }, 4000);
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }
}