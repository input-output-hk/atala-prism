import { CREDENTIAL_STATUSES } from '../../helpers/constants';
import { contactMapper } from './contactHelpers';

export function credentialMapper(credential) {
  const {
    batchId,
    credentialId,
    credentialData,
    contactId,
    externalId,
    contactData,
    connectionStatus,
    encodedSignedCredential,
    publicationStoredAt,
    issuanceProof,
    issuanceOperationHash,
    batchInclusionProof,
    sharedAt,
    revocationProof,
    credentialType
  } = credential;

  const parsedCredentialJson = JSON.parse(credentialData);

  return {
    ...parsedCredentialJson,
    batchId,
    credentialId,
    credentialData,
    encodedSignedCredential,
    publicationStoredAt,
    issuanceProof,
    issuanceOperationHash,
    batchInclusionProof,
    sharedAt,
    revocationProof,
    credentialType,
    status: getCredentialStatus(credential),
    contactData: contactMapper({
      contactId,
      externalId,
      connectionStatus,
      jsonData: contactData
    })
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
