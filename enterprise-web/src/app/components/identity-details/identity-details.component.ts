import { Component, Input, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

import { IdentityLedgerService } from '../../services/identity-ledger.service';
import { ErrorService } from '../../services/error.service';
import { IdentityRepository } from '../../services/identity.repository';
import { Identity } from '../../models/identity';

import { Observable } from 'rxjs';

@Component({
  selector: 'app-identity-details',
  templateUrl: './identity-details.component.html',
  styleUrls: ['./identity-details.component.css']
})
export class IdentityDetailsComponent implements OnInit {

  private _identity: Identity;
  endorsers$: Observable<string[]>;
  endorsements$: Observable<string[]>;

  form: FormGroup;
  endorsedIdentityField = 'data.endorsedIdentity';

  constructor(
    public errorService: ErrorService,
    public identityRepository: IdentityRepository,
    private formBuilder: FormBuilder,
    private identityLedger: IdentityLedgerService) { }

  ngOnInit() {
    this.createForm();
  }

  private createForm() {
    const values = {};
    values[this.endorsedIdentityField] = [null, [Validators.required]];
    this.form = this.formBuilder.group(values);
  }

  @Input()
  set identity(_identity: Identity) {
    this._identity = _identity;
    this.endorsers$ = this.identityLedger.getEndorsements(this.identity.identity);
    this.endorsements$ = this.identityLedger.getEndorsers(this.identity.identity);
  }

  get identity(): Identity {
    return this._identity;
  }

  endorse() {
    const endorsedIdentity = this.form.controls[this.endorsedIdentityField].value;
    if (endorsedIdentity == null) {
      alert('Identity required');
    } else {
      this.checkAvailability(endorsedIdentity, () => alert('Unknown identity'), () => this.doEndorse(endorsedIdentity));
    }
  }

  private doEndorse(endorsedIdentity: string) {
    const endorser = this.identity;
    this.identityLedger.endorse(endorser, endorsedIdentity).subscribe(_ => {
      alert('Transaction submitted, it must be applied in some minutes');
    }, error => this.onError(error));
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
}
