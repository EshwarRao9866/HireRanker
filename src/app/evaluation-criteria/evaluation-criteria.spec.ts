import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EvaluationCriteria } from './evaluation-criteria';

describe('EvaluationCriteria', () => {
  let component: EvaluationCriteria;
  let fixture: ComponentFixture<EvaluationCriteria>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EvaluationCriteria],
    }).compileComponents();

    fixture = TestBed.createComponent(EvaluationCriteria);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
