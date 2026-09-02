import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CandidateLogin } from './candidate-login';

describe('CandidateLogin', () => {
  let component: CandidateLogin;
  let fixture: ComponentFixture<CandidateLogin>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CandidateLogin],
    }).compileComponents();

    fixture = TestBed.createComponent(CandidateLogin);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
