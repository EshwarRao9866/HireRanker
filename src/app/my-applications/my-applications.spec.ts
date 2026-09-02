import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MyApplications } from './my-applications';

describe('MyApplications', () => {
  let component: MyApplications;
  let fixture: ComponentFixture<MyApplications>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MyApplications],
    }).compileComponents();

    fixture = TestBed.createComponent(MyApplications);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
