import { makeAutoObservable } from 'mobx';
import { CREDENTIAL_ID_KEY } from '../../helpers/constants';
import CredentialsIssuedBaseStore from '../domain/CredentialsIssuedBaseStore';

const defaultValues = {
  selectedCredentials: [],
  isLoadingSelection: false
};

export default class CredentialsIssuedPageStore {
  selectedCredentials = defaultValues.selectedCredentials;

  isLoadingSelection = defaultValues.isLoadingSelection;

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

  fetchMoreData() {
    return this.credentialsIssuedBaseStore.fetchMoreData();
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
    const entitiesToSelect = yield this.credentialsIssuedBaseStore.fetchAllCredentials();
    this.setSelection(checked, entitiesToSelect);
    this.isLoadingSelection = false;
  }

  setSelection(checked, entitiesList) {
    this.selectedCredentials = checked ? entitiesList.map(e => e[CREDENTIAL_ID_KEY]) : [];
  }

  handleCherryPickSelection(updatedPartialSelection) {
    this.selectedCredentials = this.invisibleSelectedCredentials.concat(updatedPartialSelection);
  }
}
