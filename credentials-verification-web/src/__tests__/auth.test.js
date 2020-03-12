import { Legacy } from '../APIs/auth';
import { config } from '../APIs/config';
import { ISSUER, VERIFIER } from '../helpers/constants';

it('Legacy method test getMetadata', () => {
  window.localStorage.setItem('userRole', ISSUER);
  const auth = new Legacy(config);
  expect(auth.getMetadata()).toMatchObject({ userId: config.issuerId });
  window.localStorage.setItem('userRole', VERIFIER);
  expect(auth.getMetadata()).toMatchObject({ userId: config.verifierId });
});
