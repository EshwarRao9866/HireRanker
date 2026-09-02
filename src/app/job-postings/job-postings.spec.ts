import { ComponentFixture, TestBed } from '@angular/core/testing';

import { JobPostings } from './job-postings';

describe('JobPostings', () => {
  let component: JobPostings;
  let fixture: ComponentFixture<JobPostings>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [JobPostings],
    }).compileComponents();

    fixture = TestBed.createComponent(JobPostings);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
