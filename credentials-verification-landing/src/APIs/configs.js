const {
  REACT_APP_ISSUER_ID,
  REACT_APP_GRPC_CLIENT,
  REACT_APP_ISSUER_NAME,
  REACT_APP_ISSUER_DID
} = window._env_;

export const config = {
  issuerId: window.localStorage.getItem('issuerId') || REACT_APP_ISSUER_ID,
  issuerName: window.localStorage.getItem('issuerName') || REACT_APP_ISSUER_NAME,
  issuerDid: window.localStorage.getItem('issuerDid') || REACT_APP_ISSUER_DID,
  grpcClient: window.localStorage.getItem('backendUrl') || REACT_APP_GRPC_CLIENT
};
