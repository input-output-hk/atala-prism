import { CONNECTION_STATUSES, CREDENTIAL_STATUSES } from '../../helpers/constants';

export const DRAFT_NOT_CONNECTED_COUNT = 2;
export const DRAFT_CONNECTED_COUNT = 3;
export const SIGNED_CONNECTED_COUNT = 2;
export const SENT_CONNECTED_COUNT = 1;
export const REVOKED_CONNECTED_COUNT = 1;

export const UNSIGNABLE_CREDENTIAL_INDEX = 0;
export const SIGNABLE_CREDENTIAL_INDEX = 1;
export const UNSENDABLE_CREDENTIAL_INDEX = 1;
export const SENDABLE_CREDENTIAL_INDEX = 2;
export const UNREVOCABLE_CREDENTIAL_INDEX = 1;
export const REVOCABLE_CREDENTIAL_INDEX = 3;

export const mockCredentials = [
  // Credential: DRAFT
  // Contact: STATUS_INVITATION_MISSING
  {
    gender: 'male',
    issuer: '!!J5',
    fullname: 'Priya Lamb',
    cardNumber: '124',
    contactName: 'Priya Lamb',
    dateOfBirth: '02/02/03',
    placeOfBirth: 'argentina',
    expirationDate: '21/02/39',
    personalNumber: '124',
    countryOfCitizenship: 'argentina',
    batchid: '',
    credentialId: '5d6ad6db-b506-4108-b5ff-638747e3b5b5',
    publicationstoredat: 0,
    issuanceoperationhash: '',
    batchinclusionproof: '',
    sharedat: 0,
    status: CREDENTIAL_STATUSES.credentialDraft,
    contactData: {
      externalId: 'debcdfb1dec543c3965d133bed318be4',
      contactName: 'Priya Lamb',
      firstName: 'Priya',
      midNames: '',
      lastName: 'Lamb',
      key: '53d6c434-3e07-4391-82fa-2e3c9f0dbb09',
      connectionStatus: CONNECTION_STATUSES.statusInvitationMissing,
      contactId: '53d6c434-3e07-4391-82fa-2e3c9f0dbb09'
    }
  },
  // Credential: DRAFT
  // Contact: STATUS_CONNECTION_ACCEPTED
  {
    gender: 'male',
    issuer: '!!J5',
    fullname: 'Klaudia Munoz',
    cardNumber: '125',
    contactName: 'Klaudia Munoz',
    dateOfBirth: '02/02/03',
    placeOfBirth: 'argentina',
    expirationDate: '21/02/39',
    personalNumber: '125',
    countryOfCitizenship: 'argentina',
    batchid: '',
    credentialId: 'af4c2c2b-7aec-419a-a95b-0af796e77fac',
    publicationstoredat: 0,
    issuanceoperationhash: '',
    batchinclusionproof: '',
    sharedat: 0,
    status: CREDENTIAL_STATUSES.credentialDraft,
    contactData: {
      externalId: '8b3339d551884afb8171d4c353fb91f0',
      contactName: 'Klaudia Munoz',
      firstName: 'Klaudia',
      midNames: '',
      lastName: 'Munoz',
      key: '6ba33ecf-7833-4899-a6df-6a681c742067',
      connectionStatus: CONNECTION_STATUSES.statusConnectionAccepted,
      contactId: '6ba33ecf-7833-4899-a6df-6a681c742067'
    }
  },
  // Credential: SIGNED
  // Contact: STATUS_CONNECTION_ACCEPTED
  {
    gender: 'male',
    issuer: '!!J5',
    fullname: 'Harleigh Wells',
    cardNumber: '123',
    contactName: 'Harleigh Wells',
    dateOfBirth: '02/02/03',
    placeOfBirth: 'argentina',
    expirationDate: '21/02/39',
    personalNumber: '123',
    countryOfCitizenship: 'argentina',
    batchid: '8905b238f27e228c96f0c0fcac2a2ae9258a754fc6cd233f00e1dd730f1a4232',
    credentialId: 'b9226e86-b534-404f-a6a8-bc3ce89130ba',
    publicationstoredat: 1615838070055,
    issuanceproof: {
      transactionid: '5a2098cc389fd76900b38489c15eeecf1ee816638bc575475a6f13d19d4041d3',
      ledger: 1
    },
    issuanceoperationhash: 'hKXpqjyoYSe9M7R286LPxF+Ee82+tu6qT6ZpRgoLDV4=',
    batchinclusionproof:
      '{"hash":"0ed73bc9fd29c94795a2f8644abf96cef12bbd9398ad94a3d1f0c32bb073192c","index":0,"siblings":[]}',
    sharedat: 0,
    status: CREDENTIAL_STATUSES.credentialSigned,
    contactData: {
      externalId: '575a095607ca4f86b59b38e7715f922c',
      contactName: 'Harleigh Wells',
      firstName: 'Harleigh',
      midNames: '',
      lastName: 'Wells',
      key: '49fcb20a-5b7e-48ee-b4f3-e39952c36c4b',
      connectionStatus: CONNECTION_STATUSES.statusConnectionAccepted,
      contactId: '49fcb20a-5b7e-48ee-b4f3-e39952c36c4b'
    }
  },
  // Credential: SIGNED
  // Contact: STATUS_CONNECTION_ACCEPTED
  {
    gender: 'male',
    issuer: '!!J5',
    fullname: 'Ayyub Conner',
    cardNumber: '126',
    contactName: 'Ayyub Conner',
    dateOfBirth: '02/02/03',
    placeOfBirth: 'argentina',
    expirationDate: '21/02/39',
    personalNumber: '126',
    countryOfCitizenship: 'argentina',
    batchid: '',
    credentialId: '944b9486-2693-49bf-b42c-2bf189acb76f',
    publicationstoredat: 0,
    issuanceoperationhash: '',
    batchinclusionproof: '',
    sharedat: 0,
    status: CREDENTIAL_STATUSES.credentialSigned,
    contactData: {
      externalId: 'f5ecf2f2037745d3be0283bd876140ff',
      contactName: 'Ayyub Conner',
      firstName: 'Ayyub',
      midNames: '',
      lastName: 'Conner',
      key: '920dfbb4-3f6b-4e6a-bfc9-732570e3e280',
      connectionStatus: CONNECTION_STATUSES.statusConnectionAccepted,
      contactId: '920dfbb4-3f6b-4e6a-bfc9-732570e3e280'
    }
  },
  // Credential: DRAFT
  // Contact: STATUS_CONNECTION_ACCEPTED
  {
    gender: 'male',
    issuer: '!!J5',
    fullname: 'Malika Ferguson',
    cardNumber: '127',
    contactName: 'Malika Ferguson',
    dateOfBirth: '02/02/03',
    placeOfBirth: 'argentina',
    expirationDate: '21/02/39',
    personalNumber: '127',
    countryOfCitizenship: 'argentina',
    batchid: '',
    credentialId: '1276a6d2-12e6-4257-bd38-b16c9600a651',
    publicationstoredat: 0,
    issuanceoperationhash: '',
    batchinclusionproof: '',
    sharedat: 0,
    status: CREDENTIAL_STATUSES.credentialDraft,
    contactData: {
      externalId: '63880e9e5cbd44319afb7c43b047cf88',
      contactName: 'Malika Ferguson',
      firstName: 'Malika',
      midNames: '',
      lastName: 'Ferguson',
      key: '49e55b84-d542-440a-8c92-eac9f6df7e90',
      connectionStatus: CONNECTION_STATUSES.statusConnectionAccepted,
      contactId: '49e55b84-d542-440a-8c92-eac9f6df7e90'
    }
  },
  // Credential: SENT
  // Contact: STATUS_CONNECTION_ACCEPTED
  {
    gender: 'male',
    issuer: '!!J5',
    fullname: 'Nick Magana',
    cardNumber: '128',
    contactName: 'Nick Magana',
    dateOfBirth: '02/02/03',
    placeOfBirth: 'argentina',
    expirationDate: '21/02/39',
    personalNumber: '128',
    countryOfCitizenship: 'argentina',
    batchid: '70ea308dc95e92accdd665d32f932498f5219478015988b21e26d0d550d855b8',
    credentialId: '5a03acaf-9d85-4e4f-a207-a3c6deb3c048',
    publicationstoredat: 1615838072807,
    issuanceproof: {
      transactionid: 'c71a3593ceee1a3154882aacb81332a7c344f9745de4939f01c56e4639113d76',
      ledger: 1
    },
    issuanceoperationhash: 'txQGcEaXiVz9hULmIVfCq+wWrR49743xq2JUPg0m5xY=',
    batchinclusionproof:
      '{"hash":"ca656f8c528c78f82509fbff4de8ed997c2d7b3e4089e4e7229547f0b1c9f391","index":0,"siblings":[]}',
    sharedat: 0,
    status: CREDENTIAL_STATUSES.credentialSent,
    contactData: {
      externalId: 'fae84b4f0ecf42db8f793b0b7c3f7bd2',
      contactName: 'Nick Magana',
      firstName: 'Nick',
      midNames: '',
      lastName: 'Magana',
      key: '2c4cb6eb-26d1-48f5-80fd-1957160af38e',
      connectionStatus: CONNECTION_STATUSES.statusConnectionAccepted,
      contactId: '2c4cb6eb-26d1-48f5-80fd-1957160af38e'
    }
  },
  // Credential: REVOKED
  // Contact: STATUS_CONNECTION_ACCEPTED
  {
    gender: 'male',
    issuer: '!!J5',
    fullname: 'Indi Avery',
    cardNumber: '130',
    contactName: 'Indi Avery',
    dateOfBirth: '02/02/03',
    placeOfBirth: 'argentina',
    expirationDate: '21/02/39',
    personalNumber: '130',
    countryOfCitizenship: 'argentina',
    batchid: '',
    credentialId: 'eb0086a7-44dc-4100-ac79-528308f4a372',
    publicationstoredat: 0,
    issuanceoperationhash: '',
    batchinclusionproof: '',
    sharedat: 0,
    status: CREDENTIAL_STATUSES.credentialRevoked,
    contactData: {
      externalId: '0308e770ba9541c58732d1a046b170b9',
      contactName: 'Indi Avery',
      firstName: 'Indi',
      midNames: '',
      lastName: 'Avery',
      key: '810eddfb-5395-42ea-a528-c6042ca1c6d0',
      connectionStatus: CONNECTION_STATUSES.statusConnectionAccepted,
      contactId: '810eddfb-5395-42ea-a528-c6042ca1c6d0'
    }
  },
  // Credential: REVOKED
  // Contact: STATUS_CONNECTION_ACCEPTED
  {
    gender: 'male',
    issuer: '!!J5',
    fullname: 'Ffion Bray',
    cardNumber: '129',
    contactName: 'Ffion Bray',
    dateOfBirth: '02/02/03',
    placeOfBirth: 'argentina',
    expirationDate: '21/02/39',
    personalNumber: '129',
    countryOfCitizenship: 'argentina',
    batchid: '',
    credentialId: 'c79d67e8-bfed-4eb7-9d52-ba9940222cad',
    publicationstoredat: 0,
    issuanceoperationhash: '',
    batchinclusionproof: '',
    sharedat: 0,
    status: CREDENTIAL_STATUSES.credentialRevoked,
    contactData: {
      externalId: 'da1811b94bb1456dad3f32b1478fea47',
      contactName: 'Ffion Bray',
      firstName: 'Ffion',
      midNames: '',
      lastName: 'Bray',
      key: '2e9524ce-1764-457b-96c8-3a0a011b3e52',
      connectionStatus: CONNECTION_STATUSES.statusConnectionAccepted,
      contactId: '2e9524ce-1764-457b-96c8-3a0a011b3e52'
    }
  },
  // Credential: DRAFT
  // Contact: STATUS_INVITATION_MISSING
  {
    gender: 'male',
    issuer: '!!J5',
    fullname: 'Soraya Love',
    cardNumber: '131',
    contactName: 'Soraya Love',
    dateOfBirth: '02/02/03',
    placeOfBirth: 'argentina',
    expirationDate: '21/02/39',
    personalNumber: '131',
    countryOfCitizenship: 'argentina',
    batchid: '',
    credentialId: '09df98d4-a2da-4026-a589-8ea89bd671a8',
    publicationstoredat: 0,
    issuanceoperationhash: '',
    batchinclusionproof: '',
    sharedat: 0,
    status: CREDENTIAL_STATUSES.credentialDraft,
    contactData: {
      externalId: '62a300409b2645c597bb143e968c1f17',
      contactName: 'Soraya Love',
      firstName: 'Soraya',
      midNames: '',
      lastName: 'Love',
      key: 'c13ad41f-2a20-41b7-bbc0-a07693f64b85',
      connectionStatus: CONNECTION_STATUSES.statusInvitationMissing,
      contactId: 'c13ad41f-2a20-41b7-bbc0-a07693f64b85'
    }
  },
  // Credential: DRAFT
  // Contact: STATUS_CONNECTION_ACCEPTED
  {
    gender: 'male',
    issuer: '!!J5',
    fullname: 'Pixie Mann',
    cardNumber: '132',
    contactName: 'Pixie Mann',
    dateOfBirth: '02/02/03',
    placeOfBirth: 'argentina',
    expirationDate: '21/02/39',
    personalNumber: '132',
    countryOfCitizenship: 'argentina',
    batchid: '',
    credentialId: '71b94eac-ea35-486e-a7ca-eba39428fb53',
    publicationstoredat: 0,
    issuanceoperationhash: '',
    batchinclusionproof: '',
    sharedat: 0,
    status: CREDENTIAL_STATUSES.credentialDraft,
    contactData: {
      externalId: '7a7a9160cd3b40b182399b65c0f086fe',
      contactName: 'Pixie Mann',
      firstName: 'Pixie',
      midNames: '',
      lastName: 'Mann',
      key: 'c4215efe-e8c8-4002-8ca8-49379f0fae4c',
      connectionStatus: CONNECTION_STATUSES.statusConnectionAccepted,
      contactId: 'c4215efe-e8c8-4002-8ca8-49379f0fae4c'
    }
  }
];
