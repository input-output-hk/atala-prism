import { Component, Input, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

import { IdentityLedgerService } from '../../services/identity-ledger.service';
import { ErrorService } from '../../services/error.service';
import { IdentityRepository } from '../../services/identity.repository';

import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Component({
  selector: 'app-identity-details',
  templateUrl: './identity-details.component.html',
  styleUrls: ['./identity-details.component.css']
})
export class IdentityDetailsComponent implements OnInit {

  private _identity: string;
  endorsers$: Observable<string[]>;
  endorsements$: Observable<string[]>;
  publicKey$: Observable<string>;

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
  set identity(_identity: string) {
    this._identity = _identity;
    this.endorsers$ = this.identityLedger.getEndorsements(this.identity);
    this.endorsements$ = this.identityLedger.getEndorsers(this.identity);
    this.publicKey$ = this.identityLedger.getKeys(this.identity)
      .pipe(
        map(x => x[0])
      );
  }

  get identity(): string {
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

  revokeEndorsement(endorsedIdentity: string) {
    const identity = this.identityRepository.get(this.identity);
    this.identityLedger.revokeEndorsement(identity, endorsedIdentity)
      .subscribe(_ => alert('The endorsement will be revoken soon'), response => this.onError(response));
  }

  belongsToMe(identity: string): boolean {
    return this.identityRepository.get(identity) != null;
  }

  private doEndorse(endorsedIdentity: string) {
    const endorser = this.identityRepository.get(this.identity);
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
