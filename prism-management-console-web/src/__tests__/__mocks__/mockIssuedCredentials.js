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
    batchId: '123',
    batchInclusionProof: 'abc',
    contactData: {
      credentialId: '0010',
      issuerId: '123',
      issuerName: 'issuer name',
      connectionStatus: CONNECTION_STATUSES.statusInvitationMissing
    },
    contactId: '123',
    credentialData: {
      html: 'abc',
      award: 'abc',
      fullName: 'abc',
      contactId: '123',
      startDate: '25/01/2022'
    },
    credentialId: '0010',
    credentialString: 'abc',
    encodedSignedCredential: 'abc',
    externalId: '123',
    issuanceOperationHash: 'abc',
    issuerId: '123',
    issuerName: 'issuer name',
    proof: {
      hash: '123',
      index: 0,
      siblings: []
    },
    publicationStoredAt: { seconds: 0, nanos: 0 },
    revokedOnOperationId: '',
    sharedAt: { seconds: 0, nanos: 0 },
    status: CREDENTIAL_STATUSES.credentialDraft
  },
  // Credential: DRAFT
  // Contact: STATUS_CONNECTION_ACCEPTED
  {
    batchId: '123',
    batchInclusionProof: 'abc',
    contactData: {
      credentialId: '0001',
      issuerId: '123',
      issuerName: 'issuer name',
      connectionStatus: CONNECTION_STATUSES.statusConnectionAccepted
    },
    contactId: '123',
    credentialData: {
      html: 'abc',
      award: 'abc',
      fullName: 'abc',
      contactId: '123',
      startDate: '25/01/2022'
    },
    credentialId: '0001',
    credentialString: 'abc',
    encodedSignedCredential: 'abc',
    externalId: '123',
    issuanceOperationHash: 'abc',
    issuerId: '123',
    issuerName: 'issuer name',
    proof: {
      hash: '123',
      index: 0,
      siblings: []
    },
    publicationStoredAt: { seconds: 0, nanos: 0 },
    revokedOnOperationId: '',
    sharedAt: { seconds: 0, nanos: 0 },
    status: CREDENTIAL_STATUSES.credentialDraft
  },
  // Credential: SIGNED
  // Contact: STATUS_CONNECTION_ACCEPTED
  {
    batchId: '123',
    batchInclusionProof: 'abc',
    contactData: {
      credentialId: '0002',
      issuerId: '123',
      issuerName: 'issuer name',
      connectionStatus: CONNECTION_STATUSES.statusConnectionAccepted
    },
    contactId: '123',
    credentialData: {
      html: 'abc',
      award: 'abc',
      fullName: 'abc',
      contactId: '123',
      startDate: '25/01/2022'
    },
    credentialId: '0002',
    credentialString: 'abc',
    encodedSignedCredential: 'abc',
    externalId: '123',
    issuanceOperationHash: 'abc',
    issuerId: '123',
    issuerName: 'issuer name',
    proof: {
      hash: '123',
      index: 0,
      siblings: []
    },
    publicationStoredAt: { seconds: 0, nanos: 0 },
    revokedOnOperationId: '',
    sharedAt: { seconds: 0, nanos: 0 },
    status: CREDENTIAL_STATUSES.credentialSigned
  },
  // Credential: SIGNED
  // Contact: STATUS_CONNECTION_ACCEPTED
  {
    batchId: '123',
    batchInclusionProof: 'abc',
    contactData: {
      credentialId: '0003',
      issuerId: '123',
      issuerName: 'issuer name',
      connectionStatus: CONNECTION_STATUSES.statusConnectionAccepted
    },
    contactId: '123',
    credentialData: {
      html: 'abc',
      award: 'abc',
      fullName: 'abc',
      contactId: '123',
      startDate: '25/01/2022'
    },
    credentialId: '0003',
    credentialString: 'abc',
    encodedSignedCredential: 'abc',
    externalId: '123',
    issuanceOperationHash: 'abc',
    issuerId: '123',
    issuerName: 'issuer name',
    proof: {
      hash: '123',
      index: 0,
      siblings: []
    },
    publicationStoredAt: { seconds: 0, nanos: 0 },
    revokedOnOperationId: '',
    sharedAt: { seconds: 0, nanos: 0 },
    status: CREDENTIAL_STATUSES.credentialSigned
  },
  // Credential: DRAFT
  // Contact: STATUS_CONNECTION_ACCEPTED
  {
    batchId: '123',
    batchInclusionProof: 'abc',
    contactData: {
      credentialId: '0004',
      issuerId: '123',
      issuerName: 'issuer name',
      connectionStatus: CONNECTION_STATUSES.statusConnectionAccepted
    },
    contactId: '123',
    credentialData: {
      html: 'abc',
      award: 'abc',
      fullName: 'abc',
      contactId: '123',
      startDate: '25/01/2022'
    },
    credentialId: '0004',
    credentialString: 'abc',
    encodedSignedCredential: 'abc',
    externalId: '123',
    issuanceOperationHash: 'abc',
    issuerId: '123',
    issuerName: 'issuer name',
    proof: {
      hash: '123',
      index: 0,
      siblings: []
    },
    publicationStoredAt: { seconds: 0, nanos: 0 },
    revokedOnOperationId: '',
    sharedAt: { seconds: 0, nanos: 0 },
    status: CREDENTIAL_STATUSES.credentialDraft
  },
  // Credential: SENT
  // Contact: STATUS_CONNECTION_ACCEPTED
  {
    batchId: '123',
    batchInclusionProof: 'abc',
    contactData: {
      credentialId: '0005',
      issuerId: '123',
      issuerName: 'issuer name',
      connectionStatus: CONNECTION_STATUSES.statusConnectionAccepted
    },
    contactId: '123',
    credentialData: {
      html: 'abc',
      award: 'abc',
      fullName: 'abc',
      contactId: '123',
      startDate: '25/01/2022'
    },
    credentialId: '0005',
    credentialString: 'abc',
    encodedSignedCredential: 'abc',
    externalId: '123',
    issuanceOperationHash: 'abc',
    issuerId: '123',
    issuerName: 'issuer name',
    proof: {
      hash: '123',
      index: 0,
      siblings: []
    },
    publicationStoredAt: { seconds: 0, nanos: 0 },
    revokedOnOperationId: '',
    sharedAt: { seconds: 0, nanos: 0 },
    status: CREDENTIAL_STATUSES.credentialSent
  },
  // Credential: REVOKED
  // Contact: STATUS_CONNECTION_ACCEPTED
  {
    batchId: '123',
    batchInclusionProof: 'abc',
    contactData: {
      credentialId: '0006',
      issuerId: '123',
      issuerName: 'issuer name',
      connectionStatus: CONNECTION_STATUSES.statusConnectionAccepted
    },
    contactId: '123',
    credentialData: {
      html: 'abc',
      award: 'abc',
      fullName: 'abc',
      contactId: '123',
      startDate: '25/01/2022'
    },
    credentialId: '0006',
    credentialString: 'abc',
    encodedSignedCredential: 'abc',
    externalId: '123',
    issuanceOperationHash: 'abc',
    issuerId: '123',
    issuerName: 'issuer name',
    proof: {
      hash: '123',
      index: 0,
      siblings: []
    },
    publicationStoredAt: { seconds: 0, nanos: 0 },
    revokedOnOperationId: '',
    sharedAt: { seconds: 0, nanos: 0 },
    status: CREDENTIAL_STATUSES.credentialRevoked
  },
  // Credential: REVOKED
  // Contact: STATUS_CONNECTION_ACCEPTED
  {
    batchId: '123',
    batchInclusionProof: 'abc',
    contactData: {
      credentialId: '0007',
      issuerId: '123',
      issuerName: 'issuer name',
      connectionStatus: CONNECTION_STATUSES.statusConnectionAccepted
    },
    contactId: '123',
    credentialData: {
      html: 'abc',
      award: 'abc',
      fullName: 'abc',
      contactId: '123',
      startDate: '25/01/2022'
    },
    credentialId: '0007',
    credentialString: 'abc',
    encodedSignedCredential: 'abc',
    externalId: '123',
    issuanceOperationHash: 'abc',
    issuerId: '123',
    issuerName: 'issuer name',
    proof: {
      hash: '123',
      index: 0,
      siblings: []
    },
    publicationStoredAt: { seconds: 0, nanos: 0 },
    revokedOnOperationId: '',
    sharedAt: { seconds: 0, nanos: 0 },
    status: CREDENTIAL_STATUSES.credentialRevoked
  },
  // Credential: DRAFT
  // Contact: STATUS_INVITATION_MISSING
  {
    batchId: '123',
    batchInclusionProof: 'abc',
    contactData: {
      credentialId: '0008',
      issuerId: '123',
      issuerName: 'issuer name',
      connectionStatus: CONNECTION_STATUSES.statusInvitationMissing
    },
    contactId: '123',
    credentialData: {
      html: 'abc',
      award: 'abc',
      fullName: 'abc',
      contactId: '123',
      startDate: '25/01/2022'
    },
    credentialId: '0008',
    credentialString: 'abc',
    encodedSignedCredential: 'abc',
    externalId: '123',
    issuanceOperationHash: 'abc',
    issuerId: '123',
    issuerName: 'issuer name',
    proof: {
      hash: '123',
      index: 0,
      siblings: []
    },
    publicationStoredAt: { seconds: 0, nanos: 0 },
    revokedOnOperationId: '',
    sharedAt: { seconds: 0, nanos: 0 },
    status: CREDENTIAL_STATUSES.credentialDraft
  },
  // Credential: DRAFT
  // Contact: STATUS_CONNECTION_ACCEPTED
  {
    batchId: '123',
    batchInclusionProof: 'abc',
    contactData: {
      credentialId: '0009',
      issuerId: '123',
      issuerName: 'issuer name',
      connectionStatus: CONNECTION_STATUSES.statusConnectionAccepted
    },
    contactId: '123',
    credentialData: {
      html: 'abc',
      award: 'abc',
      fullName: 'abc',
      contactId: '123',
      startDate: '25/01/2022'
    },
    credentialId: '0009',
    credentialString: 'abc',
    encodedSignedCredential: 'abc',
    externalId: '123',
    issuanceOperationHash: 'abc',
    issuerId: '123',
    issuerName: 'issuer name',
    proof: {
      hash: '123',
      index: 0,
      siblings: []
    },
    publicationStoredAt: { seconds: 0, nanos: 0 },
    revokedOnOperationId: '',
    sharedAt: { seconds: 0, nanos: 0 },
    status: CREDENTIAL_STATUSES.credentialDraft
  }
];
