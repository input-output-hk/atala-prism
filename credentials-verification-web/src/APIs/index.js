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
import { getCredentials, createCredential, registerUser } from './credentials/credentialsManager';
import {
  getStudents as getIndividualsAsIssuer,
  generateConnectionToken as generateConnectionTokenAsIssuer,
  getStudentCredentials
} from './credentials/studentsManager';
import {
  getIndividuals as getIndividualsAsVerifier,
  generateConnectionTokenForIndividual as generateConnectionTokenAsVerifier
} from './cstore/credentialsStore';

export { mockApi };

const getRole = issuer => {
  const { REACT_APP_VERIFIER, REACT_APP_ISSUER } = window._env_;

  return issuer ? REACT_APP_ISSUER : REACT_APP_VERIFIER;
};

const getIndividuals = issuer => {
  const functionByRole = issuer ? getIndividualsAsIssuer : getIndividualsAsVerifier;

  return (limit, lastSeenId) => functionByRole(getRole(issuer), limit, lastSeenId);
};

const generateConnectionToken = issuer => {
  const functionByRole = issuer
    ? generateConnectionTokenAsIssuer
    : generateConnectionTokenAsVerifier;

  return id => functionByRole(getRole(issuer), id);
};

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
  getWalletStatus,
  unlockWallet,
  isWalletUnlocked,
  lockWallet,
  isIssuer,
  getStudentCredentials,
  getMessagesForConnection,
  registerUser,
  getIndividuals
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
