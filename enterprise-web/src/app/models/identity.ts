import { KeyPair } from './key-pair';

export class Identity {

  constructor(
    public identity: string,
    public keyPair: KeyPair) {
  }
}
