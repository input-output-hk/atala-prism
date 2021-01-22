function DIDBased(configs, wallet) {
  const getMetadata = async (unsignedRequest, timeout) => {
    const { encodedNonce, encodedSignature, did, didKeyId } = await wallet.signMessage(
      unsignedRequest,
      timeout
    );

    return {
      requestNonce: encodedNonce,
      didSignature: encodedSignature,
      did,
      didKeyId
    };
  };

  return { getMetadata };
}

export { DIDBased };
