import { makeAutoObservable, flow, runInAction } from 'mobx';
import { credentialMapper } from '../../APIs/helpers/credentialHelpers';
import { CREDENTIAL_PAGE_SIZE, MAX_CREDENTIAL_PAGE_SIZE } from '../../helpers/constants';

const defaultValues = {
  isFetching: true,
  hasMore: true,
  credentials: []
};

const fallback = {
  credentialsList: []
};
export default class CredentialIssuedStore {
  isFetching = defaultValues.isFetching;

  credentials = defaultValues.credentials;

  hasMore = defaultValues.hasMore;

  constructor(api, rootStore) {
    this.api = api;
    this.rootStore = rootStore;
    this.transportLayerErrorHandler = rootStore.sessionState.transportLayerErrorHandler;
    this.storeName = this.constructor.name;

    makeAutoObservable(this, {
      refreshCredentials: flow.bound,
      fetchMoreData: flow.bound,
      fetchSearchResults: flow.bound,
      getCredentialsToSelect: flow.bound,
      fetchRecursively: false,
      rootStore: false
    });
  }

  get isLoadingFirstPage() {
    return this.isFetching && !this.credentials.length;
  }

  resetCredentials = () => {
    this.hasMore = defaultValues.hasMore;
    this.credentials = defaultValues.credentials;
  };

  *refreshCredentials() {
    const response = yield this.fetchRecursively({ limit: this.credentials.length });
    this.credentials = response.credentialsList;
  }

  *fetchMoreData() {
    if (!this.hasMore) return;

    const response = yield this.fetchCredentials({ offset: this.credentials.length });
    this.credentials = this.credentials.concat(response.credentialsList);
  }

  *fetchSearchResults() {
    const response = yield this.fetchCredentials({ offset: 0 });
    this.resetCredentials();
    this.credentials = this.credentials.concat(response.credentialsList);
  }

  *getCredentialsToSelect() {
    const alreadyFetched = this.credentials;

    if (!this.hasMore) return alreadyFetched;

    const response = yield this.fetchRecursively({ acc: alreadyFetched });
    this.credentials = response.credentialsList;
    return response.credentialsList;
  }

  fetchRecursively = async ({ acc = [], limit } = {}) => {
    const pageSize = Math.min(limit || Infinity, MAX_CREDENTIAL_PAGE_SIZE);
    const response = await this.fetchCredentials({
      offset: acc.length,
      pageSize
    });
    const updatedAcc = acc.concat(response.credentialsList);
    if (response.credentialsList.length < pageSize)
      return {
        credentialsList: updatedAcc
      };
    return this.fetchRecursively({ acc: updatedAcc, limit });
  };

  updateHasMoreState = (credentialsList, pageSize) => {
    if (credentialsList.length < pageSize) {
      this.hasMore = false;
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
        this.transportLayerErrorHandler.handleTransportLayerSuccess();
        this.isFetching = false;
      });
      const mappedCredentials = response.credentialsList.map(credentialMapper);
      const mappedResponse = { ...response, credentialsList: mappedCredentials };
      this.updateHasMoreState(response.credentialsList, pageSize);
      return mappedResponse;
    } catch (error) {
      const metadata = {
        store: this.storeName,
        method: 'fetchCredentials',
        verb: 'getting',
        model: 'Credentials'
      };
      runInAction(() => {
        this.transportLayerErrorHandler.handleTransportLayerError(error, metadata);
        this.isFetching = false;
      });
      return fallback;
    }
  };
}
