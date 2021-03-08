import { CREDENTIAL_STATUSES } from '../../helpers/constants';
import { contactMapper } from './contactHelpers';

export function credentialMapper(credential, credentialTypes) {
  const {
    batchid,
    credentialid,
    credentialdata,
    contactid,
    externalid,
    contactdata,
    connectionstatus,
    encodedsignedcredential,
    publicationstoredat,
    issuanceproof,
    issuanceoperationhash,
    batchinclusionproof,
    sharedat,
    revocationproof
  } = credential;

  const parsedCredentialJson = JSON.parse(credentialdata);

  return {
    ...parsedCredentialJson,
    batchid,
    credentialid,
    credentialdata,
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
      jsondata: contactdata
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
  if (sharedat) return CREDENTIAL_STATUSES.credentialSent;
  if (publicationstoredat) return CREDENTIAL_STATUSES.credentialSigned;
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
