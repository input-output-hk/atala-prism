import { mockApi } from './__mocks__';

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
import { getStudents, generateConnectionToken } from './credentials/studentsManager';

export { mockApi };
export const api = {
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
  getMessagesForConnection
};
