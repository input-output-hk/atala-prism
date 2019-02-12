import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA, } from '@angular/core';

import { IdentityDetailsComponent } from './identity-details.component';

import { FormBuilder } from '@angular/forms';

import { providers } from '../../testing/spies';

describe('IdentityDetailsComponent', () => {
  let component: IdentityDetailsComponent;
  let fixture: ComponentFixture<IdentityDetailsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ IdentityDetailsComponent ],
      providers: [
        FormBuilder,
        ...providers
      ],
      schemas: [NO_ERRORS_SCHEMA]
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
