import { CREDENTIAL_STATUSES } from '../../helpers/constants';
import { contactMapper } from './contactHelpers';

export function credentialMapper(credential, credentialTypes) {
  const {
    batchid,
    credentialid,
    credentialData,
    contactid,
    externalid,
    contactData,
    connectionstatus,
    encodedsignedcredential,
    publicationstoredat,
    issuanceproof,
    issuanceoperationhash,
    batchinclusionproof,
    sharedat,
    revocationproof
  } = credential;

  const parsedCredentialJson = JSON.parse(credentialData);

  return {
    ...parsedCredentialJson,
    batchid,
    credentialid,
    credentialData,
    encodedsignedcredential,
    publicationstoredat,
    issuanceproof,
    issuanceoperationhash,
    batchinclusionproof,
    sharedat,
    revocationproof,
    credentialType: getCredentialTypeObject(parsedCredentialJson, credentialTypes),
    status: getCredentialStatus(credential),
    contactData: contactMapper({
      contactid,
      externalid,
      connectionstatus,
      jsonData: contactData
    })
  };
}

function getCredentialTypeObject(credentialData, credentialTypes) {
  const credentialTypeKey = credentialData.credentialType;
  const { id, name, logo } = credentialTypes[credentialTypeKey] || {};
  return {
    key: credentialTypeKey,
    id,
    name,
    logo
  };
}

function getCredentialStatus(credential) {
  const { publicationstoredat, sharedat, revocationproof } = credential;
  if (revocationproof) return CREDENTIAL_STATUSES.credentialRevoked;
  if (sharedat?.seconds) return CREDENTIAL_STATUSES.credentialSent;
  if (publicationstoredat?.seconds) return CREDENTIAL_STATUSES.credentialSigned;
  return CREDENTIAL_STATUSES.credentialDraft;
}

export function credentialReceivedMapper(credentialReceived, credentialTypes) {
  const { externalid, contactName, credentialType, ...rest } = credentialReceived;
  return {
    contactData: {
      externalid,
      contactName
    },
    credentialType: credentialTypes[credentialType],
    ...rest
  };
}
