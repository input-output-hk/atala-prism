const {
  REACT_APP_VERIFIER,
  REACT_APP_ISSUER,
  REACT_APP_GRPC_CLIENT,
  REACT_APP_WALLET_GRPC_CLIENT
} = window._env_;

export const config = {
  issuerId: window.localStorage.getItem('issuerId') || REACT_APP_ISSUER,
  verifierId: window.localStorage.getItem('verifierId') || REACT_APP_VERIFIER,
  grpcClient: window.localStorage.getItem('backendUrl') || REACT_APP_GRPC_CLIENT,
  walletGrpcClient: window.localStorage.getItem('walletUrl') || REACT_APP_WALLET_GRPC_CLIENT
};
