import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { config } from '../app.config';
import { Identity } from '../models/identity';

const httpOptions = {
  headers: new HttpHeaders({ 'Content-Type': 'application/json' })
};

@Injectable()
export class IdentityLedgerService {

  private baseUrl = environment.api.url + '/identities';

  constructor(private http: HttpClient) { }

  getKeys(identity: string): Observable<string> {
    const url = `${this.baseUrl}/${identity}`;
    return this.http.get<string>(url);
  }

  exists(identity: string): Observable<any> {
    const url = `${this.baseUrl}/${identity}/exists`;
    return this.http.get<any>(url);
  }

  claim(identity: Identity): Observable<any> {
    const url = `${this.baseUrl}`;
    const body = {
      type: 'claim',
      ledgerId: config.identityLedger,
      privateKey: identity.keyPair.privateKey,
      data: {
        identity: identity.identity,
        key: identity.keyPair.publicKey
      }
    };
    return this.http.post(url, body, httpOptions);
  }

  endorse(endorser: Identity, endorsedIdentity: string): Observable<any> {
    const url = `${this.baseUrl}`;
    const body = {
      type: 'endorse',
      ledgerId: config.identityLedger,
      privateKey: endorser.keyPair.privateKey,
      data: {
        endorserIdentity: endorser.identity,
        endorsedIdentity: endorsedIdentity,
      }
    };
    return this.http.post(url, body, httpOptions);
  }

  getEndorsers(identity: string): Observable<string[]> {
    const url = `${this.baseUrl}/${identity}/endorsers`;
    return this.http.get<string[]>(url);
  }

  getEndorsements(identity: string): Observable<string[]> {
    const url = `${this.baseUrl}/${identity}/endorsements`;
    return this.http.get<string[]>(url);
  }
}
