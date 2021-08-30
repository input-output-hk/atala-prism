import { makeAutoObservable, observable, computed, action } from 'mobx';
import _ from 'lodash';
import { filterByExactMatch, filterByInclusion } from '../../helpers/filterHelpers';
import { SORTING_DIRECTIONS, TEMPLATES_SORTING_KEYS } from '../../helpers/constants';

const { ascending, descending } = SORTING_DIRECTIONS;
export class TemplateUiState {
  nameFilter = '';

  categoryFilter = null;

  lastEditedFilter = null;

  sortDirection = ascending;

  sortingBy = TEMPLATES_SORTING_KEYS.name;

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
      setNameFilter: action,
      applyFilters: false,
      rootStore: false
    });
  }

  get hasAditionalFiltersApplied() {
    return Boolean(this.categoryFilter || this.lastEditedFilter);
  }

  get hasFiltersApplied() {
    return this.hasNameFilterApplied || this.hasAditionalFiltersApplied;
  }

  get filteredTemplates() {
    const templates = this.rootStore.prismStore.templateStore.credentialTemplates;
    const filteredTemplates = templates.filter(item => {
      const matchName = filterByInclusion(this.nameFilter, item.name);
      const matchCategory = filterByExactMatch(this.categoryFilter, item.category);
      const matchDate = filterByExactMatch(this.lastEditedFilter, item.lastEdited);

      return matchName && matchCategory && matchDate;
    });

    const sortedAndFilteredTemplates = _.orderBy(
      filteredTemplates,
      [o => (this.sortingIsCaseSensitive() ? o[this.sortingBy].toLowerCase() : o[this.sortingBy])],
      this.sortDirection === ascending ? 'asc' : 'desc'
    );
    return sortedAndFilteredTemplates;
  }

  sortingIsCaseSensitive = () => this.sortingBy === TEMPLATES_SORTING_KEYS.name;

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
