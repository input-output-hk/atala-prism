import { PrismStore } from './domain/PrismStore';
import { UiState } from './ui/UiState';

export class RootStore {
  constructor(api, sessionState) {
    this.sessionState = sessionState;
    this.prismStore = new PrismStore(api, this);
    this.uiState = new UiState(api, this);
  }
}
