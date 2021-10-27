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
  const { encodedSignedCredential, contactData, credentialSubject, ...rest } = credentialReceived;
  const {
    credentialTypeName,
    credentialTypeIcon,
    credentialTypeIconFormat,
    ...restCredentialSubject
  } = credentialSubject;
  return {
    encodedSignedCredential,
    contactData: contactMapper(contactData),
    credentialData: {
      ...rest,
      ...restCredentialSubject,
      credentialTypeDetails: {
        name: credentialTypeName,
        icon: credentialTypeIcon,
        iconFormat: credentialTypeIconFormat
      }
    }
  };
}
