import { Component, Input, OnInit } from '@angular/core';

import { IdentityLedgerService } from '../../services/identity-ledger.service';
import { identity, Observable } from 'rxjs';

@Component({
  selector: 'app-identity-details',
  templateUrl: './identity-details.component.html',
  styleUrls: ['./identity-details.component.css']
})
export class IdentityDetailsComponent implements OnInit {

  private _identity: string;
  endorsers$: Observable<string[]>;
  endorsements$: Observable<string[]>;

  constructor(private identityLedger: IdentityLedgerService) { }

  ngOnInit() {
  }

  @Input()
  set identity(_identity: string) {
    this._identity = _identity;
    this.endorsers$ = this.identityLedger.getEndorsements(this.identity);
    this.endorsements$ = this.identityLedger.getEndorsers(this.identity);
  }

  get identity(): string {
    return this._identity;
  }
}
