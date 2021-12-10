import { makeAutoObservable, reaction } from 'mobx';
import { CREDENTIAL_ID_KEY } from '../../helpers/constants';
import CredentialsIssuedBaseStore from '../domain/CredentialsIssuedBaseStore';
import TemplatesBaseStore from '../domain/TemplatesBaseStore';

const checkboxStates = {
  UNCHECKED: 'UNCHECKED',
  CHECKED: 'CHECKED',
  INDETERMINATE: 'INDETERMINATE'
};

export default class CredentialsIssuedPageStore {
  selectedCredentials = [];

  isLoadingSelection = false;

  selectAllCheckboxState = checkboxStates.UNCHECKED;

  constructor(api, sessionState) {
    this.api = api;
    this.sessionState = sessionState;
    this.credentialsIssuedBaseStore = new CredentialsIssuedBaseStore(api, sessionState);
    this.templatesBaseStore = new TemplatesBaseStore(api, sessionState);

    makeAutoObservable(
      this,
      {
        api: false,
        sessionState: false,
        credentialsIssuedBaseStore: false
      },
      { autoBind: true }
    );

    reaction(() => this.credentialsIssuedBaseStore.textFilter, this.resetSelection);
    reaction(() => this.credentialsIssuedBaseStore.credentialStatusFilter, this.resetSelection);
    reaction(() => this.credentialsIssuedBaseStore.connectionStatusFilter, this.resetSelection);
    reaction(() => this.credentialsIssuedBaseStore.credentialTypeFilter, this.resetSelection);
    reaction(() => this.credentialsIssuedBaseStore.dateFilter, this.resetSelection);
    reaction(() => this.credentialsIssuedBaseStore.sortDirection, this.resetSelection);
    reaction(() => this.credentialsIssuedBaseStore.sortingBy, this.resetSelection);
  }

  get credentials() {
    return this.credentialsIssuedBaseStore.credentials;
  }

  get hasMore() {
    return this.credentialsIssuedBaseStore.hasMore;
  }

  get filterSortingProps() {
    return this.credentialsIssuedBaseStore.filterSortingProps;
  }

  get selectAllCheckboxStateProps() {
    return {
      checked: this.selectAllCheckboxState === checkboxStates.CHECKED,
      indeterminate: this.selectAllCheckboxState === checkboxStates.INDETERMINATE
    };
  }

  get isSearching() {
    return this.credentialsIssuedBaseStore.isSearching;
  }

  get isFetching() {
    return this.credentialsIssuedBaseStore.isFetching;
  }

  get isLoadingFirstPage() {
    return this.credentialsIssuedBaseStore.isLoadingFirstPage;
  }

  fetchMoreData(args) {
    return this.credentialsIssuedBaseStore.fetchMoreData(args);
  }

  refreshCredentials() {
    return this.credentialsIssuedBaseStore.refreshCredentials();
  }

  *initCredentialsIssuedStore() {
    yield this.credentialsIssuedBaseStore.initCredentialStore();
    yield this.templatesBaseStore.initTemplateStore();
  }

  *selectAllCredentials(ev) {
    this.isLoadingSelection = true;
    const { checked } = ev.target;
    this.selectAllCheckboxState = checked ? checkboxStates.CHECKED : checkboxStates.UNCHECKED;
    const entitiesToSelect = checked
      ? yield this.credentialsIssuedBaseStore.fetchAllCredentials()
      : [];
    this.selectedCredentials = entitiesToSelect.map(e => e[CREDENTIAL_ID_KEY]);
    this.isLoadingSelection = false;
  }

  resetSelection() {
    this.selectedCredentials = [];
    this.selectAllCheckboxState = checkboxStates.UNCHECKED;
    this.isLoadingSelection = false;
  }

  /**
   *  Use this as a onSelect handler in Antd Table
   * @param {Object} record - selected row's data
   * @param {boolean} selected
   */
  handleCherryPickSelection(record, selected) {
    const credentialId = record[[CREDENTIAL_ID_KEY]];
    this.selectAllCheckboxState = checkboxStates.INDETERMINATE;

    if (selected) {
      // it's important to create new array because Antd has some PureComponent/memo optimizations,
      // so change is not detected
      this.selectedCredentials = [...this.selectedCredentials, credentialId];
    } else {
      this.selectedCredentials = this.selectedCredentials.filter(scId => scId !== credentialId);
    }
  }
}
