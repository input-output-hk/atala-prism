import ContactStore from './domain/ContactStore';
import ContactUiState from './ui/ContactUiState';
import CurrentContactState from './ui/CurrentContactState';

export class RootContactStore {
  constructor(api, sessionState) {
    this.contactStore = new ContactStore(api, sessionState, this);
    this.contactUiState = new ContactUiState(this.contactStore);
    this.currentContactState = new CurrentContactState(api, this.contactStore);
  }
}
