import { makeAutoObservable, reaction } from 'mobx';
import { CREDENTIAL_ID_KEY } from '../../helpers/constants';
import CredentialsIssuedBaseStore from '../domain/CredentialsIssuedBaseStore';

const checkboxStates = {
  UNCHECKED: 'UNCHECKED',
  CHECKED: 'CHECKED',
  INDETERMINATE: 'INDETERMINATE'
};

export default class CredentialsIssuedPageStore {
  selectedCredentials = [];

  isLoadingSelection = false;

  selectAllCheckboxState = checkboxStates.UNCHECKED;

  constructor(api, sessionState) {
    this.api = api;
    this.sessionState = sessionState;
    this.credentialsIssuedBaseStore = new CredentialsIssuedBaseStore(api, sessionState);

    makeAutoObservable(
      this,
      {
        api: false,
        sessionState: false,
        credentialsIssuedBaseStore: false
      },
      { autoBind: true }
    );

    reaction(() => this.credentialsIssuedBaseStore.textFilter, this.resetSelection);
    reaction(() => this.credentialsIssuedBaseStore.credentialStatusFilter, this.resetSelection);
    reaction(() => this.credentialsIssuedBaseStore.connectionStatusFilter, this.resetSelection);
    reaction(() => this.credentialsIssuedBaseStore.credentialTypeFilter, this.resetSelection);
    reaction(() => this.credentialsIssuedBaseStore.dateFilter, this.resetSelection);
    reaction(() => this.credentialsIssuedBaseStore.sortDirection, this.resetSelection);
    reaction(() => this.credentialsIssuedBaseStore.sortingBy, this.resetSelection);
  }

  get credentials() {
    return this.credentialsIssuedBaseStore.credentials;
  }

  get hasMore() {
    return this.credentialsIssuedBaseStore.hasMore;
  }

  get filterSortingProps() {
    return this.credentialsIssuedBaseStore.filterSortingProps;
  }

  get selectAllCheckboxStateProps() {
    return {
      checked: this.selectAllCheckboxState === checkboxStates.CHECKED,
      indeterminate: this.selectAllCheckboxState === checkboxStates.INDETERMINATE
    };
  }

  get isSearching() {
    return this.credentialsIssuedBaseStore.isSearching;
  }

  get isFetching() {
    return this.credentialsIssuedBaseStore.isFetching;
  }

  get isLoadingFirstPage() {
    return this.credentialsIssuedBaseStore.isLoadingFirstPage;
  }

  get invisibleSelectedCredentials() {
    const credentialIds = this.credentials.map(c => c.credentialId);
    return this.selectedCredentials.filter(sc => !credentialIds.includes(sc));
  }

  fetchMoreData(args) {
    return this.credentialsIssuedBaseStore.fetchMoreData(args);
  }

  refreshCredentials() {
    return this.credentialsIssuedBaseStore.refreshCredentials();
  }

  initCredentialsIssuedStore() {
    return this.credentialsIssuedBaseStore.initCredentialStore();
  }

  *selectAllCredentials(ev) {
    this.isLoadingSelection = true;
    const { checked } = ev.target;
    this.selectAllCheckboxState = checked ? checkboxStates.CHECKED : checkboxStates.UNCHECKED;
    const entitiesToSelect = yield this.credentialsIssuedBaseStore.fetchAllCredentials();
    this.setSelection(checked, entitiesToSelect);
    this.isLoadingSelection = false;
  }

  setSelection(checked, entitiesList) {
    this.selectedCredentials = checked ? entitiesList.map(e => e[CREDENTIAL_ID_KEY]) : [];
  }

  resetSelection() {
    this.selectedCredentials = [];
    this.selectAllCheckboxState = checkboxStates.UNCHECKED;
    this.isLoadingSelection = false;
  }

  handleCherryPickSelection(updatedPartialSelection) {
    this.selectAllCheckboxState = checkboxStates.INDETERMINATE;
    this.selectedCredentials = this.invisibleSelectedCredentials.concat(updatedPartialSelection);
  }
}
