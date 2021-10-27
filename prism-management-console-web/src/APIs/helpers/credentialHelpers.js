import { CREDENTIAL_STATUSES } from '../../helpers/constants';
import { contactMapper } from './contactHelpers';

export function credentialMapper(credential) {
  return {
    ...credential,
    status: getCredentialStatus(credential),
    contactData: {
      ...contactMapper(credential),
      contactName: credential.credentialData.contactName
    },
    proof: getCredentialProof(credential)
  };
}

function getCredentialStatus(credential) {
  const { publicationStoredAt, sharedAt, revocationProof } = credential;
  if (revocationProof) return CREDENTIAL_STATUSES.credentialRevoked;
  if (sharedAt?.seconds) return CREDENTIAL_STATUSES.credentialSent;
  if (publicationStoredAt?.seconds) return CREDENTIAL_STATUSES.credentialSigned;
  return CREDENTIAL_STATUSES.credentialDraft;
}

function getCredentialProof(credential) {
  return credential.batchInclusionProof ? JSON.parse(credential.batchInclusionProof) : undefined;
}

export function credentialReceivedMapper(credentialReceived) {
  const { contactData, credentialSubject } = credentialReceived;
  return {
    contactData: contactMapper(contactData),
    credentialData: {
      credentialTypeDetails: {
        name: credentialSubject.credentialTypeName,
        icon: credentialSubject.credentialTypeIcon,
        iconFormat: credentialSubject.credentialTypeIconFormat
      }
    }
  };
}
