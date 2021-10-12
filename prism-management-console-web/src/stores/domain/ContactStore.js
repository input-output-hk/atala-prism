import { makeAutoObservable, observable, flow, computed, action, runInAction } from 'mobx';
import { contactMapper } from '../../APIs/helpers/contactHelpers';
import { CONTACT_PAGE_SIZE, MAX_GROUP_PAGE_SIZE } from '../../helpers/constants';

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
      rootStore: false
    });
  }

  get isLoadingFirstPage() {
    return this.isFetching && this.scrollId === undefined;
  }

  get scrollId() {
    const { hasFiltersApplied } = this.rootStore.uiState.contactUiState;
    return hasFiltersApplied ? this.resultsScrollId : this.contactsScrollId;
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
    const response = yield this.fetchContacts({ offset: this.contacts.length });
    const contactsWithKey = response.contactsList.map(contactMapper);
    this.contacts = this.contacts.concat(contactsWithKey);
    this.contactsScrollId = response.newScrollId;
  }

  *fetchSearchResults() {
    const { hasFiltersApplied, hasCustomSorting } = this.rootStore.uiState.contactUiState;
    if (!hasFiltersApplied && !hasCustomSorting) return;

    this.searchResults = [];
    this.numberOfResults = 0;
    const response = yield this.fetchContacts({ offset: 0 });
    this.searchResults = response.contactsList;
    this.resultsScrollId = response.resultsScrollId;
    return this.searchResults;
  }

  *fetchSearchResultsNextPage() {
    if (!this.hasMoreResults) return;
    const response = yield this.fetchContacts({ offset: this.searchResults.length });
    const { updateFetchedResults } = this.rootStore.uiState.contactUiState;
    this.searchResults = this.searchResults.concat(response.contactsList);
    this.resultsScrollId = response.resultsScrollId;
    updateFetchedResults();
  }

  *fetchAllContacts() {
    if (!this.hasMoreContacts) return this.contacts;
    let contactsAcc = this.contacts;
    const fetchRecursively = async () => {
      const response = await this.fetchContacts({
        offset: contactsAcc.length,
        pageSize: MAX_GROUP_PAGE_SIZE
      });
      contactsAcc = contactsAcc.concat(response.contactsList);
      if (contactsAcc.length >= response.totalNumberOfContacts)
        return { contactsList: contactsAcc, totalNumberOfContacts: response.totalNumberOfContacts };
      return fetchRecursively();
    };

    const response = yield fetchRecursively();
    runInAction(() => {
      this.contacts = response.contactsList;
      this.numberOfContacts = response.totalNumberOfContacts;
    });
    return this.contacts;
  }

  *fetchAllFilteredContacts() {
    if (!this.hasMoreResults) return this.searchResults;
    let contactsAcc = this.searchResults;
    const fetchRecursively = async () => {
      const response = await this.fetchContacts({
        offset: contactsAcc.length,
        pageSize: MAX_GROUP_PAGE_SIZE
      });
      contactsAcc = contactsAcc.concat(response.contactsList);
      if (contactsAcc.length >= response.totalNumberOfContacts)
        return { contactsList: contactsAcc, totalNumberOfContacts: response.totalNumberOfContacts };
      return fetchRecursively();
    };

    const response = yield fetchRecursively();
    runInAction(() => {
      this.searchResults = response.contactsList;
      this.numberOfResults = response.totalNumberOfContacts;
      const { updateFetchedResults } = this.rootStore.uiState.contactUiState;
      updateFetchedResults();
    });
    return this.searchResults;
  }

  fetchContacts = async ({ pageSize = CONTACT_PAGE_SIZE }) => {
    this.isFetching = true;
    try {
      const {
        nameFilter,
        dateFilter = [],
        sortDirection,
        sortingBy
      } = this.rootStore.uiState.contactUiState;
      const [createdAfter, createdBefore] = dateFilter;

      const response = await this.api.contactsManager.getContacts({
        scrollId: this.scrollId,
        pageSize,
        sort: { field: sortingBy, direction: sortDirection },
        filter: {
          name: nameFilter,
          createdBefore,
          createdAfter
        }
      });
      runInAction(() => {
        this.rootStore.handleTransportLayerSuccess();
        this.isFetching = false;
      });
      return response || fallback;
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
