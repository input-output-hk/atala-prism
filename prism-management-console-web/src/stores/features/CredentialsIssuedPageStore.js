import { makeAutoObservable } from 'mobx';
import CredentialsIssuedBaseStore from '../domain/CredentialsIssuedBaseStore';

export default class CredentialsIssuedPageStore {
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

  fetchMoreData() {
    return this.credentialsIssuedBaseStore.fetchMoreData();
  }

  refreshCredentials() {
    return this.credentialsIssuedBaseStore.refreshCredentials();
  }

  initCredentialsIssuedStore() {
    return this.credentialsIssuedBaseStore.initCredentialStore();
  }
}
