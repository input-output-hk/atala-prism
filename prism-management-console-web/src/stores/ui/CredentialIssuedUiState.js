import { makeAutoObservable, action, runInAction } from 'mobx';
import _ from 'lodash';
import { message } from 'antd';
import i18n from 'i18next';
import {
  CREDENTIAL_SORTING_KEYS_TRANSLATION,
  SEARCH_DELAY_MS,
  SORTING_DIRECTIONS
} from '../../helpers/constants';

const { ascending, descending } = SORTING_DIRECTIONS;
const { CREATED_ON, CONTACT_NAME, EXTERNAL_ID, DATE_SIGNED } = CREDENTIAL_SORTING_KEYS_TRANSLATION;
const unsupportedSorting = [CONTACT_NAME, EXTERNAL_ID, DATE_SIGNED];

const defaultValues = {
  isSearching: false,
  isSorting: false,
  searchTextFilter: undefined,
  credentialTypeFilter: undefined,
  credentialStatusFilter: undefined,
  connectionStatusFilter: undefined,
  dateFilter: null,
  sortDirection: ascending,
  sortingBy: CREATED_ON
};
export default class CredentialIssuedUiState {
  isSearching = defaultValues.isSearching;

  isSorting = defaultValues.isSorting;

  searchTextFilter = defaultValues.searchTextFilter;

  credentialStatusFilter = defaultValues.credentialStatusFilter;

  connectionStatusFilter = defaultValues.connectionStatusFilter;

  dateFilter = defaultValues.dateFilter;

  credentialTypeFilter = defaultValues.credentialTypeFilter;

  sortDirection = defaultValues.sortDirection;

  sortingBy = defaultValues.sortingBy;

  constructor(rootStore) {
    this.rootStore = rootStore;
    makeAutoObservable(this, {
      triggerBackendSearch: action.bound,
      sortingIsCaseSensitive: false,
      getCredentialValue: false,
      rootStore: false
    });
  }

  get sortingKey() {
    return this.sortingBy;
  }

  get hasFiltersApplied() {
    return this.hasSearchTextFilterApplied || this.hasAditionalFiltersApplied;
  }

  get hasAditionalFiltersApplied() {
    return (
      this.hasDateFilterApplied ||
      this.hasCredentiaTypeFilter ||
      this.hasCredentialStatusFilterApplied ||
      this.hasConnectionStatusFilterApplied
    );
  }

  get hasSearchTextFilterApplied() {
    return Boolean(this.searchTextFilter);
  }

  get hasDateFilterApplied() {
    return Boolean(this.dateFilter);
  }

  get hasCredentialStatusFilterApplied() {
    return Boolean(this.credentialStatusFilter);
  }

  get hasConnectionStatusFilterApplied() {
    return Boolean(this.connectionStatusFilter);
  }

  get hasCredentiaTypeFilter() {
    return Boolean(this.credentialTypeFilter);
  }

  get hasCustomSorting() {
    return (
      this.sortingBy !== defaultValues.sortingBy ||
      this.sortDirection !== defaultValues.sortDirection
    );
  }

  handleUnsupportedFilters = () => {
    const unsupportedFilters = {
      searchTextFilter: this.searchTextFilter,
      credentialStatusFilter: this.credentialStatusFilter,
      connectionStatusFilter: this.connectionStatusFilter
    };

    Object.keys(unsupportedFilters)
      .filter(key => Boolean(unsupportedFilters[key]))
      .map(key =>
        message.warn(
          i18n.t('errors.filtersNotSupported', { key: i18n.t(`credentials.filters.${key}`) })
        )
      );
  };

  handleUnsupportedSorting = () => {
    if (unsupportedSorting.includes(this.sortingBy)) {
      message.warn(
        i18n.t('errors.sortingNotSupported', {
          key: i18n.t(`credentials.table.columns.${this.sortingBy}`)
        })
      );
    }
  };

  triggerSearch = () => {
    this.isSearching = true;
    this.isSorting = true;
    this.triggerBackendSearch();
  };

  triggerBackendSearch = _.debounce(async () => {
    const { fetchSearchResults } = this.rootStore.prismStore.credentialIssuedStore;
    this.handleUnsupportedFilters();
    this.handleUnsupportedSorting();
    await fetchSearchResults();
    runInAction(() => {
      this.isSearching = false;
      this.isSorting = false;
    });
  }, SEARCH_DELAY_MS);

  resetState = () => {
    this.isSearching = defaultValues.isSearching;
    this.searchTextFilter = defaultValues.searchTextFilter;
    this.credentialTypeFilter = defaultValues.credentialTypeFilter;
    this.credentialStatusFilter = defaultValues.credentialStatusFilter;
    this.connectionStatusFilter = defaultValues.connectionStatusFilter;
    this.dateFilter = defaultValues.dateFilter;
    this.sortDirection = defaultValues.sortDirection;
    this.sortingBy = defaultValues.sortingBy;
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
