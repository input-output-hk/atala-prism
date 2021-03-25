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
    targetCredentials: selected.filter(({ status, contactData }) => {
      const validCredentialStatus =
        !requiredStatus?.credential?.length || requiredStatus.credential.includes(status);
      const validContactStatus =
        !requiredStatus?.contact?.length || requiredStatus.contact.includes(contactData.status);
      return validCredentialStatus && validContactStatus;
    })
  };
};

const getSelectedCredentials = (credentials, selectedCredentials) =>
  credentials.filter(c => selectedCredentials.includes(c.credentialid));

export const credentialRequiredStatus = {
  [REVOKE_CREDENTIALS]: {
    credential: [CREDENTIAL_STATUSES.credentialSigned, CREDENTIAL_STATUSES.credentialSent]
  },
  [SIGN_CREDENTIALS]: {
    credential: [CREDENTIAL_STATUSES.credentialDraft]
  },
  [SEND_CREDENTIALS]: {
    credential: [CREDENTIAL_STATUSES.credentialSigned],
    contact: [CONNECTION_STATUSES.statusConnectionAccepted]
  }
};
