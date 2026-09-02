import { ComponentFixture, TestBed } from '@angular/core/testing';

import { InterviewScheduler } from './interview-scheduler';

describe('InterviewScheduler', () => {
  let component: InterviewScheduler;
  let fixture: ComponentFixture<InterviewScheduler>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InterviewScheduler],
    }).compileComponents();

    fixture = TestBed.createComponent(InterviewScheduler);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
