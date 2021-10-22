import { REVOKE_CREDENTIALS, SEND_CREDENTIALS, SIGN_CREDENTIALS } from '../helpers/constants';
import { credentialRequiredStatus, getTargetCredentials } from '../helpers/credentialActions';
import {
  DRAFT_CONNECTED_COUNT,
  mockCredentials,
  SENT_CONNECTED_COUNT,
  SIGNED_CONNECTED_COUNT
} from './__mocks__/mockIssuedCredentials';

const selectedCredentials = mockCredentials.map(c => c.credentialId);

describe('credential actions are applied to the valid credentials', () => {
  test('it only revokes credentials on signed or sent status', () => {
    const { targetCredentials } = getTargetCredentials(
      mockCredentials,
      selectedCredentials,
      credentialRequiredStatus[REVOKE_CREDENTIALS]
    );
    // eslint-disable-next-line no-magic-numbers
    expect(targetCredentials).toHaveLength(SIGNED_CONNECTED_COUNT + SENT_CONNECTED_COUNT);
  });

  test('it only signs credentials on draft status and with contacts connected', () => {
    const { targetCredentials } = getTargetCredentials(
      mockCredentials,
      selectedCredentials,
      credentialRequiredStatus[SIGN_CREDENTIALS]
    );
    // eslint-disable-next-line no-magic-numbers
    expect(targetCredentials).toHaveLength(DRAFT_CONNECTED_COUNT);
  });

  test('it only sends signed credentials and with contacts connected', () => {
    const { targetCredentials } = getTargetCredentials(
      mockCredentials,
      selectedCredentials,
      credentialRequiredStatus[SEND_CREDENTIALS]
    );
    // eslint-disable-next-line no-magic-numbers
    expect(targetCredentials).toHaveLength(SIGNED_CONNECTED_COUNT);
  });
});
