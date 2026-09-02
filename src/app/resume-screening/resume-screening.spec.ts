import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ResumeScreening } from './resume-screening';

describe('ResumeScreening', () => {
  let component: ResumeScreening;
  let fixture: ComponentFixture<ResumeScreening>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ResumeScreening],
    }).compileComponents();

    fixture = TestBed.createComponent(ResumeScreening);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
