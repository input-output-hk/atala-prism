import { CREDENTIAL_STATUSES } from '../../helpers/constants';
import { contactMapper } from './contactHelpers';

export function credentialMapper(credential, credentialTypes) {
  const {
    credentialid,
    credentialdata,
    contactid,
    contactdata,
    connectionstatus,
    encodedsignedcredential,
    publicationstoredat,
    issuanceproof
  } = credential;

  const parsedCredentialJson = JSON.parse(credentialdata);

  return {
    ...parsedCredentialJson,
    credentialid,
    credentialdata,
    encodedsignedcredential,
    publicationstoredat,
    issuanceproof,
    credentialType: getCredentialTypeObject(parsedCredentialJson, credentialTypes),
    status: getCredentialStatus(credential),
    contactData: contactMapper({
      contactid,
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
  const { publicationstoredat, sharedat } = credential;
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
