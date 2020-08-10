const { REACT_APP_GRPC_CLIENT } = window._env_;

export const config = {
  grpcClient: getFromLocalStorage('backendUrl') || REACT_APP_GRPC_CLIENT
};

function getFromLocalStorage(key) {
  return window.localStorage.getItem(key);
}
