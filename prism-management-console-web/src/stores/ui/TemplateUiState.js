import { makeAutoObservable, observable, computed, action } from 'mobx';
import { filterByExactMatch, filterByInclusion } from '../../helpers/filterHelpers';

export class TemplateUiState {
  nameFilter = '';

  categoryFilter = '';

  lastEdited = null;

  constructor(rootStore) {
    this.rootStore = rootStore;
    makeAutoObservable(this, {
      nameFilter: observable,
      categoryFilter: observable,
      lastEdited: observable,
      hasFiltersApplied: computed,
      filteredTemplates: computed,
      setNameFilter: action,
      applyFilters: false,
      rootStore: false
    });
  }

  get hasFiltersApplied() {
    return this.nameFilter || this.categoryFilter || this.lastEditedFilter;
  }

  get filteredTemplates() {
    debugger
    const templates = this.rootStore.prismStore.templateStore.credentialTemplates;
    return this.applyFilters(templates);
  }

  setNameFilter = value => {
    debugger
    this.nameFilter = value;
  }

  applyFilters = templatesList =>
    templatesList.filter(item => {
      const matchName = filterByInclusion(this.nameFilter, item.name);
      const matchCategory = filterByExactMatch(this.categoryFilter, item.category);
      const matchDate = filterByExactMatch(this.lastEditedFilter, item.lastEdited);

      return matchName && matchCategory && matchDate;
    });
}
