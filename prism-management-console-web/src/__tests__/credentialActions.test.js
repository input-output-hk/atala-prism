import { REVOKE_CREDENTIALS, SEND_CREDENTIALS, SIGN_CREDENTIALS } from '../helpers/constants';
import {
  credentialRequiredStatus,
  getTargetCredentials,
  hasRequiredStatus
} from '../helpers/credentialActions';
import {
  DRAFT_CONNECTED_COUNT,
  mockCredentials,
  REVOCABLE_CREDENTIAL_INDEX,
  SENDABLE_CREDENTIAL_INDEX,
  SENT_CONNECTED_COUNT,
  SIGNABLE_CREDENTIAL_INDEX,
  SIGNED_CONNECTED_COUNT,
  UNREVOCABLE_CREDENTIAL_INDEX,
  UNSENDABLE_CREDENTIAL_INDEX,
  UNSIGNABLE_CREDENTIAL_INDEX
} from './__mocks__/mockIssuedCredentials';

const selectedCredentials = mockCredentials.map(c => c.credentialId);

describe('credential actions are applied to the valid credentials', () => {
  test('attempting to sign an unsignable credential is rejected by the status validator', () => {
    const unsignableCredential = mockCredentials[UNSIGNABLE_CREDENTIAL_INDEX];

    expect(
      hasRequiredStatus(unsignableCredential, credentialRequiredStatus[SIGN_CREDENTIALS])
    ).toBeFalsy();
  });

  test('attempting to sign a signable credential is NOT rejected by the status validator', () => {
    const signableCredential = mockCredentials[SIGNABLE_CREDENTIAL_INDEX];

    expect(
      hasRequiredStatus(signableCredential, credentialRequiredStatus[SIGN_CREDENTIALS])
    ).toBeTruthy();
  });

  test('attempting to send an unsendable credential is rejected by the status validator', () => {
    const unsendableCredential = mockCredentials[UNSENDABLE_CREDENTIAL_INDEX];

    expect(
      hasRequiredStatus(unsendableCredential, credentialRequiredStatus[SEND_CREDENTIALS])
    ).toBeFalsy();
  });

  test('attempting to send a sendable credential is NOT rejected by the status validator', () => {
    const sendableCredential = mockCredentials[SENDABLE_CREDENTIAL_INDEX];

    expect(
      hasRequiredStatus(sendableCredential, credentialRequiredStatus[SEND_CREDENTIALS])
    ).toBeTruthy();
  });

  test('attempting to revoke an unrevocable credential is rejected by the status validator', () => {
    const unrevocableCredential = mockCredentials[UNREVOCABLE_CREDENTIAL_INDEX];

    expect(
      hasRequiredStatus(unrevocableCredential, credentialRequiredStatus[REVOKE_CREDENTIALS])
    ).toBeFalsy();
  });

  test('attempting to revoke a revocable credential is NOT rejected by the status validator', () => {
    const revocableCredential = mockCredentials[REVOCABLE_CREDENTIAL_INDEX];

    expect(
      hasRequiredStatus(revocableCredential, credentialRequiredStatus[REVOKE_CREDENTIALS])
    ).toBeTruthy();
  });

  test('it only revokes credentials on signed or sent status', () => {
    const { targetCredentials } = getTargetCredentials(
      mockCredentials,
      selectedCredentials,
      credentialRequiredStatus[REVOKE_CREDENTIALS]
    );
    expect(targetCredentials).toHaveLength(SIGNED_CONNECTED_COUNT + SENT_CONNECTED_COUNT);
  });

  test('it only signs credentials on draft status and with contacts connected', () => {
    const { targetCredentials } = getTargetCredentials(
      mockCredentials,
      selectedCredentials,
      credentialRequiredStatus[SIGN_CREDENTIALS]
    );
    expect(targetCredentials).toHaveLength(DRAFT_CONNECTED_COUNT);
  });

  test('it only sends signed credentials and with contacts connected', () => {
    const { targetCredentials } = getTargetCredentials(
      mockCredentials,
      selectedCredentials,
      credentialRequiredStatus[SEND_CREDENTIALS]
    );

    expect(targetCredentials).toHaveLength(SIGNED_CONNECTED_COUNT);
  });
});
