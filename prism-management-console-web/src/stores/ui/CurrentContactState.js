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

  constructor(rootStore) {
    this.rootStore = rootStore;
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
    const { fetchContactById } = this.rootStore.prismStore.contactStore;

    const contact = yield fetchContactById(this.contactId);

    this.contactName = contact.contactName;
    this.externalId = contact.externalId;
    this.isLoadingContact = false;
  }

  *loadGroups() {
    this.isLoadingGroups = true;
    const { getContactGroups } = this.rootStore.prismStore.groupStore;
    const groups = yield getContactGroups(this.contactId);

    this.groups = groups;
    this.isLoadingGroups = false;
  }

  *loadCredentialsIssued() {
    this.isLoadingCredentialsIssued = true;
    const { getContactCredentials } = this.rootStore.prismStore.credentialIssuedStore;

    const credentials = yield getContactCredentials(this.contactId);

    this.credentialsIssued = credentials;
    this.isLoadingCredentialsIssued = false;
  }

  *loadCredentialsReceived() {
    this.isLoadingCredentialsReceived = true;
    const { fetchCredentials } = this.rootStore.prismStore.credentialReceivedStore;

    const { credentialsList } = yield fetchCredentials(this.contactId);

    this.credentialsReceived = credentialsList;
    this.isLoadingCredentialsReceived = false;
  }

  // actions

  *removeFromGroup(groupId, contactId) {
    const { updateGroup } = this.rootStore.prismStore.groupStore;
    yield updateGroup(groupId, { contactIdsToRemove: [contactId] });
    message.success(i18n.t('contacts.edit.success.removingFromGroup'));
    yield this.loadGroups();
  }

  *updateContact(contactId, newContactData) {
    const { updateContact } = this.rootStore.prismStore.contactStore;
    yield updateContact(contactId, newContactData);
    message.success(i18n.t('contacts.edit.success.updating'));
    yield this.loadContact();
  }
}
