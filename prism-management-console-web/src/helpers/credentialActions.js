import {
  CONNECTION_STATUSES,
  CREDENTIAL_STATUSES,
  REVOKE_CREDENTIALS,
  SEND_CREDENTIALS,
  SIGN_CREDENTIALS
} from './constants';

export const getTargetCredentials = (credentials, selectedCredentials, requiredStatus) => {
  const selected = getSelectedCredentials(credentials, selectedCredentials);

  return {
    selected,
    targetCredentials: selected.filter(cred => hasRequiredStatus(cred, requiredStatus))
  };
};

export const hasRequiredStatus = ({ status, contactData }, requiredStatus) => {
  const hasNoRequiredCredentialStatus = !requiredStatus?.credential?.length;
  const hasNoRequiredContactStatus = !requiredStatus?.contact?.length;

  const validCredentialStatus =
    hasNoRequiredCredentialStatus || requiredStatus.credential.includes(status);
  const validContactStatus =
    hasNoRequiredContactStatus || requiredStatus.contact.includes(contactData.connectionStatus);

  return validCredentialStatus && validContactStatus;
};

const getSelectedCredentials = (credentials, selectedCredentials) =>
  credentials.filter(c => selectedCredentials.includes(c.credentialId));

export const credentialRequiredStatus = {
  [REVOKE_CREDENTIALS]: {
    credential: [CREDENTIAL_STATUSES.credentialSigned, CREDENTIAL_STATUSES.credentialSent]
  },
  [SIGN_CREDENTIALS]: {
    credential: [CREDENTIAL_STATUSES.credentialDraft],
    contact: [CONNECTION_STATUSES.statusConnectionAccepted]
  },
  [SEND_CREDENTIALS]: {
    credential: [CREDENTIAL_STATUSES.credentialSigned],
    contact: [CONNECTION_STATUSES.statusConnectionAccepted]
  }
};
