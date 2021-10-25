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
  test('validation of required status for credential actions works', () => {
    const unsignableCredential = mockCredentials[UNSIGNABLE_CREDENTIAL_INDEX];
    const signableCredential = mockCredentials[SIGNABLE_CREDENTIAL_INDEX];

    const unsendableCredential = mockCredentials[UNSENDABLE_CREDENTIAL_INDEX];
    const sendableCredential = mockCredentials[SENDABLE_CREDENTIAL_INDEX];

    const unrevocableCredential = mockCredentials[UNREVOCABLE_CREDENTIAL_INDEX];
    const revocableCredential = mockCredentials[REVOCABLE_CREDENTIAL_INDEX];

    expect(
      hasRequiredStatus(unsignableCredential, credentialRequiredStatus[SIGN_CREDENTIALS])
    ).toBeFalsy();
    expect(
      hasRequiredStatus(signableCredential, credentialRequiredStatus[SIGN_CREDENTIALS])
    ).toBeTruthy();

    expect(
      hasRequiredStatus(unsendableCredential, credentialRequiredStatus[SEND_CREDENTIALS])
    ).toBeFalsy();
    expect(
      hasRequiredStatus(sendableCredential, credentialRequiredStatus[SEND_CREDENTIALS])
    ).toBeTruthy();

    expect(
      hasRequiredStatus(unrevocableCredential, credentialRequiredStatus[REVOKE_CREDENTIALS])
    ).toBeFalsy();
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
