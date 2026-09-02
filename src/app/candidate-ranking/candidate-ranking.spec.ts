import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CandidateRanking } from './candidate-ranking';

describe('CandidateRanking', () => {
  let component: CandidateRanking;
  let fixture: ComponentFixture<CandidateRanking>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CandidateRanking],
    }).compileComponents();

    fixture = TestBed.createComponent(CandidateRanking);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
