import { CREDENTIAL_STATUSES } from '../../helpers/constants';
import { contactMapper } from './contactHelpers';

export function credentialMapper(credential, credentialTypes) {
  const { credentialid, credentialdata, contactid, contactdata, connectionstatus } = credential;

  const parsedCredentialJson = JSON.parse(credentialdata);

  return {
    ...parsedCredentialJson,
    credentialid,
    credentialdata,
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
  const { publicationstoredat, sharedAt } = credential;
  if (sharedAt) return CREDENTIAL_STATUSES.credentialSent;
  if (publicationstoredat) return CREDENTIAL_STATUSES.credentialSigned;
  return CREDENTIAL_STATUSES.credentialDraft;
}

export function credentialRecievedMapper(credentialRecieved) {
  const { credentialSubject, ...rest } = credentialRecieved;

  return {
    contactData: {
      externalid: credentialSubject.identityNumber,
      contactName: credentialSubject.name
    },
    ...rest
  };
}
