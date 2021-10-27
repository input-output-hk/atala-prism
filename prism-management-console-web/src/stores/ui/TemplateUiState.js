import { makeAutoObservable, observable, computed, action } from 'mobx';
import { computedFn } from 'mobx-utils';
import _ from 'lodash';
import { filterByExactMatch, filterByInclusion } from '../../helpers/filterHelpers';
import { SORTING_DIRECTIONS, TEMPLATES_SORTING_KEYS } from '../../helpers/constants';

const { ascending, descending } = SORTING_DIRECTIONS;

const defaultValues = {
  nameFilter: '',
  categoryFilter: null,
  lastEditedFilter: null,
  sortDirection: ascending,
  sortingBy: TEMPLATES_SORTING_KEYS.name
};

export default class TemplateUiState {
  nameFilter = defaultValues.nameFilter;

  categoryFilter = defaultValues.categoryFilter;

  lastEditedFilter = defaultValues.lastEditedFilter;

  sortDirection = defaultValues.sortDirection;

  sortingBy = defaultValues.sortingBy;

  constructor(rootStore) {
    this.rootStore = rootStore;
    makeAutoObservable(this, {
      nameFilter: observable,
      categoryFilter: observable,
      lastEditedFilter: observable,
      hasFiltersApplied: computed,
      hasNameFilterApplied: computed,
      hasAditionalFiltersApplied: computed,
      filteredTemplates: computed({ requiresReaction: true }),
      toggleSortDirection: action,
      setNameFilter: action,
      resetState: action,
      applyFilters: false,
      applySorting: false,
      sortingIsCaseSensitive: false,
      rootStore: false
    });
  }

  get hasAditionalFiltersApplied() {
    return Boolean(this.categoryFilter || this.lastEditedFilter);
  }

  get hasNameFilterApplied() {
    return Boolean(this.nameFilter);
  }

  get hasFiltersApplied() {
    return this.hasNameFilterApplied || this.hasAditionalFiltersApplied;
  }

  get filteredTemplates() {
    const templates = this.rootStore.prismStore.templateStore.credentialTemplates;
    const filteredTemplates = this.applyFilters(templates);
    return this.applySorting(filteredTemplates);
  }

  filteredTemplatesByCategory = computedFn(category =>
    this.filteredTemplates.filter(ct => category.id === ct.category)
  );

  applyFilters = templates =>
    templates.filter(item => {
      const matchName = filterByInclusion(this.nameFilter, item.name);
      const matchCategory = filterByExactMatch(this.categoryFilter, item.category);
      const matchDate = filterByExactMatch(this.lastEditedFilter, item.lastEdited);

      return matchName && matchCategory && matchDate;
    });

  applySorting = templates =>
    _.orderBy(
      templates,
      [o => (this.sortingIsCaseSensitive() ? o[this.sortingBy].toLowerCase() : o[this.sortingBy])],
      this.sortDirection === ascending ? 'asc' : 'desc'
    );

  sortingIsCaseSensitive = () => this.sortingBy === TEMPLATES_SORTING_KEYS.name;

  resetState = () => {
    this.nameFilter = defaultValues.nameFilter;
    this.categoryFilter = defaultValues.categoryFilter;
    this.lastEditedFilter = defaultValues.lastEditedFilter;
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
