import { mockApi } from './__mocks__';
import { getGroups } from './__mocks__/groups';

import { getConnectionsPaginated, getMessagesForConnection } from './connector/connector';
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
  // These are the mocked apis that will be hardcoded
  // in the alpha version
  getGroups,
  // These are the real interactions with the backend
  generateConnectionToken,
  getConnectionsPaginated,
  createWallet,
  getDid,
  getCredentials,
  createCredential,
  getStudents,
  getWalletStatus,
  unlockWallet,
  isWalletUnlocked,
  lockWallet,
  isIssuer,
  getMessagesForConnection
};
