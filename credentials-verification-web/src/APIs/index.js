import { mockApi } from './__mocks__';

import { generateConnectionToken, getConnectionsPaginated } from './connector/connector';
import { createWallet, getDid } from './wallet/wallet';
import { getCredentials, createCredential } from './credentials/credentialsManager';

export { mockApi };
export const api = {
  generateConnectionToken,
  getConnectionsPaginated,
  createWallet,
  getDid,
  getCredentials,
  createCredential
};
