import { mockApi } from './__mocks__';

import { generateConnectionToken, getConnectionsPaginated } from './connector/connector';
import {
  createWallet,
  getDid,
  getWalletStatus,
  unlockWallet,
  isWalletUnlocked
} from './wallet/wallet';
import { getCredentials, createCredential } from './credentials/credentialsManager';
import { getStudents } from './credentials/studentsManager';

export { mockApi };
export const api = {
  generateConnectionToken,
  getConnectionsPaginated,
  createWallet,
  getDid,
  getCredentials,
  createCredential,
  getStudents,
  getWalletStatus,
  unlockWallet,
  isWalletUnlocked
};
