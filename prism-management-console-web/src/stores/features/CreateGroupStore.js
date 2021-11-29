import { makeAutoObservable } from 'mobx';
import ContactsBaseStore from '../domain/ContactsBaseStore';
import Logger from '../../helpers/Logger';

const defaultValues = {
  isLoadingContacts: false,
  isSaving: false
};

export default class CreateGroupStore {
  isLoadingContacts = defaultValues.isLoadingContacts;

  isSaving = defaultValues.isSaving;

  constructor(api, sessionState) {
    this.api = api;
    this.contactsBaseStore = new ContactsBaseStore(api, sessionState);

    makeAutoObservable(
      this,
      {
        api: false,
        contactsBaseStore: false
      },
      { autoBind: true }
    );
  }

  get contacts() {
    return this.contactsBaseStore.contacts;
  }

  get hasFiltersApplied() {
    return this.contactsBaseStore.hasFiltersApplied;
  }

  get filterSortingProps() {
    return this.contactsBaseStore.filterSortingProps;
  }

  get isSearching() {
    return this.contactsBaseStore.isSearching;
  }

  get isFetching() {
    return this.contactsBaseStore.isFetching;
  }

  get isFetchingMore() {
    return this.contactsBaseStore.isFetchingMore;
  }

  async init() {
    this.isLoadingContacts = true;
    await this.contactsBaseStore.initContactStore();
    this.isLoadingContacts = false;
  }

  fetchMoreGroupContacts = () => {
    this.contactsBaseStore.fetchMoreData();
  };

  // FIXME: select only filtered members
  getContactsToSelect = () => this.api.contactsManager.getAllContacts();

  *createGroup({ name, members, onSuccess, onError }) {
    this.isSaving = true;

    try {
      // TODO: better to do atomically on backend
      const newGroup = yield this.api.groupsManager.createGroup(name);

      if (members) {
        yield this.api.groupsManager.updateGroup(newGroup.id, { contactIdsToAdd: members });
      }

      onSuccess?.();
    } catch (error) {
      Logger.error('[CreateGroupStore.createGroup] Error: ', error);
      onError?.();
    } finally {
      this.isSaving = false;
    }
  }
}
