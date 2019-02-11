import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { IdentityDetailsComponent } from './identity-details.component';

import { providers } from '../../testing/spies';

describe('IdentityDetailsComponent', () => {
  let component: IdentityDetailsComponent;
  let fixture: ComponentFixture<IdentityDetailsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ IdentityDetailsComponent ],
      providers: providers
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(IdentityDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
