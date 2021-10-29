import { getHolders, inviteHolder } from './holders';
import { getGroups, deleteGroup } from './groups';
import {
  getCredentials as getMockCredentials,
  getCredentialTypes,
  getCategoryTypes,
  getCredentialsGroups,
  getTotalCredentials,
  deleteCredential
} from './credentials';
import { savePictureInS3, saveCredential, saveDraft } from './credentialInteractions';
import { getTermsAndConditions, getPrivacyPolicy } from './documents';
import { toProtoDate } from './helpers';
import { getSettings, editSettings } from './settings';
import {
  isWalletUnlocked,
  getSessionFromExtension,
  clearSession,
  setSessionState,
  setSessionErrorHandler
} from './login';

// TODO update mock api.
//  - It should have the same structure that real Api
//  - Remove functions that don't exist anymore
//  - There are many function in real Api that don't exist here
export const mockApi = {
  getHolders,
  inviteHolder,
  getGroups,
  deleteGroup,
  getCredentialTypes,
  getCategoryTypes,
  getCredentialsGroups,
  getMockCredentials,
  getTotalCredentials,
  deleteCredential,
  savePictureInS3,
  saveCredential,
  saveDraft,
  getTermsAndConditions,
  getPrivacyPolicy,
  toProtoDate,
  getSettings,
  editSettings,
  wallet: {
    isWalletUnlocked,
    getSessionFromExtension,
    clearSession,
    setSessionState,
    setSessionErrorHandler
  }
};
