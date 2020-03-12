import { ISSUER } from '../helpers/constants';

function Legacy(configs) {
  const getMetadata = () => {
    const isIssuer = configs.userRole.get() === ISSUER;
    return { userId: configs.userId(isIssuer) };
  };

  return { getMetadata };
}

function DIDBased(configs, wallet) {
  const getMetadata = async unsignedRequest => {
    const nonce = wallet.getNonce();
    const did = wallet.getDid();
    const signedRequest = await wallet.signMessage(unsignedRequest, nonce);
    return {
      requestNonce: nonce,
      did,
      didKeyId: '', // TODO get keyId
      didSignature: signedRequest
    };
  };

  return { getMetadata };
}

export { Legacy, DIDBased };
