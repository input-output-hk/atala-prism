import { mockApi } from './__mocks__';

import Connector from './connector/connector';
import Wallet from './wallet/wallet';
import CredentialsManager from './credentials/credentialsManager';
import ContactsManager from './contacts/contactsManager';
import GroupsManager from './credentials/groupsManager';
import Admin from './admin/admin';

export { mockApi };

// TODO when the bulk imports really exist replace this mocked promise
const mockApiCall = file => new Promise(resolve => resolve(file));
const importStudentBulk = mockApiCall;
const importIndividualBulk = mockApiCall;

const importBulk = (issuer, fileWithBulk) => {
  const functionByRole = issuer ? importStudentBulk : importIndividualBulk;

  return functionByRole(fileWithBulk);
};

function Api(configuration, authenticator) {
  this.configuration = configuration;
  this.admin = new Admin(this.configuration);
  this.wallet = new Wallet(this.configuration);
  this.authenticator = new authenticator(this.configuration, this.wallet);
  this.contactsManager = new ContactsManager(this.configuration, this.authenticator);
  this.groupsManager = new GroupsManager(this.configuration, this.authenticator);
  this.connector = new Connector(this.configuration, this.authenticator);
  this.credentialsManager = new CredentialsManager(this.configuration, this.authenticator);
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
  getPayments: mockApi.getPayments,
  getCurrencies: mockApi.getCurrencies,
  getAmounts: mockApi.getAmounts,
  getSettings: mockApi.getSettings,
  editSettings: mockApi.editSettings,
  importBulk
};
