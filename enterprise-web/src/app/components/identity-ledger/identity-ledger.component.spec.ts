import { NO_ERRORS_SCHEMA, } from '@angular/core';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { IdentityLedgerComponent } from './identity-ledger.component';
import { FormBuilder } from '@angular/forms';

import { providers } from '../../testing/spies';

describe('IdentityLedgerComponent', () => {
  let component: IdentityLedgerComponent;
  let fixture: ComponentFixture<IdentityLedgerComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ IdentityLedgerComponent ],
      providers: [
        FormBuilder,
        ...providers
      ],
      schemas: [
        NO_ERRORS_SCHEMA
      ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(IdentityLedgerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
