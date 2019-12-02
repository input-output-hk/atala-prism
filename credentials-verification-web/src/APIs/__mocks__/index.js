import { getHolders, inviteHolder } from './holders';
import { getGroups, deleteGroup } from './groups';
import {
  getCredentialTypes,
  getCategoryTypes,
  getCredentialsGroups,
  getTotalCredentials
} from './credentials';
import { savePictureInS3, saveCredential, saveDraft } from './credentialInteractions';
import { getTermsAndConditions, getPrivacyPolicy } from './documents';
import { toProtoDate } from './helpers';
import { getCredentialSummaries } from './credentialSummaries';
import { getPayments, getCurrencies, getAmounts } from './payments';
import { getSettings, editSettings } from './settings';

export const mockApi = {
  getHolders,
  inviteHolder,
  getGroups,
  deleteGroup,
  getCredentialTypes,
  getCategoryTypes,
  getCredentialsGroups,
  getTotalCredentials,
  savePictureInS3,
  saveCredential,
  saveDraft,
  getTermsAndConditions,
  getPrivacyPolicy,
  toProtoDate,
  getCredentialSummaries,
  getPayments,
  getCurrencies,
  getAmounts,
  getSettings,
  editSettings
};
