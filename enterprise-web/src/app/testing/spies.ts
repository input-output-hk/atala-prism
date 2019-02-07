import { ErrorService } from '../services/error.service';
import { IdentityLedgerService } from '../services/identity-ledger.service';
import { KeyPairsService } from '../services/key-pairs.service';
import { IdentityRepository } from '../services/identity.repository';

export const spies = {
  errorService: jasmine.createSpyObj('ErrorService', [
    'renderErrors',
    'hasWrongValue',
    'getFieldError']),
  identityLedgerService: jasmine.createSpyObj('IdentityLedgerService', ['claim']),
  keyPairsService: jasmine.createSpyObj('KeyPairsService', ['create']),
  identityRepository: jasmine.createSpyObj('IdentityRepository', [
    'getUnconfirmedIdentities',
    'getConfirmedIdentities'])
};

export const providers = [
  { provide: ErrorService, useValue: spies.errorService },
  { provide: IdentityLedgerService, useValue: spies.identityLedgerService },
  { provide: KeyPairsService, useValue: spies.keyPairsService },
  { provide: IdentityRepository, useValue: spies.identityRepository }
];
