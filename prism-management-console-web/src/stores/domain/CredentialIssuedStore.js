import { makeAutoObservable, observable, flow, computed, action, runInAction } from 'mobx';
import { credentialMapper } from '../../APIs/helpers/credentialHelpers';
import { CREDENTIAL_PAGE_SIZE, MAX_CREDENTIAL_PAGE_SIZE } from '../../helpers/constants';

const defaultValues = {
  isFetching: false,
  hasMoreCredentials: true,
  hasMoreResults: true,
  credentials: [],
  searchResults: []
};

const fallback = {
  credentialsList: []
};
export default class CredentialIssuedStore {
  isFetching = defaultValues.isFetching;

  credentials = defaultValues.credentials;

  searchResults = defaultValues.searchResults;

  constructor(api, rootStore) {
    this.api = api;
    this.rootStore = rootStore;
    this.storeName = this.constructor.name;

    makeAutoObservable(this, {
      isFetching: observable,
      isSaving: observable,
      credentials: observable,
      searchResults: observable,
      isLoadingFirstPage: computed,
      hasMore: computed,
      hasMoreCredentials: observable,
      hasMoreResults: observable,
      fetchMoreData: computed,
      resetCredentials: action,
      fetchCredentialsNextPage: flow.bound,
      fetchSearchResults: flow.bound,
      fetchSearchResultsNextPage: flow.bound,
      getCredentialsToSelect: flow.bound,
      fetchCredentials: action,
      updateCredentialName: flow.bound,
      updateCredentialMembers: flow.bound,
      updateCredential: action,
      updateHasMoreState: action,
      fetchRecursively: false,
      rootStore: false
    });
  }

  get isLoadingFirstPage() {
    return this.isFetching && !this.credentials.length;
  }

  get hasMore() {
    const { hasFiltersApplied } = this.rootStore.uiState.credentialIssuedUiState;
    return hasFiltersApplied ? this.hasMoreResults : this.hasMoreCredentials;
  }

  get fetchMoreData() {
    const { hasFiltersApplied, hasCustomSorting } = this.rootStore.uiState.credentialIssuedUiState;
    return hasFiltersApplied || hasCustomSorting
      ? this.fetchSearchResultsNextPage
      : this.fetchCredentialsNextPage;
  }

  resetCredentials = () => {
    this.isFetching = defaultValues.isFetching;
    this.hasMoreCredentials = defaultValues.hasMoreCredentials;
    this.hasMoreResults = defaultValues.hasMoreResults;
    this.credentials = defaultValues.credentials;
    this.searchResults = defaultValues.searchResults;
  };

  *refreshCredentialsIssued() {
    const pageSizeIsWithinBoundary = this.credentials.length <= MAX_CREDENTIAL_PAGE_SIZE;
    const response = pageSizeIsWithinBoundary
      ? yield this.fetchCredentials({ offset: 0, pageSize: this.credentials.length })
      : yield this.fetchRecursively();
    this.credentials = response.credentialsList;
  }

  *fetchCredentialsNextPage() {
    if (!this.hasMoreCredentials && this.isLoadingFirstPage) return;
    const response = yield this.fetchCredentials({ offset: this.credentials.length });
    this.credentials = this.credentials.concat(response.credentialsList);
  }

  *fetchSearchResults() {
    const { hasFiltersApplied, hasCustomSorting } = this.rootStore.uiState.credentialIssuedUiState;
    if (!hasFiltersApplied && !hasCustomSorting) return;

    this.searchResults = [];
    this.hasMoreResults = true;
    const response = yield this.fetchCredentials({ offset: 0 });
    this.searchResults = response.credentialsList;
    return this.searchResults;
  }

  *fetchSearchResultsNextPage() {
    if (!this.hasMoreResults) return;
    const response = yield this.fetchCredentials({ offset: this.searchResults.length });
    this.searchResults = this.searchResults.concat(response.credentialsList);
  }

  *getCredentialsToSelect() {
    const { hasFiltersApplied } = this.rootStore.uiState.credentialIssuedUiState;
    const alreadyFetched = hasFiltersApplied ? this.searchResults : this.credentials;

    if (!this.hasMore) return alreadyFetched;

    const response = yield this.fetchRecursively(alreadyFetched);
    this.updateStoredCredentials(response);
    return response.credentialsList;
  }

  updateStoredCredentials = response => {
    const { hasFiltersApplied } = this.rootStore.uiState.credentialIssuedUiState;
    if (hasFiltersApplied) {
      this.searchResults = response.credentialsList;
    } else {
      this.credentials = response.credentialsList;
    }
  };

  fetchRecursively = async (acc = [], limit) => {
    const response = await this.fetchCredentials({
      offset: acc.length,
      pageSize: MAX_CREDENTIAL_PAGE_SIZE
    });
    const updatedAcc = acc.concat(response.credentialsList);
    if (response.credentialsList.length >= MAX_CREDENTIAL_PAGE_SIZE)
      return {
        credentialsList: updatedAcc
      };
    return this.fetchRecursively(updatedAcc, limit);
  };

  updateHasMoreState = (credentialsList, pageSize) => {
    const { hasFiltersApplied } = this.rootStore.uiState.credentialIssuedUiState;
    if (credentialsList.length < pageSize) {
      if (hasFiltersApplied) this.hasMoreResults = false;
      else this.hasMoreCredentials = false;
    }
  };

  fetchCredentials = async ({ offset = 0, pageSize = CREDENTIAL_PAGE_SIZE } = {}) => {
    this.isFetching = true;
    try {
      const {
        // TODO: implement missing filters on the backend
        // nameFilter,
        // credentialStatusFilter,
        // connectionStatusFilter,
        credentialTypeFilter,
        dateFilter = [],
        sortDirection,
        sortingBy
      } = this.rootStore.uiState.credentialIssuedUiState;
      const response = await this.api.credentialsManager.getCredentials({
        offset,
        pageSize,
        sort: { field: sortingBy, direction: sortDirection },
        filter: {
          credentialType: credentialTypeFilter,
          date: dateFilter
        }
      });
      runInAction(() => {
        this.rootStore.handleTransportLayerSuccess();
        this.isFetching = false;
      });
      const mappedCredentials = response.credentialsList.map(credentialMapper);
      const mappedResponse = { ...response, credentialsList: mappedCredentials };
      this.updateHasMoreState(response.credentialsList, pageSize);
      return mappedResponse || fallback;
    } catch (error) {
      const metadata = {
        store: this.storeName,
        method: 'fetchCredentials',
        verb: 'getting',
        model: 'Credentials'
      };
      runInAction(() => {
        this.rootStore.handleTransportLayerError(error, metadata);
        this.isFetching = false;
      });
      return fallback;
    }
  };

  updateCredential = async (id, change) => {
    this.isSaving = true;
    try {
      const response = await this.api.credentialsManager.updateCredential(id, change);
      runInAction(() => {
        this.rootStore.handleTransportLayerSuccess();
        this.isSaving = false;
      });
      return response;
    } catch (error) {
      const metadata = {
        store: this.storeName,
        method: 'updateCredential',
        verb: 'saving',
        model: 'Credential'
      };
      runInAction(() => {
        this.rootStore.handleTransportLayerError(error, metadata);
        this.isSaving = false;
      });
    }
  };

  createCredential = async ({ name, members }) => {
    const newCredential = await this.api.credentialsManager.createCredential(name);
    if (members) await this.updateCredential(newCredential.id, { contactIdsToAdd: members });
  };
}
