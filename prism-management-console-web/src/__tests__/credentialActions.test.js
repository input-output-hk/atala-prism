import { REVOKE_CREDENTIALS, SEND_CREDENTIALS, SIGN_CREDENTIALS } from '../helpers/constants';
import { credentialRequiredStatus, getTargetCredentials } from '../helpers/credentialActions';
import { mockCredentials } from './__mocks__/mockIssuedCredentials';

const selectedCredentials = mockCredentials.map(c => c.credentialid);

describe('credential actions are applied to the valid credentials', () => {
  test('it only revokes credentials on signed or sent status', () => {
    const { targetCredentials } = getTargetCredentials(
      mockCredentials,
      selectedCredentials,
      credentialRequiredStatus[REVOKE_CREDENTIALS]
    );
    // eslint-disable-next-line no-magic-numbers
    expect(targetCredentials).toHaveLength(4);
  });

  test('it only signs credentials on draft status', () => {
    const { targetCredentials } = getTargetCredentials(
      mockCredentials,
      selectedCredentials,
      credentialRequiredStatus[SIGN_CREDENTIALS]
    );
    // eslint-disable-next-line no-magic-numbers
    expect(targetCredentials).toHaveLength(3);
  });

  test('it only sends signed credentials and with contacts connected', () => {
    const { targetCredentials } = getTargetCredentials(
      mockCredentials,
      selectedCredentials,
      credentialRequiredStatus[SEND_CREDENTIALS]
    );
    // eslint-disable-next-line no-magic-numbers
    expect(targetCredentials).toHaveLength(1);
  });
});
