import { CREDENTIAL_STATUSES } from '../../helpers/constants';
import { contactMapper } from './contactHelpers';

export function credentialMapper(credential) {
  return {
    ...credential,
    status: getCredentialStatus(credential),
    contactData: contactMapper(credential)
  };
}

function getCredentialStatus(credential) {
  const { publicationStoredAt, sharedAt, revocationProof } = credential;
  if (revocationProof) return CREDENTIAL_STATUSES.credentialRevoked;
  if (sharedAt?.seconds) return CREDENTIAL_STATUSES.credentialSent;
  if (publicationStoredAt?.seconds) return CREDENTIAL_STATUSES.credentialSigned;
  return CREDENTIAL_STATUSES.credentialDraft;
}

export function credentialReceivedMapper(credentialReceived, credentialTypes) {
  const { externalId, contactName, credentialType, ...rest } = credentialReceived;
  return {
    contactData: {
      externalId,
      contactName
    },
    credentialType: credentialTypes[credentialType],
    ...rest
  };
}
