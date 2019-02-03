import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

import { IdentityLedgerService } from '../../services/identity-ledger.service';
import { KeyPairsService } from '../../services/key-pairs.service';
import { ErrorService } from '../../services/error.service';
import { IdentityRepository } from '../../services/identity.repository';
import { Identity } from '../../models/identity';

const identityField = 'data.identity';

@Component({
  selector: 'app-identity-ledger',
  templateUrl: './identity-ledger.component.html',
  styleUrls: ['./identity-ledger.component.css']
})
export class IdentityLedgerComponent implements OnInit, OnDestroy {

  form: FormGroup;
  selectedIdentity: string;

  private timer = null;
  private confirmed: string[] = [];

  constructor(
      public errorService: ErrorService,
      public identityRepository: IdentityRepository,
      private formBuilder: FormBuilder,
      private identityLedger: IdentityLedgerService,
      private keyPairs: KeyPairsService) {

  }

  ngOnInit() {
    this.createForm();
    const confirmIdentities = () => {
      // TODO: Validate that the identity keys matches what the sever has
      this.identityRepository.getUnconfirmedIdentities().forEach(identity => {
        this.checkAvailability(identity.identity, () => {}, () => {
          this.confirmed.push(identity.identity);
          this.identityRepository.confirm(identity);
        });
      });
    };
    this.timer = setInterval(confirmIdentities, 5000);
  }

  ngOnDestroy() {
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
  }

  isConfirmed(identity: string): boolean {
    return this.confirmed.indexOf(identity) >= 0;
  }

  private createForm() {
    const values = {};
    values[identityField] = [null, [Validators.required]];
    this.form = this.formBuilder.group(values);
  }

  claim() {
    const identity = this.form.controls[identityField].value;
    if (identity == null) {
      alert('Identity required');
    } else {
      this.checkIfAvailable(identity, () => this.newKeyPair(pair => this.doClaim(new Identity(identity, pair))));
    }
  }

  private doClaim(identity: Identity) {
    this.identityLedger.claim(identity).subscribe(_ => {
      this.identityRepository.create(identity);
    }, error => this.onError(error));
  }

  private newKeyPair(callback: (any) => any) {
    this.keyPairs.generate().subscribe(callback, error => this.onError(error));
  }

  private checkIfAvailable(identity: string, callback: () => any) {
    this.checkAvailability(identity, callback, () => this.errorService.setFieldError(this.form, identityField, 'Identity already taken'));
  }

  private checkAvailability(identity: string, onAvailableCallback, onUnavailableCallback: () => any) {
    this.identityLedger.exists(identity).subscribe(response => {
      if (response.exists) {
        onUnavailableCallback();
      } else {
        onAvailableCallback();
      }
    }, error => this.onError(error));
  }

  private onError(response: any) {
    this.errorService.renderServerErrors(this.form, response);
  }

  select(identity: string): void {
    this.selectedIdentity = identity;
  }
}
