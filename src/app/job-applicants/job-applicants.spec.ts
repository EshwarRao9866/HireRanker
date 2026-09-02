import { ComponentFixture, TestBed } from '@angular/core/testing';

import { JobApplicants } from './job-applicants';

describe('JobApplicants', () => {
  let component: JobApplicants;
  let fixture: ComponentFixture<JobApplicants>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [JobApplicants],
    }).compileComponents();

    fixture = TestBed.createComponent(JobApplicants);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
