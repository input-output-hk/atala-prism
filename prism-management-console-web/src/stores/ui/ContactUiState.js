import { makeAutoObservable, observable, computed, action } from 'mobx';
import _ from 'lodash';
import {
  filterByDateRange,
  filterByExactMatch,
  filterByMultipleKeys
} from '../../helpers/filterHelpers';
import {
  CONTACT_SORTING_KEYS,
  CONTACT_SORTING_KEYS_TRANSLATION,
  SEARCH_DELAY_MS,
  SORTING_DIRECTIONS
} from '../../helpers/constants';

const { ascending, descending } = SORTING_DIRECTIONS;

const defaultValues = {
  isSearching: false,
  isSorting: false,
  textFilter: '',
  statusFilter: '',
  dateFilter: '',
  sortDirection: ascending,
  sortingBy: CONTACT_SORTING_KEYS.name,
  fetchedResults: null
};
export default class ContactUiState {
  isSearching = defaultValues.isSearching;

  isSorting = defaultValues.isSorting;

  textFilter = defaultValues.textFilter;

  statusFilter = defaultValues.statusFilter;

  dateFilter = defaultValues.dateFilter;

  sortDirection = defaultValues.sortDirection;

  sortingBy = defaultValues.sortingBy;

  fetchedResults = defaultValues.fetchedResults;

  constructor(rootStore) {
    this.rootStore = rootStore;
    makeAutoObservable(this, {
      isSearching: observable,
      isSorting: observable,
      textFilter: observable,
      statusFilter: observable,
      dateFilter: observable,
      sortDirection: observable,
      sortingBy: observable,
      fetchedResults: observable,
      sortingKey: computed,
      hasFiltersApplied: computed,
      hasTextFilterApplied: computed,
      hasDateFilterApplied: computed,
      hasCustomSorting: computed,
      displayedContacts: computed,
      sortedFilteredContacts: computed({ requiresReaction: true }),
      triggerSearch: action,
      triggerBackendSearch: action.bound,
      applyFilters: false,
      applySorting: false,
      sortingIsCaseSensitive: false,
      resetState: action,
      setFilterValue: action,
      toggleSortDirection: action,
      setSortingBy: action,
      rootStore: false
    });
  }

  get sortingKey() {
    return CONTACT_SORTING_KEYS_TRANSLATION[this.sortingBy];
  }

  get hasFiltersApplied() {
    return this.hasTextFilterApplied || this.hasDateFilterApplied || this.hasStatusFilterApplied;
  }

  get hasTextFilterApplied() {
    return Boolean(this.textFilter);
  }

  get hasDateFilterApplied() {
    return Boolean(this.dateFilter);
  }

  get hasStatusFilterApplied() {
    return Boolean(this.statusFilter);
  }

  get hasCustomSorting() {
    return (
      this.sortingBy !== defaultValues.sortingBy ||
      this.sortDirection !== defaultValues.sortDirection
    );
  }

  get displayedContacts() {
    const { contacts } = this.rootStore.prismStore.contactStore;
    return this.hasFiltersApplied || this.hasCustomSorting ? this.sortedFilteredContacts : contacts;
  }

  get sortedFilteredContacts() {
    if (this.fetchedResults) return this.fetchedResults;
    const { contacts, searchResults } = this.rootStore.prismStore.contactStore;
    const allFetchedContacts = contacts.concat(searchResults);
    const contactsToFilter = _.uniqBy(allFetchedContacts, c => c.contactId);
    const unsortedFilteredContacts = this.applyFilters(contactsToFilter);
    const sortedFilteredContacts = this.applySorting(unsortedFilteredContacts);
    return sortedFilteredContacts;
  }

  triggerSearch = () => {
    this.isSearching = this.hasFiltersApplied;
    this.isSorting = this.hasCustomSorting;
    this.fetchedResults = null;
    this.triggerBackendSearch();
  };

  triggerBackendSearch = _.debounce(async () => {
    const { fetchSearchResults } = this.rootStore.prismStore.contactStore;
    await fetchSearchResults();
    this.updateFetchedResults();
  }, SEARCH_DELAY_MS);

  updateFetchedResults = () => {
    const { searchResults } = this.rootStore.prismStore.contactStore;
    this.fetchedResults = searchResults;
    this.isSearching = false;
    this.isSorting = false;
  };

  applyFilters = contacts =>
    contacts.filter(item => {
      const matchText =
        !this.hasTextFilterApplied ||
        filterByMultipleKeys(this.textFilter, item, ['name', 'externalId']);
      const matchDate =
        !this.hasDateFilterApplied || filterByDateRange(this.dateFilter, item.createdAt);
      const matchStatus =
        !this.hasStatusFilterApplied ||
        filterByExactMatch(this.statusFilter, item.connectionStatus);

      return matchText && matchDate && matchStatus;
    });

  applySorting = contacts =>
    _.orderBy(
      contacts,
      [
        contact =>
          this.sortingIsCaseSensitive()
            ? contact[this.sortingKey].toLowerCase()
            : contact[this.sortingKey]
      ],
      this.sortDirection === ascending ? 'asc' : 'desc'
    );

  sortingIsCaseSensitive = () => this.sortingBy === CONTACT_SORTING_KEYS.name;

  resetState = () => {
    this.isSearching = defaultValues.isSearching;
    this.textFilter = defaultValues.textFilter;
    this.dateFilter = defaultValues.lastEditedFilter;
    this.sortDirection = defaultValues.sortDirection;
    this.sortingBy = defaultValues.sortingBy;
    this.fetchedResults = defaultValues.fetchedResults;
  };

  setFilterValue = (key, value) => {
    this[key] = value;
  };

  toggleSortDirection = () => {
    this.sortDirection = this.sortDirection === ascending ? descending : ascending;
  };

  setSortingBy = value => {
    this.sortingBy = value;
  };
}
