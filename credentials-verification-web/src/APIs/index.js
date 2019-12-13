import { mockApi } from './__mocks__';
import { getGroups } from './__mocks__/groups';

import {
  getConnectionsPaginated,
  getMessagesForConnection,
  issueCredential
} from './connector/connector';
import {
  createWallet,
  getDid,
  getWalletStatus,
  lockWallet,
  unlockWallet,
  isWalletUnlocked,
  isIssuer
} from './wallet/wallet';
import { getCredentials, createCredential } from './credentials/credentialsManager';
import {
  getStudents,
  generateConnectionToken,
  getStudentCredentials
} from './credentials/studentsManager';

export { mockApi };
export const api = {
  // These are the mocked apis that will be hardcoded
  // in the alpha version
  getGroups,
  // These are the real interactions with the backend
  generateConnectionToken,
  getConnectionsPaginated,
  createWallet,
  getDid,
  getCredentials,
  issueCredential,
  createCredential,
  getStudents,
  getWalletStatus,
  unlockWallet,
  isWalletUnlocked,
  lockWallet,
  isIssuer,
  getStudentCredentials,
  getMessagesForConnection
};

export const hardcodedApi = {
  getTermsAndConditions: mockApi.getTermsAndConditions,
  getPrivacyPolicy: mockApi.getPrivacyPolicy,
  getGroups: mockApi.getGroups,
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
  editSettings: mockApi.editSettings
};
