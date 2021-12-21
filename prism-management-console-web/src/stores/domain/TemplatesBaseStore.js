import _ from 'lodash';
import { computed, makeAutoObservable } from 'mobx';
import { SORTING_DIRECTIONS, TEMPLATES_SORTING_KEYS } from '../../helpers/constants';
import { filterByExactMatch, filterByInclusion } from '../../helpers/filterHelpers';

const { ascending, descending } = SORTING_DIRECTIONS;

const defaultValues = {
  isFetchingTemplates: false,
  isFetchingCategories: false,
  credentialTemplates: [],
  templateCategories: [],
  nameFilter: undefined,
  categoryFilter: undefined,
  lastEditedFilter: undefined,
  sortDirection: ascending,
  sortingBy: TEMPLATES_SORTING_KEYS.name
};

export default class TemplatesBaseStore {
  isFetchingTemplates = defaultValues.isFetchingTemplates;

  isFetchingCategories = defaultValues.isFetchingCategories;

  credentialTemplates = defaultValues.credentialTemplates;

  templateCategories = defaultValues.templateCategories;

  nameFilter = defaultValues.nameFilter;

  categoryFilter = defaultValues.categoryFilter;

  lastEditedFilter = defaultValues.lastEditedFilter;

  sortDirection = defaultValues.sortDirection;

  sortingBy = defaultValues.sortingBy;

  constructor(api, sessionState) {
    this.api = api;
    this.transportLayerErrorHandler = sessionState.transportLayerErrorHandler;
    this.storeName = this.constructor.name;

    makeAutoObservable(
      this,
      {
        filteredTemplates: computed({ requiresReaction: true }),
        api: false,
        transportLayerErrorHandler: false
      },
      { autoBind: true }
    );
  }

  get isLoading() {
    return this.isFetchingTemplates || this.isFetchingCategories;
  }

  *initTemplateStore(contactId) {
    this.resetTemplatesAndCategories();
    this.resetFilters();
    this.resetSorting();
    this.contactIdFilter = contactId;
    yield this.fetchTemplates();
    yield this.fetchCategories();
  }

  resetTemplatesAndCategories() {
    this.credentialTemplates = defaultValues.credentialTemplates;
    this.templateCategories = defaultValues.templateCategories;
    this.isFetchingTemplates = defaultValues.isFetchingTemplates;
    this.isFetchingCategories = defaultValues.isFetchingCategories;
  }

  resetFilters() {
    this.nameFilter = defaultValues.nameFilter;
    this.resetAdditionalFilters();
  }

  resetAdditionalFilters() {
    this.categoryFilter = defaultValues.categoryFilter;
    this.lastEditedFilter = defaultValues.lastEditedFilter;
  }

  resetSorting() {
    this.sortDirection = defaultValues.sortDirection;
    this.sortingBy = defaultValues.sortingBy;
  }

  // ********************** //
  // FILTERS
  // ********************** //

  get hasAdditionalFiltersApplied() {
    return Boolean(this.categoryFilter || this.lastEditedFilter);
  }

  get hasNameFilterApplied() {
    return Boolean(this.nameFilter);
  }

  get hasFiltersApplied() {
    return this.hasNameFilterApplied || this.hasAdditionalFiltersApplied;
  }

  get filteredTemplates() {
    const filteredTemplates = this.applyFilters(this.credentialTemplates);
    return this.applySorting(filteredTemplates);
  }

  get hasCustomSorting() {
    return this.sortingBy !== TEMPLATES_SORTING_KEYS.name || this.sortDirection !== ascending;
  }

  get filterSortingProps() {
    const {
      nameFilter,
      categoryFilter,
      lastEditedFilter,
      hasFiltersApplied,
      hasAdditionalFiltersApplied,
      sortDirection,
      sortingBy,
      setSortingBy,
      setFilterValue,
      resetAdditionalFilters,
      toggleSortDirection
    } = this;
    return {
      nameFilter,
      categoryFilter,
      lastEditedFilter,
      hasFiltersApplied,
      hasAdditionalFiltersApplied,
      sortDirection,
      sortingBy,
      setSortingBy,
      setFilterValue,
      resetAdditionalFilters,
      toggleSortDirection
    };
  }

  applyFilters(templates) {
    return templates.filter(item => {
      const matchName = filterByInclusion(this.nameFilter, item?.name);
      const matchCategory = filterByExactMatch(this.categoryFilter, item.category);
      const matchDate = filterByExactMatch(this.lastEditedFilter, item.lastEdited);

      return matchName && matchCategory && matchDate;
    });
  }

  applySorting = templates =>
    _.orderBy(
      templates,
      [t => (this.sortingIsCaseSensitive() ? t[this.sortingBy]?.toLowerCase() : t[this.sortingBy])],
      this.sortDirection === ascending ? 'asc' : 'desc'
    );

  sortingIsCaseSensitive = () => this.sortingBy === TEMPLATES_SORTING_KEYS.name;

  setFilterValue = (key, value) => {
    // TODO: check if filter is valid?
    this[key] = value;
  };

  toggleSortDirection = () => {
    this.sortDirection = this.sortDirection === ascending ? descending : ascending;
  };

  setSortingBy = value => {
    this.sortingBy = value;
  };

  // ********************** //
  // DATA AND FETCHING
  // ********************** //

  *fetchTemplates() {
    this.isFetchingTemplates = true;
    try {
      const response = yield this.api.credentialTypesManager.getCredentialTypes();
      this.credentialTemplates = response;
      this.transportLayerErrorHandler.handleTransportLayerSuccess();
    } catch (error) {
      const metadata = {
        store: this.storeName,
        method: 'fetchTemplates',
        verb: 'getting',
        model: 'Templates'
      };
      this.transportLayerErrorHandler.handleTransportLayerError(error, metadata);
    }
    this.isFetchingTemplates = false;
  }

  *getCredentialTemplateDetails(id) {
    try {
      const result = yield this.api.credentialTypesManager.getCredentialTypeDetails(id);
      this.transportLayerErrorHandler.handleTransportLayerSuccess();
      return result;
    } catch (error) {
      const metadata = {
        store: this.storeName,
        method: 'getCredentialTemplateDetails',
        verb: 'getting',
        model: 'Template Details'
      };
      this.transportLayerErrorHandler.handleTransportLayerError(error, metadata);
    }
  }

  *fetchCategories() {
    this.isFetchingCategories = true;
    try {
      const response = yield this.api.credentialTypesManager.getTemplateCategories();
      this.transportLayerErrorHandler.handleTransportLayerSuccess();
      this.templateCategories = response;
    } catch (error) {
      const metadata = {
        store: this.storeName,
        method: 'fetchCategories',
        verb: 'getting',
        model: 'Template Categories'
      };
      this.transportLayerErrorHandler.handleTransportLayerError(error, metadata);
    }
    this.isFetchingCategories = false;
  }
}
