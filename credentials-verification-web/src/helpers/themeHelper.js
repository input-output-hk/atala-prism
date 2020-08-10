import { ISSUER } from './constants';

const isIssuer = role => role === ISSUER;

export const getThemeByRole = role => ({
  class: () => (isIssuer(role) ? 'IssuerUser' : 'VerifierUser'),
  title: () => (isIssuer(role) ? 'theme.title.issuer' : 'theme.title.verifier')
});
