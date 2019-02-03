import { NO_ERRORS_SCHEMA, } from '@angular/core';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { IdentityLedgerComponent } from './identity-ledger.component';
import { FormBuilder } from '@angular/forms';

import { ErrorService } from '../../services/error.service';
import { IdentityLedgerService } from '../../services/identity-ledger.service';
import { KeyPairsService } from '../../services/key-pairs.service';
import { IdentityRepository } from '../../services/identity.repository';

describe('IdentityLedgerComponent', () => {
  let component: IdentityLedgerComponent;
  let fixture: ComponentFixture<IdentityLedgerComponent>;

  const errorServiceSpy: jasmine.SpyObj<ErrorService> = jasmine.createSpyObj('ErrorService', [
    'renderErrors',
    'hasWrongValue',
    'getFieldError']);
  const identityLedgerServiceSpy: jasmine.SpyObj<IdentityLedgerService> = jasmine.createSpyObj('IdentityLedgerService', ['claim']);
  const keyPairsServiceSpy: jasmine.SpyObj<KeyPairsService> = jasmine.createSpyObj('KeyPairsService', ['create']);
  const identityRepositorySpy: jasmine.SpyObj<IdentityRepository> = jasmine.createSpyObj('IdentityRepository', [
    'getUnconfirmedIdentities',
    'getConfirmedIdentities']);

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ IdentityLedgerComponent ],
      providers: [
        FormBuilder,
        { provide: ErrorService, useValue: errorServiceSpy },
        { provide: IdentityLedgerService, useValue: identityLedgerServiceSpy },
        { provide: KeyPairsService, useValue: keyPairsServiceSpy },
        { provide: IdentityRepository, useValue: identityRepositorySpy }
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
