import URLSafeBase64 from 'urlsafe-base64';
import { ISSUER } from '../helpers/constants';

function Legacy(configs) {
  const getMetadata = () => {
    const isIssuer = configs.userRole.get() === ISSUER;
    return { userId: configs.userId(isIssuer) };
  };

  return { getMetadata };
}

function DIDBased(configs, wallet) {
  const bytesToURLSafeBase64 = buff => URLSafeBase64.encode(Buffer.from(buff));

  const getMetadata = async unsignedRequest => {
    const nonce = wallet.getNonce();
    const signedRequest = await wallet.signMessage(unsignedRequest, nonce);
    const signature = signedRequest.getSignature();
    const did = signedRequest.getDid();
    const didKeyId = signedRequest.getDidkeyid();
    return {
      requestNonce: bytesToURLSafeBase64(nonce),
      did,
      didKeyId,
      didSignature: bytesToURLSafeBase64(signature)
    };
  };

  return { getMetadata };
}

export { Legacy, DIDBased };
