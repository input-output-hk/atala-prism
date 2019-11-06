import React from 'react';
import { withRouter } from 'react-router-dom';
import Main from './components/Main';
import { generateConnectionToken } from './APIs/connector/connector';
import { getHolders, inviteHolder } from './APIs/__mocks__/holders';
import { getGroups, deleteGroup } from './APIs/__mocks__/groups';
import {
  getCredentials,
  getCredentialTypes,
  getCategoryTypes,
  getCredentialsGroups,
  getTotalCredentials
} from './APIs/__mocks__/credentials';
import { getCredentialSummaries } from './APIs/__mocks__/credentialSummaries';
import {
  savePictureInS3,
  saveCredential,
  saveDraft
} from './APIs/__mocks__/credentialInteractions';
import { getTermsAndConditions, getPrivacyPolicy } from './APIs/__mocks__/documents';
import { getCurrencies, getAmounts, getPayments } from './APIs/__mocks__/payments';
import { getDid } from './APIs/wallet/wallet';

/* Here should be the header and the sidebar */
const App = () => (
  <Main
    apiProvider={{
      getHolders,
      inviteHolder,
      getCredentials,
      getCredentialTypes,
      getCategoryTypes,
      getCredentialsGroups,
      getGroups,
      deleteGroup,
      getTotalCredentials,
      getCredentialSummaries,
      savePictureInS3,
      saveCredential,
      saveDraft,
      geConnectionToken: generateConnectionToken,
      getDid,
      getTermsAndConditions,
      getPrivacyPolicy,
      getCurrencies,
      getAmounts,
      getPayments
    }}
  />
);

export default withRouter(App);
