import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ShortlistedCandidates } from './shortlisted-candidates';

describe('ShortlistedCandidates', () => {
  let component: ShortlistedCandidates;
  let fixture: ComponentFixture<ShortlistedCandidates>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ShortlistedCandidates],
    }).compileComponents();

    fixture = TestBed.createComponent(ShortlistedCandidates);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
