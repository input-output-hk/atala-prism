import GroupStore from './domain/GroupStore';
import CurrentGroupState from './ui/CurrentGroupState';
import GroupUiState from './ui/GroupUiState';

export class RootGroupStore {
  constructor(api, sessionState) {
    this.groupStore = new GroupStore(api, sessionState, this);
    this.groupUiState = new GroupUiState(this.groupStore);
    this.currentGroupState = new CurrentGroupState(api, this.groupStore);
  }
}
