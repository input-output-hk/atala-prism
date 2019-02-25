import { Injectable } from '@angular/core';
import { Identity } from '../models/identity';

@Injectable()
export class IdentityRepository {

  private values: [Identity, boolean][] = [];

  constructor() {
    this.values = this.getAll();
  }

  get(identity: string): Identity {
    return this.values.map(x => x[0]).find(x => x.identity === identity);
  }

  getUnconfirmedIdentities() {
    return this.values.filter(x => x[1] === false).map(x => x[0]);
  }

  getConfirmedIdentities() {
    return this.values.filter(x => x[1]).map(x => x[0]);
  }

  create(identity: Identity): void {
    const value = {
      confirmed: false,
      keyPair: identity.keyPair
    };

    localStorage.setItem(identity.identity, JSON.stringify(value));
    this.values.push([identity, false]);
  }

  confirm(identity: Identity): void {
    const json = localStorage.getItem(identity.identity);
    const value = JSON.parse(json);
    value.confirmed = true;

    localStorage.setItem(identity.identity, JSON.stringify(value));

    this.values = this.values.filter(x => x[0].identity !== identity.identity);
    this.values.push([identity, true]);
  }

  private getAll(): [Identity, boolean][] {
    const size = localStorage.length;
    const result = [];
    for (let x = 0; x < size; x++) {
      const identity = localStorage.key(x);
      const value = JSON.parse(localStorage.getItem(identity));
      result.push([new Identity(identity, value.keyPair), value.confirmed]);
    }

    return result;
  }
}
