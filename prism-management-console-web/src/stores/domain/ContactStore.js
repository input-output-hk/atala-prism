import { makeAutoObservable, observable, flow, computed, action, runInAction } from 'mobx';
import { contactMapper } from '../../APIs/helpers/contactHelpers';
import { CONTACT_PAGE_SIZE, MAX_CONTACT_PAGE_SIZE } from '../../helpers/constants';

const defaultValues = {
  isFetching: false,
  contacts: [],
  searchResults: [],
  contactsScrollId: undefined,
  resultsScrollId: undefined
};

const fallback = {
  contactsList: [],
  newScrollId: undefined
};
export default class ContactStore {
  isFetching = defaultValues.isFetching;

  contacts = defaultValues.contacts;

  contactsScrollId = defaultValues.contactsScrollId;

  searchResults = defaultValues.searchResults;

  resultsScrollId = defaultValues.resultsScrollId;

  constructor(api, rootStore) {
    this.api = api;
    this.rootStore = rootStore;
    this.storeName = this.constructor.name;

    makeAutoObservable(this, {
      isFetching: observable,
      contacts: observable,
      contactsScrollId: observable,
      searchResults: observable,
      resultsScrollId: observable,
      isLoadingFirstPage: computed,
      scrollId: computed,
      hasMore: computed,
      hasMoreContacts: computed,
      hasMoreResults: computed,
      fetchMoreData: computed,
      resetContacts: action,
      fetchContactsNextPage: flow.bound,
      fetchSearchResults: flow.bound,
      fetchSearchResultsNextPage: flow.bound,
      updateFetchedResults: action,
      fetchAllContacts: flow.bound,
      fetchAllFilteredContacts: flow.bound,
      fetchContacts: action,
      fetchRecursively: false,
      rootStore: false
    });
  }

  get isLoadingFirstPage() {
    return this.isFetching && this.contactsScrollId === undefined;
  }

  get scrollId() {
    const { hasFiltersApplied, hasCustomSorting } = this.rootStore.uiState.contactUiState;
    return hasFiltersApplied || hasCustomSorting ? this.resultsScrollId : this.contactsScrollId;
  }

  get hasMore() {
    const { hasFiltersApplied } = this.rootStore.uiState.contactUiState;
    return hasFiltersApplied ? this.hasMoreResults : this.hasMoreContacts;
  }

  get hasMoreContacts() {
    return Boolean(this.contactsScrollId);
  }

  get hasMoreResults() {
    return Boolean(this.resultsScrollId);
  }

  get fetchMoreData() {
    const { hasFiltersApplied, hasCustomSorting } = this.rootStore.uiState.contactUiState;
    return hasFiltersApplied || hasCustomSorting
      ? this.fetchSearchResultsNextPage
      : this.fetchContactsNextPage;
  }

  resetContacts = () => {
    this.isFetching = defaultValues.isFetching;
    this.contacts = defaultValues.contacts;
    this.searchResults = defaultValues.searchResults;
  };

  refreshContacts = () => {
    // TODO: implement
  };

  *fetchContactsNextPage() {
    if (!this.hasMoreContacts && this.isLoadingFirstPage) return;
    const response = yield this.fetchContacts({ scrollId: this.contactsScrollId });
    this.contacts = this.contacts.concat(response.contactsList);
    this.contactsScrollId = response.newScrollId;
  }

  *fetchSearchResults() {
    const { hasFiltersApplied, hasCustomSorting } = this.rootStore.uiState.contactUiState;
    if (!hasFiltersApplied && !hasCustomSorting) return;

    this.searchResults = [];
    this.numberOfResults = 0;
    const response = yield this.fetchContacts({ scrollId: '' });
    this.searchResults = response.contactsList;
    this.resultsScrollId = response.newScrollId;
    return this.searchResults;
  }

  *fetchSearchResultsNextPage() {
    if (!this.hasMoreResults) return;
    const response = yield this.fetchContacts({ scrollId: this.resultsScrollId });
    const { updateFetchedResults } = this.rootStore.uiState.contactUiState;
    this.searchResults = this.searchResults.concat(response.contactsList);
    this.resultsScrollId = response.newScrollId;
    updateFetchedResults();
  }

  *getContactsToSelect() {
    const { hasFiltersApplied } = this.rootStore.uiState.contactUiState;
    const alreadyFetched = hasFiltersApplied ? this.searchResults : this.contacts;

    if (!this.hasMore) return alreadyFetched;

    const response = yield this.fetchRecursively(alreadyFetched);
    this.updateStoredContacts(response);
    return response.contactsList;
  }

  updateStoredContacts = response => {
    const { hasFiltersApplied, updateFetchedResults } = this.rootStore.uiState.contactUiState;
    if (hasFiltersApplied) {
      this.searchResults = response.contactsList;
      this.resultsScrollId = '';
      updateFetchedResults();
    } else {
      this.contacts = response.contactsList;
      this.contactsScrollId = '';
    }
  };

  fetchRecursively = async (acc, scrollId) => {
    const response = await this.fetchContacts({
      scrollId,
      pageSize: MAX_CONTACT_PAGE_SIZE
    });
    const updatedAcc = acc.concat(response.contactsList);
    return response.newScrollId
      ? this.fetchRecursively(updatedAcc, response.newScrollId)
      : { contactsList: updatedAcc };
  };

  fetchContacts = async ({ scrollId, pageSize = CONTACT_PAGE_SIZE } = {}) => {
    this.isFetching = true;
    try {
      const {
        textFilter,
        dateFilter,
        statusFilter,
        sortDirection,
        sortingBy
      } = this.rootStore.uiState.contactUiState;

      const response = await this.api.contactsManager.getContacts({
        scrollId,
        pageSize,
        sort: { field: sortingBy, direction: sortDirection },
        filter: {
          searchText: textFilter,
          createdAt: dateFilter,
          connectionStatus: statusFilter
        }
      });
      runInAction(() => {
        this.rootStore.handleTransportLayerSuccess();
        this.isFetching = false;
      });
      const contactsWithKey = response.contactsList.map(contactMapper);
      const mappedResponse = { ...response, contactsList: contactsWithKey };
      return mappedResponse || fallback;
    } catch (error) {
      const metadata = {
        store: this.storeName,
        method: 'fetchContacts',
        verb: 'getting',
        model: 'Contacts'
      };
      runInAction(() => {
        this.rootStore.handleTransportLayerError(error, metadata);
        this.isFetching = false;
      });
    }
  };
}
