import { mockApi } from './__mocks__';

import Connector from './connector/connector';
import Wallet from './wallet/wallet';
import CredentialsManager from './credentials/credentialsManager';
import ContactsManager from './contacts/contactsManager';
import GroupsManager from './credentials/groupsManager';
import CredentialsViewManager from './credentials/credentialsViewManager';
import CredentialsReceivedManager from './credentials/credentialsReceivedManager';
import SummaryManager from './summary/summaryManager';
import CredentialTypesManager from './credentials/credentialTypesManager';

export { mockApi };

// TODO when the bulk imports really exist replace this mocked promise
const mockApiCall = file => Promise.resolve(file);
const importContactBulk = mockApiCall;
const importBulk = (_, fileWithBulk) => importContactBulk(fileWithBulk);

function Api(configuration, authenticator) {
  this.configuration = configuration;
  this.wallet = new Wallet(this.configuration);
  this.authenticator = new authenticator(this.configuration, this.wallet);
  this.contactsManager = new ContactsManager(this.configuration, this.authenticator);
  this.groupsManager = new GroupsManager(this.configuration, this.authenticator);
  this.connector = new Connector(this.configuration, this.authenticator);
  this.credentialsManager = new CredentialsManager(this.configuration, this.authenticator);
  this.credentialsReceivedManager = new CredentialsReceivedManager(
    this.configuration,
    this.authenticator
  );
  this.credentialsViewManager = new CredentialsViewManager(this.configuration, this.authenticator);
  this.credentialTypesManager = new CredentialTypesManager(this.configuration, this.authenticator);
  this.summaryManager = new SummaryManager(this.configuration, this.authenticator);
}

export default Api;

export const hardcodedApi = {
  getTermsAndConditions: mockApi.getTermsAndConditions,
  getPrivacyPolicy: mockApi.getPrivacyPolicy,
  getCredentialTypes: mockApi.getCredentialTypes,
  getCategoryTypes: mockApi.getCategoryTypes,
  getCredentialsGroups: mockApi.getCredentialsGroups,
  getTotalCredentials: mockApi.getTotalCredentials,
  savePictureInS3: mockApi.savePictureInS3,
  saveDraft: mockApi.saveDraft,
  getSettings: mockApi.getSettings,
  editSettings: mockApi.editSettings,
  importBulk
};
