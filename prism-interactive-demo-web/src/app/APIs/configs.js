
const GATSBY_GRPC_CLIENT = process.env.GATSBY_GRPC_CLIENT
const GATSBY_ISSUER_ID = process.env.GATSBY_ISSUER_ID
const GATSBY_ISSUER_NAME = process.env.GATSBY_ISSUER_NAME
const GATSBY_ISSUER_DID = process.env.GATSBY_ISSUER_DID
const GATSBY_MAILCHIMP_URL = process.env.GATSBY_MAILCHIMP_URL
const GATSBY_MAILCHIMP_U = process.env.GATSBY_MAILCHIMP_U
const GATSBY_MAILCHIMP_ID = process.env.GATSBY_MAILCHIMP_ID
const GATSBY_FIREBASE_CONFIG = process.env.GATSBY_FIREBASE_CONFIG

export const config = {
  grpcClient: GATSBY_GRPC_CLIENT,
  issuerId: GATSBY_ISSUER_ID,
  issuerName: GATSBY_ISSUER_NAME,
  issuerDid:   GATSBY_ISSUER_DID,
  mailchimpURL:  GATSBY_MAILCHIMP_URL,
  mailchimpU:  GATSBY_MAILCHIMP_U,
  mailchimpID:  GATSBY_MAILCHIMP_ID,
  firebaseConfig: GATSBY_FIREBASE_CONFIG
};
