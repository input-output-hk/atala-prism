const {
  REACT_APP_ISSUER_ID,
  REACT_APP_GRPC_CLIENT,
  REACT_APP_ISSUER_NAME,
  REACT_APP_ISSUER_DID,
  REACT_APP_MAILCHIMP_URL,
  REACT_APP_MAILCHIMP_U,
  REACT_APP_MAILCHIMP_ID
} = window._env_;

export const config = {
  issuerId: window.localStorage.getItem('issuerId') || REACT_APP_ISSUER_ID,
  issuerName: window.localStorage.getItem('issuerName') || REACT_APP_ISSUER_NAME,
  issuerDid: window.localStorage.getItem('issuerDid') || REACT_APP_ISSUER_DID,
  grpcClient: window.localStorage.getItem('backendUrl') || REACT_APP_GRPC_CLIENT,
  mailchimpURL: window.localStorage.getItem('mailchimpURL') || REACT_APP_MAILCHIMP_URL,
  mailchimpU: window.localStorage.getItem('mailchimpU') || REACT_APP_MAILCHIMP_U,
  mailchimpID: window.localStorage.getItem('mailchimpID') || REACT_APP_MAILCHIMP_ID
};
