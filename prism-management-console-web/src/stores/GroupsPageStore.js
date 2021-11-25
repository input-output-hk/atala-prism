import { makeAutoObservable } from 'mobx';
import GroupsBaseStore from './GroupsBaseStore';
import Logger from '../helpers/Logger';

export default class GroupsPageStore {
  isSaving = false;

  constructor(api, sessionState) {
    this.api = api;
    this.transportLayerErrorHandler = sessionState.transportLayerErrorHandler;
    this.groupsBaseStore = new GroupsBaseStore(api, sessionState);
    this.storeName = this.constructor.name;

    makeAutoObservable(
      this,
      {
        api: false,
        transportLayerErrorHandler: false,
        groupsBaseStore: false,
        storeName: false
      },
      { autoBind: true }
    );
  }

  init() {
    this.isSaving = false;
    this.groupsBaseStore.init();
  }

  get groups() {
    return this.groupsBaseStore.groups;
  }

  get isLoadingFirstPage() {
    return this.groupsBaseStore.isLoadingFirstPage;
  }

  get hasFiltersApplied() {
    return this.groupsBaseStore.hasFiltersApplied;
  }

  get hasMore() {
    return this.groupsBaseStore.hasMore;
  }

  get filterSortingProps() {
    return this.groupsBaseStore.filterSortingProps;
  }

  get isSearching() {
    return this.groupsBaseStore.isSearching;
  }

  get isFetching() {
    return this.groupsBaseStore.isFetching;
  }

  fetchMoreData() {
    return this.groupsBaseStore.fetchMoreData();
  }

  refresh({ refreshCountDiff } = {}) {
    return this.groupsBaseStore.refreshGroups({ refreshCountDiff });
  }

  *createGroup({ name, members, onSuccess, onError }) {
    this.isSaving = true;

    try {
      // TODO: better to do atomically on backend
      const newGroup = yield this.api.groupsManager.createGroup(name);

      if (members) {
        yield this.updateGroup(newGroup.id, { contactIdsToAdd: members });
      }

      onSuccess();
    } catch (error) {
      Logger.error('[GroupsPageStore.createGroup] Error: ', error);
      onError();
    } finally {
      this.isSaving = false;
    }
  }

  *updateGroup(id, change) {
    this.isSaving = true;

    try {
      const response = yield this.api.groupsManager.updateGroup(id, change);
      this.transportLayerErrorHandler.handleTransportLayerSuccess();
      return response;
    } catch (error) {
      const metadata = {
        store: this.storeName,
        method: 'updateGroup',
        verb: 'saving',
        model: 'Group'
      };
      this.transportLayerErrorHandler.handleTransportLayerError(error, metadata);
    } finally {
      this.isSaving = false;
    }
  }

  *deleteGroup({ groupId, onSuccess, onError }) {
    this.isSaving = true;

    try {
      yield this.api.groupsManager.deleteGroup(groupId);
      yield this.refresh({ refreshCountDiff: -1 });
      onSuccess();
    } catch (error) {
      Logger.error('[GroupsPageStore.deleteGroup] Error: ', error);
      onError();
    } finally {
      this.isSaving = false;
    }
  }

  *copyGroup({ groupName, copyName, numberOfContacts, onSuccess, onError }) {
    // TODO: probably a good idea is to move this logic to the server
    this.isSaving = true;

    try {
      const { id } = yield this.api.groupsManager.createGroup(copyName);
      const { contactsList } = yield this.api.contactsManager.getContacts({
        limit: numberOfContacts,
        groupName
      });

      yield this.api.groupsManager.updateGroup(id, {
        contactIdsToAdd: contactsList.map(({ contactId }) => contactId)
      });

      yield this.refresh({ refreshCountDiff: 1 });
      onSuccess();
    } catch (error) {
      Logger.error('[GroupsPageStore.copyGroup] Error: ', error);
      onError();
    } finally {
      this.isSaving = false;
    }
  }
}
