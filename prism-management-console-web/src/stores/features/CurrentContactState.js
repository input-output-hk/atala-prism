import { message } from 'antd';
import i18n from 'i18next';
import { flow, makeAutoObservable } from 'mobx';

const defaultValues = {
  isLoadingContact: false,
  isLoadingGroups: false,
  isLoadingCredentialsIssued: false,
  isLoadingCredentialsReceived: false,
  isSaving: false,
  contactId: undefined,
  externalId: undefined,
  contactName: undefined,
  groups: [],
  credentialsIssued: [],
  credentialsReceived: []
};

export default class CurrentContactState {
  isLoadingContact = defaultValues.isLoadingContact;

  isLoadingGroups = defaultValues.isLoadingGroups;

  isLoadingCredentialsIssued = defaultValues.isLoadingCredentialsIssued;

  isLoadingCredentialsReceived = defaultValues.isLoadingCredentialsReceived;

  isSaving = defaultValues.isSaving;

  contactId = defaultValues.contactId;

  externalId = defaultValues.externalId;

  contactName = defaultValues.contactName;

  groups = defaultValues.groups;

  credentialsIssued = defaultValues.credentialsIssued;

  credentialsReceived = defaultValues.credentialsReceived;

  constructor(api, sessionState) {
    this.api = api;
    this.sessionState = sessionState;

    makeAutoObservable(this, {
      loadContact: flow.bound,
      loadGroups: flow.bound,
      loadCredentialsIssued: flow.bound,
      loadCredentialsReceived: flow.bound,
      removeFromGroup: flow.bound,
      updateContact: flow.bound,
      rootStore: false
    });
  }

  get contactIsLoaded() {
    return this.contactName;
  }

  // initialization

  init = contactId => {
    this.contactId = contactId;
    this.loadContact();
    this.loadGroups();
    this.loadCredentialsIssued();
    this.loadCredentialsReceived();
  };

  *loadContact() {
    this.isLoadingContact = true;

    const contact = yield this.api.contactsManager.getContact(this.contactId);

    this.contactName = contact.contactName;
    this.externalId = contact.externalId;
    this.isLoadingContact = false;
  }

  *loadGroups() {
    this.isLoadingGroups = true;
    const { groupsList } = yield this.api.groupsManager.getGroups({ contactId: this.contactId });

    this.groups = groupsList;
    this.isLoadingGroups = false;
  }

  *loadCredentialsIssued() {
    this.isLoadingCredentialsIssued = true;

    const credentials = yield this.api.credentialsManager.getContactCredentials(this.contactId);

    this.credentialsIssued = credentials;
    this.isLoadingCredentialsIssued = false;
  }

  *loadCredentialsReceived() {
    this.isLoadingCredentialsReceived = true;

    const credentialsList = yield this.api.credentialsReceivedManager.getReceivedCredentials(
      this.contactId,
      this.api.contactsManager
    );

    this.credentialsReceived = credentialsList;
    this.isLoadingCredentialsReceived = false;
  }

  // actions

  *removeFromGroup(groupId, contactId) {
    this.isSaving = true;
    yield this.api.groupsManager.updateGroup(groupId, { contactIdsToRemove: [contactId] });
    message.success(i18n.t('contacts.edit.success.removingFromGroup'));
    yield this.loadGroups();
    this.isSaving = false;
  }

  *updateContact(contactId, newContactData) {
    this.isSaving = true;
    yield this.api.contactsManager.updateContact(contactId, newContactData);
    message.success(i18n.t('contacts.edit.success.updating'));
    yield this.loadContact();
    this.isSaving = false;
  }
}
