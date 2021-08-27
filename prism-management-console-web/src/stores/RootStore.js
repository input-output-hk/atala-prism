import { makeAutoObservable } from 'mobx';
import { PrismStore } from './PrismStore';
import { UiState } from './UiState';

export class RootStore {
  constructor(api) {
    this.prismStore = new PrismStore(api, this);
    this.uiState = new UiState(this);
  }
}
