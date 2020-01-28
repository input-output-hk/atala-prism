import { ISSUER } from './constants';
import { config } from './config';

const isIssuer = config.userRole === ISSUER;

export const theme = {
  class: isIssuer ? 'IssuerUser' : 'VerifierUser',
  title: isIssuer ? 'theme.title.issuer' : 'theme.title.verifier'
};
