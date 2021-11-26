import { CREDENTIAL_STATUSES } from '../../helpers/constants';
import { contactMapper } from './contactHelpers';

export function credentialMapper(credential) {
  const credentialObject = credential.toObject();
  const credentialString = credential.getCredentialData();
  const credentialData = JSON.parse(credentialString);

  return {
    ...credentialObject,
    credentialData,
    credentialString,
    status: getCredentialStatus(credentialObject),
    contactData: {
      ...contactMapper(credentialObject),
      contactName: credentialData.contactName
    },
    proof: getCredentialProof(credentialObject)
  };
}

function getCredentialStatus(credential) {
  const { publicationStoredAt, sharedAt, revokedOnOperationId } = credential;
  if (revokedOnOperationId) return CREDENTIAL_STATUSES.credentialRevoked;
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
